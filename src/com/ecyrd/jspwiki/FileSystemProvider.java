/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.io.*;
import java.util.Properties;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;
import java.util.ArrayList;
import org.apache.log4j.Category;

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


    public void initialize( Properties properties )
        throws NoRequiredPropertyException
    {
        log.debug("Initing FileSystemProvider");
        m_pageDirectory = WikiEngine.getRequiredProperty( properties, PROP_PAGEDIR );

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
        return java.net.URLEncoder.encode( pagename );
    }

    /**
     *  This makes the reverse of mangleName
     */
    protected String unmangleName( String filename )
    {
        return java.net.URLDecoder.decode( filename );
    }

    /**
     *  Finds a Wiki page from the page repository.
     */
    private File findPage( String page )
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
    {
        return getPageText( page );
    }

    public String getPageText( String page )
    {
        StringBuffer result = new StringBuffer();

        File pagedata = findPage( page );

        if( pagedata.exists() && pagedata.canRead() )
        {
            Reader in = null;
            StringWriter out = null;

            try
            {
                in = new BufferedReader(new FileReader(pagedata) );
                out = new StringWriter();

                int c;

                while( (c = in.read()) != -1  )
                {
                    out.write( c );
                }

                result.append( out.toString() );
            }
            catch( IOException e )
            {
                log.error("Failed to read", e);
            }
            finally
            {
                try
                {
                    if( out != null ) out.close();
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
            log.warn("Failed to load page '"+page+"' from '"+pagedata.getAbsolutePath()+"'");
        }

        String res = result.toString();

        return res;
    }

    public void putPageText( WikiPage page, String text )
    {
        File file = findPage( page.getName() );

        try
        {
            PrintWriter out = new PrintWriter(new FileWriter( file ));

            out.print( text );

            out.close();
        }
        catch( IOException e )
        {
            log.error( "Saving failed" );
        }
    }

    public Collection getAllPages()
    {
        log.debug("Getting all pages...");

        ArrayList set = new ArrayList();

        File wikipagedir = new File( m_pageDirectory );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        for( int i = 0; i < wikipages.length; i++ )
        {
            String wikiname = wikipages[i].getName();
            int cutpoint = wikiname.lastIndexOf( FILE_EXT );
            WikiPage page = new WikiPage( unmangleName(wikiname.substring(0,cutpoint)) );

            page.setLastModified( new Date(wikipages[i].lastModified()) );

            set.add( page );
        }

        return set;        
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

            log.debug("Searching page "+wikipages[i].getPath() );

            String filename = wikipages[i].getName();
            int cutpoint    = filename.lastIndexOf( FILE_EXT );
            String wikiname = filename.substring(0,cutpoint);

            wikiname = unmangleName( wikiname );

            try
            {
                BufferedReader in = new BufferedReader( new FileReader(wikipages[i] ) );
                int scores[] = new int[ query.length ];
                
                while( (line = in.readLine()) != null )
                {
                    line = line.toLowerCase();

                    for( int j = 0; j < query.length; j++ )
                    {
                        if( line.indexOf( query[j].word ) != -1 )
                        {
                            log.debug("   Match found for "+query[j].word );

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

    public WikiPage getPageInfo( String page )
    {
        File file = findPage( page );

        if( !file.exists() ) return null;

        WikiPage p = new WikiPage( page );
        p.setLastModified( new Date(file.lastModified()) );

        return p;
    }

    /**
     *  The FileSystemProvider provides only one version.
     */
    public Collection getVersionHistory( String page )
    {
        ArrayList list = new ArrayList();

        list.add( getPageInfo( page ) );

        return list;
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
