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
import java.util.*;
import org.apache.log4j.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

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

    /** Determines the command to be used for 'diff'.  This program must
        be able to output diffs in the unified format. It defaults to
        'diff -u %s1 %s2'.*/
    public static final String PROP_DIFFCOMMAND  = "jspwiki.diffCommand";

    /** If true, then the user name will be stored with the page data.*/
    public static final String PROP_STOREUSERNAME= "jspwiki.storeUserName";

    /** If true, logs the IP address of the editor on saving. */
    public static final String PROP_STOREIPADDRESS= "jspwiki.storeIPAddress";

    /** Define the used encoding.  Currently supported are ISO-8859-1 and UTF-8 */
    public static final String PROP_ENCODING     = "jspwiki.encoding";

    private String         m_diffCommand = "diff -u %s1 %s2"; 

    private static Hashtable c_engines = new Hashtable();

    private static final String NO_PROVIDER_MSG = 
        "Internal configuration error: No provider was found.";

    /** Should the user info be saved with the page data as well? */
    private boolean        m_saveUserInfo = true;

    /** If true, logs the IP address of the editor */
    private boolean        m_storeIPAddress = true;

    /** If true, uses UTF8 encoding for all data */

    private boolean        m_useUTF8      = true;

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

        m_diffCommand = props.getProperty( PROP_DIFFCOMMAND, m_diffCommand );

        m_saveUserInfo   = "true".equals( props.getProperty( PROP_STOREUSERNAME, "true" ) );
        m_storeIPAddress = "true".equals( props.getProperty( PROP_STOREIPADDRESS, "true" ) );

        m_useUTF8        = "UTF-8".equals( props.getProperty( PROP_ENCODING, "ISO-8859-1" ) );

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
     *  Internal method for getting a property.  This is used by the
     *  TranslatorReader for example.
     */

    protected Properties getWikiProperties()
    {
        return m_properties;
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
        if( m_provider == null ) 
            return NO_PROVIDER_MSG;

        return m_provider.getPageText( page );
    }

    /**
     * Returns the unconverted text of the given version of a page,
     * if it exists.
     *
     * @param page WikiName of the page to fetch
     * @param version  Version of the page to fetch
     */
    public String getText( String page, int version )
    {
        if( m_provider == null ) 
            return NO_PROVIDER_MSG;

	// FIXME: What do we get if the version doesn't exist?
	// Null. Need to implement a check.
        return m_provider.getPageText( page, version );
    }


    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     */
    public String getHTML( String page )
    {
        String pagedata = getText( page );
	return( textToHTML( pagedata ) );
    }

    /**
     *  Returns the converted HTML of the page's specific version.
     *  The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param page WikiName of the page to convert.
     *  @param version Version number to fetch
     */
    public String getHTML( String page, int version )
    {
	String pagedata = null;
	if(version >= 0)
	    pagedata = getText( page, version );
	else
	    pagedata = getText( page );

	return( textToHTML( pagedata ) );
    }

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     */
    protected String textToHTML( String pagedata )
    {
        StringBuffer result = new StringBuffer();

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

        m_provider.putPageText( new WikiPage(page), text );
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

        // Error protection or if the user info has been disabled.
        if( request == null || m_saveUserInfo == false ) 
        {
            saveText( page, text );
            return;
        }

        WikiPage p = new WikiPage( page );

        // Get the user authentication - if he's not been authenticated
        // we store the IP address.  Unless we've been asked not to.

        String author = request.getRemoteUser();
        if( author == null && m_storeIPAddress )
            author = request.getRemoteAddr();

        //  If no author has been defined, then
        //  use whatever default WikiPage gives us.

        if( author != null )
            p.setAuthor( author );

        m_provider.putPageText( p, text );
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

    /**
     *  Returns the current version of the page.
     */
    public int getVersion( String page )
    {
        if( m_provider == null ) 
            return -1;

        WikiPage p = m_provider.getPageInfo( page );

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
     */
    public String getDiff( String page, int version1, int version2 )
    {
        String page1 = m_provider.getPageText( page, version1 );
        String page2 = m_provider.getPageText( page, version2 );

        String diff  = makeDiff( page1, page2 );

        diff = TranslatorReader.replaceString( diff, "<", "&lt;" );
        diff = TranslatorReader.replaceString( diff, ">", "&gt;" );

        return diff;
    }

    /**
     *  Makes the diff by calling "diff" program.
     */
    private String makeDiff( String p1, String p2 )
    {
        File f1 = null, f2 = null;
        String diff = null;

        try
        {
            f1 = FileUtil.newTmpFile( p1 );
            f2 = FileUtil.newTmpFile( p2 );

            String cmd = TranslatorReader.replaceString( m_diffCommand,
                                                         "%s1",
                                                         f1.getPath() );
            cmd = TranslatorReader.replaceString( cmd,
                                                  "%s2",
                                                  f2.getPath() );

            String output = FileUtil.runSimpleCommand( cmd, f1.getParent() );

            diff = output;
        }
        catch( IOException e )
        {
            log.error("Failed to do file diff",e);
        }
        catch( InterruptedException e )
        {
            log.error("Interrupted",e);
        }
        finally
        {
            if( f1 != null ) f1.delete();
            if( f2 != null ) f2.delete();
        }

        return diff;
    }
}
