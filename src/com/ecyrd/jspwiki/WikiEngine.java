package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import javax.servlet.*;

public class WikiEngine
{
    private static final Category   log = Category.getInstance(WikiEngine.class);

    private String m_pagelocation = "/home/jalkanen/Projects/JSPWiki/src/wikipages";

    public static final String FILE_EXT = ".txt";
    public static final String PROP_PAGEDIR = "jspwiki.wikiFiles";

    private static boolean c_configured = false;

    public WikiEngine( Properties properties )
        throws IllegalArgumentException
    {
        initialize( properties );
    }

    public WikiEngine( ServletContext context )
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
        }
    }

    private void initialize( Properties props )
        throws IllegalArgumentException
    {
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

    private static String getRequiredProperty( Properties props, String key )
    {
        String value = props.getProperty(key);

        if( value == null )
            throw new IllegalArgumentException( "Property "+key+" is required" );

        return value;
    }

    private File findPage( String page )
    {
        return new File( m_pagelocation, page+FILE_EXT );
    }

    public boolean pageExists( String page )
    {
        File pagefile = findPage( page );

        return pagefile.exists();
    }

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

    public Collection findPages( String query )
    {
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer( query, " \t," );

        String[] words = new String[st.countTokens()];
        int word = 0;

        while( st.hasMoreTokens() )
        {
            words[word++] = st.nextToken().toLowerCase();
        }

        File wikipagedir = new File( m_pagelocation );

        File[] wikipages = wikipagedir.listFiles( new WikiFileFilter() );

        for( int i = 0; i < wikipages.length; i++ )
        {
            String line = null;
            int score = 0;

            log.debug("Searching page "+wikipages[i].getPath() );

            try
            {
                BufferedReader in = new BufferedReader( new FileReader(wikipages[i] ) );

                while( (line = in.readLine()) != null )
                {
                    line = line.toLowerCase();

                    for( int j = 0; j < words.length; j++ )
                    {
                        if( line.indexOf( words[j] ) != -1 )
                        {
                            log.debug("   Match found for "+words[j] );
                            score++;
                        }
                    }
                }

                if( score > 0 )
                {
                    String wikiname = wikipages[i].getName();

                    int cutpoint = wikiname.lastIndexOf( FILE_EXT );
                    v.add( wikiname.substring(0,cutpoint) );
                }
            }
            catch( IOException e )
            {
                log.error( "Failed to read", e );
            }
        }

        return v;
    }

    public Date pageLastChanged( String page )
    {
        File file = findPage( page );

        if( !file.exists() ) return null;

        return new Date( file.lastModified() );
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
