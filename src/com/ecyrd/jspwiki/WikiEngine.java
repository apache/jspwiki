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

    private WikiPageProvider m_provider;

    /** True, if log4j has been configured. */
    // FIXME: If you run multiple applications, the first application
    // to run defines where the log goes.  Not what we want.
    private static boolean c_configured = false;

    /** Stores properties. */
    private Properties     m_properties;

    public static final String PROP_PAGEPROVIDER = "jspwiki.pageProvider";

    private static Hashtable c_engines = new Hashtable();

    private static final String NO_PROVIDER_MSG = 
        "Internal configuration error: No provider was found.";

    /**
     *  Gets a WikiEngine related to this servlet.
     */
    public static WikiEngine getInstance( ServletConfig config )
    {
        ServletContext context = config.getServletContext();        
        String appid = context.getRealPath("/");

        config.getServletContext().log( "Application "+appid+" requests WikiEngine.");

        WikiEngine engine = (WikiEngine) c_engines.get( appid );

        if( engine == null )
        {
            config.getServletContext().log(" Assigning new log to "+appid);
            engine = new WikiEngine( config.getServletContext() );

            c_engines.put( appid, engine );
        }

        return engine;
    }

    /**
     *  Instantiate the WikiEngine using a given set of properties.
     */
    public WikiEngine( Properties properties )
        throws NoRequiredPropertyException
    {
        initialize( properties );
    }

    /**
     *  Instantiate using this method when you're running as a servlet and
     *  WikiEngine will figure out where to look for the property file.
     */
    protected WikiEngine( ServletContext context )
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
            context.log( Release.APPNAME+": Unable to load and setup properties from "+propertyFile+". "+e.getMessage() );
        }
    }

    /**
     *  Does all the real initialization.
     */
    private void initialize( Properties props )
        throws NoRequiredPropertyException
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

        log.debug("Configuring WikiEngine...");

        String classname = getRequiredProperty( props, PROP_PAGEPROVIDER );

        log.debug("Provider="+classname);

        try
        {
            Class providerclass = Class.forName( classname );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing provider class "+m_provider);
            m_provider.initialize( props );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new IllegalArgumentException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new IllegalArgumentException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
            throw new IllegalArgumentException("illegal provider class");
        }


        log.info("WikiEngine configured.");
    }

    /**
     *  Throws an exception if a property is not found.
     */
    static String getRequiredProperty( Properties props, String key )
        throws NoRequiredPropertyException
    {
        String value = props.getProperty(key);

        if( value == null )
        {
            throw new NoRequiredPropertyException( "Required property not found",
                                                   key );
        }

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

        // Error cases.
        if( m_provider == null ) return false;

        return m_provider.pageExists( page );
    }

    /**
     *  Returns the unconverted text of a page.
     *
     *  @param page WikiName of the page to fetch.
     */
    public String getText( String page )
    {
        if( m_provider == null ) 
            return NO_PROVIDER_MSG;

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
        if( m_provider == null ) 
            return;

        m_provider.putPageText( page, text );
    }

    public Collection getRecentChanges()
    {
        if( m_provider == null ) 
            return null;

        Collection pages = m_provider.getAllPages();

        TreeSet sortedPages = new TreeSet( new PageTimeComparator() );

        sortedPages.addAll( pages );

        return sortedPages;
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

        if( m_provider == null ) 
            return null;

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
     *  If the page does not exist, returns null.
     */
    public Date pageLastChanged( String page )
    {
        if( m_provider == null ) 
            return null;

        WikiPage p = m_provider.getPageInfo( page );

        if( p != null )
            return p.getLastModified();

        return null;
    }

    public int getVersion( String page )
    {
        if( m_provider == null ) 
            return -1;

        WikiPage p = m_provider.getPageInfo( page );

        if( p != null )
            return p.getVersion();

        return -1;
    }

    public Collection getVersionHistory( String page )
    {
        if( m_provider == null ) 
            return null;

        return m_provider.getVersionHistory( page );
    }
}
