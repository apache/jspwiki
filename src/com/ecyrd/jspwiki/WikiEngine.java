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

    private WikiPageProvider m_provider = new FileSystemProvider();

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

    /**
     *  Does all the real initialization.
     */
    private void initialize( Properties props )
        throws IllegalArgumentException
    {
        m_properties = props;
        //
        //  Initialized log4j.  However, make sure that
        //  we don't initialize it multiple times.
        //
        if( !c_configured )
        {
            PropertyConfigurator.configure( props );
            c_configured = true;
        }


        m_provider.initialize( props );


        log.info("WikiEngine configured.");
    }

    /**
     *  Throws an exception if a property is not found.
     */
    static String getRequiredProperty( Properties props, String key )
    {
        String value = props.getProperty(key);

        if( value == null )
            throw new IllegalArgumentException( "Property "+key+" is required" );

        return value;
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

        return m_provider.pageExists( page );
    }

    /**
     *  Returns the unconverted text of a page.
     *
     *  @param page WikiName of the page to fetch.
     */
    public String getText( String page )
    {
        return m_provider.getPageText( page );
    }

    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     */
    public String getHTML( String page )
    {
        StringBuffer result = new StringBuffer();
        String pagedata = getText( page );

        Reader in = new StringReader( pagedata );

        StringWriter out = null;

        try
        {
            in = new TranslatorReader( this, new BufferedReader(in) );
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

        return result.toString();
    }


    public void saveText( String page, String text )
    {
        m_provider.putPageText( page, text );
    }

    public Collection getRecentChanges()
    {
        return m_provider.getRecentChanges();
    }

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
        StringTokenizer st = new StringTokenizer( query, " \t," );

        QueryItem[] items = new QueryItem[st.countTokens()];
        int word = 0;

        log.debug("Expecting "+items.length+" items");

        //
        //  Parse incoming search string
        //

        while( st.hasMoreTokens() )
        {
            log.debug("Item "+word);
            String token = st.nextToken().toLowerCase();

            items[word] = new QueryItem();

            switch( token.charAt(0) )
            {
              case '+':
                items[word].type = QueryItem.REQUIRED;
                token = token.substring(1);
                log.debug("Required word: "+token);
                break;
                
              case '-':
                items[word].type = QueryItem.FORBIDDEN;
                token = token.substring(1);
                log.debug("Forbidden word: "+token);
                break;

              default:
                items[word].type = QueryItem.REQUESTED;
                log.debug("Requested word: "+token);
                break;
            }

            items[word++].word = token;
        }

        Collection results = m_provider.findPages( items );
        
        return results;
    }

    /**
     *  Returns the date the page was last changed.
     */
    public Date pageLastChanged( String page )
    {
        return m_provider.pageLastChanged(page);
    }
}
