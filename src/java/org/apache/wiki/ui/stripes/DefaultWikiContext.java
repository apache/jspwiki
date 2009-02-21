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
package org.apache.wiki.ui.stripes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.permissions.AllPermission;


/**
 * <p>
 * Default implementation of {@link org.apache.wiki.WikiContext}.
 * </p>
 * 
 * @author Andrew R. Jaquith
 * @since 3.0
 */
public class DefaultWikiContext implements WikiContext
{
    private WikiPage m_page;

    private WikiPage m_realPage;

    private WikiEngine m_engine;

    private String m_template = "default";

    private HashMap<String, Object> m_variableMap = new HashMap<String, Object>();

    private String m_requestContext = null;

    private WikiSession m_session = null;

    /**
     * Stores the HttpServletRequest. May be null, if the request did not come
     * from a servlet.
     */
    protected HttpServletRequest m_request = null;

    /**
     * Returns a shallow clone of the WikiContext.
     * 
     * @since 2.1.37.
     * @return A shallow clone of the WikiContext
     */
    public Object clone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited
            // objects
            // get the right type
            DefaultWikiContext copy = (DefaultWikiContext) super.clone();

            copy.m_engine = m_engine;

            copy.m_template = m_template;
            copy.m_variableMap = m_variableMap;
            copy.m_request = m_request;
            copy.m_session = m_session;
            copy.m_page = m_page;
            copy.m_realPage = m_realPage;
            return copy;
        }
        catch( CloneNotSupportedException e )
        {
        } // Never happens

        return null;
    }

    /**
     * Creates a deep clone of the WikiContext. This is useful when you want to
     * be sure that you don't accidentally mess with page attributes, etc.
     * 
     * @since 2.8.0
     * @return A deep clone of the WikiContext.
     */
    @SuppressWarnings("unchecked")
    public WikiContext deepClone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited
            // objects
            // get the right type
            DefaultWikiContext copy = (DefaultWikiContext) super.clone();

            // No need to deep clone these
            copy.m_engine = m_engine;

            copy.m_template = m_template;
            copy.m_variableMap = (HashMap<String, Object>) m_variableMap.clone();
            copy.m_request = m_request;
            copy.m_session = m_session;
            copy.m_page = (WikiPage) m_page.clone();
            copy.m_realPage = (WikiPage) m_realPage.clone();
            return copy;
        }
        catch( CloneNotSupportedException e )
        {
        } // Never happens

        return null;
    }

    /**
     * Locates the i18n ResourceBundle, based on the {@link java.util.Locale} object
     * returned by {@link javax.servlet.http.HttpServletRequest#getLocale()}.
     * 
     * @see org.apache.wiki.i18n.InternationalizationManager
     * @param bundle The name of the bundle you are looking for.
     * @return A resource bundle object
     * @throws MissingResourceException If the bundle cannot be found
     */
    // FIXME: This method should really cache the ResourceBundles or
    // something...
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException
    {
        Locale loc = getHttpRequest().getLocale();

        ResourceBundle b = m_engine.getInternationalizationManager().getBundle( bundle, loc );

        return b;
    }

    /**
     * Convenience method that gets the current user. Delegates the lookup to
     * the WikiSession associated with this WikiContect. May return null, in
     * case the current user has not yet been determined; or this is an internal
     * system. If the WikiSession has not been set, <em>always</em> returns
     * null.
     * 
     * @return The current user; or maybe null in case of internal calls.
     */
    public Principal getCurrentUser()
    {
        if( m_session == null )
        {
            // This shouldn't happen, really...
            return WikiPrincipal.GUEST;
        }
        return m_session.getUserPrincipal();
    }

    /**
     * Returns the handling engine.
     * 
     * @return The wikiengine owning this context.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     * This method will safely return any HTTP parameters that might have been
     * defined. You should use this method instead of peeking directly into the
     * result of getHttpRequest(), since this method is smart enough to do all
     * of the right things, figure out UTF-8 encoded parameters, etc.
     * 
     * @since 2.0.13.
     * @param paramName Parameter name to look for.
     * @return HTTP parameter, or null, if no such parameter existed.
     */
    public String getHttpParameter( String paramName )
    {
        String result = null;

        if( m_request != null )
        {
            result = m_request.getParameter( paramName );
        }

        return result;
    }

    /**
     * If the request did originate from a HTTP request, then the HTTP request
     * can be fetched here. However, it the request did NOT originate from a
     * HTTP request, then this method will return null, and YOU SHOULD CHECK FOR
     * IT!
     * 
     * @return Null, if no HTTP request was done.
     * @since 2.0.13.
     */
    public HttpServletRequest getHttpRequest()
    {
        return m_request;
    }

    /**
     * Returns the page that is being handled.
     * 
     * @return the page which was fetched.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     * Gets a reference to the real page whose content is currently being
     * rendered. If your plugin e.g. does some variable setting, be aware that
     * if it is embedded in the LeftMenu or some other page added with
     * InsertPageTag, you should consider what you want to do - do you wish to
     * really reference the "master" page or the included page.
     * <p>
     * For example, in the default template, there is a page called "LeftMenu".
     * Whenever you access a page, e.g. "Main", the master page will be Main,
     * and that's what the getPage() will return - regardless of whether your
     * plugin resides on the LeftMenu or on the Main page. However,
     * getRealPage() will return "LeftMenu".
     * 
     * @return A reference to the real page.
     * @see org.apache.wiki.tags.InsertPageTag
     * @see org.apache.wiki.parser.JSPWikiMarkupParser
     */
    public WikiPage getRealPage()
    {
        return m_realPage;
    }

    /**
     * Returns the request context.
     * 
     * @return The name of the request context (e.g. VIEW).
     */
    public String getRequestContext()
    {
        return m_requestContext;
    }

    /**
     * Gets the template that is to be used throughout this request.
     * 
     * @since 2.1.15.
     * @return template name
     */
    public String getTemplate()
    {
        return m_template;
    }

    /**
     * Creates an URL for the given request context.
     * 
     * @param context e.g. WikiContext.EDIT
     * @param page The page to which to link
     * @return An URL to the page, honours the absolute/relative setting in
     *         jspwiki.properties
     */
    public String getURL( String context, String page )
    {
        return getURL( context, page, null );
    }

    /**
     * Returns an URL from a page. It this WikiContext instance was constructed
     * with an actual HttpServletRequest, we will attempt to construct the URL
     * using HttpUtil, which preserves the HTTPS portion if it was used.
     * 
     * @param context The request context (e.g. WikiContext.UPLOAD)
     * @param page The page to which to link
     * @param params A list of parameters, separated with "&amp;"
     * @return An URL to the given context and page.
     */
    public String getURL( String context, String page, String params )
    {
        boolean absolute = "absolute".equals( m_engine.getVariable( this, WikiEngine.PROP_REFSTYLE ) );

        // FIXME: is rather slow
        return m_engine.getURL( context, page, params, absolute );

    }

    /**
     * Gets a previously set variable.
     * 
     * @param key The variable name.
     * @return The variable contents.
     */
    public Object getVariable( String key )
    {
        return m_variableMap.get( key );
    }

    /**
     * A shortcut to generate a VIEW url.
     * 
     * @param page The page to which to link.
     * @return An URL to the page. This honours the current absolute/relative
     *         setting.
     */
    public String getViewURL( String page )
    {
        return getURL( VIEW, page, null );
    }

    /**
     * Returns the WikiSession associated with the context. This method is
     * guaranteed to always return a valid WikiSession. If this context was
     * constructed without an associated HttpServletRequest, it will return
     * {@link WikiSession#guestSession(WikiEngine)}.
     * 
     * @return The WikiSession associate with this context.
     */
    public WikiSession getWikiSession()
    {
        return m_session;
    }

    /**
     * Returns true, if the current user has administrative permissions (i.e.
     * the omnipotent AllPermission).
     * 
     * @since 2.4.46
     * @return true, if the user has all permissions.
     */
    public boolean hasAdminPermissions()
    {
        boolean admin = false;

        WikiEngine engine = getEngine();
        admin = engine.getAuthorizationManager().checkPermission( getWikiSession(),
                                                                  new AllPermission( engine.getApplicationName() ) );

        return admin;
    }

    /**
     * Sets the page that is being handled. If the "real page" is <code>null</code>, it is
     * set to the same value also.
     * 
     * @param page The wikipage
     * @since 2.1.37.
     */
    public void setPage( WikiPage page )
    {
        m_page = page;
        if ( m_realPage == null )
        {
            m_realPage = page;
        }
    }

    /**
     * Sets a reference to the real page whose content is currently being
     * rendered.
     * <p>
     * Sometimes you may want to render the page using some other page's
     * context. In those cases, it is highly recommended that you set the
     * setRealPage() to point at the real page you are rendering. Please see
     * InsertPageTag for an example.
     * <p>
     * Also, if your plugin e.g. does some variable setting, be aware that if it
     * is embedded in the LeftMenu or some other page added with InsertPageTag,
     * you should consider what you want to do - do you wish to really reference
     * the "master" page or the included page.
     * 
     * @param page The real page which is being rendered.
     * @return The previous real page
     * @since 2.3.14
     * @see org.apache.wiki.tags.InsertPageTag
     */
    public WikiPage setRealPage( WikiPage page )
    {
        WikiPage old = m_realPage;
        m_realPage = page;
        return old;
    }

    /**
     * Sets the request context. See above for the different request contexts
     * (VIEW, EDIT, etc.)
     * 
     * @param arg The request context (one of the predefined contexts.)
     */
    public void setRequestContext( String arg )
    {
        m_requestContext = arg;
    }

    /**
     * Sets the template to be used for this request.
     * 
     * @param dir The template name
     * @since 2.1.15.
     */
    public void setTemplate( String dir )
    {
        m_template = dir;
    }

    /**
     * Sets a variable. The variable is valid while the WikiContext is valid,
     * i.e. while page processing continues. The variable data is discarded once
     * the page processing is finished.
     * 
     * @param key The variable name.
     * @param data The variable value.
     */
    public void setVariable( String key, Object data )
    {
        m_variableMap.put( key, data );
    }

    /**
     * Sets the WikiEngine for this WikiContext. This method <em>must</em> be called
     * as soon as possible after instantiation.
     * @param engine the WikiEngine
     */
    protected void setEngine( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    /**
     * Sets the HttpServletRequest for this WikiContext. This method <em>must</em> be called
     * as soon as possible after instantiation.
     * @param request the HTTP request object
     */
    protected void setHttpRequest( HttpServletRequest request )
    {
        m_request = request;
    }

    /**
     * Sets the WikiSession for this WikiContext. This method <em>must</em> be called
     * as soon as possible after instantiation.
     * @param wikiSession the WikiSession
     */
    protected void setWikiSession( WikiSession wikiSession )
    {
        m_session = wikiSession;
    }
}
