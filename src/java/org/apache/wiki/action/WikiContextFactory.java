/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.action;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.controller.ActionResolver;
import net.sourceforge.stripes.controller.AnnotatedClassActionResolver;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.controller.UrlBindingFactory;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageAlreadyExistsException;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerInfo;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.url.StripesURLConstructor;


/**
 * <p>
 * Class that looks up {@link WikiActionBean}s, and resolves special pages and
 * JSPs on behalf of a WikiEngine. WikiActionBeanResolver will automatically
 * resolve page names with singular/plural variants. It can also detect the
 * correct WikiActionBean based on parameters supplied in an HTTP request, or
 * due to the JSP being accessed.
 * </p>
 * 
 * @since 2.4.22
 */
public final class WikiContextFactory
{
    private static final String ATTR_CONTEXT = "wikiContext";

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

    private static final Logger log = LoggerFactory.getLogger( WikiContextFactory.class );

    private static final long serialVersionUID = 1L;

   /**
     * This method can be used to find the WikiContext programmatically from a
     * JSP PageContext. We check the request context. The wiki context, if it
     * exists, is looked up using the key
     * {@link #ATTR_CONTEXT}.
     * 
     * @since 2.4
     * @param pageContext the JSP page context
     * @return Current WikiContext, or null, of no context exists.
     */
    public static WikiContext findContext( PageContext pageContext )
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        WikiContext context = (WikiContext) request.getAttribute( ATTR_CONTEXT );
        return context;
    }

    /**
     * <p>
     * Saves the supplied WikiContext, and the related WikiEngine and
     * WikiSession, in request scope. The WikiContext is saved as an attribute
     * named {@link #ATTR_CONTEXT}.
     * </p>
     * <p>
     * Note: when the WikiContext is saved, it will be guaranteed to have a
     * non-null WikiPage. If the context as supplied has a WikiPage that is
     * <code>null</code>, the
     * {@link org.apache.wiki.WikiEngine#getFrontPage()} will be consulted,
     * and that page will be used.
     * </p>
     * 
     * @param request the HTTP request
     * @param context the WikiContext to save
     */
    public static void saveContext( ServletRequest request, WikiContext context )
    {
        request.setAttribute( ATTR_CONTEXT, context );
    }

    private final WikiEngine m_engine;

    private String m_mockContextPath;

    private boolean m_contextMap_inited = false;

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

        // Set the path prefix for constructing synthetic Stripes mock requests;
        // trailing slash is removed.
        m_mockContextPath = StripesURLConstructor.getContextPath( engine );
    }

    /**
     * Looks up and returns the correct HandlerInfo class corresponding to a
     * supplied wiki context. The supplied context name is matched against the
     * values annotated using
     * {@link org.apache.wiki.ui.stripes.WikiRequestContext}. If a match is not
     * found, this method throws an IllegalArgumentException.
     * 
     * @param requestContext the context to look up
     * @return the WikiActionBean event handler info
     */
    public HandlerInfo findEventHandler( String requestContext )
    {
        if ( !m_contextMap_inited )
        {
            initRequestContextMap();
        }
        HandlerInfo handler = m_contextMap.get( requestContext );
        if( handler == null )
        {
            throw new IllegalArgumentException( "No HandlerInfo found for request context '" + requestContext + "'!" );
        }
        return handler;
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
        if ( !m_contextMap_inited )
        {
            initRequestContextMap();
        }

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
     *  Provides a clean shortcut to newViewContext(null,null,page).
     * 
     *  @param page The WikiPage object for this page.
     *  @return A new WikiPage.
     */
    public WikiActionBeanContext newViewContext( WikiPage page )
    {
        return newViewContext( null, null, page );
    }
    
    /**
     * Initializes the internal map that matches wiki request contexts with
     * HandlerInfo objects. The internal map is lazily inited, because it
     * <em>must</em> happen after the StripesFilter initializes.
     */
    private void initRequestContextMap()
    {
        Configuration stripesConfig = StripesFilter.getConfiguration();
        if ( stripesConfig == null )
        {
            throw new InternalWikiException( "Could not obtain Stripes configuration. FATAL." );
        }
        ActionResolver resolver = stripesConfig.getActionResolver();
        if ( resolver instanceof AnnotatedClassActionResolver )
        {
            UrlBindingFactory urlBindings = ((AnnotatedClassActionResolver)resolver).getUrlBindingFactory();
            Collection<Class<? extends ActionBean>> beanClasses = urlBindings.getActionBeanClasses();

            // Stash the contexts and corresponding classes into a Map.
            for( Class<? extends ActionBean> beanClass : beanClasses )
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
                        log.debug( "Discovered request context '" + requestContext + "' for WikiActionBean="
                                   + beanClass.getCanonicalName() + ",event=" + handler.getEventName() );
                    }
                }
            }
        }
        m_contextMap_inited = true;
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
        context.setServletContext( m_engine.getServletContext() );

        // Set the request context (and related event name)
        context.setRequestContext( requestContext );

        // Run the login stack
        m_engine.getAuthenticationManager().login( request );

        // Extract and set the WikiPage
        if( page == null )
        {
            WikiPath pageName = extractPageFromParameter( request );

            // For view action, default to front page
            if( pageName == null && WikiContext.VIEW.equals( requestContext ) )
            {
                page = m_engine.getFrontPage(null);
            }

            // Make sure the page is resolved properly (taking into account
            // funny plurals)
            if( pageName != null )
            {
                try
                {
                    page = resolvePage( request, pageName );
                }
                catch( PageNotFoundException e )
                {
                    // If we can't find the page, it must not exist yet (which is ok)
                }
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
    protected final WikiPath extractPageFromParameter( HttpServletRequest request )
    {
        // Corner case when request == null
        if( request == null )
        {
            return null;
        }

        // Extract the page name from the URL directly
        String[] pages = request.getParameterValues( "page" );
        WikiPath page = null;
        if( pages != null && pages.length > 0 )
        {
            page = WikiPath.valueOf(pages[0]);
            try
            {
                // Look for variants
                WikiPath finalPage = m_engine.getFinalPageName( page );
                
                // If no variant, use whatever the user supplied.
                if( finalPage == null ) return page;
                
                return finalPage;
            }
            catch( ProviderException e )
            {
                // FIXME: Should not ignore!
            }
        }

        // Didn't resolve; return null
        return null;
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
    protected final WikiPage resolvePage( HttpServletRequest request, WikiPath page ) throws PageNotFoundException, ProviderException
    {
        // See if the user included a version parameter
        int version = WikiProvider.LATEST_VERSION;
        String rev = request.getParameter( "version" );

        if( rev != null )
        {
            version = Integer.parseInt( rev );
        }

        try
        {
            return m_engine.getContentManager().getPage( page, version );
        }
        catch( PageNotFoundException e )
        {
            String pageName = MarkupParser.cleanLink( page.getPath() );
            try
            {
                return m_engine.createPage( WikiPath.valueOf( pageName ) );
            }
            catch( PageAlreadyExistsException e1 )
            {
                // This should not happen
                throw new ProviderException( e1.getMessage(), e1 );
            }
        }
    }

}
