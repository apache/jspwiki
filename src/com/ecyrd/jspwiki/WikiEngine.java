package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import javax.servlet.*;

/**
 *  Provides Wiki services to the JSP page.
 *
 *  There are some problems with this class:
 *  <UL>
 *   <LI>There is no synchronization of pages: we rely on the filesystem
 *       synchronization.
 *   <LI>There is a separate instance of this class for every JSP page, which
 *       means that we can't use synchronized methods.  There should really
 *       be a single class per <I>application</I>, which is a bit more
 *       problematic.
 *   <LI>If we have a single class per JVM, then we can't have multiple
 *       page repositories, or multiple page names.
 *  </UL>
 *  @author Janne Jalkanen
 */
public class WikiEngine
{
    private static final Category   log = Category.getInstance(WikiEngine.class);

    private String m_pagelocation = "/home/jalkanen/Projects/JSPWiki/src/wikipages";

    /**
     *  All files should have this extension to be recognized as JSPWiki files.
     *  We default to .txt, because that is probably easiest for Windows users,
     *  and guarantees correct handling.
     */
    public static final String FILE_EXT = ".txt";

    /**
     *  Name of the property that defines where page directories are.
     */
    private static final String PROP_PAGEDIR = "jspwiki.wikiFiles";

    /** True, if log4j has been configured. */
    // FIXME: If you run multiple applications, the first application
    // to run defines where the log goes.  Not what we want.
    private static boolean c_configured = false;

    /** Stores properties. */
    private Properties     m_properties;

    /**
     *  Instantiate the WikiEngine using a given set of properties.
     */
    public WikiEngine( Properties properties )
        throws IllegalArgumentException
    {
        initialize( properties );
    }

    /**
     *  Instantiate using this method when you're running as a servlet and
     *  WikiEngine will figure out where to look for the property file.
     */
    public WikiEngine( ServletContext context )
        throws ServletException
    {
        String propertyFile = context.getRealPath("/WEB-INF/jspwiki.properties");

        Properties props = new Properties();

        try
        {
            props.load( new FileInputStream(propertyFile) );

            initialize( props );
        }
        catch( Exception e )
        {
            context.log( Release.APPNAME+": Unable to load and setup properties from "+propertyFile );

            throw new ServletException( "Failed to load properties", e );
        }
    }

    /**
     *  Does all the real initialization.
     */
    private void initialize( Properties props )
        throws IllegalArgumentException
    {
        m_properties = props;

        m_pagelocation = getRequiredProperty( props, PROP_PAGEDIR );

        //
        //  Initialized log4j.  However, make sure that
        //  we don't initialize it multiple times.
        //
        if( !c_configured )
        {
            PropertyConfigurator.configure( props );
            c_configured = true;
        }

        log.info("WikiEngine configured.");
        log.debug("files at "+m_pagelocation );
    }

    /**
     *  Throws an exception if a property is not found.
     */
    private static String getRequiredProperty( Properties props, String key )
    {
        String value = props.getProperty(key);

        if( value == null )
            throw new IllegalArgumentException( "Property "+key+" is required" );

        return value;
    }

    /**
     *  Finds a Wiki page from the page repository.
     */
    private File findPage( String page )
    {
        return new File( m_pagelocation, page+FILE_EXT );
    }

    /**
     *  If the page is a special page, then returns a direct URL
     *  to that page.  Otherwise returns null.
     */
    public String getSpecialPageReference( String original )
    {
        String propname = "jspwiki.specialPage."+original;
        String specialpage = m_properties.getProperty( propname );

        return specialpage;
    }

    /**
     *  Returns true, if the requested page exists.
     *
     *  @param page WikiName of the page.
     */
    public boolean pageExists( String page )
    {
        if( getSpecialPageReference(page) != null ) return true;

        File pagefile = findPage( page );

        return pagefile.exists();
    }

    /**
     *  Returns the unconverted text of a page.
     *
     *  @param page WikiName of the page to fetch.
     */
    public String getText( String page )
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

        return result.toString();        
    }

    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     */
    public String getHTML( String page )
    {
        StringBuffer result = new StringBuffer();

        File pagedata = findPage( page );

        if( pagedata.exists() && pagedata.canRead() )
        {
            Reader in = null;
            StringWriter out = null;

            log.debug("Reading");

            try
            {
                in = new TranslatorReader( this, new BufferedReader(new FileReader(pagedata) ) );
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

        return result.toString();
    }


    public void saveText( String page, String text )
    {
        File file = findPage( page );

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

    public Collection getRecentChanges()
    {
        TreeSet set = new TreeSet( new PageTimeComparator() );

        File wikipagedir = new File( m_pagelocation );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        for( int i = 0; i < wikipages.length; i++ )
        {
            String wikiname = wikipages[i].getName();
            int cutpoint = wikiname.lastIndexOf( FILE_EXT );
            WikiPage page = new WikiPage( wikiname.substring(0,cutpoint) );

            page.setLastModified( new Date(wikipages[i].lastModified()) );

            log.debug("Adding..." +page);

            set.add( page );
        }

        return set;
    }

    private static final int REQUIRED = 1;
    private static final int FORBIDDEN = -1;
    private static final int REQUESTED = 0;

    /**
     *  Parses an incoming search request, then
     *  does a search.
     *  <P>
     *  Search language is simple: prepend a word
     *  with a + to force a word to be included (all files
     *  not containing that word are automatically rejected),
     *  '-' to cause the rejection of all those files that contain
     *  that word.
     */

    // FIXME: does not support phrase searches yet, but for them
    // we need a version which reads the whole page into the memory
    // once.
    
    public Collection findPages( String query )
    {
        TreeSet res = new TreeSet( new SearchResultComparator() );
        StringTokenizer st = new StringTokenizer( query, " \t," );

        String[] words = new String[st.countTokens()];
        int[] required = new int[st.countTokens()];

        int word = 0;

        //
        //  Parse incoming search string
        //

        while( st.hasMoreTokens() )
        {
            String token = st.nextToken().toLowerCase();
            
            switch( token.charAt(0) )
            {
              case '+':
                required[word] = REQUIRED;
                token = token.substring(1);
                log.debug("Required word: "+token);
                break;
                
              case '-':
                required[word] = FORBIDDEN;
                token = token.substring(1);
                log.debug("Forbidden word: "+token);
                break;

              default:
                required[word] = REQUESTED;
                log.debug("Requested word: "+token);
                break;
            }

            words[word++] = token;
        }

        //
        //  Do the find
        //
        File wikipagedir = new File( m_pagelocation );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

    nextfile:
        for( int i = 0; i < wikipages.length; i++ )
        {
            String line = null;

            log.debug("Searching page "+wikipages[i].getPath() );

            String filename = wikipages[i].getName();
            int cutpoint    = filename.lastIndexOf( FILE_EXT );
            String wikiname = filename.substring(0,cutpoint);

            try
            {
                BufferedReader in = new BufferedReader( new FileReader(wikipages[i] ) );
                int scores[] = new int[ words.length ];
                
                while( (line = in.readLine()) != null )
                {
                    line = line.toLowerCase();

                    for( int j = 0; j < words.length; j++ )
                    {
                        if( line.indexOf( words[j] ) != -1 )
                        {
                            log.debug("   Match found for "+words[j] );

                            if( required[j] != FORBIDDEN )
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

                    if( wikiname.toLowerCase().indexOf( words[j] ) != -1 &&
                        required[j] != FORBIDDEN )
                        scores[j] += 5;

                    //  Filter out pages if the search word is marked 'required'
                    //  but they have no score.

                    if( required[j] == REQUIRED && scores[j] == 0 )
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
     *  Returns the date the page was last changed.
     */
    public Date pageLastChanged( String page )
    {
        File file = findPage( page );

        if( !file.exists() ) return null;

        return new Date( file.lastModified() );
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

    public class SearchResultComparator
        implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            SearchResult s1 = (SearchResult)o1;
            SearchResult s2 = (SearchResult)o2;

            // Bigger scores are first.

            int res = s2.getScore() - s1.getScore();

            if( res == 0 )
                res = s1.getName().compareTo(s2.getName());

            return res;
        }
    }

    public class PageTimeComparator
        implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            WikiPage w1 = (WikiPage)o1;
            WikiPage w2 = (WikiPage)o2;
            
            // This gets most recent on top
            int timecomparison = w2.getLastModified().compareTo( w1.getLastModified() );

            if( timecomparison == 0 )
            {
                return w1.getName().compareTo( w2.getName() );
            }

            return timecomparison;
        }
    }

    public class WikiFileFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return name.endsWith( FILE_EXT );
        }
    }
}
