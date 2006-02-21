/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;

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
 *  @author Janne Jalkanen
 */
public class VersioningFileProvider
    extends AbstractFileProvider
    implements VersioningProvider
{
    private static final Logger     log = Logger.getLogger(VersioningFileProvider.class);
   
    public static final String      PAGEDIR      = "OLD";
    public static final String      PROPERTYFILE = "page.properties";

    private CachedProperties        m_cachedProperties;
    
    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        super.initialize( engine, properties );
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
        throws ProviderException
    {
        int version = -1;
        
        try
        {
            Properties props = getPageProperties( page );
            
            for( Iterator i = props.keySet().iterator(); i.hasNext(); )
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
    private Properties getPageProperties( String page )
        throws IOException
    {
        File propertyFile = new File( findOldPageDir(page), PROPERTYFILE );

        if( propertyFile != null && propertyFile.exists() )
        {
            long lastModified = propertyFile.lastModified();

            // The profiler showed that when calling the history of a page the propertyfile
            //   was read just as much times as there were versions of that file. The loading
            //   of a propertyfile is a cpu-intensive jobs. So now hold on to the last propertyfile
            //   read because the next method will with a high probability ask for the same propertyfile.
            //   The time it took to show a historypage with 267 versions dropped with 300%. 

            CachedProperties cp = m_cachedProperties;
            
            if( cp != null 
                && cp.m_page.equals(page) 
                && cp.m_lastModified == lastModified)
            {
                return cp.m_props;
            }
            
            InputStream in = new FileInputStream( propertyFile );

            Properties props = new Properties();

            props.load(in);

            in.close();
            
            cp = new CachedProperties();
            cp.m_page = page;
            cp.m_lastModified = lastModified;
            cp.m_props = props;
            
            m_cachedProperties = cp; // Atomic
            
            return props;
        }
        
        return new Properties(); // Returns an empty object
    }

    /**
     *  Writes the page properties back to the file system.
     *  Note that it WILL overwrite any previous properties.
     */
    private void putPageProperties( String page, Properties properties )
        throws IOException
    {
        File propertyFile = new File( findOldPageDir(page), PROPERTYFILE );

        OutputStream out = new FileOutputStream( propertyFile );

        properties.store( out, " JSPWiki page properties for "+page+". DO NOT MODIFY!" );

        out.close();
    }

    /**
     *  Figures out the real version number of the page and also checks
     *  for its existence.
     *
     *  @throws NoSuchVersionException if there is no such version.
     */
    private int realVersion( String page, int requestedVersion )
        throws NoSuchVersionException,
               ProviderException
    {
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
    private String readFile( File pagedata )
        throws ProviderException
    {
        String      result = null;
        InputStream in     = null;

        if( pagedata.exists() )
        {
            if( pagedata.canRead() )
            {
                try
                {          
                    in = new FileInputStream( pagedata );
                    result = FileUtil.readContents( in, m_encoding );
                }
                catch( IOException e )
                {
                    log.error("Failed to read", e);
                    throw new ProviderException("I/O error: "+e.getMessage());
                }
                finally
                {
                    try
                    {
                        if( in  != null ) in.close();
                    }
                    catch( Exception e ) 
                    {
                        log.fatal("Closing failed",e);
                    }
                }
            }
            else
            {
                log.warn("Failed to read page from '"+pagedata.getAbsolutePath()+"', possibly a permissions problem");
                throw new ProviderException("I cannot read the requested page.");
            }
        }
        else
        {
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
    public synchronized void putPageText( WikiPage page, String text )
        throws ProviderException
    {
        //
        //  This is a bit complicated.  We'll first need to
        //  copy the old file to be the newest file.
        //

        File pageDir = findOldPageDir( page.getName() );

        if( !pageDir.exists() )
        {
            pageDir.mkdirs();
        }

        int  latest  = findLatestVersion( page.getName() );

        try
        {
            //
            // Copy old data, if one exists.
            //

            File oldFile = findPage( page.getName() );

            // Figure out which version should the old page be?
            // Numbers should always start at 1.
            // "most recent" = -1 ==> 1
            // "first"       = 1  ==> 2

            int versionNumber = (latest > 0) ? latest : 1;

            if( oldFile != null && oldFile.exists() )
            {
                InputStream in = new BufferedInputStream( new FileInputStream( oldFile ) );
                File pageFile = new File( pageDir, Integer.toString( versionNumber )+FILE_EXT );
                OutputStream out = new BufferedOutputStream( new FileOutputStream( pageFile ) );

                FileUtil.copyContents( in, out );

                out.close();
                in.close();

                //
                // We need also to set the date, since we rely on this.
                //
                pageFile.setLastModified( oldFile.lastModified() );

                //
                // Kludge to make the property code to work properly.
                //
                versionNumber++;
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

            props.setProperty( versionNumber+".author", (page.getAuthor() != null) ? page.getAuthor() : "unknown" );

            putPageProperties( page.getName(), props );
        }
        catch( IOException e )
        {
            log.error( "Saving failed", e );
        }
    }

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

            if( file != null && file.exists() )
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
                if( author != null )
                {
                    p.setAuthor( author );
                }
            }
            catch( IOException e )
            {
                log.error( "Cannot get author for page"+page+": ", e );
            }
        }

        return p;
    }

    public boolean pageExists( String pageName, int version )
    {
        File dir = findOldPageDir( pageName );

        if( !dir.exists() || !dir.isDirectory() )
        {
            return false;
        }

        File file = new File( dir, version+FILE_EXT );

        if( file != null && file.exists() )
        {
            return true;
        }
        
        return false;
    }

    /**
     *  FIXME: Does not get user information.
     */
    public List getVersionHistory( String page )
    throws ProviderException
    {
        ArrayList list = new ArrayList();

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

    /**
     *  Removes the relevant page directory under "OLD" -directory as well,
     *  but does not remove any extra subdirectories from it.  It will only
     *  touch those files that it thinks to be WikiPages.
     */
    // FIXME: Should log errors.
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

    public void deleteVersion( String page, int version )
        throws ProviderException
    {
        File dir = findOldPageDir( page );

        int latest = findLatestVersion( page );

        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latest || 
            (version == 1 && latest == -1) )
        {
            //
            //  Delete the properties
            //
            try
            {
                Properties props = getPageProperties( page );
                props.remove( ((latest > 0) ? latest : 1)+".author" );
                putPageProperties( page, props );
            }
            catch( IOException e )
            {
                log.error("Unable to modify page properties",e);
                throw new ProviderException("Could not modify page properties");
            }

            // We can let the FileSystemProvider take care
            // of the actual deletion
            super.deleteVersion( page, WikiPageProvider.LATEST_VERSION );
            
            //
            //  Copy the old file to the new location
            //
            latest = findLatestVersion( page );
            
            File pageDir = findOldPageDir( page );
            File previousFile = new File( pageDir, Integer.toString(latest)+FILE_EXT );

            try
            {
                if( previousFile != null && previousFile.exists() )
                {
                    InputStream in = new BufferedInputStream( new FileInputStream( previousFile ) );
                    File pageFile = findPage(page);
                    OutputStream out = new BufferedOutputStream( new FileOutputStream( pageFile ) );

                    FileUtil.copyContents( in, out );

                    out.close();
                    in.close();

                    //
                    // We need also to set the date, since we rely on this.
                    //
                    pageFile.setLastModified( previousFile.lastModified() );
                }
            }
            catch( IOException e )
            {
                log.fatal("Something wrong with the page directory - you may have just lost data!",e);
            }
                        
            return;
        }

        File pageFile = new File( dir, ""+version+FILE_EXT );

        if( pageFile.exists() )
        {
            if( !pageFile.delete() )
            {
                log.error("Unable to delete page.");
            }
        }
        else
        {
            throw new NoSuchVersionException("Page "+page+", version="+version);
        }
    }

    // FIXME: This is kinda slow, we should need to do this only once.
    public Collection getAllPages() throws ProviderException
    {
        Collection pages = super.getAllPages();
        Collection returnedPages = new ArrayList();
        
        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage page = (WikiPage) i.next();
            
            WikiPage info = getPageInfo( page.getName(), WikiProvider.LATEST_VERSION );
 
            returnedPages.add( info );
        }
        
        return returnedPages;
    }
    
    public String getProviderInfo()
    {
        return "";
    }

    public void movePage( String from,
                          String to )
        throws ProviderException
    {
        // Move the file itself
        File fromFile = findPage( from );
        File toFile = findPage( to );
        
        fromFile.renameTo( toFile );
        
        // Move any old versions
        File fromOldDir = findOldPageDir( from );
        File toOldDir = findOldPageDir( to );
        
        fromOldDir.renameTo( toOldDir );
    }
    
    private static class CachedProperties
    {
        String m_page;
        Properties m_props;
        long m_lastModified;
    }
}
