/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.tags.WikiTagBase;
import com.ecyrd.jspwiki.ui.Command;
import com.ecyrd.jspwiki.ui.GroupCommand;
import com.ecyrd.jspwiki.ui.Installer;
import com.ecyrd.jspwiki.ui.PageCommand;
import com.ecyrd.jspwiki.ui.CommandResolver;
import com.ecyrd.jspwiki.ui.WikiCommand;

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
 *
 *  @see com.ecyrd.jspwiki.plugin.Counter
 *  
 *  @author Janne Jalkanen
 *  @author Andrew R. Jaquith
 */
public class WikiContext
    implements Cloneable, Command
{
    private    Command m_command = null;
    
    WikiPage   m_page;
    WikiPage   m_realPage;
    WikiEngine m_engine;
    String     m_template = "default";

    Map        m_variableMap = new HashMap();

    HttpServletRequest m_request = null;

    WikiSession m_session = null;

    /** User is administering JSPWiki (Install, SecurityConfig). */
    public static final String    INSTALL  = WikiCommand.INSTALL.getRequestContext();
    
    /** The VIEW context - the user just wants to view the page
        contents. */
    public static final String    VIEW     = PageCommand.VIEW.getRequestContext();

    /** The EDIT context - the user is editing the page. */
    public static final String    EDIT     = PageCommand.EDIT.getRequestContext();

    /** User is preparing for a login/authentication. */
    public static final String    LOGIN    = WikiCommand.LOGIN.getRequestContext();

    /** User is preparing to log out. */
    public static final String    LOGOUT   = WikiCommand.LOGOUT.getRequestContext();

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String    DIFF     = PageCommand.DIFF.getRequestContext();

    /** User is viewing page history. */
    public static final String    INFO     = PageCommand.INFO.getRequestContext();

    /** User is previewing the changes he just made. */
    public static final String    PREVIEW  = PageCommand.PREVIEW.getRequestContext();

    /** User has an internal conflict, and does quite not know what to
        do. Please provide some counseling. */
    public static final String    CONFLICT = PageCommand.CONFLICT.getRequestContext();

    /** An error has been encountered and the user needs to be informed. */
    public static final String    ERROR    = WikiCommand.ERROR.getRequestContext();

    public static final String    UPLOAD   = PageCommand.UPLOAD.getRequestContext();

    public static final String    COMMENT  = PageCommand.COMMENT.getRequestContext();
    
    public static final String    FIND     = WikiCommand.FIND.getRequestContext();

    /** User wishes to create a new group */
    public static final String    CREATE_GROUP = WikiCommand.CREATE_GROUP.getRequestContext();
    
    /** User is deleting an existing group. */
    public static final String    DELETE_GROUP = GroupCommand.DELETE_GROUP.getRequestContext();
    
    /** User is editing an existing group. */
    public static final String    EDIT_GROUP = GroupCommand.EDIT_GROUP.getRequestContext();
    
    /** User is viewing an existing group */
    public static final String    VIEW_GROUP = GroupCommand.VIEW_GROUP.getRequestContext();
    
    public static final String    PREFS    = WikiCommand.PREFS.getRequestContext();
    
    public static final String    RENAME   = PageCommand.RENAME.getRequestContext();
    public static final String    DELETE   = PageCommand.DELETE.getRequestContext();
    public static final String    ATTACH   = PageCommand.ATTACH.getRequestContext();
    public static final String    RSS      = PageCommand.RSS.getRequestContext();

    /** This is not a JSPWiki context, use it to access static files. */
    public static final String    NONE     = PageCommand.NONE.getRequestContext();  
    
    /** Same as NONE; this is just a clarification. */
    public static final String    OTHER    = PageCommand.OTHER.getRequestContext();
    
    protected static final Logger log      = Logger.getLogger( WikiContext.class );
    
    private static final Permission DUMMY_PERMISSION  = new java.util.PropertyPermission( "os.name", "read" );

    /**
     *  Create a new WikiContext for the given WikiPage. Delegates to
     * {@link #WikiContext(WikiEngine, HttpServletRequest, WikiPage)}.
     *  @param engine The WikiEngine that is handling the request.
     *  @param page   The WikiPage.  If you want to create a
     *  WikiContext for an older version of a page, you must use this
     *  constructor. 
     */
    public WikiContext( WikiEngine engine, WikiPage page )
    {
        this( engine, null, findCommand( engine, null, page ) );
    }
    
    /**
     * <p>
     * Creates a new WikiContext for the given WikiEngine, Command and
     * HttpServletRequest.
     * </p>
     * <p>
     * This constructor will also look up the HttpSession associated with the
     * request, and determine if a WikiSession object is present. If not, a new
     * one is created.
     * </p>
     * <p>
     * After the WikiSession object is obtained, the current authentication
     * status is checked. If not authenticated, or if the login status reported
     * by the container has changed, the constructor attempts to log in the user
     * with
     * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#login(HttpServletRequest)}.
     * If an login process throws an exception, this method logs the error but
     * does not re-throw it.
     * </p>
     * @param engine The WikiEngine that is handling the request
     * @param request The HttpServletRequest that should be associated with this
     *            context. This parameter may be <code>null</code>.
     * @param command the command
     * @throws IllegalArgumentException if <code>engine</code> or
     *             <code>command</code> are <code>null</code>
     */
    public WikiContext( WikiEngine engine, HttpServletRequest request, Command command )
    {
        super();
        if ( engine == null || command == null )
        {
            throw new IllegalArgumentException( "Parameter engine and command must not be null." );
        }
        
        m_engine = engine;
        m_request = request;
        m_session = WikiSession.getWikiSession( engine, request );
        m_command = command;
        
        // If PageCommand, get the WikiPage
        if ( command instanceof PageCommand )
        {
            m_page = (WikiPage)((PageCommand)command).getTarget();
        }
        
        // If page not supplied, default to front page to avoid NPEs
        if ( m_page == null )
        {
            m_page = m_engine.getPage( m_engine.getFrontPage() );
        }
        
        m_realPage = m_page;
        
        // Log in the user if new session or the container status changed
        boolean doLogin = ( (request != null) && m_session.isNew() );
        
        // Debugging...
        if ( log.isDebugEnabled() )
        {
            HttpSession session = ( request == null ) ? null : request.getSession( false );
            String sid = ( session == null ) ? "(null)" : session.getId();
            log.debug( "Creating WikiContext for session ID=" + sid + "; target=" + getName() );
            log.debug( "Do we need to log the user in? " + doLogin );
        }
        
        if ( doLogin || m_session.isContainerStatusChanged( request ) )
        {
            try 
            {
                engine.getAuthenticationManager().login( request );
            }
            catch ( WikiSecurityException e )
            {
                // Login failed because config was screwy
                log.error( "Could not log in user: " + e.getMessage() );
            }
        }

        // Mark the session as "not new"
        if ( m_session.isNew() )
        {
            m_session.setNew( false );
        }
        
        // Figure out what template to use
        setDefaultTemplate( request );
    }
    
    /**
     * Creates a new WikiContext for the given WikiEngine, WikiPage and
     * HttpServletRequest. This method simply looks up the appropriate Command
     * using {@link #findCommand(WikiEngine, HttpServletRequest, WikiPage)} and
     * delegates to
     * {@link #WikiContext(WikiEngine, HttpServletRequest, Command)}.
     * @param engine The WikiEngine that is handling the request
     * @param request The HttpServletRequest that should be associated with this
     *            context. This parameter may be <code>null</code>.
     * @param page The WikiPage. If you want to create a WikiContext for an
     *            older version of a page, you must supply this parameter
     */
    public WikiContext(WikiEngine engine, HttpServletRequest request, WikiPage page) 
    {
        this( engine, request, findCommand( engine, request, page ) );
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getContentTemplate()
     */
    public String getContentTemplate()
    {
        return m_command.getContentTemplate();
    }
    
    /**
     * @see com.ecyrd.jspwiki.ui.Command#getJSP()
     */
    public String getJSP()
    {
        return m_command.getContentTemplate();
    }

    /**
     *  Sometimes you may want to render the page using some other page's context.
     *  In those cases, it is highly recommended that you set the setRealPage()
     *  to point at the real page you are rendering.
     *  
     *  @param page  The real page which is being rendered.
     *  @since 2.3.14
     *  @return The previous real page
     */
    public WikiPage setRealPage( WikiPage page )
    {
        WikiPage old = m_realPage;
        m_realPage = page;
        updateCommand( m_command.getRequestContext() );
        return old;
    }
    
    public WikiPage getRealPage()
    {
        return m_realPage;
    }
    
    /**
     *  Figure out to which page we are really going to.  Considers
     *  special page names from the jspwiki.properties, and possible aliases.
     *  This method forwards requests to 
     *  {@link com.ecyrd.jspwiki.ui.CommandResolver#getSpecialPageReference(String)}.
     *  @return A complete URL to the new page to redirect to
     *  @since 2.2
     */

    public String getRedirectURL()
    {
        String pagename = m_page.getName();
        String redirURL = null;
        
        redirURL = m_engine.getCommandResolver().getSpecialPageReference( pagename );

        if( redirURL == null )
        {
            String alias = (String)m_page.getAttribute( WikiPage.ALIAS );
            
            if( alias != null )
            {
                redirURL = getViewURL( alias );
            }
            else
            {
                redirURL = (String)m_page.getAttribute( WikiPage.REDIRECT );
            }
        }

        return redirURL;
    }
    
    /**
     *  Returns the handling engine.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     *  Returns the page that is being handled.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     *  Sets the page that is being handled.
     *
     *  @since 2.1.37.
     */
    public void setPage( WikiPage page )
    {
        m_page = page;
        updateCommand( m_command.getRequestContext() );
    }

    /**
     *  Returns the request context.
     */
    public String getRequestContext()
    {
        return m_command.getRequestContext();
    }

    /**
     *  Sets the request context.  See above for the different
     *  request contexts (VIEW, EDIT, etc.)
     *
     *  @param arg The request context (one of the predefined contexts.)
     */
    public void setRequestContext( String arg )
    {
        updateCommand( arg );
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getTarget()
     */
    public Object getTarget()
    {
        return m_command.getTarget();
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getURLPattern()
     */
    public String getURLPattern()
    {
        return m_command.getURLPattern();
    }

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable( String key )
    {
        return m_variableMap.get( key );
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
        m_variableMap.put( key, data );
        updateCommand( m_command.getRequestContext() );
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
        String result = null;

        if( m_request != null )
        {
            result = m_request.getParameter( paramName );
        }

        return result;
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
        return m_request;
    }

    /**
     *  Sets the template to be used for this request.
     *  @since 2.1.15.
     */
    public void setTemplate( String dir )
    {
        m_template = dir;
    }

    /**
     * Returns the target of this wiki context: a page, group name or JSP. If
     * the associated Command is a PageCommand, this method returns the page's
     * name. Otherwise, this method delegates to the associated Command's
     * {@link com.ecyrd.jspwiki.ui.Command#getName()} method. Calling classes
     * can rely on the results of this method for looking up canonically-correct
     * page or group names. Because it does not automatically assume that the
     * wiki context is a PageCommand, calling this method is inherently safer
     * than calling <code>getPage().getName()</code>.
     * @return the name of the target of this wiki context
     * @see com.ecyrd.jspwiki.ui.PageCommand#getName()
     * @see com.ecyrd.jspwiki.ui.GroupCommand#getName()
     */
    public String getName()
    {
        if ( m_command instanceof PageCommand )
        {
            return m_page.getName();
        }
        return m_command.getName();
    }
    
    /**
     *  Gets the template that is to be used throughout this request.
     *  @since 2.1.15.
     */
    public String getTemplate()
    {
        return m_template;
    }

    /**
     *  Convenience method that gets the current user. Delegates the
     *  lookup to the WikiSession associated with this WikiContect. 
     *  May return null, in case the current
     *  user has not yet been determined; or this is an internal system.
     *  If the WikiSession has not been set, <em>always</em> returns null.
     */
    public Principal getCurrentUser()
    {
        if (m_session == null) 
        {
            // This shouldn't happen, really...
            return WikiPrincipal.GUEST;
        }
        return m_session.getUserPrincipal();
    }

    public String getViewURL( String page )
    {
        return getURL( VIEW, page, null );
    }

    public String getURL( String context,
                          String page )
    {
        return getURL( context, page, null );
    }

    /**
     *  Returns an URL from a page. It this WikiContext instance was constructed
     *  with an actual HttpServletRequest, we will attempt to construct the
     *  URL using HttpUtil, which preserves the HTTPS portion if it was used.
     */
    public String getURL( String context,
                          String page,
                          String params )
    {
        boolean absolute = "absolute".equals(m_engine.getVariable( this, WikiEngine.PROP_REFSTYLE ));

        // FIXME: is rather slow
        return m_engine.getURL( context,
                                page,
                                params,
                                absolute );

    }

    /**
     * Returns the Command associated with this WikiContext.
     * @return the command
     */
    public Command getCommand()
    {
        return m_command;
    }
    
    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     */
    public Object clone()
    {
        try
        {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            WikiContext copy = (WikiContext)super.clone();
        
            copy.m_engine = m_engine;
            copy.m_command = m_command;
        
            copy.m_template       = m_template;
            copy.m_variableMap    = m_variableMap;
            copy.m_request        = m_request;
            copy.m_session        = m_session;
            copy.m_page           = m_page;
            copy.m_realPage       = m_realPage;
            return copy;
        }
        catch( CloneNotSupportedException e ){} // Never happens
        
        return null;
    }
    
    /**
     * Returns the WikiSession associated with the context.
     * This method is guaranteed to always return a valid WikiSession. 
     * If this context was constructed without an associated 
     * HttpServletRequest, it will return {@link WikiSession#guestSession(WikiEngine)}.
     */  
    public WikiSession getWikiSession() 
    {
        return m_session;
    }
    
    /**
     *  This method can be used to find the WikiContext programmatically
     *  from a JSP PageContext. We check the page context scope
     *  first, then the request context. The wiki context, if it exists,
     *  is looked up using the key 
     *  {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     *  
     *  @since 2.4
     * @param pageContext the JSP page context
     * @return Current WikiContext, or null, of no context exists.
     */
    public static WikiContext findContext( PageContext pageContext )
    {
        // Try the page context first, then the request
        WikiContext context = (WikiContext)pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT, PageContext.PAGE_SCOPE );
        if ( context == null )
        {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            context = (WikiContext)request.getAttribute( WikiTagBase.ATTR_CONTEXT );
        }
        return context;
    }
    
    /**
     * Returns the permission required to successfully execute this context. 
     * For example, the a wiki context of VIEW for a certain page means that
     * the PagePermission "view" is required for the page. In some cases, no
     * particular permission is required, in which case a dummy permission will
     * be returned ({@link java.util.PropertyPermission}<code> "os.name",
     * "read"</code>). This method is guaranteed to always return a valid, 
     * non-null permission.
     * @return the permission
     * @since 2.4
     */
    public Permission requiredPermission()
    {
        // This is a filthy rotten hack -- absolutely putrid
        if ( WikiCommand.INSTALL.equals( m_command ) )
        {
            // See if admin users exists
            boolean adminExists = false;
            try
            {
                UserManager userMgr = m_engine.getUserManager();
                UserDatabase userDb = userMgr.getUserDatabase();
                userDb.findByLoginName( Installer.ADMIN_ID );
                adminExists = true;
            }
            catch ( NoSuchPrincipalException e )
            {
                return DUMMY_PERMISSION;
            }
            if ( adminExists )
            {
                return new AllPermission( m_engine.getApplicationName() );
            }
        }
        
        // TODO: we should really break the contract so that this
        // method returns null, but until then we will use this hack
        if ( m_command.requiredPermission() == null )
        {
            return DUMMY_PERMISSION;
        }
        
        return m_command.requiredPermission();
    }
    
    /**
     * Associates a target with the current Command and returns
     * the new targeted Command. If the Command associated with this
     * WikiContext is already "targeted", it is returned instead.
     * @see com.ecyrd.jspwiki.ui.Command#targetedCommand(java.lang.Object)
     */
    public Command targetedCommand( Object target )
    {
        if ( m_command.getTarget() == null )
        {
            return m_command.targetedCommand( target );
        }
        return m_command;
    }

    /**
     * Checks whether the current user has access to this wiki context,
     * by obtaining the required Permission ({@link #requiredPermission()})
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
     */
    public boolean hasAccess( HttpServletResponse response ) throws IOException
    {
        return hasAccess( response, true );
    }

    /**
     * Checks whether the current user has access to this wiki context (and
     * optionally redirects if not), by obtaining the required Permission ({@link #requiredPermission()})
     * and delegating the access check to
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as attribute
     * with the key name {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * @return the result of the access check
     */
    public boolean hasAccess( HttpServletResponse response, boolean redirect ) throws IOException
    {
        AuthorizationManager mgr = m_engine.getAuthorizationManager();
        boolean allowed = mgr.checkPermission( m_session, requiredPermission() );
        
        // Stash the wiki context
        if( allowed )
        {
            if ( m_request != null && m_request.getAttribute( WikiTagBase.ATTR_CONTEXT ) == null )
            {
                m_request.setAttribute( WikiTagBase.ATTR_CONTEXT, this );
            }
        }
        
        // If access not allowed, redirect
        if( !allowed && redirect )
        {
            Principal currentUser  = m_session.getUserPrincipal();
            if( m_session.isAuthenticated() )
            {
                log.info("User "+currentUser.getName()+" has no access - forbidden (permission=" + requiredPermission() + ")" );
                String pageurl = m_engine.encodeName( m_page.getName() );
                m_session.addMessage("You don't have access to '" + getName() + "'. Do you want to log in as another user?.");
                response.sendRedirect( m_engine.getURL(WikiContext.NONE,"Login.jsp","page="+pageurl, false ) );
            }
            else
            {
                log.info("User "+currentUser.getName()+" has no access - redirecting (permission=" + requiredPermission() + ")");
                String pageurl = m_engine.encodeName( m_page.getName() );
                m_session.addMessage("You don't have access to '" + getName() + "'. Please log in first.");
                response.sendRedirect( m_engine.getURL(WikiContext.NONE, "Login.jsp", "page="+pageurl, false ) );
            }
        }
        return allowed;
    }
    
    /**
     * Figures out which template a new WikiContext should be using.
     * @param request the HTTP request
     */
    protected void setDefaultTemplate( HttpServletRequest request )
    {
        // FIXME: Most definitely this should be checked for
        //        existence, or else it is possible to create pages that
        //        cannot be shown.
        String template = m_engine.getTemplateDir();
        
        //  Figure out which template we should be using for this page.
        if ( request != null )
        {
            template = request.getParameter( "skin" );
        }
        
        // If request doesn't supply the value, extract from wiki page
        if( template == null )
        {
            WikiPage page = getPage();
            if ( page != null )
            {
                template = (String)page.getAttribute( WikiEngine.PROP_TEMPLATEDIR );
            }

        }
        
        // If something over-wrote the default, set the new value.
        if ( template != null )
        {
            setTemplate( template );
        }
    }
    
    /**
     * Looks up and returns a PageCommand based on a supplied WikiPage and HTTP
     * request. First, the appropriate Command is obtained by examining the HTTP
     * request; the default is {@link PageCommand#VIEW}. If the Command is a
     * PageCommand (and it should be, in most cases), a targeted Command is
     * created using the (non-<code>null</code>) WikiPage as target.
     * @param engine the wiki engine
     * @param request the HTTP request
     * @param page the wiki page
     * @return the correct command
     */
    protected static Command findCommand( WikiEngine engine, HttpServletRequest request, WikiPage page )
    {
        String defaultContext = PageCommand.VIEW.getRequestContext();
        Command command = engine.getCommandResolver().findCommand( request, defaultContext );
        if ( command instanceof PageCommand && page != null )
        {
            command = command.targetedCommand( page );
        }
        return command;
    }

    /**
     * Protected method that updates the internally cached Command. 
     * Will always be called when the page name, request context, or variable
     * changes.
     * @param requestContext the desired request context
     * @since 2.4
     */
    protected void updateCommand( String requestContext ) 
    {
        if ( requestContext == null )
        {
            m_command = PageCommand.NONE;
        }
        else
        {
            CommandResolver resolver = m_engine.getCommandResolver();
            m_command = resolver.findCommand( m_request, requestContext );
        }
        
        if ( m_command instanceof PageCommand && m_page != null )
        {
            m_command = m_command.targetedCommand( m_page );
        }
    }
}
