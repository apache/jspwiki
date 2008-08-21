package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.controller.StripesConstants;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.util.ResolverUtil;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.tags.WikiTagBase;
import com.ecyrd.jspwiki.url.StripesURLConstructor;

/**
 * <p>
 * Class that resolves special pages and JSPs on behalf of a WikiEngine.
 * WikiActionBeanResolver will automatically resolve page names with
 * singular/plural variants. It can also detect the correct Command based on
 * parameters supplied in an HTTP request, or due to the JSP being accessed.
 * </p>
 * <p>
 * <p>
 * WikiActionBeanResolver's static {@link #findCommand(String)} method is the
 * simplest method; it looks up and returns the Command matching a supplied wiki
 * context. For example, looking up the request context <code>view</code>
 * returns {@link PageCommand#VIEW}. Use this method to obtain static Command
 * instances that aren't targeted at a particular page or group.
 * </p>
 * <p>
 * For more complex lookups in which the caller supplies an HTTP request,
 * {@link #findCommand(HttpServletRequest, String)} will look up and return the
 * correct Command. The String parameter <code>defaultContext</code> supplies
 * the request context to use if it cannot be detected. However, note that the
 * default wiki context may be over-ridden if the request was for a "special
 * page."
 * </p>
 * <p>
 * For example, suppose the WikiEngine's properties specify a special page
 * called <code>UserPrefs</code> that redirects to
 * <code>UserPreferences.jsp</code>. The ordinary lookup method
 * {@linkplain #findCommand(String)} using a supplied context <code>view</code>
 * would return {@link PageCommand#VIEW}. But the
 * {@linkplain #findCommand(HttpServletRequest, String)} method, when passed the
 * same context (<code>view</code>) and an HTTP request containing the page
 * parameter value <code>UserPrefs</code>, will instead return
 * {@link WikiCommand#PREFS}.
 * </p>
 * 
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public final class WikiActionBeanFactory
{
    /**
     * The PageContext attribute name of the WikiActionBean stored by
     * WikiInterceptor.
     */
    public static final String ATTR_ACTIONBEAN = "wikiActionBean";

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
    
    private static final Logger log = Logger.getLogger( WikiActionBeanFactory.class );

    private static final long serialVersionUID = 1L;

    /** Prefix in jspwiki.properties signifying special page keys. */
    private static final String PROP_SPECIALPAGE = "jspwiki.specialPage.";

    /** Default list of packages to search for WikiActionBean implementations. */
    private static final String DEFAULT_ACTIONBEAN_PACKAGES = "com.ecyrd.jspwiki.action";

    /** Property in jspwiki.properties that specifies packages to search for WikiActionBean implementations. */
    private static final String PROPS_ACTIONBEAN_PACKAGES = "jspwiki.actionBean.packages";

    /** Private map with JSPs as keys, Resolutions as values */
    private final Map<String, RedirectResolution> m_specialRedirects;

    private final WikiEngine m_engine;

    private String m_mockContextPath;

    /** If true, we'll also consider english plurals (+s) a match. */
    private boolean m_matchEnglishPlurals;

    /** Maps (pre-3.0) request contexts map to WikiActionBeans. */
    private final Map<String, HandlerInfo> m_contextMap = new HashMap<String, HandlerInfo>();

    /**
     * Initializes the internal map that matches wiki request contexts with HandlerInfo objects.
     * @param properties
     */
    private void initRequestContextMap( Properties properties )
    {
        // Look up all classes that are WikiActionBeans.
        String beanPackagesProp = properties.getProperty( PROPS_ACTIONBEAN_PACKAGES, DEFAULT_ACTIONBEAN_PACKAGES ).trim();
        String[] beanPackages = beanPackagesProp.split( "," );
        Set<Class<? extends WikiActionBean>> beanClasses = findBeanClasses( beanPackages);

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
     * Searches a set of named packages for WikiActionBean implementations, and returns any it finds.
     * @param beanPackages the packages to search on the current classpath, separated by commas
     * @return the discovered classes
     */
    private Set<Class<? extends WikiActionBean>> findBeanClasses( String[] beanPackages )
    {
        ResolverUtil<WikiActionBean> resolver = new ResolverUtil<WikiActionBean>();
        resolver.findImplementations( WikiActionBean.class, beanPackages );
        return resolver.getClasses();
    }

    /**
     * Looks up and returns the correct HandlerInfo class corresponding to a
     * supplied wiki context. The supplied context name is matched against the
     * values annotated using
     * {@link com.ecyrd.jspwiki.action.WikiRequestContext}. If a match is not
     * found, this method throws an IllegalArgumentException.
     * 
     * @param context the context to look up
     * @return the WikiActionBean event handler info
     */
    public HandlerInfo findEventHandler( String context )
    {
        HandlerInfo handler = m_contextMap.get( context );
        if( handler == null )
        {
            throw new IllegalArgumentException( "No HandlerInfo found for request context '" + context + "'!" );
        }
        return handler;
    }

    /**
     * Constructs a WikiActionBeanResolver for a given WikiEngine. This
     * constructor will extract the special page references for this wiki and
     * store them in a cache used for resolution.
     * 
     * @param engine the wiki engine
     * @param properties the properties used to initialize the wiki
     */
    public WikiActionBeanFactory( WikiEngine engine, Properties properties )
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
     * Skims through a supplied set of Properties and looks for anything with the "special page"
     * prefix, and creates Stripes {@link net.sourceforge.stripes.action.RedirectResolution} objects
     * for any that are found.
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
     * Creates a WikiActionBean instance, associates an HTTP request and
     * response with it, and incorporates the correct WikiPage into the bean if
     * required. This method will determine what page the user requested by
     * delegating to
     * {@link #extractPageFromParameter(String, HttpServletRequest)}.
     * </p>
     * <p>
     * This method will <em>always</em>return a WikiActionBean that is
     * properly instantiated. It will also create a new {@WikiActionBeanContext}
     * and associate it with the action bean. The supplied request and response
     * objects will be associated with the WikiActionBeanContext. The
     * <code>beanClass</code>is required. If either the <code>request</code>
     * or <code>response</code> parameters are <code>null</code>,
     * appropriate mock objects will be substituted instead.
     * </p>
     * <p>
     * This method performs a similar role to the &lt;stripes:useActionBean&gt;
     * tag, in the sense that it will instantiate an arbitrary WikiActionBean
     * class and, in the case of WikiContext subclasses, bind a WikiPage to it.
     * However, it lacks some of the capabilities the JSP tag possesses. For
     * example, although this method will correctly identity the page requested
     * by the user (by inspecting request parameters), it will not do anything
     * special if the page is a "special page." If special page resolution and
     * redirection is required, use the &lt;stripes:useActionBean&gt; JSP tag
     * instead.
     * </p>
     * 
     * @param request the HTTP request
     * @param response the HTTP request
     * @param beanClass the request context to use by default</code>
     * @return the resolved wiki action bean
     * @see net.sourceforge.stripes.tag.UseActionBeanTag
     */
    public WikiActionBean newActionBean( HttpServletRequest request, HttpServletResponse response,
                                         Class<? extends WikiActionBean> beanClass ) throws WikiException
    {
        return newInstance( beanClass, request, response, null );
    }

    /**
     * Creates a new ViewActionBean for the given WikiEngine, WikiPage and
     * HttpServletRequest. This method performs a similar role to the
     * &lt;stripes:useActionBean&gt; tag, in the sense that it will instantiate
     * an arbitrary WikiActionBean class and, in the case of WikiContext
     * subclasses, bind a WikiPage to it. However, it lacks some of the
     * capabilities the JSP tag possesses. For example, although this method
     * will correctly identity the page requested by the user (by inspecting
     * request parameters), it will not do anything special if the page is a
     * "special page." If special page resolution and redirection is required,
     * use the &lt;stripes:useActionBean&gt; JSP tag instead.
     * 
     * @param request The HttpServletRequest that should be associated with this
     *            context. This parameter may be <code>null</code>.
     * @param response The HttpServletResponse that should be associated with
     *            this context. This parameter may be <code>null</code>.
     * @param page The WikiPage. If you want to create a WikiContext for an
     *            older version of a page, you must supply this parameter
     * @see net.sourceforge.stripes.tag.UseActionBeanTag
     */
    public ViewActionBean newViewActionBean( HttpServletRequest request, HttpServletResponse response, WikiPage page )
    {
        // Create a new "view" ActionBean, and swallow any exceptions
        ViewActionBean bean = null;
        try
        {
            bean = (ViewActionBean) newInstance( ViewActionBean.class, request, response, page );
            if( bean == null )
            {
                throw new IllegalStateException( "Could not create new ViewActionBean! This indicates a bug..." );
            }
        }
        catch( WikiException e )
        {
            e.printStackTrace();
            log.error( e.getMessage() );
        }
        return bean;
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
     * Creates and returns a new WikiActionBean based on a supplied class, with
     * an instantiated {@link WikiActionBeanContext}. The
     * WikiActionBeanContext's request and response properties will be set to
     * those supplied by the caller; if not supplied, synthetic instances will
     * be substituted.
     * 
     * @param beanClass the bean class that should be newly instantiated
     * @param request
     * @param response
     * @return the newly instantiated bean
     */
    protected WikiActionBean newInstance( Class<? extends WikiActionBean> beanClass, HttpServletRequest request,
                                          HttpServletResponse response, WikiPage page ) throws WikiException
    {
        // Instantiate the ActionBean first
        WikiActionBean bean = null;
        if( beanClass == null )
        {
            throw new IllegalArgumentException( "Bean class cannot be null!" );
        }
        {
            try
            {
                bean = beanClass.newInstance();
            }
            catch( Exception e )
            {
                throw new WikiException( "Could not create ActionBean: " + e.getMessage() );
            }
        }

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

        // Create the WikiActionBeanContext and set all of its relevant properties
        WikiActionBeanContext actionBeanContext = new WikiActionBeanContext();
        bean.setContext( actionBeanContext );
        actionBeanContext.setRequest( request );
        actionBeanContext.setResponse( response );
        actionBeanContext.setWikiEngine( m_engine );
        actionBeanContext.setServletContext( m_engine.getServletContext() );
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( request.getSession() );
        actionBeanContext.setWikiSession( wikiSession );

        // Set the event name for this action bean to the default handler
        actionBeanContext.setEventName( HandlerInfo.getDefaultHandlerInfo( beanClass ).getEventName() );

        // If ActionBean is a WikiContext, extract and set the WikiPage
        if( bean instanceof WikiContext )
        {
            if( page == null )
            {
                String pageName = extractPageFromParameter( request );

                // For view action, default to front page
                if( pageName == null && bean instanceof ViewActionBean )
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
                ((WikiContext) bean).setPage( page );
            }
        }

        return bean;
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

    /**
     * Returns the WikiActionBean associated with the current
     * {@link javax.servlet.http.HttpServletRequest}. The ActionBean will be
     * retrieved from attribute {@link WikiInterceptor#ATTR_ACTIONBEAN}. If
     * an ActionBean is not found under this name, the standard Stripes  attribute
     * {@link net.sourceforge.stripes.controller.StripesConstants#REQ_ATTR_ACTION_BEAN}
     * will be attempted.
     * 
     * @param pageContext the
     * @return the WikiActionBean
     * @throws IllegalStateException if the WikiActionBean was not found in the
     *         request scope
     */
    public static WikiActionBean findActionBean( ServletRequest request )
    {
        WikiActionBean bean = (WikiActionBean) request.getAttribute( ATTR_ACTIONBEAN );
        if ( bean == null )
        {
            log.debug( "WikiActionBean not found under request attribute '" + ATTR_ACTIONBEAN +
                       "'; trying standard Stripes attribute '" + StripesConstants.REQ_ATTR_ACTION_BEAN + "'." );
            bean = (WikiActionBean) request.getAttribute( StripesConstants.REQ_ATTR_ACTION_BEAN  );
        }
        
        if ( bean == null )
        {
            throw new IllegalStateException( "WikiActionBean not found in request! Something failed to stash it..." );
        }
        return bean;
    }

    /**
     * <p>
     * Saves the supplied WikiActionBean and its associated WikiPage as
     * in request scope. The action bean is saved as an
     * attribute named {@link WikiInterceptor#ATTR_ACTIONBEAN}. If the action
     * bean was also a WikiContext instance, it is saved as an attribute named
     * {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}. Among other
     * things, by saving these items as attributes, they can be accessed via JSP
     * Expression Language variables, in this case
     * <code>${wikiActionBean}</code> and <code>${wikiContext}</code>
     * respectively.
     * </p>
     * <p>
     * Note: the WikiPage set by this method is guaranteed to be non-null. If
     * the WikiActionBean is not a WikiContext, or it is a WikiContext but its
     * WikiPage is <code>null</code>, the
     * {@link com.ecyrd.jspwiki.WikiEngine#getFrontPage()} will be consulted,
     * and that page will be used.
     * </p>
     * 
     * @param request the HTTP request
     * @param actionBean the WikiActionBean to save
     */
    public static void saveActionBean( HttpServletRequest request, WikiActionBean actionBean )
    {
        // Stash WikiEngine as a request attribute (can be
        // used later as ${wikiEngine} in EL markup)
        WikiEngine engine = actionBean.getEngine();
        request.setAttribute( ATTR_WIKIENGINE, engine );
        
        // Stash the WikiSession as a request attribute
        WikiSession wikiSession = SessionMonitor.getInstance( engine ).find( request.getSession() );
        request.setAttribute( ATTR_WIKISESSION, wikiSession );
        
        // Stash the WikiActionBean
        request.setAttribute( ATTR_ACTIONBEAN, actionBean );

        // Stash it again as a WikiContext (or synthesize a fake one)
        WikiContext wikiContext;
        WikiPage page;
        if( actionBean instanceof WikiContext )
        {
            wikiContext = (WikiContext) actionBean;
            page = wikiContext.getPage();
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
                wikiContext.setPage( page );
            }
        }
        else
        {
            HttpServletResponse response = actionBean.getContext().getResponse();
            page = engine.getPage( engine.getFrontPage() );
            wikiContext = engine.getWikiActionBeanFactory().newViewActionBean( request, response, page );
        }
        request.setAttribute( WikiTagBase.ATTR_CONTEXT, wikiContext );

        // Debug messages
        if( log.isDebugEnabled() )
        {
            log.debug( "Stashed WikiActionBean '" + actionBean + "' in page scope." );
            log.debug( "Stashed WikiPage '" + page.getName() + "' in page scope." );
        }

    }

}
