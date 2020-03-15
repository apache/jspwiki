/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.providers;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Provides basic, versioning attachments.
 *
 *  <PRE>
 *   Structure is as follows:
 *      attachment_dir/
 *         ThisPage/
 *            attachment.doc/
 *               attachment.properties
 *               1.doc
 *               2.doc
 *               3.doc
 *            picture.png/
 *               attachment.properties
 *               1.png
 *               2.png
 *         ThatPage/
 *            picture.png/
 *               attachment.properties
 *               1.png
 *             
 *  </PRE>
 *
 *  The names of the directories will be URLencoded.
 *  <p>
 *  "attachment.properties" consists of the following items:
 *  <UL>
 *   <LI>1.author = author name for version 1 (etc)
 *  </UL>
 */
public class BasicAttachmentProvider implements WikiAttachmentProvider {

    private Engine m_engine;
    private String m_storageDir;
    
    /** The property name for where the attachments should be stored.  Value is <tt>{@value}</tt>. */
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";
    
    /*
     * Disable client cache for files with patterns
     * since 2.5.96
     */
    private Pattern m_disableCache = null;
    
    /** The property name for specifying which attachments are not cached.  Value is <tt>{@value}</tt>. */
    public static final String PROP_DISABLECACHE = "jspwiki.basicAttachmentProvider.disableCache";

    /** The name of the property file. */
    public static final String PROPERTY_FILE = "attachment.properties";

    /** The default extension for the page attachment directory name. */
    public static final String DIR_EXTENSION = "-att";
    
    /** The default extension for the attachment directory. */
    public static final String ATTDIR_EXTENSION = "-dir";
    
    private static final Logger log = Logger.getLogger( BasicAttachmentProvider.class );

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        m_engine = engine;
        m_storageDir = TextUtil.getCanonicalFilePathProperty( properties, PROP_STORAGEDIR,
                                                       System.getProperty("user.home") + File.separator + "jspwiki-files");

        final String patternString = engine.getWikiProperties().getProperty( PROP_DISABLECACHE );
        if ( patternString != null ) {
            m_disableCache = Pattern.compile(patternString);
        }

        //  Check if the directory exists - if it doesn't, create it.
        final File f = new File( m_storageDir );
        if( !f.exists() ) {
            f.mkdirs();
        }

        // Some sanity checks
        if( !f.exists() ) {
            throw new IOException( "Could not find or create attachment storage directory '" + m_storageDir + "'" );
        }

        if( !f.canWrite() ) {
            throw new IOException( "Cannot write to the attachment storage directory '" + m_storageDir + "'" );
        }

        if( !f.isDirectory() ) {
            throw new IOException( "Your attachment storage points to a file, not a directory: '" + m_storageDir + "'" );
        }
    }

    /**
     *  Finds storage dir, and if it exists, makes sure that it is valid.
     *
     *  @param wikipage Page to which this attachment is attached.
     */
    private File findPageDir( String wikipage ) throws ProviderException {
        wikipage = mangleName( wikipage );

        final File f = new File( m_storageDir, wikipage + DIR_EXTENSION );
        if( f.exists() && !f.isDirectory() ) {
            throw new ProviderException( "Storage dir '" + f.getAbsolutePath() + "' is not a directory!" );
        }

        return f;
    }

    private static String mangleName( final String wikiname ) {
        return TextUtil.urlEncodeUTF8( wikiname );
    }

    private static String unmangleName( final String filename )
    {
        return TextUtil.urlDecodeUTF8( filename );
    }
    
    /**
     *  Finds the dir in which the attachment lives.
     */
    private File findAttachmentDir( final Attachment att ) throws ProviderException {
        File f = new File( findPageDir( att.getParentName() ), mangleName( att.getFileName() + ATTDIR_EXTENSION ) );

        //  Migration code for earlier versions of JSPWiki. Originally, we used plain filename.  Then we realized we need
        //  to urlencode it.  Then we realized that we have to use a postfix to make sure illegal file names are never formed.
        if( !f.exists() ) {
            File oldf = new File( findPageDir( att.getParentName() ), mangleName( att.getFileName() ) );
            if( oldf.exists() ) {
                f = oldf;
            } else {
                oldf = new File( findPageDir( att.getParentName() ), att.getFileName() );
                if( oldf.exists() ) {
                    f = oldf;
                }
            }
        }

        return f;
    }

    /**
     * Goes through the repository and decides which version is the newest one in that directory.
     *
     * @return Latest version number in the repository, or 0, if there is no page in the repository.
     */
    private int findLatestVersion( final Attachment att ) throws ProviderException {
        final File attDir  = findAttachmentDir( att );
        final String[] pages = attDir.list( new AttachmentVersionFilter() );
        if( pages == null ) {
            return 0; // No such thing found.
        }

        int version = 0;
        for( final String page : pages ) {
            final int cutpoint = page.indexOf( '.' );
            final String pageNum = ( cutpoint > 0 ) ? page.substring( 0, cutpoint ) : page;

            try {
                final int res = Integer.parseInt( pageNum );

                if( res > version ) {
                    version = res;
                }
            } catch( final NumberFormatException e ) {
            } // It's okay to skip these.
        }

        return version;
    }

    /**
     *  Returns the file extension.  For example "test.png" returns "png".
     *  <p>
     *  If file has no extension, will return "bin"
     *  
     *  @param filename The file name to check
     *  @return The extension.  If no extension is found, returns "bin".
     */
    protected static String getFileExtension( final String filename ) {
        String fileExt = "bin";

        final int dot = filename.lastIndexOf('.');
        if( dot >= 0 && dot < filename.length()-1 ) {
            fileExt = mangleName( filename.substring( dot+1 ) );
        }

        return fileExt;
    }

    /**
     *  Writes the page properties back to the file system.
     *  Note that it WILL overwrite any previous properties.
     */
    private void putPageProperties( final Attachment att, final Properties properties ) throws IOException, ProviderException {
        final File attDir = findAttachmentDir( att );
        final File propertyFile = new File( attDir, PROPERTY_FILE );
        try( final OutputStream out = new FileOutputStream( propertyFile ) ) {
            properties.store( out, " JSPWiki page properties for " + att.getName() + ". DO NOT MODIFY!" );
        }
    }

    /**
     *  Reads page properties from the file system.
     */
    private Properties getPageProperties( final Attachment att ) throws IOException, ProviderException {
        final Properties props = new Properties();
        final File propertyFile = new File( findAttachmentDir(att), PROPERTY_FILE );
        if( propertyFile.exists() ) {
            try( final InputStream in = new FileInputStream( propertyFile ) ) {
                props.load( in );
            }
        }
        
        return props;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void putAttachmentData( final Attachment att, final InputStream data ) throws ProviderException, IOException {
        final File attDir = findAttachmentDir( att );

        if( !attDir.exists() ) {
            attDir.mkdirs();
        }
        final int latestVersion = findLatestVersion( att );
        final int versionNumber = latestVersion + 1;

        final File newfile = new File( attDir, versionNumber + "." + getFileExtension( att.getFileName() ) );
        try( final OutputStream out = new FileOutputStream( newfile ) ) {
            log.info( "Uploading attachment " + att.getFileName() + " to page " + att.getParentName() );
            log.info( "Saving attachment contents to " + newfile.getAbsolutePath() );
            FileUtil.copyContents( data, out );

            final Properties props = getPageProperties( att );

            String author = att.getAuthor();
            if( author == null ) {
                author = "unknown"; // FIXME: Should be localized, but cannot due to missing WikiContext
            }
            props.setProperty( versionNumber + ".author", author );

            final String changeNote = att.getAttribute( WikiPage.CHANGENOTE );
            if( changeNote != null ) {
                props.setProperty( versionNumber + ".changenote", changeNote );
            }
            
            putPageProperties( att, props );
        } catch( final IOException e ) {
            log.error( "Could not save attachment data: ", e );
            throw (IOException) e.fillInStackTrace();
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getProviderInfo() {
        return "";
    }

    private File findFile( final File dir, final Attachment att ) throws FileNotFoundException, ProviderException {
        int version = att.getVersion();
        if( version == WikiProvider.LATEST_VERSION ) {
            version = findLatestVersion( att );
        }

        final String ext = getFileExtension( att.getFileName() );
        File f = new File( dir, version + "." + ext );

        if( !f.exists() ) {
            if( "bin".equals( ext ) ) {
                final File fOld = new File( dir, version + "." );
                if( fOld.exists() ) {
                    f = fOld;
                }
            }
            if( !f.exists() ) {
                throw new FileNotFoundException( "No such file: " + f.getAbsolutePath() + " exists." );
            }
        }

        return f;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public InputStream getAttachmentData( final Attachment att ) throws IOException, ProviderException {
        final File attDir = findAttachmentDir( att );
        try {
            final File f = findFile( attDir, att );
            return new FileInputStream( f );
        } catch( final FileNotFoundException e ) {
            log.error( "File not found: " + e.getMessage() );
            throw new ProviderException( "No such page was found." );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public List< Attachment > listAttachments( final Page page ) throws ProviderException {
        final List< Attachment > result = new ArrayList<>();
        final File dir = findPageDir( page.getName() );
        final String[] attachments = dir.list();
        if( attachments != null ) {
            //  We now have a list of all potential attachments in the directory.
            for( final String attachment : attachments ) {
                final File f = new File( dir, attachment );
                if( f.isDirectory() ) {
                    String attachmentName = unmangleName( attachment );

                    //  Is it a new-stylea attachment directory?  If yes, we'll just deduce the name.  If not, however,
                    //  we'll check if there's a suitable property file in the directory.
                    if( attachmentName.endsWith( ATTDIR_EXTENSION ) ) {
                        attachmentName = attachmentName.substring( 0, attachmentName.length() - ATTDIR_EXTENSION.length() );
                    } else {
                        final File propFile = new File( f, PROPERTY_FILE );
                        if( !propFile.exists() ) {
                            //  This is not obviously a JSPWiki attachment, so let's just skip it.
                            continue;
                        }
                    }

                    final Attachment att = getAttachmentInfo( page, attachmentName, WikiProvider.LATEST_VERSION );
                    //  Sanity check - shouldn't really be happening, unless you mess with the repository directly.
                    if( att == null ) {
                        throw new ProviderException( "Attachment disappeared while reading information:"
                                + " if you did not touch the repository, there is a serious bug somewhere. " + "Attachment = " + attachment
                                + ", decoded = " + attachmentName );
                    }

                    result.add( att );
                }
            }
        }

        return result;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< Attachment > findAttachments( final QueryItem[] query ) {
        return new ArrayList<>();
    }

    /**
     *  {@inheritDoc}
     */
    // FIXME: Very unoptimized.
    @Override
    public List< Attachment > listAllChanged( final Date timestamp ) throws ProviderException {
        final File attDir = new File( m_storageDir );
        if( !attDir.exists() ) {
            throw new ProviderException( "Specified attachment directory " + m_storageDir + " does not exist!" );
        }

        final ArrayList< Attachment > list = new ArrayList<>();
        final String[] pagesWithAttachments = attDir.list( new AttachmentFilter() );

        for( final String pagesWithAttachment : pagesWithAttachments ) {
            String pageId = unmangleName( pagesWithAttachment );
            pageId = pageId.substring( 0, pageId.length() - DIR_EXTENSION.length() );

            final Collection< Attachment > c = listAttachments( new WikiPage( m_engine, pageId ) );
            for( final Attachment att : c ) {
                if( att.getLastModified().after( timestamp ) ) {
                    list.add( att );
                }
            }
        }

        list.sort( new PageTimeComparator() );

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Attachment getAttachmentInfo( final Page page, final String name, int version ) throws ProviderException {
        final Attachment att = new Attachment( m_engine, page.getName(), name );
        final File dir = findAttachmentDir( att );

        if( !dir.exists() ) {
            // log.debug("Attachment dir not found - thus no attachment can exist.");
            return null;
        }
        
        if( version == WikiProvider.LATEST_VERSION ) {
            version = findLatestVersion(att);
        }

        att.setVersion( version );
        
        // Should attachment be cachable by the client (browser)?
        if( m_disableCache != null ) {
            final Matcher matcher = m_disableCache.matcher( name );
            if( matcher.matches() ) {
                att.setCacheable( false );
            }
        }

        // System.out.println("Fetching info on version "+version);
        try {
            final Properties props = getPageProperties( att );
            att.setAuthor( props.getProperty( version+".author" ) );
            final String changeNote = props.getProperty( version+".changenote" );
            if( changeNote != null ) {
                att.setAttribute( WikiPage.CHANGENOTE, changeNote );
            }

            final File f = findFile( dir, att );
            att.setSize( f.length() );
            att.setLastModified( new Date( f.lastModified() ) );
        } catch( final FileNotFoundException e ) {
            log.error( "Can't get attachment properties for " + att, e );
            return null;
        } catch( final IOException e ) {
            log.error("Can't read page properties", e );
            throw new ProviderException("Cannot read page properties: "+e.getMessage());
        }
        // FIXME: Check for existence of this particular version.

        return att;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public List< Attachment > getVersionHistory( final Attachment att ) {
        final ArrayList< Attachment > list = new ArrayList<>();
        try {
            final int latest = findLatestVersion( att );
            for( int i = latest; i >= 1; i-- ) {
                final Attachment a = getAttachmentInfo( new WikiPage( m_engine, att.getParentName() ), att.getFileName(), i );

                if( a != null ) {
                    list.add( a );
                }
            }
        } catch( final ProviderException e ) {
            log.error( "Getting version history failed for page: " + att, e );
            // FIXME: SHould this fail?
        }

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deleteVersion( final Attachment att ) throws ProviderException {
        // FIXME: Does nothing yet.
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deleteAttachment( final Attachment att ) throws ProviderException {
        final File dir = findAttachmentDir( att );
        final String[] files = dir.list();
        for( final String s : files ) {
            final File file = new File( dir.getAbsolutePath() + "/" + s );
            file.delete();
        }
        dir.delete();
    }

    /**
     *  Returns only those directories that contain attachments.
     */
    public static class AttachmentFilter implements FilenameFilter {
        /**
         *  {@inheritDoc}
         */
        @Override
        public boolean accept( final File dir, final String name )
        {
            return name.endsWith( DIR_EXTENSION );
        }
    }

    /**
     *  Accepts only files that are actual versions, no control files.
     */
    public static class AttachmentVersionFilter implements FilenameFilter {
        /**
         *  {@inheritDoc}
         */
        @Override
        public boolean accept( final File dir, final String name )
        {
            return !name.equals( PROPERTY_FILE );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void moveAttachmentsForPage( final String oldParent, final String newParent ) throws ProviderException {
        final File srcDir = findPageDir( oldParent );
        final File destDir = findPageDir( newParent );

        log.debug( "Trying to move all attachments from " + srcDir + " to " + destDir );

        // If it exists, we're overwriting an old page (this has already been confirmed at a higher level), so delete any existing attachments.
        if( destDir.exists() ) {
            log.error( "Page rename failed because target dirctory " + destDir + " exists" );
        } else {
            // destDir.getParentFile().mkdir();
            srcDir.renameTo( destDir );
        }
    }

}

