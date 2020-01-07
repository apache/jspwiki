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
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 *  Provides a simple directory based repository for Wiki pages.
 *  Pages are held in a directory structure:
 *  <PRE>
 *    Main.txt
 *    Foobar.txt
 *    OLD/
 *       Main/
 *          1.txt
 *          2.txt
 *          page.properties
 *       Foobar/
 *          page.properties
 *  </PRE>
 *
 *  In this case, "Main" has three versions, and "Foobar" just one version.
 *  <P>
 *  The properties file contains the necessary metainformation (such as author)
 *  information of the page.  DO NOT MESS WITH IT!
 *
 *  <P>
 *  All files have ".txt" appended to make life easier for those
 *  who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *
 */
public class VersioningFileProvider extends AbstractFileProvider {

    private static final Logger     log = Logger.getLogger(VersioningFileProvider.class);

    /** Name of the directory where the old versions are stored. */
    public static final String      PAGEDIR      = "OLD";

    /** Name of the property file which stores the metadata. */
    public static final String      PROPERTYFILE = "page.properties";

    private CachedProperties        m_cachedProperties;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        super.initialize( engine, properties );
        // some additional sanity checks :
        File oldpages = new File(getPageDirectory(), PAGEDIR);
        if (!oldpages.exists())
        {
            if (!oldpages.mkdirs())
            {
                throw new IOException("Failed to create page version directory " + oldpages.getAbsolutePath());
            }
        }
        else
        {
            if (!oldpages.isDirectory())
            {
                throw new IOException("Page version directory is not a directory: " + oldpages.getAbsolutePath());
            }
            if (!oldpages.canWrite())
            {
                throw new IOException("Page version directory is not writable: " + oldpages.getAbsolutePath());
            }
        }
        log.info("Using directory " + oldpages.getAbsolutePath() + " for storing old versions of pages");
    }

    /**
     *  Returns the directory where the old versions of the pages
     *  are being kept.
     */
    private File findOldPageDir( String page )
    {
        if( page == null )
        {
            throw new InternalWikiException("Page may NOT be null in the provider!");
        }

        File oldpages = new File( getPageDirectory(), PAGEDIR );

        return new File( oldpages, mangleName(page) );
    }

    /**
     *  Goes through the repository and decides which version is
     *  the newest one in that directory.
     *
     *  @return Latest version number in the repository, or -1, if
     *          there is no page in the repository.
     */

    // FIXME: This is relatively slow.
    /*
    private int findLatestVersion( String page )
    {
        File pageDir = findOldPageDir( page );

        String[] pages = pageDir.list( new WikiFileFilter() );

        if( pages == null )
        {
            return -1; // No such thing found.
        }

        int version = -1;

        for( int i = 0; i < pages.length; i++ )
        {
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
*/
    private int findLatestVersion( String page )
    {
        int version = -1;

        try
        {
            Properties props = getPageProperties( page );

            for( Iterator<Object> i = props.keySet().iterator(); i.hasNext(); )
            {
                String key = (String)i.next();

                if( key.endsWith(".author") )
                {
                    int cutpoint = key.indexOf('.');
                    if( cutpoint > 0 )
                    {
                        String pageNum = key.substring(0,cutpoint);

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
            }
        }
        catch( IOException e )
        {
            log.error("Unable to figure out latest version - dying...",e);
        }

        return version;
    }

    /**
     *  Reads page properties from the file system.
     */
    private Properties getPageProperties( final String page ) throws IOException {
        final File propertyFile = new File( findOldPageDir(page), PROPERTYFILE );
        if( propertyFile.exists() ) {
            final long lastModified = propertyFile.lastModified();

            //
            //   The profiler showed that when calling the history of a page the propertyfile
            //   was read just as much times as there were versions of that file. The loading
            //   of a propertyfile is a cpu-intensive job. So now hold on to the last propertyfile
            //   read because the next method will with a high probability ask for the same propertyfile.
            //   The time it took to show a historypage with 267 versions dropped with 300%.
            //

            CachedProperties cp = m_cachedProperties;

            if( cp != null && cp.m_page.equals( page ) && cp.m_lastModified == lastModified ) {
                return cp.m_props;
            }

            try( InputStream in = new BufferedInputStream(new FileInputStream( propertyFile ) ) ) {
                Properties props = new Properties();
                props.load( in );
                cp = new CachedProperties( page, props, lastModified );
                m_cachedProperties = cp; // Atomic

                return props;
            }
        }

        return new Properties(); // Returns an empty object
    }

    /**
     *  Writes the page properties back to the file system.
     *  Note that it WILL overwrite any previous properties.
     */
    private void putPageProperties( final String page, final Properties properties ) throws IOException {
        final File propertyFile = new File( findOldPageDir(page), PROPERTYFILE );
        try( final OutputStream out = new FileOutputStream( propertyFile ) ) {
            properties.store( out, " JSPWiki page properties for "+page+". DO NOT MODIFY!" );
        }

        // The profiler showed the probability was very high that when  calling for the history of
        // a page the propertyfile would be read as much times as there were versions of that file.
        // It is statistically likely the propertyfile will be examined many times before it is updated.
        final CachedProperties cp = new CachedProperties( page, properties, propertyFile.lastModified() );
        m_cachedProperties = cp; // Atomic
    }

    /**
     *  Figures out the real version number of the page and also checks for its existence.
     *
     *  @throws NoSuchVersionException if there is no such version.
     */
    private int realVersion( String page, int requestedVersion ) throws NoSuchVersionException {
        //
        //  Quickly check for the most common case.
        //
        if( requestedVersion == WikiProvider.LATEST_VERSION )
        {
            return -1;
        }

        int latest = findLatestVersion(page);

        if( requestedVersion == latest ||
            (requestedVersion == 1 && latest == -1 ) )
        {
            return -1;
        }
        else if( requestedVersion <= 0 || requestedVersion > latest )
        {
            throw new NoSuchVersionException("Requested version "+requestedVersion+", but latest is "+latest );
        }

        return requestedVersion;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public synchronized String getPageText( String page, int version )
        throws ProviderException
    {
        File dir = findOldPageDir( page );

        version = realVersion( page, version );
        if( version == -1 )
        {
            // We can let the FileSystemProvider take care
            // of these requests.
            return super.getPageText( page, WikiPageProvider.LATEST_VERSION );
        }

        File pageFile = new File( dir, ""+version+FILE_EXT );

        if( !pageFile.exists() )
            throw new NoSuchVersionException("Version "+version+"does not exist.");

        return readFile( pageFile );
    }


    // FIXME: Should this really be here?
    private String readFile( final File pagedata ) throws ProviderException {
        String result = null;
        if( pagedata.exists() ) {
            if( pagedata.canRead() ) {
                try( final InputStream in = new FileInputStream( pagedata ) ) {
                    result = FileUtil.readContents( in, m_encoding );
                } catch( IOException e ) {
                    log.error("Failed to read", e);
                    throw new ProviderException("I/O error: "+e.getMessage());
                }
            } else {
                log.warn("Failed to read page from '"+pagedata.getAbsolutePath()+"', possibly a permissions problem");
                throw new ProviderException("I cannot read the requested page.");
            }
        } else {
            // This is okay.
            // FIXME: is it?
            log.info("New page");
        }

        return result;
    }

    // FIXME: This method has no rollback whatsoever.

    /*
      This is how the page directory should look like:

         version    pagedir       olddir
          none       empty         empty
           1         Main.txt (1)  empty
           2         Main.txt (2)  1.txt
           3         Main.txt (3)  1.txt, 2.txt
    */
    /**
     *  {@inheritDoc}
     */
    @Override
    public synchronized void putPageText( final WikiPage page, final String text ) throws ProviderException {
        //
        //  This is a bit complicated.  We'll first need to
        //  copy the old file to be the newest file.
        //
        final int  latest  = findLatestVersion( page.getName() );
        final File pageDir = findOldPageDir( page.getName() );
        if( !pageDir.exists() ) {
            pageDir.mkdirs();
        }

        try {
            //
            // Copy old data to safety, if one exists.
            //
            final File oldFile = findPage( page.getName() );

            // Figure out which version should the old page be?
            // Numbers should always start at 1.
            // "most recent" = -1 ==> 1
            // "first"       = 1  ==> 2

            int versionNumber = (latest > 0) ? latest : 1;
            final boolean firstUpdate = (versionNumber == 1);

            if( oldFile != null && oldFile.exists() ) {
                final File pageFile = new File( pageDir, versionNumber + FILE_EXT );
                try( InputStream in = new BufferedInputStream( new FileInputStream( oldFile ) );
                     OutputStream out = new BufferedOutputStream( new FileOutputStream( pageFile ) ) ) {
                    FileUtil.copyContents( in, out );

                    //
                    // We need also to set the date, since we rely on this.
                    //
                    pageFile.setLastModified( oldFile.lastModified() );

                    //
                    // Kludge to make the property code to work properly.
                    //
                    versionNumber++;
                }
            }

            //
            //  Let superclass handler writing data to a new version.
            //
            super.putPageText( page, text );

            //
            //  Finally, write page version data.
            //
            // FIXME: No rollback available.
            Properties props = getPageProperties( page.getName() );

            String authorFirst = null;
            // if the following file exists, we are NOT migrating from FileSystemProvider
            File pagePropFile = new File(getPageDirectory() + File.separator + PAGEDIR + File.separator + mangleName(page.getName()) + File.separator + "page" + FileSystemProvider.PROP_EXT);
            if( firstUpdate && ! pagePropFile.exists() ) {
                // we might not yet have a versioned author because the
                // old page was last maintained by FileSystemProvider
                Properties props2 = getHeritagePageProperties( page.getName() );

                // remember the simulated original author (or something)
                // in the new properties
                authorFirst = props2.getProperty( "1.author", "unknown" );
                props.setProperty( "1.author", authorFirst );
            }

            String newAuthor = page.getAuthor();
            if ( newAuthor == null )
            {
                newAuthor = ( authorFirst != null ) ? authorFirst : "unknown";
            }
            page.setAuthor(newAuthor);
            props.setProperty( versionNumber + ".author", newAuthor );

            String changeNote = page.getAttribute(WikiPage.CHANGENOTE);
            if( changeNote != null ) {
                props.setProperty( versionNumber + ".changenote", changeNote );
            }

            // Get additional custom properties from page and add to props
            getCustomProperties( page, props );
            putPageProperties( page.getName(), props );
        } catch( final IOException e ) {
            log.error( "Saving failed", e );
            throw new ProviderException("Could not save page text: "+e.getMessage());
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    {
        int latest = findLatestVersion(page);
        int realVersion;

        WikiPage p = null;

        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latest ||
            (version == 1 && latest == -1) )
        {
            //
            // Yes, we need to talk to the top level directory
            // to get this version.
            //
            // I am listening to Press Play On Tape's guitar version of
            // the good old C64 "Wizardry" -tune at this moment.
            // Oh, the memories...
            //
            realVersion = (latest >= 0) ? latest : 1;

            p = super.getPageInfo( page, WikiPageProvider.LATEST_VERSION );

            if( p != null )
            {
                p.setVersion( realVersion );
            }
        }
        else
        {
            //
            //  The file is not the most recent, so we'll need to
            //  find it from the deep trenches of the "OLD" directory
            //  structure.
            //
            realVersion = version;
            File dir = findOldPageDir( page );

            if( !dir.exists() || !dir.isDirectory() )
            {
                return null;
            }

            File file = new File( dir, version+FILE_EXT );

            if( file.exists() )
            {
                p = new WikiPage( m_engine, page );

                p.setLastModified( new Date(file.lastModified()) );
                p.setVersion( version );
            }
        }

        //
        //  Get author and other metadata information
        //  (Modification date has already been set.)
        //
        if( p != null )
        {
            try
            {
                Properties props = getPageProperties( page );
                String author = props.getProperty( realVersion+".author" );
                if ( author == null )
                {
                    // we might not have a versioned author because the
                    // old page was last maintained by FileSystemProvider
                    Properties props2 = getHeritagePageProperties( page );
                    author = props2.getProperty( WikiPage.AUTHOR );
                }
                if ( author != null )
                {
                    p.setAuthor( author );
                }

                String changenote = props.getProperty( realVersion+".changenote" );
                if( changenote != null ) p.setAttribute( WikiPage.CHANGENOTE, changenote );

                // Set the props values to the page attributes
                setCustomProperties(p, props);
            }
            catch( IOException e )
            {
                log.error( "Cannot get author for page"+page+": ", e );
            }
        }

        return p;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean pageExists( String pageName, int version )
    {
        if (version == WikiPageProvider.LATEST_VERSION || version == findLatestVersion( pageName ) ) {
            return pageExists(pageName);
        }

        File dir = findOldPageDir( pageName );

        if( !dir.exists() || !dir.isDirectory() )
        {
            return false;
        }

        File file = new File( dir, version+FILE_EXT );

        return file.exists();

    }

    /**
     *  {@inheritDoc}
     */
     // FIXME: Does not get user information.
    @Override
    public List< WikiPage > getVersionHistory( String page ) throws ProviderException {
        ArrayList<WikiPage> list = new ArrayList<>();
        int latest = findLatestVersion( page );

        // list.add( getPageInfo(page,WikiPageProvider.LATEST_VERSION) );

        for( int i = latest; i > 0; i-- )
        {
            WikiPage info = getPageInfo( page, i );

            if( info != null )
            {
                list.add( info );
            }
        }

        return list;
    }

    /*
     * Support for migration of simple properties created by the
     * FileSystemProvider when coming under Versioning management.
     * Simulate an initial version.
     */
    private Properties getHeritagePageProperties( final String page ) throws IOException {
        final File propertyFile = new File( getPageDirectory(), mangleName( page ) + FileSystemProvider.PROP_EXT );
        if ( propertyFile.exists() ) {
            final long lastModified = propertyFile.lastModified();

            CachedProperties cp = m_cachedProperties;
            if ( cp != null && cp.m_page.equals(page) && cp.m_lastModified == lastModified ) {
                return cp.m_props;
            }

            try( final InputStream in = new BufferedInputStream( new FileInputStream( propertyFile ) ) ) {
                final Properties props = new Properties();
                props.load( in );

                final String originalAuthor = props.getProperty( WikiPage.AUTHOR );
                if ( originalAuthor.length() > 0 ) {
                    // simulate original author as if already versioned but put non-versioned property in special cache too
                    props.setProperty( "1.author", originalAuthor );

                    // The profiler showed the probability was very high that when calling for the history of a page the
                    // propertyfile would be read as much times as there were versions of that file. It is statistically
                    // likely the propertyfile will be examined many times before it is updated.
                    cp = new CachedProperties( page, props, propertyFile.lastModified() );
                    m_cachedProperties = cp; // Atomic
                }

                return props;
            }
        }

        return new Properties(); // Returns an empty object
    }

    /**
     *  Removes the relevant page directory under "OLD" -directory as well,
     *  but does not remove any extra subdirectories from it.  It will only
     *  touch those files that it thinks to be WikiPages.
     *
     *  @param page {@inheritDoc}
     *  @throws {@inheritDoc}
     */
    // FIXME: Should log errors.
    @Override
    public void deletePage( String page )
        throws ProviderException
    {
        super.deletePage( page );

        File dir = findOldPageDir( page );

        if( dir.exists() && dir.isDirectory() )
        {
            File[] files = dir.listFiles( new WikiFileFilter() );

            for( int i = 0; i < files.length; i++ )
            {
                files[i].delete();
            }

            File propfile = new File( dir, PROPERTYFILE );

            if( propfile.exists() )
            {
                propfile.delete();
            }

            dir.delete();
        }
    }

    /**
     *  {@inheritDoc}
     *
     *  Deleting versions has never really worked, JSPWiki assumes that version histories are "not gappy".
     *  Using deleteVersion() is definitely not recommended.
     */
    @Override
    public void deleteVersion( final String page, final int version ) throws ProviderException {
        final File dir = findOldPageDir( page );
        int latest = findLatestVersion( page );
        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latest ||
            (version == 1 && latest == -1) ) {
            //
            //  Delete the properties
            //
            try {
                final Properties props = getPageProperties( page );
                props.remove( ((latest > 0) ? latest : 1)+".author" );
                putPageProperties( page, props );
            } catch( final IOException e ) {
                log.error("Unable to modify page properties",e);
                throw new ProviderException("Could not modify page properties: " + e.getMessage());
            }

            // We can let the FileSystemProvider take care
            // of the actual deletion
            super.deleteVersion( page, WikiPageProvider.LATEST_VERSION );

            //
            //  Copy the old file to the new location
            //
            latest = findLatestVersion( page );

            final File pageDir = findOldPageDir( page );
            final File previousFile = new File( pageDir, latest + FILE_EXT );
            final File pageFile = findPage(page);
            try( final InputStream in = new BufferedInputStream( new FileInputStream( previousFile ) );
                 final OutputStream out = new BufferedOutputStream( new FileOutputStream( pageFile ) ) ) {
                if( previousFile.exists() ) {
                    FileUtil.copyContents( in, out );
                    //
                    // We need also to set the date, since we rely on this.
                    //
                    pageFile.setLastModified( previousFile.lastModified() );
                }
            } catch( final IOException e ) {
                log.fatal("Something wrong with the page directory - you may have just lost data!",e);
            }

            return;
        }

        final File pageFile = new File( dir, ""+version+FILE_EXT );

        if( pageFile.exists() ) {
            if( !pageFile.delete() ) {
                log.error("Unable to delete page." + pageFile.getPath() );
            }
        } else {
            throw new NoSuchVersionException("Page "+page+", version="+version);
        }
    }

    /**
     *  {@inheritDoc}
     */
    // FIXME: This is kinda slow, we should need to do this only once.
    @Override
    public Collection< WikiPage > getAllPages() throws ProviderException {
        final Collection< WikiPage > pages = super.getAllPages();
        final Collection< WikiPage > returnedPages = new ArrayList<>();
        for( final WikiPage page : pages ) {
            WikiPage info = getPageInfo( page.getName(), WikiProvider.LATEST_VERSION );
            returnedPages.add( info );
        }

        return returnedPages;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getProviderInfo()
    {
        return "";
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void movePage( final String from, final String to ) {
        // Move the file itself
        final File fromFile = findPage( from );
        final File toFile = findPage( to );
        fromFile.renameTo( toFile );

        // Move any old versions
        final File fromOldDir = findOldPageDir( from );
        final File toOldDir = findOldPageDir( to );
        fromOldDir.renameTo( toOldDir );
    }

    /*
     * The profiler showed that when calling the history of a page, the propertyfile was read just as many
     * times as there were versions of that file. The loading of a propertyfile is a cpu-intensive job.
     * This Class holds onto the last propertyfile read, because the probability is high that the next call
     * will with ask for the same propertyfile. The time it took to show a historypage with 267 versions dropped
     * by 300%. Although each propertyfile in a history could be cached, there is likely to be little performance
     * gain over simply keeping the last one requested.
     */
    private static class CachedProperties {
        String m_page;
        Properties m_props;
        long m_lastModified;

        /*
         * Because a Constructor is inherently synchronised, there is no need to synchronise the arguments.
         *
         * @param engine WikiEngine instance
         * @param props  Properties to use for initialization
         */
        public CachedProperties( final String pageName, final Properties props, final long lastModified ) {
            if ( pageName == null ) {
                throw new NullPointerException ( "pageName must not be null!" );
            }
            this.m_page = pageName;
            if ( props == null ) {
                throw new NullPointerException ( "properties must not be null!" );
            }
            m_props = props;
            this.m_lastModified = lastModified;
        }
    }
}
