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
package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.rss.RSSGenerator;

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
    public static final String PROP_INTERWIKIREF = "jspwiki.interWikiRef.";

    /** If true, then the user name will be stored with the page data.*/
    public static final String PROP_STOREUSERNAME= "jspwiki.storeUserName";

    /** If true, logs the IP address of the editor on saving. */
    public static final String PROP_STOREIPADDRESS= "jspwiki.storeIPAddress";

    /** Define the used encoding.  Currently supported are ISO-8859-1 and UTF-8 */
    public static final String PROP_ENCODING     = "jspwiki.encoding";

    /** The name for the base URL to use in all references. */
    public static final String PROP_BASEURL      = "jspwiki.baseURL";

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiUserProfile";

    private static Hashtable c_engines = new Hashtable();

    private static final String NO_PROVIDER_MSG = 
        "Internal configuration error: No provider was found.";

    /** Should the user info be saved with the page data as well? */
    private boolean        m_saveUserInfo = true;

    /** If true, logs the IP address of the editor */
    private boolean        m_storeIPAddress = true;

    /** If true, uses UTF8 encoding for all data */
    private boolean        m_useUTF8      = true;

    /** Stores the base URL. */
    private String         m_baseURL;

    /** Store the file path to the basic URL.  When we're not running as
        a servlet, it defaults to the user's current directory. */
    private String         m_rootPath = System.getProperty("user.dir");

    /** Stores references between wikipages. */
    private ReferenceManager m_referenceManager = null;

    /** Stores the Plugin manager */
    private PluginManager    m_pluginManager;

    /** Does all our diffs for us. */
    private DifferenceEngine m_differenceEngine;

    /** Generates RSS feed when requested. */
    private RSSGenerator     m_rssGenerator;

    /**
     *  Gets a WikiEngine related to this servlet.
     */
    public static synchronized WikiEngine getInstance( ServletConfig config )
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
        throws NoRequiredPropertyException,
               ServletException
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
            m_rootPath = context.getRealPath("/");

            props.load( new FileInputStream(propertyFile) );

            initialize( props );

            log.debug("Root path for this Wiki is: '"+m_rootPath+"'");
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
        throws NoRequiredPropertyException,
               ServletException
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

        m_saveUserInfo   = "true".equals( props.getProperty( PROP_STOREUSERNAME, "true" ) );
        m_storeIPAddress = "true".equals( props.getProperty( PROP_STOREIPADDRESS, "true" ) );

        m_useUTF8        = "UTF-8".equals( props.getProperty( PROP_ENCODING, "ISO-8859-1" ) );
        m_baseURL        = props.getProperty( PROP_BASEURL, "" );
        
        //
        //  Find the page provider
        //

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

        try
        {
            m_pluginManager    = new PluginManager();
            m_differenceEngine = new DifferenceEngine( props, getContentEncoding() );
            m_rssGenerator     = new RSSGenerator( this, props );

            initReferenceManager();            
        }
        catch( Exception e )
        {
            // RuntimeExceptions may occur here, even if they shouldn't.
            log.error( "Unable to start.", e );
            throw new ServletException( "Unable to start", e );
        }

        // FIXME: I wonder if this should be somewhere else.
        new RSSThread().start();

        log.info("WikiEngine configured.");
    }

    /**
     *  Runs the RSS generation thread.
     *  FIXME: MUST be somewhere else, this is not a good place.
     */
    private class RSSThread extends Thread
    {
        public void run()
        {
            try
            {
                while(true)
                {
                    Writer out = null;
                    Reader in  = null;

                    try
                    {
                        //
                        //  Generate RSS file, output it to
                        //  default "rss.rdf".
                        //
                        log.info("Regenerating RSS feed.");

                        String feed = m_rssGenerator.generate();

                        File file = new File(m_rootPath,"rss.rdf"); // FIXME: magic

                        in  = new StringReader(feed);
                        out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(file), "UTF-8") );

                        FileUtil.copyContents( in, out );
                    }
                    catch( IOException e )
                    {
                        log.error("Cannot generate RSS feed", e );
                    }
                    finally
                    {
                        try
                        {
                            if( in != null )  in.close();
                            if( out != null ) out.close();
                        }
                        catch( IOException e )
                        {
                            log.fatal("Could not close I/O for RSS", e );
                            break;
                        }
                    }

                    // FIXME: Magic number, should be user settable.
                    // Currently generates RSS at startup and then every hour.
                    Thread.sleep(60*60*1000L);
                } // while
                
            }
            catch(InterruptedException e)
            {
                log.error("RSS thread interrupted, no more RSS feeds", e);
            }
        }
    }


    /**
       Initializes the reference manager. Scans all existing WikiPages for
       internal links and adds them to the ReferenceManager object.
    */
    private void initReferenceManager()
    {
        long start = System.currentTimeMillis();
        log.info( "Starting cross reference scan of WikiPages" );

        Collection pages = m_provider.getAllPages();

        // Build a new manager with default key lists.
        if( m_referenceManager == null )
        {
            m_referenceManager = new ReferenceManager( this, pages );
        }
        
        // Scan the existing pages from disk and update references in the manager.
        Iterator it = pages.iterator();
        while( it.hasNext() )
        {
            WikiPage page = (WikiPage)it.next();
            String content = m_provider.getPageText( page.getName(), 
                                                     WikiPageProvider.LATEST_VERSION );
            m_referenceManager.updateReferences( page.getName(), 
                                                 scanWikiLinks( content ) );
        }

        log.info( "Cross reference scan done (" +
                  (System.currentTimeMillis()-start) +
                  " ms)" );
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
     *  Internal method for getting a property.  This is used by the
     *  TranslatorReader for example.
     */

    protected Properties getWikiProperties()
    {
        return m_properties;
    }

    /**
     *  Returns the base URL.  Always prepend this to any reference
     *  you make.
     *
     *  @since 1.6.1
     */

    public String getBaseURL()
    {
        return m_baseURL;
    }
    
    /**
     *  This is a safe version of the Servlet.Request.getParameter() routine.
     *  Unfortunately, the default version always assumes that the incoming
     *  character set is ISO-8859-1, even though it was something else.
     *  This means that we need to make a new string using the correct
     *  encoding.
     *  <P>
     *  Incidentally, this is almost the same as encodeName(), below.
     *  I am not yet entirely sure if it's safe to merge the code.
     *
     *  @since 1.5.3
     */
    public String safeGetParameter( ServletRequest request, String name )
    {
        try
        {
            String res = request.getParameter( name );
            if( res != null ) 
            {
                res = new String(res.getBytes("ISO-8859-1"),
                                 getContentEncoding() );
            }

            return res;
        }
        catch( UnsupportedEncodingException e )
        {
            log.fatal( "Unsupported encoding", e );
            return "";
        }

    }


    /**
     *  Returns an URL to some other Wiki that we know.
     *
     *  @return null, if no such reference was found.
     */
    public String getInterWikiURL( String wikiName )
    {
        return m_properties.getProperty(PROP_INTERWIKIREF+wikiName);
    }

    /**
     *  Returns a collection of all supported InterWiki links.
     */
    public Collection getAllInterWikiLinks()
    {
        Vector v = new Vector();

        for( Enumeration i = m_properties.propertyNames(); i.hasMoreElements(); )
        {
            String prop = (String) i.nextElement();

            if( prop.startsWith( PROP_INTERWIKIREF ) )
            {
                v.add( prop.substring( prop.lastIndexOf(".")+1 ) );
            }
        }

        return v;
    }

    /**
     *  Returns a collection of all image types that get inlined.
     */

    public Collection getAllInlinedImagePatterns()
    {
        return TranslatorReader.getImagePatterns( this );
    }

    /**
     *  If the page is a special page, then returns a direct URL
     *  to that page.  Otherwise returns null.
     *  <P>
     *  Special pages are non-existant references to other pages.
     *  For example, you could define a special page reference
     *  "RecentChanges" which would always be redirected to "RecentChanges.jsp"
     *  instead of trying to find a Wiki page called "RecentChanges".
     */
    public String getSpecialPageReference( String original )
    {
        String propname = "jspwiki.specialPage."+original;
        String specialpage = m_properties.getProperty( propname );

        return specialpage;
    }

    /**
     *  Returns the name of the application.
     */

    // FIXME: Should use servlet context as a default instead of a constant.
    public String getApplicationName()
    {
        String appName = m_properties.getProperty("jspwiki.applicationName");

        if( appName == null )
            return Release.APPNAME;

        return appName;
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
     *  Turns a WikiName into something that can be 
     *  called through using an URL.
     *
     *  @since 1.4.1
     */
    public String encodeName( String pagename )
    {
        if( m_useUTF8 )
            return TextUtil.urlEncodeUTF8( pagename );
        else
            return java.net.URLEncoder.encode( pagename );
    }

    public String decodeName( String pagerequest )
    {
        if( m_useUTF8 )
            return TextUtil.urlDecodeUTF8( pagerequest );

        else
            return java.net.URLDecoder.decode( pagerequest );
    }

    /**
     *  Returns the IANA name of the character set encoding we're
     *  supposed to be using right now.
     *
     *  @since 1.5.3
     */
    public String getContentEncoding()
    {
        if( m_useUTF8 ) 
            return "UTF-8";

        return "ISO-8859-1";
    }

    /**
     *  Returns the unconverted text of a page.
     *
     *  @param page WikiName of the page to fetch.
     */
    public String getText( String page )
    {
        return getText( page, -1 );
    }

    /**
     * Returns the unconverted text of the given version of a page,
     * if it exists.  This method also replaces the HTML entities.
     *
     * @param page WikiName of the page to fetch
     * @param version  Version of the page to fetch
     */
    public String getText( String page, int version )
    {
        String result = getPureText( page, version );

        result = TextUtil.replaceEntities( result );

        return result;
    }

    /**
     *  Returns the pure text of a page, no conversions.
     *
     *  @version If WikiPageProvider.LATEST_VERSION, then uses the 
     *  latest version.
     */
    // FIXME: Should throw an exception on unknown page/version?
    public String getPureText( String page, int version )
    {
        if( m_provider == null ) 
            return NO_PROVIDER_MSG;

        String result = null;

        result = m_provider.getPageText( page, version );

        if( result == null )
            result = "";

        return result;
    }

    /**
     *  Returns the converted HTML of the page using a different
     *  context than the default context.
     */

    public String getHTML( WikiContext context, WikiPage page )
    {
	String pagedata = null;

        pagedata = getPureText( page.getName(), page.getVersion() );

        String res = textToHTML( context, pagedata );

	return res;
    }
    
    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     */
    public String getHTML( String page )
    {
        return getHTML( page, -1 );
    }

    /**
     *  Returns the converted HTML of the page's specific version.
     *  The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param pagename WikiName of the page to convert.
     *  @param version Version number to fetch
     */
    public String getHTML( String pagename, int version )
    {
        WikiContext context = new WikiContext( this,
                                               pagename );
        WikiPage page = new WikiPage( pagename );
        page.setVersion( version );
        
        String res = getHTML( context, page );

	return res;
    }

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     */
    public String textToHTML( WikiContext context, String pagedata )
    {
        return textToHTML( context, pagedata, null, null );
    }

    /**
     *  Reads a WikiPageful of data from a String and returns all links
     *  internal to this Wiki in a Collection.
     */
    protected Collection scanWikiLinks( String pagedata )
    {
        LinkCollector localCollector = new LinkCollector();        

        textToHTML( new WikiContext(this,""),
                    pagedata,
                    localCollector,
                    null );

        return localCollector.getLinks();
    }

    /**
     *  Helper method for combining simple  textToHTML() and scanWikiLinks().
     */
    public String textToHTML( WikiContext context, 
                              String pagedata, 
                              StringTransmutator localLinkHook,
                              StringTransmutator extLinkHook )
    {
        String result = "";

        if( pagedata == null ) 
        {
            log.error("NULL pagedata to textToHTML()");
            return null;
        }

        TranslatorReader in = null;
        Collection links = null;

        try
        {
            in = new TranslatorReader( context,
                                       new StringReader( pagedata ) );

            in.addLocalLinkHook( localLinkHook );
            in.addExternalLinkHook( extLinkHook );
            result = FileUtil.readContents( in );
        }
        catch( IOException e )
        {
            log.error("Failed to scan page data: ", e);
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

        return( result );
    }

    /**
     *  Writes the WikiText of a page into the
     *  page repository.
     *
     *  @param page Page name
     *  @param text The Wiki markup for the page.
     */
    public void saveText( String page, String text )
    {
        if( m_provider == null ) 
            return;

        text = TextUtil.normalizePostData(text);

        // Hook into cross reference collection.
        m_referenceManager.updateReferences( page, scanWikiLinks( text ) );

        m_provider.putPageText( new WikiPage(page), text );
    }

    // FIXME: This is a terrible time waster, parsing the 
    // textual data every time it is used.

    public String getUserName( HttpServletRequest request )
    {
        // Get the user authentication - if he's not been authenticated
        // we store the IP address.  Unless we've been asked not to.

        String author = request.getRemoteUser();

        //
        //  Try to fetch something from a cookie.
        //
        if( author == null )
        {
            Cookie[] cookies = request.getCookies();

            if( cookies != null )
            {
                for( int i = 0; i < cookies.length; i++ )
                {
                    if( cookies[i].getName().equals( PREFS_COOKIE_NAME ) )
                    {
                        UserProfile p = new UserProfile(cookies[i].getValue());

                        author = p.getName();

                        break;
                    }
                }
            }
        }

        return author;
    }

    /**
     *  @param request The HTTP Servlet request associated with this
     *                 transaction.
     *  @since 1.5.1
     */
    public void saveText( String page, String text, HttpServletRequest request )
    {
        if( m_provider == null )
        {
            return;
        }

        text = TextUtil.normalizePostData(text);

        // Error protection or if the user info has been disabled.
        if( request == null || m_saveUserInfo == false ) 
        {
            saveText( page, text );
        }
        else
        {
            // Hook into cross reference collection.
            // Notice that this is definitely after the saveText() call above, 
            // since it can be called externally and we only want this done once.
            m_referenceManager.updateReferences( page, scanWikiLinks( text ) );

            WikiPage p = new WikiPage( page );

            String author = getUserName( request );

            //
            //  If no author name has been set, then use the 
            //  IP address, if allowed.
            //

            if( author == null && m_storeIPAddress )
                author = request.getRemoteAddr();

            //  If no author has been defined, then
            //  use whatever default WikiPage gives us.

            if( author != null )
                p.setAuthor( author );

            m_provider.putPageText( p, text );
        }
    }

    /**
     *  Returns the number of pages in this Wiki
     */
    public int getPageCount()
    {
        return m_provider.getAllPages().size();
    }

    /**
     *  Returns the provider name
     */

    public String getCurrentProvider()
    {
        return m_provider.getClass().getName();
    }

    /**
     *  return information about current provider.
     *  @since 1.6.4
     */
    public String getCurrentProviderInfo()
    {
        return m_provider.getProviderInfo();
    }

    /**
     *  Returns a Collection of WikiPages, sorted in time
     *  order of last change.
     */
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
     *  Return a bunch of information from the web page.
     */

    public WikiPage getPage( String pagereq )
    {
        if( m_provider == null ) 
            return null;

        WikiPage p = m_provider.getPageInfo( pagereq, 
                                             WikiPageProvider.LATEST_VERSION );

        return p;
    }

    /**
     *  Returns specific information about a Wiki page.
     *  @since 1.6.7.
     */

    public WikiPage getPage( String pagereq, int version )
    {
        if( m_provider == null )
            return null;

        WikiPage p = m_provider.getPageInfo( pagereq, version );

        return p;
    }

    /**
     *  Returns the date the page was last changed.
     *  If the page does not exist, returns null.
     *  @deprecated
     */
    public Date pageLastChanged( String page )
    {
        if( m_provider == null ) 
            return null;

        WikiPage p = m_provider.getPageInfo( page, WikiPageProvider.LATEST_VERSION );

        if( p != null )
            return p.getLastModified();

        return null;
    }

    /**
     *  Returns the current version of the page.
     *  @deprecated
     */
    public int getVersion( String page )
    {
        if( m_provider == null ) 
            return -1;

        WikiPage p = m_provider.getPageInfo( page, WikiPageProvider.LATEST_VERSION );

        if( p != null )
            return p.getVersion();

        return -1;
    }

    /**
     *  Returns a Collection of WikiPages containing the
     *  version history of a page.
     */
    public Collection getVersionHistory( String page )
    {
        if( m_provider == null ) 
            return null;

        return m_provider.getVersionHistory( page );
    }

    /**
     *  Returns a diff of two versions of a page.
     *
     *  @param page Page to return
     *  @param version1 Version number of the old page.  If -1, then uses current page.
     *  @param version2 Version number of the new page.  If -1, then uses current page.
     *
     *  @return A HTML-ized difference between two pages.  If there is no difference,
     *          returns an empty string.
     */
    public String getDiff( String page, int version1, int version2 )
    {
        String page1 = getPureText( page, version1 );
        String page2 = getPureText( page, version2 );

        String diff  = m_differenceEngine.makeDiff( page1, page2 );

        diff = TextUtil.replaceEntities( diff );
        
        try
        {
            if( diff.length() > 0 )
            {
                diff = m_differenceEngine.colorizeDiff( diff );
            }
        }
        catch( IOException e )
        {
            log.error("Failed to colorize diff result.", e);
        }

        return diff;
    }

    /**
     *  Returns this object's ReferenceManager.
     *  @since 1.6.1
     */
    // (FIXME: We may want to protect this, though...)
    public ReferenceManager getReferenceManager()
    {
        return m_referenceManager;
    }

    /**      
     *  Returns the current plugin manager.
     *  @since 1.6.1
     */

    public PluginManager getPluginManager()
    {
        return m_pluginManager;
    }
}
