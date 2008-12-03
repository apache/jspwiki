package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.util.ResolverUtil;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.preferences.Preferences;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.tags.WikiTagBase;
import com.ecyrd.jspwiki.url.StripesURLConstructor;

/**
 * <p>
 * Class that looks up {@link WikiActionBean}s, and resolves special pages and
 * JSPs on behalf of a WikiEngine. WikiActionBeanResolver will automatically
 * resolve page names with singular/plural variants. It can also detect the
 * correct WikiActionBean based on parameters supplied in an HTTP request, or
 * due to the JSP being accessed.
 * </p>
 * 
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public final class WikiContextFactory
{
    /**
     * The PageContext attribute name of the WikiEngine stored by
     * WikiInterceptor.
     */
    public static final String ATTR_WIKIENGINE = "wikiEngine";

    /**
     * The PageContext attribute name of the WikiSession stored by
     * WikiInterceptor.
     */
    public static final String ATTR_WIKISESSION = "wikiSession";

    /** Default list of packages to search for WikiActionBean implementations. */
    public static final String DEFAULT_ACTIONBEAN_PACKAGES = "com.ecyrd.jspwiki.action";

    /**
     * Property in jspwiki.properties that specifies packages to search for
     * WikiActionBean implementations.
     */
    public static final String PROPS_ACTIONBEAN_PACKAGES = "jspwiki.actionBean.packages";

    private static final Logger log = LoggerFactory.getLogger( WikiContextFactory.class );

    private static final long serialVersionUID = 1L;

    /** Prefix in jspwiki.properties signifying special page keys. */
    private static final String PROP_SPECIALPAGE = "jspwiki.specialPage.";

    /**
     * This method can be used to find the WikiContext programmatically from a
     * JSP PageContext. We check the request context. The wiki context, if it
     * exists, is looked up using the key
     * {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * 
     * @since 2.4
     * @param pageContext the JSP page context
     * @return Current WikiContext, or null, of no context exists.
     */
    public static WikiContext findContext( PageContext pageContext )
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        WikiContext context = (WikiContext) request.getAttribute( WikiTagBase.ATTR_CONTEXT );
        return context;
    }

    /**
     * Returns the locale of the HTTP request if available, otherwise returns
     * the default Locale of the server.
     * 
     * @return A valid locale object
     * @param context The WikiContext
     */
    public static Locale getLocale( WikiContext context )
    {
        return Preferences.getLocale( context );
        /*
         * HttpServletRequest request = context.getHttpRequest(); return (
         * request != null ) ? request.getLocale() : Locale.getDefault();
         */
    }

    /**
     * <p>
     * Saves the supplied WikiContext, and the related WikiEngine and
     * WikiSession, in request scope. The WikiContext is saved as an attribute
     * named {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}. The
     * WikiEngine is also saved as {@link #ATTR_WIKIENGINE}, and the
     * WikiSession as {@link #ATTR_WIKISESSION}. Among other things, by saving
     * these items as attributes, they can be accessed via JSP Expression
     * Language variables, for example <code>${wikiContext}</code>.
     * </p>
     * <p>
     * Note: when the WikiContext is saved, it will be guaranteed to have a
     * non-null WikiPage. If the context as supplied has a WikiPage that is
     * <code>null</code>, the
     * {@link com.ecyrd.jspwiki.WikiEngine#getFrontPage()} will be consulted,
     * and that page will be used.
     * </p>
     * 
     * @param request the HTTP request
     * @param context the WikiContext to save
     */
    public static void saveContext( HttpServletRequest request, WikiContext context )
    {
        // Stash WikiEngine as a request attribute (can be
        // used later as ${wikiEngine} in EL markup)
        WikiEngine engine = context.getEngine();
        request.setAttribute( ATTR_WIKIENGINE, engine );

        // Stash the WikiSession as a request attribute
        WikiSession wikiSession = SessionMonitor.getInstance( engine ).find( request.getSession() );
        request.setAttribute( ATTR_WIKISESSION, wikiSession );

        WikiPage page = context.getPage();
        if( page == null )
        {
            // If the page supplied was blank, default to the front page to
            // avoid NPEs
            page = engine.getPage( engine.getFrontPage() );
            // Front page does not exist?
            if( page == null )
            {
                page = new WikiPage( engine, engine.getFrontPage() );
            }
            context.setPage( page );
        }
        request.setAttribute( WikiTagBase.ATTR_CONTEXT, context );
    }

    /** Private map with JSPs as keys, Resolutions as values */
    private final Map<String, RedirectResolution> m_specialRedirects;

    private final WikiEngine m_engine;

    private String m_mockContextPath;

    /** If true, we'll also consider english plurals (+s) a match. */
    private boolean m_matchEnglishPlurals;

    /** Maps (pre-3.0) request contexts map to WikiActionBeans. */
    private final Map<String, HandlerInfo> m_contextMap = new HashMap<String, HandlerInfo>();

    /**
     * Constructs a WikiActionBeanResolver for a given WikiEngine. This
     * constructor will extract the special page references for this wiki and
     * store them in a cache used for resolution.
     * 
     * @param engine the wiki engine
     * @param properties the properties used to initialize the wiki
     */
    public WikiContextFactory( WikiEngine engine, Properties properties )
    {
        super();
        m_engine = engine;
        m_specialRedirects = new HashMap<String, RedirectResolution>();

        initRequestContextMap( properties );
        initSpecialPageRedirects( properties );

        // Do we match plurals?
        m_matchEnglishPlurals = TextUtil.getBooleanProperty( properties, WikiEngine.PROP_MATCHPLURALS, true );

        // Set the path prefix for constructing synthetic Stripes mock requests;
        // trailing slash is removed.
        m_mockContextPath = StripesURLConstructor.getContextPath( engine );

        // TODO: make packages to search in ActionBeanResolver configurable
        // (currently hard-coded)
    }

    /**
     * Looks up and returns the correct HandlerInfo class corresponding to a
     * supplied wiki context. The supplied context name is matched against the
     * values annotated using
     * {@link com.ecyrd.jspwiki.action.WikiRequestContext}. If a match is not
     * found, this method throws an IllegalArgumentException.
     * 
     * @param requestContext the context to look up
     * @return the WikiActionBean event handler info
     */
    public HandlerInfo findEventHandler( String requestContext )
    {
        HandlerInfo handler = m_contextMap.get( requestContext );
        if( handler == null )
        {
            throw new IllegalArgumentException( "No HandlerInfo found for request context '" + requestContext + "'!" );
        }
        return handler;
    }

    /**
     * <p>
     * Returns the correct page name, or <code>null</code>, if no such page
     * can be found. Aliases are considered.
     * </p>
     * <p>
     * In some cases, page names can refer to other pages. For example, when you
     * have matchEnglishPlurals set, then a page name "Foobars" will be
     * transformed into "Foobar", should a page "Foobars" not exist, but the
     * page "Foobar" would. This method gives you the correct page name to refer
     * to.
     * </p>
     * <p>
     * This facility can also be used to rewrite any page name, for example, by
     * using aliases. It can also be used to check the existence of any page.
     * </p>
     * 
     * @since 2.4.20
     * @param page the page name.
     * @return The rewritten page name, or <code>null</code>, if the page
     *         does not exist.
     */
    public final String getFinalPageName( String page ) throws ProviderException
    {
        boolean isThere = simplePageExists( page );
        String finalName = page;

        if( !isThere && m_matchEnglishPlurals )
        {
            if( page.endsWith( "s" ) )
            {
                finalName = page.substring( 0, page.length() - 1 );
            }
            else
            {
                finalName += "s";
            }

            isThere = simplePageExists( finalName );
        }

        if( !isThere )
        {
            finalName = MarkupParser.wikifyLink( page );
            isThere = simplePageExists( finalName );

            if( !isThere && m_matchEnglishPlurals )
            {
                if( finalName.endsWith( "s" ) )
                {
                    finalName = finalName.substring( 0, finalName.length() - 1 );
                }
                else
                {
                    finalName += "s";
                }

                isThere = simplePageExists( finalName );
            }
        }

        return isThere ? finalName : null;
    }

    /**
     * <p>
     * If the page is a special page, this method returns a direct URL to that
     * page; otherwise, it returns <code>null</code>.
     * </p>
     * <p>
     * Special pages are non-existant references to other pages. For example,
     * you could define a special page reference "RecentChanges" which would
     * always be redirected to "RecentChanges.jsp" instead of trying to find a
     * Wiki page called "RecentChanges".
     * </p>
     * TODO: fix this algorithm
     */
    public final String getSpecialPageReference( String page )
    {
        RedirectResolution resolution = m_specialRedirects.get( page );

        if( resolution != null )
        {
            return resolution.getUrl();
        }

        return null;
    }

    /**
     * <p>
     * Creates a WikiActionBeanContext instance, associates an HTTP request and
     * response with it, and sets the correct WikiPage into the context if
     * required. This method will determine what page the user requested by
     * delegating to {@link #extractPageFromParameter(HttpServletRequest)}.
     * </p>
     * <p>
     * This method will <em>always</em>return a {@link WikiActionBeanContext}
     * that is properly instantiated. The supplied request and response objects
     * will be associated with the WikiActionBeanContext. The
     * <code>requestContext</code>is required. If either the
     * <code>request</code> or <code>response</code> parameters are
     * <code>null</code>, appropriate mock objects will be substituted
     * instead.
     * </p>
     * <p>
     * This method performs a similar role to the Stripes
     * {@link net.sourceforge.stripes.controller.ActionBeanContextFactory#getContextInstance(HttpServletRequest, HttpServletResponse)}
     * method, in the sense that it will instantiate an arbitrary
     * ActionBeanContext class. However, although this method will correctly
     * identity the page requested by the user (by inspecting request
     * parameters), it will not do anything special if the page is a "special
     * page."
     * </p>
     * 
     * @param request the HTTP request
     * @param response the HTTP request
     * @param requestContext the request context to use by default
     * @return the new WikiActionBeanContext
     */
    public WikiActionBeanContext newContext( HttpServletRequest request, HttpServletResponse response, String requestContext )
                                                                                                                              throws WikiException
    {
        return createContext( requestContext, request, response, null );
    }

    /**
     * <p>
     * Creates a new WikiActionBeanContext for the given HttpServletRequest,
     * HttpServletResponse and WikiPage, using the {@link WikiContext#VIEW}
     * request context. Similar to method
     * {@link #newContext(HttpServletRequest, HttpServletResponse, String)},
     * this method will <em>always</em>return a {@link WikiActionBeanContext}
     * that is properly instantiated. The supplied request and response objects
     * will be associated with the WikiActionBeanContext. If either the
     * <code>request</code> or <code>response</code> parameters are
     * <code>null</code>, appropriate mock objects will be substituted
     * instead.
     * </p>
     * 
     * @param request The HttpServletRequest that should be associated with this
     *            context. This parameter may be <code>null</code>.
     * @param response The HttpServletResponse that should be associated with
     *            this context. This parameter may be <code>null</code>.
     * @param page The WikiPage. If you want to create a WikiContext for an
     *            older version of a page, you must supply this parameter
     * @return the new WikiActionBeanContext
     */
    public WikiActionBeanContext newViewContext( HttpServletRequest request, HttpServletResponse response, WikiPage page )
    {
        // Create a new "view" WikiActionBeanContext, and swallow any exceptions
        WikiActionBeanContext ctx = null;
        try
        {
            ctx = createContext( WikiContext.VIEW, request, response, page );
            if( ctx == null )
            {
                throw new IllegalStateException( "Could not create new WikiContext of type VIEW! This indicates a bug..." );
            }
        }
        catch( WikiException e )
        {
            e.printStackTrace();
            log.error( e.getMessage() );
        }
        return ctx;
    }

    /**
     * Searches a set of named packages for WikiActionBean implementations, and
     * returns any it finds.
     * 
     * @param beanPackages the packages to search on the current classpath,
     *            separated by commas
     * @return the discovered classes
     */
    private Set<Class<? extends WikiActionBean>> findBeanClasses( String[] beanPackages )
    {
        ResolverUtil<WikiActionBean> resolver = new ResolverUtil<WikiActionBean>();
        resolver.findImplementations( WikiActionBean.class, beanPackages );
        return resolver.getClasses();
    }

    /**
     * Initializes the internal map that matches wiki request contexts with
     * HandlerInfo objects.
     * 
     * @param properties
     */
    private void initRequestContextMap( Properties properties )
    {
        // Look up all classes that are WikiActionBeans.
        String beanPackagesProp = properties.getProperty( PROPS_ACTIONBEAN_PACKAGES, DEFAULT_ACTIONBEAN_PACKAGES ).trim();
        String[] beanPackages = beanPackagesProp.split( "," );
        Set<Class<? extends WikiActionBean>> beanClasses = findBeanClasses( beanPackages );

        // Stash the contexts and corresponding classes into a Map.
        for( Class<? extends WikiActionBean> beanClass : beanClasses )
        {
            Map<Method, HandlerInfo> handlerMethods = HandlerInfo.getHandlerInfoCollection( beanClass );
            for( HandlerInfo handler : handlerMethods.values() )
            {
                String requestContext = handler.getRequestContext();
                if( m_contextMap.containsKey( requestContext ) )
                {
                    HandlerInfo duplicateHandler = m_contextMap.get( requestContext );
                    log.error( "Bean class " + beanClass.getCanonicalName() + " contains @WikiRequestContext annotation '"
                               + requestContext + "' that duplicates one already declared for "
                               + duplicateHandler.getActionBeanClass() );
                }
                else
                {
                    m_contextMap.put( requestContext, handler );
                    log.info( "Discovered request context '" + requestContext + "' for WikiActionBean="
                              + beanClass.getCanonicalName() + ",event=" + handler.getEventName() );
                }
            }
        }
    }

    /**
     * Skims through a supplied set of Properties and looks for anything with
     * the "special page" prefix, and creates Stripes
     * {@link net.sourceforge.stripes.action.RedirectResolution} objects for any
     * that are found.
     */
    private void initSpecialPageRedirects( Properties properties )
    {
        for( Map.Entry entry : properties.entrySet() )
        {
            String key = (String) entry.getKey();
            if( key.startsWith( PROP_SPECIALPAGE ) )
            {
                String specialPage = key.substring( PROP_SPECIALPAGE.length() );
                String redirectUrl = (String) entry.getValue();
                if( specialPage != null && redirectUrl != null )
                {
                    specialPage = specialPage.trim();
                    redirectUrl = redirectUrl.trim();
                    RedirectResolution resolution = m_specialRedirects.get( specialPage );
                    if( resolution == null )
                    {
                        resolution = new RedirectResolution( redirectUrl );
                        m_specialRedirects.put( specialPage, resolution );
                    }
                }
            }
        }
    }

    /**
     * Creates and returns a new WikiActionBean based on a supplied class, with
     * an instantiated {@link WikiActionBeanContext}. The
     * WikiActionBeanContext's request and response properties will be set to
     * those supplied by the caller; if not supplied, synthetic instances will
     * be substituted.
     * 
     * @param requestContext the request context to use by default
     * @param request
     * @param response
     * @return the newly instantiated bean
     */
    protected WikiActionBeanContext createContext( String requestContext, HttpServletRequest request, HttpServletResponse response,
                                                   WikiPage page ) throws WikiException
    {
        // Create synthetic request if not supplied
        if( request == null )
        {
            request = new MockHttpServletRequest( m_mockContextPath, "/Wiki.jsp" );
            MockHttpSession session = new MockHttpSession( m_engine.getServletContext() );
            ((MockHttpServletRequest) request).setSession( session );
        }

        // Create synthetic response if not supplied
        if( response == null )
        {
            response = new MockHttpServletResponse();
        }
        // Create the WikiActionBeanContext and set all of its relevant
        // properties
        WikiActionBeanContext context = new WikiActionBeanContext();
        context.setRequest( request );
        context.setResponse( response );
        context.setEngine( m_engine );
        context.setServletContext( m_engine.getServletContext() );
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        context.setWikiSession( wikiSession );

        // Set the request context (and related event name)
        context.setRequestContext( requestContext );

        // Extract and set the WikiPage
        if( page == null )
        {
            String pageName = extractPageFromParameter( request );

            // For view action, default to front page
            if( pageName == null && WikiContext.VIEW.equals( requestContext ) )
            {
                pageName = m_engine.getFrontPage();
            }

            // Make sure the page is resolved properly (taking into account
            // funny plurals)
            if( pageName != null )
            {
                page = resolvePage( request, pageName );
            }
        }

        if( page != null )
        {
            context.setPage( page );
        }

        return context;
    }

    /**
     * <p>
     * Determines the correct wiki page based on a supplied HTTP request. This
     * method attempts to determine the page requested by a user, taking into
     * account special pages. The resolution algorithm will extract the page
     * name from the URL by looking for the first parameter value returned for
     * the <code>page</code> parameter. If a page name was, in fact, passed in
     * the request, this method the correct name after taking into account
     * potential plural matches.
     * </p>
     * <p>
     * If neither of these methods work, or if the request is <code>null</code>
     * this method returns <code>null</code>
     * </p>.
     * 
     * @param request the HTTP request
     * @return the resolved page name
     */
    protected final String extractPageFromParameter( HttpServletRequest request )
    {
        // Corner case when request == null
        if( request == null )
        {
            return null;
        }

        // Extract the page name from the URL directly
        String[] pages = request.getParameterValues( "page" );
        String page = null;
        if( pages != null && pages.length > 0 )
        {
            page = pages[0];
            try
            {
                // Look for singular/plural variants; if one
                // not found, take the one the user supplied
                String finalPage = getFinalPageName( page );
                if( finalPage != null )
                {
                    page = finalPage;
                }
            }
            catch( ProviderException e )
            {
                // FIXME: Should not ignore!
            }
            return page;
        }

        // Didn't resolve; return null
        return page;
    }

    /**
     * Looks up and returns the correct, versioned WikiPage based on a supplied
     * page name and optional <code>version</code> parameter passed in an HTTP
     * request. If the <code>version</code> parameter does not exist in the
     * request, the latest version is returned.
     * 
     * @param request the HTTP request
     * @param page the name of the page to look up; this page <em>must</em>
     *            exist
     * @return the wiki page
     */
    protected final WikiPage resolvePage( HttpServletRequest request, String page )
    {
        // See if the user included a version parameter
        WikiPage wikipage;
        int version = WikiProvider.LATEST_VERSION;
        String rev = request.getParameter( "version" );

        if( rev != null )
        {
            version = Integer.parseInt( rev );
        }

        wikipage = m_engine.getPage( page, version );

        if( wikipage == null )
        {
            page = MarkupParser.cleanLink( page );
            wikipage = new WikiPage( m_engine, page );
        }
        return wikipage;
    }

    /**
     * Determines whether a "page" exists by examining the list of special pages
     * and querying the page manager.
     * 
     * @param page the page to seek
     * @return <code>true</code> if the page exists, <code>false</code>
     *         otherwise
     */
    protected final boolean simplePageExists( String page ) throws ProviderException
    {
        if( m_specialRedirects.containsKey( page ) )
        {
            return true;
        }
        return m_engine.getPageManager().pageExists( page );
    }
}
