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
package com.ecyrd.jspwiki;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;

import net.sourceforge.stripes.validation.Validate;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.action.*;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;
import com.ecyrd.jspwiki.tags.WikiTagBase;
import com.ecyrd.jspwiki.preferences.Preferences;

/**
 *  <p>Provides state information throughout the processing of a page.  A
 *  WikiContext is born when the JSP pages that are the main entry
 *  points, are invoked.  The JSPWiki engine creates the new
 *  WikiContext, which basically holds information about the page, the
 *  handling engine, and in which context (view, edit, etc) the
 *  call was done.</p>
 *  <p>A WikiContext also provides request-specific variables, which can
 *  be used to communicate between plugins on the same page, or
 *  between different instances of the same plugin.  A WikiContext
 *  variable is valid until the processing of the page has ended.  For
 *  an example, please see the Counter plugin.</p>
 *  <p>When a WikiContext is created, it automatically associates a
 *  {@link WikiSession} object with the user's HttpSession. The
 *  WikiSession contains information about the user's authentication
 *  status, and is consulted by {@link #getCurrentUser()}.
 *  object</p>
 *  <p>Do not cache the page object that you get from the WikiContext; always
 *  use getPage()!</p>
 *
 *  @see com.ecyrd.jspwiki.plugin.Counter
 *
 *  @author Andrew R. Jaquith
 */
public abstract class WikiContext extends AbstractActionBean
    implements Cloneable
{
    private    WikiPage   m_page;
    private    WikiPage   m_realPage;
    private    WikiEngine m_engine;
    private    String     m_template = "default";

    private    HashMap<String,Object> m_variableMap = new HashMap<String,Object>();

    /**
     *  Stores the HttpServletRequest.  May be null, if the request did not
     *  come from a servlet.
     */
    protected  HttpServletRequest m_request = null;

    private    WikiSession m_session = null;

    /** User is administering JSPWiki (Install, SecurityConfig). */
    public static final String    INSTALL  = HandlerInfo.getHandlerInfo( InstallActionBean.class, "install" ).getRequestContext();

    /** The VIEW context - the user just wants to view the page
        contents. */
    public static final String    VIEW     = HandlerInfo.getHandlerInfo( ViewActionBean.class, "view" ).getRequestContext();

    /** User wants to view or administer workflows. */
    public static final String    WORKFLOW = HandlerInfo.getHandlerInfo( WorkflowActionBean.class, "view" ).getRequestContext();

    /** The EDIT context - the user is editing the page. */
    public static final String    EDIT     = HandlerInfo.getHandlerInfo( EditActionBean.class, "edit" ).getRequestContext();

    /** User is preparing for a login/authentication. */
    public static final String    LOGIN    = HandlerInfo.getHandlerInfo( LoginActionBean.class, "login" ).getRequestContext();

    /** User is preparing to log out. */
    public static final String    LOGOUT   = HandlerInfo.getHandlerInfo( LoginActionBean.class, "logout" ).getRequestContext();

    /** JSPWiki wants to display a message. */
    public static final String    MESSAGE  = HandlerInfo.getHandlerInfo( MessageActionBean.class, "message" ).getRequestContext();

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String    DIFF     = HandlerInfo.getHandlerInfo( EditActionBean.class, "diff" ).getRequestContext();

    /** User is viewing page history. */
    public static final String    INFO     = HandlerInfo.getHandlerInfo( PageInfoActionBean.class, "info" ).getRequestContext();

    /** User is previewing the changes he just made. */
    public static final String    PREVIEW  = HandlerInfo.getHandlerInfo( EditActionBean.class, "preview" ).getRequestContext();

    /** User has an internal conflict, and does quite not know what to
        do. Please provide some counseling. */
    public static final String    CONFLICT = HandlerInfo.getHandlerInfo( PageModifiedActionBean.class, "conflict" ).getRequestContext();

    /** An error has been encountered and the user needs to be informed. */
    public static final String    ERROR    = HandlerInfo.getHandlerInfo( ErrorActionBean.class, "error" ).getRequestContext();

    /** User is uploading something. */
    public static final String    UPLOAD   = HandlerInfo.getHandlerInfo( UploadActionBean.class, "upload" ).getRequestContext();

    /** User is commenting something. */
    public static final String    COMMENT  = HandlerInfo.getHandlerInfo( CommentActionBean.class, "comment" ).getRequestContext();

    /** User is searching for content. */
    public static final String    FIND     = HandlerInfo.getHandlerInfo( SearchActionBean.class, "find" ).getRequestContext();

    /** User wishes to create a new group */
    public static final String    CREATE_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "create" ).getRequestContext();

    /** User is deleting an existing group. */
    public static final String    DELETE_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "delete" ).getRequestContext();

    /** User is editing an existing group. */
    public static final String    EDIT_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "save" ).getRequestContext();

    /** User is viewing an existing group */
    public static final String    VIEW_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "view" ).getRequestContext();

    /** User is editing preferences */
    public static final String    PREFS    = HandlerInfo.getHandlerInfo( UserPreferencesActionBean.class, "createAssertedName" ).getRequestContext();

    /** User is renaming a page. */
    public static final String    RENAME   = HandlerInfo.getHandlerInfo( RenameActionBean.class, "rename" ).getRequestContext();

    /** User is deleting a page or an attachment. */
    public static final String    DELETE   = HandlerInfo.getHandlerInfo( DeleteActionBean.class, "delete" ).getRequestContext();

    /** User is downloading an attachment. */
    public static final String    ATTACH   = HandlerInfo.getHandlerInfo( AttachActionBean.class, "upload" ).getRequestContext();

    /** RSS feed is being generated. */
    public static final String    RSS      = HandlerInfo.getHandlerInfo( RSSActionBean.class, "rss" ).getRequestContext();

    /** This is not a JSPWiki context, use it to access static files. */
    public static final String    NONE     = "none";  

    /** Same as NONE; this is just a clarification. */
    public static final String    OTHER    = "other";

    /** User is doing administrative things. */
    public static final String    ADMIN    = "admin";

    private static final Logger   log      = Logger.getLogger( WikiContext.class );

    /**
     * Creates a new WikiContext, without a WikiEngine, Request or WikiPage. This constructor should
     * <em>never be called;</em> factory methods such as {@link WikiEngine#createContext(HttpServletRequest, String)}
     * and {@link WikiActionBeanFactory#newActionBean(HttpServletRequest, HttpServletResponse, Class)} should
     * be used instead.
     */
    public WikiContext()
    {
        super();
    }

    /**
     *  Sets a reference to the real page whose content is currently being
     *  rendered.
     *  <p>
     *  Sometimes you may want to render the page using some other page's context.
     *  In those cases, it is highly recommended that you set the setRealPage()
     *  to point at the real page you are rendering.  Please see InsertPageTag
     *  for an example.
     *  <p>
     *  Also, if your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *
     *  @param page  The real page which is being rendered.
     *  @return The previous real page
     *  @since 2.3.14
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     */
    public WikiPage setRealPage( WikiPage page )
    {
        WikiPage old = m_realPage;
        m_realPage = page;
        return old;
    }

    /**
     *  Gets a reference to the real page whose content is currently being rendered.
     *  If your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *  <p>
     *  For example, in the default template, there is a page called "LeftMenu".
     *  Whenever you access a page, e.g. "Main", the master page will be Main, and
     *  that's what the getPage() will return - regardless of whether your plugin
     *  resides on the LeftMenu or on the Main page.  However, getRealPage()
     *  will return "LeftMenu".
     *
     *  @return A reference to the real page.
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     *  @see com.ecyrd.jspwiki.parser.JSPWikiMarkupParser
     */
    public WikiPage getRealPage()
    {
        return m_realPage;
    }

    /**
     *  Returns the handling engine.
     *
     *  @return The wikiengine owning this context.
     */
    public WikiEngine getEngine()
    {
        return super.getEngine();
    }

    /**
     *  Returns the page that is being handled.
     *
     *  @return the page which was fetched.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     *  Sets the page that is being handled.
     *
     *  @param page The wikipage
     *  @since 2.1.37.
     */
    @Validate(required = true)
    public void setPage( WikiPage page )
    {
        m_page = page;
        m_realPage = m_page;
    }

    /**
     *  Returns the request context.
     *  @return The name of the request context (e.g. VIEW).
     */
    public String getRequestContext()
    {
        return super.getRequestContext();
    }

    /**
     * Sets the request context. See above for the different request contexts
     * (VIEW, EDIT, etc.) This argument must correspond exactly to the value of
     * a Stripes event handler method's
     * {@link com.ecyrd.jspwiki.action.WikiRequestContext} annotation for the
     * bean class. For event handlers that do not have an
     * {@linkplain com.ecyrd.jspwiki.action.WikiRequestContext} annotation,
     * callers can supply a request context value based on the bean class and
     * the event name; see the
     * {@link com.ecyrd.jspwiki.action.HandlerInfo#getRequestContext()}
     * documentation for more details.
     * 
     * @param arg The request context (one of the predefined contexts.)
     * @throws if the supplied request context does not correspond
     * to a {@linkplain com.ecyrd.jspwiki.action.WikiRequestContext}
     * annotation, or the automatically request context name
     */
    public void setRequestContext( String arg )
    {
        super.setRequestContext( arg );
    }

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable( String key )
    {
        return super.getVariable( key );
    }

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    public void setVariable( String key, Object data )
    {
        super.setVariable( key, data );
    }

    /**
     *  This method will safely return any HTTP parameters that
     *  might have been defined.  You should use this method instead
     *  of peeking directly into the result of getHttpRequest(), since
     *  this method is smart enough to do all of the right things,
     *  figure out UTF-8 encoded parameters, etc.
     *
     *  @since 2.0.13.
     *  @param paramName Parameter name to look for.
     *  @return HTTP parameter, or null, if no such parameter existed.
     */
    public String getHttpParameter( String paramName )
    {
        return super.getHttpParameter( paramName );
    }

    /**
     *  If the request did originate from a HTTP request,
     *  then the HTTP request can be fetched here.  However, it the request
     *  did NOT originate from a HTTP request, then this method will
     *  return null, and YOU SHOULD CHECK FOR IT!
     *
     *  @return Null, if no HTTP request was done.
     *  @since 2.0.13.
     */
    public HttpServletRequest getHttpRequest()
    {
        return super.getHttpRequest();
    }

    /**
     *  Sets the template to be used for this request.
     *
     *  @param dir The template name
     *  @since 2.1.15.
     */
    public void setTemplate( String dir )
    {
        super.setTemplate( dir );
    }

    /**
     * Returns the name of the WikiPage associated with this wiki context.
     * @return the page name
     */
    public String getName()
    {
        return m_page != null ? m_page.getName() : "<no page>";
    }

    /**
     *  Gets the template that is to be used throughout this request.
     *  @since 2.1.15.
     *  @return template name
     */
    public String getTemplate()
    {
        return super.getTemplate();
    }

    /**
     *  Convenience method that gets the current user. Delegates the
     *  lookup to the WikiSession associated with this WikiContect.
     *  May return null, in case the current
     *  user has not yet been determined; or this is an internal system.
     *  If the WikiSession has not been set, <em>always</em> returns null.
     *
     *  @return The current user; or maybe null in case of internal calls.
     */
    public Principal getCurrentUser()
    {
        return super.getCurrentUser();
    }

    /**
     *  A shortcut to generate a VIEW url.
     *
     *  @param page The page to which to link.
     *  @return An URL to the page.  This honours the current absolute/relative setting.
     */
    public String getViewURL( String page )
    {
        return getURL( VIEW, page, null );
    }

    /**
     *  Creates an URL for the given request context.
     *
     *  @param context e.g. WikiContext.EDIT
     *  @param page The page to which to link
     *  @return An URL to the page, honours the absolute/relative setting in jspwiki.properties
     */
    public String getURL( String context,
                          String page )
    {
        return getURL( context, page, null );
    }

    /**
     *  Returns an URL from a page. It this WikiContext instance was constructed
     *  with an actual HttpServletRequest, we will attempt to construct the
     *  URL using HttpUtil, which preserves the HTTPS portion if it was used.
     *
     *  @param context The request context (e.g. WikiContext.UPLOAD)
     *  @param page    The page to which to link
     *  @param params  A list of parameters, separated with "&amp;"
     *
     *  @return An URL to the given context and page.
     */
    public String getURL( String context,
                          String page,
                          String params )
    {
        boolean absolute = "absolute".equals( getEngine().getVariable( this, WikiEngine.PROP_REFSTYLE ) );

        // FIXME: is rather slow
        return getEngine().getURL( context,
                                page,
                                params,
                                absolute );

    }

    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     *  @return A shallow clone of the WikiContext
     */
    public Object clone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            WikiContext copy = (WikiContext)super.clone();

            copy.m_variableMap    = m_variableMap;
            copy.m_page           = m_page;
            copy.m_realPage       = m_realPage;
            WikiActionBeanContext context = getContext();
            copy.setContext( context );
            String template = getTemplate();
            copy.setTemplate( template );
            return copy;
        }
        catch( CloneNotSupportedException e ){} // Never happens

        return null;
    }

    /**
     *  Creates a deep clone of the WikiContext.  This is useful when you want
     *  to be sure that you don't accidentally mess with page attributes, etc.
     *  
     *  @since  2.8.0
     *  @return A deep clone of the WikiContext.
     */
    public WikiContext deepClone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            WikiContext copy = (WikiContext)super.clone();

            //  No need to deep clone these
            copy.m_engine  = m_engine;

            copy.m_template       = m_template;
            copy.m_variableMap    = (HashMap<String,Object>)m_variableMap.clone();
            copy.m_request        = m_request;
            copy.m_session        = m_session;
            copy.m_page           = (WikiPage)m_page.clone();
            copy.m_realPage       = (WikiPage)m_realPage.clone();
            return copy;
        }
        catch( CloneNotSupportedException e ){} // Never happens

        return null;
    }
    
    /**
     *  Returns the WikiSession associated with the context.
     *  This method is guaranteed to always return a valid WikiSession.
     *  If this context was constructed without an associated
     *  HttpServletRequest, it will return {@link WikiSession#guestSession(WikiEngine)}.
     *
     *  @return The WikiSession associate with this context.
     */
    public WikiSession getWikiSession()
    {
        return super.getWikiSession();
    }

    /**
     *  This method can be used to find the WikiContext programmatically
     *  from a JSP PageContext. We check the request context. 
     *  The wiki context, if it exists,
     *  is looked up using the key
     *  {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     *
     *  @since 2.4
     *  @param pageContext the JSP page context
     *  @return Current WikiContext, or null, of no context exists.
     */
    public static WikiContext findContext( PageContext pageContext )
    {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        WikiContext context = (WikiContext)request.getAttribute( WikiTagBase.ATTR_CONTEXT );
        return context;
    }

    /**
     * Checks whether the current user has access to this wiki context,
     * by obtaining the required Permission (see {@link HandlerInfo#getPermission(WikiActionBean)})
     * and delegating the access check to
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as an attribute
     * with the key name {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * Note that this method will automatically redirect the user to
     * a login or error page, as appropriate, if access fails. This is
     * NOT guaranteed to be default behavior in the future.
     * @param response the http response
     * @return the result of the access check
     * @throws IOException In case something goes wrong
     * @deprecated callers should not need to call this method, and should rely on {@link WikiInterceptor} instead to control access
     */
    public boolean hasAccess( HttpServletResponse response ) throws IOException
    {
        return hasAccess( response, true );
    }

    /**
     * Checks whether the current user has access to this wiki context (and
     * optionally redirects if not), by obtaining the required Permission (see {@link HandlerInfo#getPermission(WikiActionBean)})
     * and delegating the access check to
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as attribute
     * with the key name {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * Note: for 3.0, this method obtains the required permission by looking up its permission information
     * by calling {@link com.ecyrd.jspwiki.action.HandlerInfo#getDefaultHandlerInfo(Class)}.
     * The method that contains the {@link net.sourceforge.stripes.action.DefaultHandler} annotation
     * is the one that will be used to determine the correct permission. If no method containing a
     * DefaultHandler is found, this method returns <code>true</code>.
     * @return the result of the access check
     * @param response The servlet response object
     * @param redirect If true, makes an automatic redirect to the response
     * @throws IOException If something goes wrong
     * @deprecated callers should not need to call this method, and should rely on {@link WikiInterceptor} instead to control access
     */
    public boolean hasAccess( HttpServletResponse response, boolean redirect ) throws IOException
    {
        // Look up the HandlerInfo for the current Stripes event handler
        String eventMethod = getContext().getEventName();
        HandlerInfo handlerInfo;
        if ( eventMethod == null )
        {
            handlerInfo = HandlerInfo.getDefaultHandlerInfo( this.getClass() );
        }
        else
        {
            handlerInfo = HandlerInfo.getHandlerInfo( this.getClass(), eventMethod );
        }
        
        // Get the required permission
        Permission requiredPermission = null;
        try
        {
            requiredPermission = handlerInfo.getPermission( this );
        }
        catch ( ELException e ) {
            log.error( "Could not evaluate event handler permission for  " + this.getClass() + ", method=" + handlerInfo.getHandlerMethod().getName() );
        }
        
        WikiSession wikiSession = getWikiSession();
        AuthorizationManager mgr = getEngine().getAuthorizationManager();
        boolean allowed = requiredPermission == null ? true : mgr.checkPermission( getWikiSession(), requiredPermission );
        ResourceBundle rb = getBundle( InternationalizationManager.CORE_BUNDLE );

        // Stash the wiki context
        if( allowed )
        {
            if ( getContext().getRequest().getAttribute( WikiTagBase.ATTR_CONTEXT ) == null )
            {
                getContext().getRequest().setAttribute( WikiTagBase.ATTR_CONTEXT, this );
            }
        }

        // If access not allowed, redirect
        if( !allowed && redirect )
        {
            Principal currentUser  = wikiSession.getUserPrincipal();
            Object[] arguments = { getName() };
            if( wikiSession.isAuthenticated() )
            {
                log.info("User "+currentUser.getName()+" has no access - forbidden (permission=" + requiredPermission + ")" );
                String pageurl = m_page.getName();
                wikiSession.addMessage( MessageFormat.format( rb.getString("security.error.noaccess.logged"), arguments) );
                response.sendRedirect( getEngine().getURL(WikiContext.LOGIN, pageurl, null, false ) );
            }
            else
            {
                log.info("User "+currentUser.getName()+" has no access - redirecting (permission=" + requiredPermission + ")");
                String pageurl = m_page.getName();
                wikiSession.addMessage( MessageFormat.format( rb.getString("security.error.noaccess"), arguments) );
                response.sendRedirect( getEngine().getURL(WikiContext.LOGIN, pageurl, null, false ) );
            }
        }
        return allowed;
    }

    /**
     *  Returns true, if the current user has administrative permissions (i.e. the omnipotent
     *  AllPermission).
     *
     *  @since 2.4.46
     *  @return true, if the user has all permissions.
     */
    public boolean hasAdminPermissions()
    {
        boolean admin = false;

        WikiEngine m_engine = getEngine();
        admin = m_engine.getAuthorizationManager().checkPermission( getWikiSession(),
                                                                    new AllPermission(m_engine.getApplicationName()) );

        return admin;
    }

    /**
     *  Locates the i18n ResourceBundle given.  This method interprets
     *  the request locale, and uses that to figure out which language the
     *  user wants.
     *  @see com.ecyrd.jspwiki.i18n.InternationalizationManager
     *  @param bundle The name of the bundle you are looking for.
     *  @return A resource bundle object
     *  @throws MissingResourceException If the bundle cannot be found
     */
    // FIXME: This method should really cache the ResourceBundles or something...
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException
    {
        return super.getBundle( bundle );
    }

    /**
     *  Returns the locale of the HTTP request if available,
     *  otherwise returns the default Locale of the server.
     *
     *  @return A valid locale object
     *  @param context The WikiContext
     *  @deprecated use {@link com.ecyrd.jspwiki.action.WikiActionBeanContext#getLocale()} instead
     */
    public static Locale getLocale( WikiContext context )
    {
        return context.getContext().getLocale();
/*
        HttpServletRequest request = context.getHttpRequest();
        return ( request != null )
                ? request.getLocale() : Locale.getDefault();
*/
    }

}
