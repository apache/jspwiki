package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import org.apache.log4j.Category;

public class WikiEngine
{
    private static final Category   log = Category.getInstance(WikiEngine.class);

    private String m_pagelocation = "/home/jalkanen/Projects/JSPWiki/src/wikipages";

    public static final String FILE_EXT = ".txt";

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

                    int cutpoint = wikiname.lastIndexOf(".txt");
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

    public class WikiFileFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return name.endsWith(".txt");
        }
    }
}
