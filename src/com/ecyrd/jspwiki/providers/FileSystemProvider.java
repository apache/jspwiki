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

import java.io.*;
import java.util.Properties;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;
import java.util.ArrayList;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.*;

/**
 *  Provides a simple directory based repository for Wiki pages.
 *  <P>
 *  All files have ".txt" appended to make life easier for those
 *  who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *
 *  @author Janne Jalkanen
 */
public class FileSystemProvider
    implements WikiPageProvider
{
    private static final Category   log = Category.getInstance(FileSystemProvider.class);
    private String m_pageDirectory = "/tmp/";

    protected String m_encoding;

    /**
     *  Name of the property that defines where page directories are.
     */
    public static final String      PROP_PAGEDIR = "jspwiki.fileSystemProvider.pageDir";

    /**
     *  All files should have this extension to be recognized as JSPWiki files.
     *  We default to .txt, because that is probably easiest for Windows users,
     *  and guarantees correct handling.
     */
    public static final String FILE_EXT = ".txt";

    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     *  @throw FileNotFoundException If the specified page directory does not exist.
     *  @throw IOException In case the specified page directory is a file, not a directory.
     */
    public void initialize( Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing FileSystemProvider");
        m_pageDirectory = WikiEngine.getRequiredProperty( properties, PROP_PAGEDIR );

        File f = new File(m_pageDirectory);

        if( !f.exists() )
        {
            throw new FileNotFoundException("Page directory does not exist: "+m_pageDirectory);
        }
        else if( !f.isDirectory() )
        {
            throw new IOException("Page directory is not a directory: "+m_pageDirectory);
        }

        m_encoding      = properties.getProperty( WikiEngine.PROP_ENCODING, 
                                                  DEFAULT_ENCODING );

        log.info("Wikipages are read from : "+m_pageDirectory);
    }

    String getPageDirectory()
    {
        return m_pageDirectory;
    }

    /**
     *  This makes sure that the queried page name
     *  is still readable by the file system.
     */
    protected String mangleName( String pagename )
    {
        // FIXME: Horrible kludge, very slow, etc.
        if( "UTF-8".equals( m_encoding ) )
            return TextUtil.urlEncodeUTF8( pagename );

        return java.net.URLEncoder.encode( pagename );
    }

    /**
     *  This makes the reverse of mangleName
     */
    protected String unmangleName( String filename )
    {
        // FIXME: Horrible kludge, very slow, etc.
        if( "UTF-8".equals( m_encoding ) )
            return TextUtil.urlDecodeUTF8( filename );

        return java.net.URLDecoder.decode( filename );
    }

    /**
     *  Finds a Wiki page from the page repository.
     */
    protected File findPage( String page )
    {
        return new File( m_pageDirectory, mangleName(page)+FILE_EXT );
    }

    
    public boolean pageExists( String page )
    {
        File pagefile = findPage( page );

        return pagefile.exists();        
    }

    /**
     *  This implementation just returns the current version, as filesystem
     *  does not provide versioning information for now.
     */
    public String getPageText( String page, int version )
        throws ProviderException
    {
        return getPageText( page );
    }

    /**
     *  Read the text directly from the correct file.
     */
    private String getPageText( String page )
    {
        String result  = null;
        InputStream in = null;

        File pagedata = findPage( page );

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
                log.warn("Failed to read page '"+page+"' from '"+pagedata.getAbsolutePath()+"', possibly a permissions problem");
            }
        }
        else
        {
            // This is okay.
            log.info("New page '"+page+"'");
        }

        return result;
    }

    public void putPageText( WikiPage page, String text )        
    {
        File file = findPage( page.getName() );

        try
        {
            PrintWriter out = new PrintWriter(new OutputStreamWriter( new FileOutputStream( file ),
                                                                      m_encoding ));

            out.print( text );

            out.close();
        }
        catch( IOException e )
        {
            log.error( "Saving failed" );
        }
    }

    public Collection getAllPages()
        throws ProviderException
    {
        log.debug("Getting all pages...");

        ArrayList set = new ArrayList();

        File wikipagedir = new File( m_pageDirectory );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        if( wikipages == null )
        {
            log.error("Wikipages directory does not exist!");
            throw new InternalWikiException("Page directory does not exist");
        }

        for( int i = 0; i < wikipages.length; i++ )
        {
            String wikiname = wikipages[i].getName();
            int cutpoint = wikiname.lastIndexOf( FILE_EXT );

            WikiPage page = getPageInfo( unmangleName(wikiname.substring(0,cutpoint)),
                                         WikiPageProvider.LATEST_VERSION );
            if( page == null )
            {
                // This should not really happen.
                // FIXME: Should we throw an exception here?
                log.error("Page "+wikiname+" was found in directory listing, but could not be located individually.");
                continue;
            }
            
            set.add( page );
        }

        return set;        
    }

    public Collection getAllChangedSince( Date date )
    {
        return new ArrayList(); // FIXME
    }

    public int getPageCount()
    {
        File wikipagedir = new File( m_pageDirectory );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        return wikipages.length;
    }

    public Collection findPages( QueryItem[] query )
    {
        File wikipagedir = new File( m_pageDirectory );
        TreeSet res = new TreeSet( new SearchResultComparator() );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

    nextfile:
        for( int i = 0; i < wikipages.length; i++ )
        {
            String line = null;

            // log.debug("Searching page "+wikipages[i].getPath() );

            String filename = wikipages[i].getName();
            int cutpoint    = filename.lastIndexOf( FILE_EXT );
            String wikiname = filename.substring(0,cutpoint);

            wikiname = unmangleName( wikiname );

            try
            {
                FileInputStream input = new FileInputStream( wikipages[i] );
                String pagetext       = FileUtil.readContents( input, m_encoding );

                int scores[] = new int[ query.length ];

                BufferedReader in = new BufferedReader( new StringReader(pagetext) );

                while( (line = in.readLine()) != null )
                {
                    line = line.toLowerCase();

                    for( int j = 0; j < query.length; j++ )
                    {
                        int index = -1;

                        while( (index = line.indexOf( query[j].word, index+1 )) != -1 )
                        {
                            // log.debug("   Match found for "+query[j].word );

                            if( query[j].type != QueryItem.FORBIDDEN )
                            {
                                scores[j]++; // Mark, found this word n times
                            }
                            else
                            {
                                // Found something that was forbidden.
                                continue nextfile;
                            }
                        }
                    }
                }

                //
                //  Check that we have all required words.
                //

                int totalscore = 0;

                for( int j = 0; j < scores.length; j++ )
                {
                    // Give five points for each occurrence
                    // of the word in the wiki name.

                    if( wikiname.toLowerCase().indexOf( query[j].word ) != -1 &&
                        query[j].type != QueryItem.FORBIDDEN )
                        scores[j] += 5;

                    //  Filter out pages if the search word is marked 'required'
                    //  but they have no score.

                    if( query[j].type == QueryItem.REQUIRED && scores[j] == 0 )
                        continue nextfile;

                    //
                    //  Count the total score for this page.
                    //
                    totalscore += scores[j];
                }

                if( totalscore > 0 )
                {
                    res.add( new SearchResultImpl(wikiname,totalscore) );
                }
            }
            catch( IOException e )
            {
                log.error( "Failed to read", e );
            }
        }

        return res;
    }

    /**
     *  Always returns the latest version, since FileSystemProvider
     *  does not support versioning.
     */
    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    {
        File file = findPage( page );

        if( !file.exists() )
        {
            return null;
        }

        WikiPage p = new WikiPage( page );
        p.setLastModified( new Date(file.lastModified()) );

        return p;
    }

    /**
     *  The FileSystemProvider provides only one version.
     */
    public Collection getVersionHistory( String page )
        throws ProviderException
    {
        ArrayList list = new ArrayList();

        list.add( getPageInfo( page, WikiPageProvider.LATEST_VERSION ) );

        return list;
    }

    public String getProviderInfo()
    {
        return "";
    }

    public class WikiFileFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return name.endsWith( FILE_EXT );
        }
    }

    /**
     *  Searches return this class.
     */
    public class SearchResultImpl
        implements SearchResult
    {
        int    m_score;
        String m_name;

        public SearchResultImpl( String name, int score )
        {
            m_name = name;
            m_score = score;
        }

        public String getName()
        {
            return m_name;
        }

        public int getScore()
        {
            return m_score;
        }
    }


}
