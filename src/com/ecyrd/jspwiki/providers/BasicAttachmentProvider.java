/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import java.util.Collection;
import java.util.Properties;
import java.util.ArrayList;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;

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
 *  "attachment.properties" consists of the following items:
 *  <UL>
 *   <LI>1.author = author name for version 1 (etc)
 *  </UL>
 */
public class BasicAttachmentProvider
    implements WikiAttachmentProvider
{
    private String m_storageDir;
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";

    public static final String PROPERTY_FILE   = "attachment.properties";

    public static final String DIR_EXTENSION   = "-att";

    static final Category log = Category.getInstance( BasicAttachmentProvider.class );

    public void initialize( Properties properties ) 
        throws NoRequiredPropertyException,
               IOException
    {
        m_storageDir = WikiEngine.getRequiredProperty( properties, PROP_STORAGEDIR );
    }

    /**
     *  Finds storage dir, makes sure it exists.
     *
     *  @param wikipage Page to which this attachment is attached.
     */
    private File findPageDir( String wikipage )
        throws ProviderException
    {
        File f = new File( m_storageDir, wikipage+DIR_EXTENSION );

        if( !f.exists() )
        {
            f.mkdirs();
        }

        if( !f.isDirectory() )
        {
            throw new ProviderException("Storage dir '"+f.getAbsolutePath()+"' is not a directory!");
        }

        return f;
    }

    /**
     *  Finds the dir in which the attachment lives.
     */
    private File findAttachmentDir( Attachment att )
        throws ProviderException
    {
        File f = new File( findPageDir(att.getName()), att.getFileName() );

        return f;
    }

    /**
     *  Goes through the repository and decides which version is
     *  the newest one in that directory.
     *
     *  @return Latest version number in the repository, or 0, if
     *          there is no page in the repository.
     */
    private int findLatestVersion( Attachment att )
        throws ProviderException
    {
        // File pageDir = findPageDir( att.getName() );
        File attDir  = findAttachmentDir( att );

        log.debug("Finding pages in "+attDir.getAbsolutePath());
        String[] pages = attDir.list( new AttachmentVersionFilter() );

        if( pages == null )
        {
            return 0; // No such thing found.
        }

        int version = 0;

        for( int i = 0; i < pages.length; i++ )
        {
            log.debug("Checking: "+pages[i]);
            int cutpoint = pages[i].indexOf( '.' );
            if( cutpoint > 0 )
            {
                String pageNum = pages[i].substring( 0, cutpoint );

                try
                {
                    int res = Integer.parseInt( pageNum );

                    if( res > version )
                    {
                        version = res;
                    }
                }
                catch( NumberFormatException e ) {} // It's okay to skip these.
            }
        }

        return version;
    }

    /**
     *  Returns the file extension.  For example "test.png" returns "png".
     */
    protected static String getFileExtension( String filename )
    {
        String fileExt = "";

        int dot = filename.lastIndexOf('.');
        if( dot >= 0 )
        {
            fileExt = filename.substring( dot+1 );
        }

        return fileExt;
    }

    /**
     *  Writes the page properties back to the file system.
     *  Note that it WILL overwrite any previous properties.
     */
    private void putPageProperties( Attachment att, Properties properties )
        throws IOException,
               ProviderException
    {
        File attDir = findAttachmentDir( att );
        File propertyFile = new File( attDir, PROPERTY_FILE );

        OutputStream out = new FileOutputStream( propertyFile );

        properties.store( out, 
                          " JSPWiki page properties for "+
                          att.getName()+"/"+att.getFileName()+
                          ". DO NOT MODIFY!" );

        out.close();
    }

    /**
     *  Reads page properties from the file system.
     */
    private Properties getPageProperties( Attachment att )
        throws IOException,
               ProviderException
    {
        Properties props = new Properties();

        File propertyFile = new File( findAttachmentDir(att), PROPERTY_FILE );

        if( propertyFile != null && propertyFile.exists() )
        {
            InputStream in = new FileInputStream( propertyFile );

            props.load(in);

            in.close();
        }
        
        return props;
    }

    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException,
               IOException
    {
        OutputStream out = null;
        File attDir = findAttachmentDir( att );

        if(!attDir.exists())
        {
            attDir.mkdirs();
        }

        int latestVersion = findLatestVersion( att );

        // System.out.println("Latest version is "+latestVersion);

        try
        {
            int versionNumber = latestVersion+1;

            File newfile = new File( attDir, versionNumber+"."+
                                     getFileExtension(att.getFileName()) );

            log.info("Uploading attachment "+att.getFileName()+" to page "+att.getName());
            log.info("Saving attachment contents to "+newfile.getAbsolutePath());
            out = new FileOutputStream(newfile);

            FileUtil.copyContents( data, out );

            out.close();

            Properties props = getPageProperties( att );

            String author = att.getAuthor();

            if( author == null )
            {
                author = "unknown";
            }

            props.setProperty( versionNumber+".author", author );
            putPageProperties( att, props );
        }
        catch( IOException e )
        {
            log.error( "Could not save attachment data: ", e );
            throw (IOException) e.fillInStackTrace();
        }
        finally
        {
                if( out != null ) out.close();
        }
    }

    public String getProviderInfo()
    {
        return "";
    }

    private File findFile( File dir, Attachment att )
        throws FileNotFoundException,
               ProviderException
    {
        int version = att.getVersion();

        if( version == WikiProvider.LATEST_VERSION )
        {
            version = findLatestVersion( att );
        }

        File f = new File( dir, version+"."+getFileExtension(att.getFileName()) );

        if( !f.exists() )
        {
            throw new FileNotFoundException("No such file: "+f.getAbsolutePath()+" exists.");
        }

        return f;
    }

    public InputStream getAttachmentData( Attachment att )
        throws IOException,
               ProviderException
    {
        File attDir = findAttachmentDir( att );

        File f = findFile( attDir, att );

        return new FileInputStream( f );
    }

    public Collection listAttachments( WikiPage page )
        throws ProviderException
    {
        Collection result = new ArrayList();

        File dir = findPageDir( page.getName() );

        String[] attachments = dir.list();

        for( int i = 0; i < attachments.length; i++ )
        {
            File f = new File( dir, attachments[i] );

            if( f.isDirectory() )
            {
                Attachment att = getAttachmentInfo( page, attachments[i],
                                                    WikiProvider.LATEST_VERSION );

                result.add( att );
            }
        }

        return result;
    }

    public Collection findAttachments( QueryItem[] query )
    {
        return null;
    }

    public Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException
    {
        File dir = new File( findPageDir( page.getName() ), name );

        if( !dir.exists() )
        {
            log.debug("Attachment dir not found - thus no attachment can exist.");
            return null;
        }

        Attachment att = new Attachment( page.getName() );
        att.setFileName( name );
        
        if( version == WikiProvider.LATEST_VERSION )
        {
            version = findLatestVersion(att);
        }

        att.setVersion( version );
        

        // System.out.println("Fetching info on version "+version);
        try
        {
            Properties props = getPageProperties(att);

            att.setAuthor( props.getProperty( version+".author" ) );

            File f = findFile( dir, att );

            att.setSize( f.length() );
        }
        catch( IOException e )
        {
            log.error("Can't read page properties", e );
            throw new ProviderException("Cannot read page properties: "+e.getMessage());
        }
        // FIXME: Check for existence of this particular version.

        return att;
    }

    public Collection getVersionHistory( String wikiname )
    {
        return null;
    }

    /**
     *  Returns only those directories that contain attachments.
     */
    public class AttachmentFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return name.endsWith( DIR_EXTENSION );
        }
    }

    /**
     *  Accepts only files that are actual versions, no control files.
     */
    public class AttachmentVersionFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return !name.equals( PROPERTY_FILE );
        }
    }

}

