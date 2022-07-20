/*
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
package org.apache.wiki;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.ui.CommandResolver;
import org.apache.wiki.ui.Installer;
import org.apache.wiki.ui.PageCommand;
import org.apache.wiki.ui.WikiCommand;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.security.Permission;
import java.security.Principal;
import java.util.HashMap;
import java.util.PropertyPermission;

/**
 *  <p>Provides state information throughout the processing of a page.  A WikiContext is born when the JSP pages that are the main entry
 *  points, are invoked.  The JSPWiki engine creates the new WikiContext, which basically holds information about the page, the
 *  handling engine, and in which context (view, edit, etc) the call was done.</p>
 *  <p>A WikiContext also provides request-specific variables, which can be used to communicate between plugins on the same page, or
 *  between different instances of the same plugin.  A WikiContext variable is valid until the processing of the page has ended.  For
 *  an example, please see the Counter plugin.</p>
 *  <p>When a WikiContext is created, it automatically associates a {@link WikiSession} object with the user's HttpSession. The
 *  WikiSession contains information about the user's authentication status, and is consulted by {@link #getCurrentUser()} object.</p>
 *  <p>Do not cache the page object that you get from the WikiContext; always use getPage()!</p>
 *
 *  @see org.apache.wiki.plugin.Counter
 */
public class WikiContext implements Context, Command {

    private Command  m_command;
    private WikiPage m_page;
    private WikiPage m_realPage;
    private Engine   m_engine;
    private String   m_template = "default";

    private HashMap< String, Object > m_variableMap = new HashMap<>();

    /** Stores the HttpServletRequest.  May be null, if the request did not come from a servlet. */
    protected HttpServletRequest m_request;

    private Session m_session;

    /** User is doing administrative things. */
    public static final String ADMIN = ContextEnum.WIKI_ADMIN.getRequestContext();

    /** User is downloading an attachment. */
    public static final String ATTACH = ContextEnum.PAGE_ATTACH.getRequestContext();

    /** User is commenting something. */
    public static final String COMMENT = ContextEnum.PAGE_COMMENT.getRequestContext();

    /** User has an internal conflict, and does quite not know what to do. Please provide some counseling. */
    public static final String CONFLICT = ContextEnum.PAGE_CONFLICT.getRequestContext();

    /** User wishes to create a new group */
    public static final String CREATE_GROUP = ContextEnum.WIKI_CREATE_GROUP.getRequestContext();

    /** User is deleting a page or an attachment. */
    public static final String DELETE = ContextEnum.PAGE_DELETE.getRequestContext();

    /** User is deleting an existing group. */
    public static final String DELETE_GROUP = ContextEnum.GROUP_DELETE.getRequestContext();

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String DIFF = ContextEnum.PAGE_DIFF.getRequestContext();

    /** The EDIT context - the user is editing the page. */
    public static final String EDIT = ContextEnum.PAGE_EDIT.getRequestContext();

    /** User is editing an existing group. */
    public static final String EDIT_GROUP = ContextEnum.GROUP_EDIT.getRequestContext();

    /** An error has been encountered and the user needs to be informed. */
    public static final String ERROR = ContextEnum.WIKI_ERROR.getRequestContext();

    /** User is searching for content. */
    public static final String FIND = ContextEnum.WIKI_FIND.getRequestContext();

    /** User is viewing page history. */
    public static final String INFO = ContextEnum.PAGE_INFO.getRequestContext();

    /** User is administering JSPWiki (Install, SecurityConfig). */
    public static final String INSTALL = ContextEnum.WIKI_INSTALL.getRequestContext();

    /** User is preparing for a login/authentication. */
    public static final String LOGIN = ContextEnum.WIKI_LOGIN.getRequestContext();

    /** User is preparing to log out. */
    public static final String LOGOUT = ContextEnum.WIKI_LOGOUT.getRequestContext();

    /** JSPWiki wants to display a message. */
    public static final String MESSAGE = ContextEnum.WIKI_MESSAGE.getRequestContext();

    /** This is not a JSPWiki context, use it to access static files. */
    public static final String NONE = ContextEnum.PAGE_NONE.getRequestContext();

    /** Same as NONE; this is just a clarification. */
    public static final String OTHER = ContextEnum.PAGE_NONE.getRequestContext();

    /** User is editing preferences */
    public static final String PREFS = ContextEnum.WIKI_PREFS.getRequestContext();

    /** User is previewing the changes he just made. */
    public static final String PREVIEW = ContextEnum.PAGE_PREVIEW.getRequestContext();

    /** User is renaming a page. */
    public static final String RENAME = ContextEnum.PAGE_RENAME.getRequestContext();

    /** RSS feed is being generated. */
    public static final String RSS = ContextEnum.PAGE_RSS.getRequestContext();

    /** User is uploading something. */
    public static final String UPLOAD = ContextEnum.PAGE_UPLOAD.getRequestContext();

    /** The VIEW context - the user just wants to view the page contents. */
    public static final String VIEW = ContextEnum.PAGE_VIEW.getRequestContext();

    /** User is viewing an existing group */
    public static final String VIEW_GROUP = ContextEnum.GROUP_VIEW.getRequestContext();

    /** User wants to view or administer workflows. */
    public static final String WORKFLOW = ContextEnum.WIKI_WORKFLOW.getRequestContext();

    private static final Logger LOG = LogManager.getLogger( WikiContext.class );

    private static final Permission DUMMY_PERMISSION = new PropertyPermission( "os.name", "read" );

    /**
     *  Create a new WikiContext for the given WikiPage. Delegates to {@link #WikiContext(Engine, HttpServletRequest, Page)}.
     *
     *  @param engine The Engine that is handling the request.
     *  @param page The WikiPage. If you want to create a WikiContext for an older version of a page, you must use this constructor.
     */
    public WikiContext( final Engine engine, final Page page ) {
        this( engine, null, findCommand( engine, null, page ) );
    }

    /**
     * <p>
     * Creates a new WikiContext for the given Engine, Command and HttpServletRequest.
     * </p>
     * <p>
     * This constructor will also look up the HttpSession associated with the request, and determine if a Session object is present.
     * If not, a new one is created.
     * </p>
     * @param engine The Engine that is handling the request
     * @param request The HttpServletRequest that should be associated with this context. This parameter may be <code>null</code>.
     * @param command the command
     * @throws IllegalArgumentException if <code>engine</code> or <code>command</code> are <code>null</code>
     */
    public WikiContext( final Engine engine, final HttpServletRequest request, final Command command ) throws IllegalArgumentException {
        if ( engine == null || command == null ) {
            throw new IllegalArgumentException( "Parameter engine and command must not be null." );
        }

        m_engine = engine;
        m_request = request;
        m_session = Wiki.session().find( engine, request );
        m_command = command;

        // If PageCommand, get the WikiPage
        if( command instanceof PageCommand ) {
            m_page = ( WikiPage )command.getTarget();
        }

        // If page not supplied, default to front page to avoid NPEs
        if( m_page == null ) {
            m_page = ( WikiPage )m_engine.getManager( PageManager.class ).getPage( m_engine.getFrontPage() );

            // Front page does not exist?
            if( m_page == null ) {
                m_page = ( WikiPage )Wiki.contents().page( m_engine, m_engine.getFrontPage() );
            }
        }

        m_realPage = m_page;

        // Special case: retarget any empty 'view' PageCommands to the front page
        if ( PageCommand.VIEW.equals( command ) && command.getTarget() == null ) {
            m_command = command.targetedCommand( m_page );
        }

        // Debugging...
        final HttpSession session = ( request == null ) ? null : request.getSession( false );
        final String sid = session == null ? "(null)" : session.getId();
        LOG.debug( "Creating WikiContext for session ID={}; target={}", sid, getName() );

        // Figure out what template to use
        setDefaultTemplate( request );
    }

    /**
     * Creates a new WikiContext for the given Engine, WikiPage and HttpServletRequest. This method simply looks up the appropriate
     * Command using {@link #findCommand(Engine, HttpServletRequest, Page)} and delegates to
     * {@link #WikiContext(Engine, HttpServletRequest, Command)}.
     *
     * @param engine The Engine that is handling the request
     * @param request The HttpServletRequest that should be associated with this context. This parameter may be <code>null</code>.
     * @param page The WikiPage. If you want to create a WikiContext for an older version of a page, you must supply this parameter
     */
    public WikiContext( final Engine engine, final HttpServletRequest request, final Page page ) {
        this( engine, request, findCommand( engine, request, page ) );
    }

    /**
     *  Creates a new WikiContext from a supplied HTTP request, using a default wiki context.
     *
     *  @param engine The Engine that is handling the request
     *  @param request the HTTP request
     *  @param requestContext the default context to use
     *  @see org.apache.wiki.ui.CommandResolver
     *  @see org.apache.wiki.api.core.Command
     *  @since 2.1.15.
     */
    public WikiContext( final Engine engine, final HttpServletRequest request, final String requestContext ) {
        this( engine, request, engine.getManager( CommandResolver.class ).findCommand( request, requestContext ) );
        if( !engine.isConfigured() ) {
            throw new InternalWikiException( "Engine has not been properly started.  It is likely that the configuration is faulty.  Please check all logs for the possible reason." );
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.core.Command#getContentTemplate()
     */
    @Override
    public String getContentTemplate()
    {
        return m_command.getContentTemplate();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.core.Command#getJSP()
     */
    @Override
    public String getJSP()
    {
        return m_command.getContentTemplate();
    }

    /**
     *  Sets a reference to the real page whose content is currently being rendered.
     *  <p>
     *  Sometimes you may want to render the page using some other page's context. In those cases, it is highly recommended that you set
     *  the setRealPage() to point at the real page you are rendering.  Please see InsertPageTag for an example.
     *  <p>
     *  Also, if your plugin e.g. does some variable setting, be aware that if it is embedded in the LeftMenu or some other page added
     *  with InsertPageTag, you should consider what you want to do - do you wish to really reference the "master" page or the included
     *  page.
     *
     *  @param page  The real page which is being rendered.
     *  @return The previous real page
     *  @since 2.3.14
     *  @see org.apache.wiki.tags.InsertPageTag
     */
    @Override
    public WikiPage setRealPage( final Page page ) {
        final WikiPage old = m_realPage;
        m_realPage = ( WikiPage )page;
        updateCommand( m_command.getRequestContext() );
        return old;
    }

    /**
     *  Gets a reference to the real page whose content is currently being rendered. If your plugin e.g. does some variable setting, be
     *  aware that if it is embedded in the LeftMenu or some other page added with InsertPageTag, you should consider what you want to
     *  do - do you wish to really reference the "master" page or the included page.
     *  <p>
     *  For example, in the default template, there is a page called "LeftMenu". Whenever you access a page, e.g. "Main", the master
     *  page will be Main, and that's what the getPage() will return - regardless of whether your plugin resides on the LeftMenu or on
     *  the Main page.  However, getRealPage() will return "LeftMenu".
     *
     *  @return A reference to the real page.
     *  @see org.apache.wiki.tags.InsertPageTag
     *  @see org.apache.wiki.parser.JSPWikiMarkupParser
     */
    @Override
    public WikiPage getRealPage()
    {
        return m_realPage;
    }

    /**
     *  Figure out to which page we are really going to.  Considers special page names from the jspwiki.properties, and possible aliases.
     *  This method forwards requests to {@link org.apache.wiki.ui.CommandResolver#getSpecialPageReference(String)}.
     *  @return A complete URL to the new page to redirect to
     *  @since 2.2
     */
    @Override
    public String getRedirectURL() {
        final String pagename = m_page.getName();
        String redirURL = m_engine.getManager( CommandResolver.class ).getSpecialPageReference( pagename );
        if( redirURL == null ) {
            final String alias = m_page.getAttribute( WikiPage.ALIAS );
            if( alias != null ) {
                redirURL = getViewURL( alias );
            } else {
                redirURL = m_page.getAttribute( WikiPage.REDIRECT );
            }
        }

        return redirURL;
    }

    /**
     *  Returns the handling engine.
     *
     *  @return The engine owning this context.
     */
    @Override
    public WikiEngine getEngine() {
        return ( WikiEngine )m_engine;
    }

    /**
     *  Returns the page that is being handled.
     *
     *  @return the page which was fetched.
     */
    @Override
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
    @Override
    public void setPage( final Page page ) {
        m_page = (WikiPage)page;
        updateCommand( m_command.getRequestContext() );
    }

    /**
     *  Returns the request context.
     *
     *  @return The name of the request context (e.g. VIEW).
     */
    @Override
    public String getRequestContext()
    {
        return m_command.getRequestContext();
    }

    /**
     *  Sets the request context.  See above for the different request contexts (VIEW, EDIT, etc.)
     *
     *  @param arg The request context (one of the predefined contexts.)
     */
    @Override
    public void setRequestContext( final String arg )
    {
        updateCommand( arg );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.core.Command#getTarget()
     */
    @Override
    public Object getTarget()
    {
        return m_command.getTarget();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.core.Command#getURLPattern()
     */
    @Override
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
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T getVariable( final String key ) {
        return ( T )m_variableMap.get( key );
    }

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid, i.e. while page processing continues.  The variable data
     *  is discarded once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    @Override
    public void setVariable( final String key, final Object data ) {
        m_variableMap.put( key, data );
        updateCommand( m_command.getRequestContext() );
    }

    /**
     * This is just a simple helper method which will first check the context if there is already an override in place, and if there is not,
     * it will then check the given properties.
     *
     * @param key What key are we searching for?
     * @param defValue Default value for the boolean
     * @return {@code true} or {@code false}.
     */
    @Override
    public boolean getBooleanWikiProperty( final String key, final boolean defValue ) {
        final String bool = getVariable( key );
        if( bool != null ) {
            return TextUtil.isPositive( bool );
        }

        return TextUtil.getBooleanProperty( getEngine().getWikiProperties(), key, defValue );
    }

    /**
     *  This method will safely return any HTTP parameters that might have been defined.  You should use this method instead
     *  of peeking directly into the result of getHttpRequest(), since this method is smart enough to do all the right things,
     *  figure out UTF-8 encoded parameters, etc.
     *
     *  @since 2.0.13.
     *  @param paramName Parameter name to look for.
     *  @return HTTP parameter, or null, if no such parameter existed.
     */
    @Override
    public String getHttpParameter( final String paramName ) {
        String result = null;
        if( m_request != null ) {
            result = m_request.getParameter( paramName );
        }

        return result;
    }

    /**
     *  If the request did originate from an HTTP request, then the HTTP request can be fetched here.  However, if the request
     *  did NOT originate from an HTTP request, then this method will return null, and YOU SHOULD CHECK FOR IT!
     *
     *  @return Null, if no HTTP request was done.
     *  @since 2.0.13.
     */
    @Override
    public HttpServletRequest getHttpRequest()
    {
        return m_request;
    }

    /**
     *  Sets the template to be used for this request.
     *
     *  @param dir The template name
     *  @since 2.1.15.
     */
    @Override
    public void setTemplate( final String dir )
    {
        m_template = dir;
    }

    /**
     * Returns the target of this wiki context: a page, group name or JSP. If the associated Command is a PageCommand, this method
     * returns the page's name. Otherwise, this method delegates to the associated Command's {@link org.apache.wiki.api.core.Command#getName()}
     * method. Calling classes can rely on the results of this method for looking up canonically-correct page or group names. Because it
     * does not automatically assume that the wiki context is a PageCommand, calling this method is inherently safer than calling
     * {@code getPage().getName()}.
     *
     * @return the name of the target of this wiki context
     * @see org.apache.wiki.ui.PageCommand#getName()
     * @see org.apache.wiki.ui.GroupCommand#getName()
     */
    @Override
    public String getName() {
        if ( m_command instanceof PageCommand ) {
            return m_page != null ? m_page.getName() : "<no page>";
        }
        return m_command.getName();
    }

    /**
     *  Gets the template that is to be used throughout this request.
     *
     *  @since 2.1.15.
     *  @return template name
     */
    @Override
    public String getTemplate()
    {
        return m_template;
    }

    /**
     *  Convenience method that gets the current user. Delegates the lookup to the WikiSession associated with this WikiContect.
     *  May return null, in case the current user has not yet been determined; or this is an internal system. If the WikiSession has not
     *  been set, <em>always</em> returns null.
     *
     *  @return The current user; or maybe null in case of internal calls.
     */
    @Override
    public Principal getCurrentUser() {
        if (m_session == null) {
            // This shouldn't happen, really...
            return WikiPrincipal.GUEST;
        }
        return m_session.getUserPrincipal();
    }

    /**
     *  A shortcut to generate a VIEW url.
     *
     *  @param page The page to which to link.
     *  @return A URL to the page.  This honours the current absolute/relative setting.
     */
    @Override
    public String getViewURL( final String page ) {
        return getURL( ContextEnum.PAGE_VIEW.getRequestContext(), page, null );
    }

    /**
     *  Creates a URL for the given request context.
     *
     *  @param context e.g. WikiContext.EDIT
     *  @param page The page to which to link
     *  @return A URL to the page, honours the absolute/relative setting in jspwiki.properties
     */
    @Override
    public String getURL( final String context, final String page ) {
        return getURL( context, page, null );
    }

    /**
     *  Returns a URL from a page. It this WikiContext instance was constructed with an actual HttpServletRequest, we will attempt to
     *  construct the URL using HttpUtil, which preserves the HTTPS portion if it was used.
     *
     *  @param context The request context (e.g. WikiContext.UPLOAD)
     *  @param page The page to which to link
     *  @param params A list of parameters, separated with "&amp;"
     *
     *  @return A URL to the given context and page.
     */
    @Override
    public String getURL( final String context, final String page, final String params ) {
        // FIXME: is rather slow
        return m_engine.getURL( context, page, params );
    }

    /**
     * Returns the Command associated with this WikiContext.
     *
     * @return the command
     */
    @Override
    public Command getCommand() {
        return m_command;
    }

    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     *  @return A shallow clone of the WikiContext
     */
    @Override
    public WikiContext clone() {
        try {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            final WikiContext copy = (WikiContext)super.clone();

            copy.m_engine = m_engine;
            copy.m_command = m_command;

            copy.m_template    = m_template;
            copy.m_variableMap = m_variableMap;
            copy.m_request     = m_request;
            copy.m_session     = m_session;
            copy.m_page        = m_page;
            copy.m_realPage    = m_realPage;
            return copy;
        } catch( final CloneNotSupportedException e ){} // Never happens

        return null;
    }

    /**
     *  Creates a deep clone of the WikiContext.  This is useful when you want to be sure that you don't accidentally mess with page
     *  attributes, etc.
     *
     *  @since  2.8.0
     *  @return A deep clone of the WikiContext.
     */
    @Override
    @SuppressWarnings("unchecked")
    public WikiContext deepClone() {
        try {
            // super.clone() must always be called to make sure that inherited objects
            // get the right type
            final WikiContext copy = (WikiContext)super.clone();

            //  No need to deep clone these
            copy.m_engine  = m_engine;
            copy.m_command = m_command; // Static structure

            copy.m_template    = m_template;
            copy.m_variableMap = (HashMap<String,Object>)m_variableMap.clone();
            copy.m_request     = m_request;
            copy.m_session     = m_session;
            copy.m_page        = m_page.clone();
            copy.m_realPage    = m_realPage.clone();
            return copy;
        }
        catch( final CloneNotSupportedException e ){} // Never happens

        return null;
    }

    /**
     *  Returns the Session associated with the context. This method is guaranteed to always return a valid Session.
     *  If this context was constructed without an associated HttpServletRequest, it will return
     *  {@link org.apache.wiki.WikiSession#guestSession(Engine)}.
     *
     *  @return The Session associated with this context.
     */
    @Override
    public WikiSession getWikiSession() {
        return ( WikiSession )m_session;
    }

    /**
     * This method can be used to find the WikiContext programmatically from a JSP PageContext. We check the request context.
     * The wiki context, if it exists, is looked up using the key {@link #ATTR_CONTEXT}.
     *
     * @since 2.4
     * @param pageContext the JSP page context
     * @return Current WikiContext, or null, of no context exists.
     * @deprecated use {@link Context#findContext( PageContext )} instead.
     * @see Context#findContext( PageContext )
     */
    @Deprecated
    public static WikiContext findContext( final PageContext pageContext ) {
        final HttpServletRequest request = ( HttpServletRequest )pageContext.getRequest();
        return ( WikiContext )request.getAttribute( ATTR_CONTEXT );
    }

    /**
     * Returns the permission required to successfully execute this context. For example, a wiki context of VIEW for a certain page
     * means that the PagePermission "view" is required for the page. In some cases, no particular permission is required, in which case
     * a dummy permission will be returned ({@link java.util.PropertyPermission}<code> "os.name", "read"</code>). This method is guaranteed
     * to always return a valid, non-null permission.
     *
     * @return the permission
     * @since 2.4
     */
    @Override
    public Permission requiredPermission() {
        // This is a filthy rotten hack -- absolutely putrid
        if ( WikiCommand.INSTALL.equals( m_command ) ) {
            // See if admin users exists
            try {
                final UserManager userMgr = m_engine.getManager( UserManager.class );
                final UserDatabase userDb = userMgr.getUserDatabase();
                userDb.findByLoginName( Installer.ADMIN_ID );
            } catch ( final NoSuchPrincipalException e ) {
                return DUMMY_PERMISSION;
            }
            return new AllPermission( m_engine.getApplicationName() );
        }

        // TODO: we should really break the contract so that this
        // method returns null, but until then we will use this hack
        if( m_command.requiredPermission() == null ) {
            return DUMMY_PERMISSION;
        }

        return m_command.requiredPermission();
    }

    /**
     * Associates a target with the current Command and returns the new targeted Command. If the Command associated with this
     * WikiContext is already "targeted", it is returned instead.
     *
     * @see org.apache.wiki.api.core.Command#targetedCommand(java.lang.Object)
     *
     * {@inheritDoc}
     */
    @Override
    public Command targetedCommand( final Object target ) {
        if ( m_command.getTarget() == null ) {
            return m_command.targetedCommand( target );
        }
        return m_command;
    }

    /**
     *  Returns true, if the current user has administrative permissions (i.e. the omnipotent AllPermission).
     *
     *  @since 2.4.46
     *  @return true, if the user has all permissions.
     */
    @Override
    public boolean hasAdminPermissions() {
        return m_engine.getManager( AuthorizationManager.class ).checkPermission( getWikiSession(), new AllPermission( m_engine.getApplicationName() ) );
    }

    /**
     * Figures out which template a new WikiContext should be using.
     * @param request the HTTP request
     */
    protected void setDefaultTemplate( final HttpServletRequest request ) {
        final String defaultTemplate = m_engine.getTemplateDir();

        //  Figure out which template we should be using for this page.
        String template = null;
        if ( request != null ) {
            final String skin = request.getParameter( "skin" );
            if( skin != null )
            {
                template = skin.replaceAll("\\p{Punct}", "");
            }

        }

        // If request doesn't supply the value, extract from wiki page
        if( template == null ) {
            final WikiPage page = getPage();
            if ( page != null ) {
                template = page.getAttribute( Engine.PROP_TEMPLATEDIR );
            }

        }

        // If something over-wrote the default, set the new value.
        if ( template != null ) {
            setTemplate( template );
        } else {
            setTemplate( defaultTemplate );
        }
    }

    /**
     * Looks up and returns a PageCommand based on a supplied WikiPage and HTTP request. First, the appropriate Command is obtained by
     * examining the HTTP request; the default is {@link ContextEnum#PAGE_VIEW}. If the Command is a PageCommand (and it should be, in most
     * cases), a targeted Command is created using the (non-<code>null</code>) WikiPage as target.
     *
     * @param engine the wiki engine
     * @param request the HTTP request
     * @param page the wiki page
     * @return the correct command
     */
    protected static Command findCommand( final Engine engine, final HttpServletRequest request, final Page page ) {
        final String defaultContext = ContextEnum.PAGE_VIEW.getRequestContext();
        Command command = engine.getManager( CommandResolver.class ).findCommand( request, defaultContext );
        if ( command instanceof PageCommand && page != null ) {
            command = command.targetedCommand( page );
        }
        return command;
    }

    /**
     * Protected method that updates the internally cached Command. Will always be called when the page name, request context, or variable
     * changes.
     *
     * @param requestContext the desired request context
     * @since 2.4
     */
    protected void updateCommand( final String requestContext ) {
        if ( requestContext == null ) {
            m_command = PageCommand.NONE;
        } else {
            final CommandResolver resolver = m_engine.getManager( CommandResolver.class );
            m_command = resolver.findCommand( m_request, requestContext );
        }

        if ( m_command instanceof PageCommand && m_page != null ) {
            m_command = m_command.targetedCommand( m_page );
        }
    }

}
