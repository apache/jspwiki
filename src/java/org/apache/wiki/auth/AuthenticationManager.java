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
package org.apache.wiki.auth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebContainerAuthorizer;
import org.apache.wiki.auth.login.*;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.util.TimedCounterList;



/**
 * Manages authentication activities for a WikiEngine: user login, logout, and
 * credential refreshes. This class uses JAAS to determine how users log in.
 * <p>
 * The login procedure is protected in addition by a mechanism which prevents
 * a hacker to try and force-guess passwords by slowing down attempts to log in
 * into the same account.  Every login attempt is recorded, and stored for a while
 * (currently ten minutes), and each login attempt during that time incurs a penalty
 * of 2^login attempts milliseconds - that is, 10 login attempts incur a login penalty of 1.024 seconds.
 * The delay is currently capped to 20 seconds.
 * 
 * @author Andrew Jaquith
 * @author Erik Bunn
 * @since 2.3
 */
public final class AuthenticationManager
{
    /** How many milliseconds the logins are stored before they're cleaned away. */
    private static final long LASTLOGINS_CLEANUP_TIME = 10*60*1000L; // Ten minutes

    private static final long MAX_LOGIN_DELAY         = 20*1000L; // 20 seconds
    
    /** The name of the built-in cookie assertion module */
    public static final String                 COOKIE_MODULE       =  CookieAssertionLoginModule.class.getName();

    /** The name of the built-in cookie authentication module */
    public static final String                 COOKIE_AUTHENTICATION_MODULE =  CookieAuthenticationLoginModule.class.getName();

    /** If this jspwiki.properties property is <code>true</code>, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";
    
    /** If this jspwiki.properties property is <code>true</code>, allow cookies to be used for authentication. */
    public static final String                 PROP_ALLOW_COOKIE_AUTH = "jspwiki.cookieAuthentication";
    
    /**
     *  This property determines whether we use JSPWiki authentication or not.
     *  Possible values are AUTH_JAAS or AUTH_CONTAINER.
     *  <p>
     *  Setting this is now deprecated - we do not guarantee that it works.
     *  
     *  @deprecated
     */
    public  static final String                PROP_SECURITY       = "jspwiki.security";

    /** Value specifying that the user wants to use the container-managed security, just like in JSPWiki 2.2. */
    public static final String                SECURITY_OFF      = "off";

    /** Value specifying that the user wants to use the built-in JAAS-based system */
    public static final String                SECURITY_JAAS     = "jaas";

    /** Whether logins should be throttled to limit brute-forcing attempts. Defaults to true. */
    public static final String                 PROP_LOGIN_THROTTLING = "jspwiki.login.throttling";

    protected static final Logger              log                 = LoggerFactory.getLogger( AuthenticationManager.class );

    /** Prefix for LoginModule options key/value pairs. */
    protected static final String                 PREFIX_LOGIN_MODULE_OPTIONS = "jspwiki.loginModule.options.";

    /** If this jspwiki.properties property is <code>true</code>, allow cookies to be used to assert identities. */
    protected static final String                 PROP_ALLOW_COOKIE_ASSERTIONS = "jspwiki.cookieAssertions";

    /** The {@link javax.security.auth.spi.LoginModule} to use for custom authentication. */
    protected static final String                 PROP_LOGIN_MODULE = "jspwiki.loginModule.class";
    
    /** Empty Map passed to JAAS {@link #doJAASLogin(Class, CallbackHandler, Map)} method. */
    protected static final Map<String,String> EMPTY_MAP = Collections.unmodifiableMap( new HashMap<String,String>() );
    
    /** Class (of type LoginModule) to use for custom authentication. */
    protected Class<? extends LoginModule> m_loginModuleClass = UserDatabaseLoginModule.class;
    
    /** Options passed to {@link javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)}; 
     * initialized by {@link #initialize(WikiEngine, Properties)}. */
    protected Map<String,String> m_loginModuleOptions = new HashMap<String,String>();

    /** Just to provide compatibility with the old versions.  The same
     *  as SECURITY_OFF.
     *
     *  @deprecated use {@link #SECURITY_OFF} instead
     */
    protected static final String             SECURITY_CONTAINER = "container";

    /** The default {@link javax.security.auth.spi.LoginModule} class name to use for custom authentication. */
    private static final String                 DEFAULT_LOGIN_MODULE = "org.apache.wiki.auth.login.UserDatabaseLoginModule";
    
    /** Empty principal set. */
    private static final Set<Principal> NO_PRINCIPALS = new HashSet<Principal>();

    /** Static Boolean for lazily-initializing the "allows assertions" flag */
    private boolean                     m_allowsCookieAssertions  = true;

    private boolean                     m_throttleLogins = true;

    /** Static Boolean for lazily-initializing the "allows cookie authentication" flag */
    private boolean                     m_allowsCookieAuthentication = false;

    private WikiEngine                         m_engine            = null;
    
    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    private boolean               m_useJAAS = true;

    /** Keeps a list of the usernames who have attempted a login recently. */
    
    private TimedCounterList<String> m_lastLoginAttempts = new TimedCounterList<String>();
    
    /**
     * Creates an AuthenticationManager instance for the given WikiEngine and
     * the specified set of properties. All initialization for the modules is
     * done here.
     * @param engine the wiki engine
     * @param props the properties used to initialize the wiki engine
     * @throws WikiException if the AuthenticationManager cannot be initialized
     */
    @SuppressWarnings("unchecked")
    public final void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );

        // Should J2SE policies be used for authorization?
        m_useJAAS = SECURITY_JAAS.equals(props.getProperty( PROP_SECURITY, SECURITY_JAAS ));
        
        // Should we allow cookies for assertions? (default: yes)
        m_allowsCookieAssertions = TextUtil.getBooleanProperty( props,
                                                              PROP_ALLOW_COOKIE_ASSERTIONS,
                                                              true );
        
        // Should we allow cookies for authentication? (default: no)
        m_allowsCookieAuthentication = TextUtil.getBooleanProperty( props,
                                                                    PROP_ALLOW_COOKIE_AUTH,
                                                                    false );
        
        // Should we throttle logins? (default: yes)
        m_throttleLogins = TextUtil.getBooleanProperty( props,
                                                        PROP_LOGIN_THROTTLING,
                                                        true );

        // Look up the LoginModule class
        String loginModuleClassName = TextUtil.getStringProperty( props, PROP_LOGIN_MODULE, DEFAULT_LOGIN_MODULE );
        try
        {
            m_loginModuleClass = (Class<? extends LoginModule>) Class.forName( loginModuleClassName );
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            throw new WikiException(e.getMessage());
        }
        
        // Initialize the LoginModule options
        initLoginModuleOptions( props );
    }

    /**
     * Returns true if this WikiEngine uses container-managed authentication.
     * This method is used primarily for cosmetic purposes in the JSP tier, and
     * performs no meaningful security function per se. Delegates to
     * {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer#isContainerAuthorized()},
     * if used as the external authorizer; otherwise, returns <code>false</code>.
     * @return <code>true</code> if the wiki's authentication is managed by
     *         the container, <code>false</code> otherwise
     */
    public final boolean isContainerAuthenticated()
    {
        if( !m_useJAAS ) return true;

        try
        {
            Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
            if ( authorizer instanceof WebContainerAuthorizer )
            {
                 return ( ( WebContainerAuthorizer )authorizer ).isContainerAuthorized();
            }
        }
        catch ( WikiException e )
        {
            // It's probably ok to fail silently...
        }
        return false;
    }

    /**
     * <p>Logs in the user by attempting to populate a WikiSession Subject from
     * a web servlet request by examining the request
     *  for the presence of container credentials and user cookies. The processing
     * logic is as follows:
     * </p>
     * <ul>
     * <li>If the WikiSession had previously been unauthenticated, check to see if
     * user has subsequently authenticated. To be considered "authenticated,"
     * the request must supply one of the following (in order of preference):
     * the container <code>userPrincipal</code>, container <code>remoteUser</code>,
     * or authentication cookie. If the user is authenticated, this method fires event
     * {@link org.apache.wiki.event.WikiSecurityEvent#LOGIN_AUTHENTICATED}
     * with two parameters: a Principal representing the login principal,
     * and the current WikiSession. In addition, if the authorizer is of type
     * WebContainerAuthorizer, this method iterates through the container roles returned by
     * {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer#getRoles()},
     * tests for membership in each one, and adds those that pass to the Subject's principal set.</li>
     * <li>If, after checking for authentication, the WikiSession is still Anonymous,
     * this method next checks to see if the user has "asserted" an identity
     * by supplying an assertion cookie. If the user is found to be asserted,
     * this method fires event {@link org.apache.wiki.event.WikiSecurityEvent#LOGIN_ASSERTED}
     * with two parameters: <code>WikiPrincipal(<em>cookievalue</em>)</code>, and
     * the current WikiSession.</li>
     * <li>If, after checking for authenticated and asserted status, the  WikiSession is
     * <em>still</em> anonymous, this method fires event
     * {@link org.apache.wiki.event.WikiSecurityEvent#LOGIN_ANONYMOUS} with
     * two parameters: <code>WikiPrincipal(<em>remoteAddress</em>)</code>,
     * and the current WikiSession </li>
     * </ul>
     * @param request servlet request for this user
     * @return always returns <code>true</code> (because anonymous login, at least, will always succeed)
     * @throws org.apache.wiki.auth.WikiSecurityException if the user cannot be logged in for any reason
     * @since 2.3
     */
    public final boolean login( HttpServletRequest request ) throws WikiSecurityException
    {
        HttpSession httpSession = request.getSession();
        WikiSession session = SessionMonitor.getInstance(m_engine).find( httpSession );
        AuthenticationManager authenticationMgr = m_engine.getAuthenticationManager();
        AuthorizationManager authorizationMgr = m_engine.getAuthorizationManager();
        CallbackHandler handler = null;
        Map<String,String> options = EMPTY_MAP;

        // If user not authenticated, check if container logged them in, or if
        // there's an authentication cookie
        Set<Principal> principals = null;
        if ( !session.isAuthenticated() )
        {
            // Create a callback handler
            try
            {
                handler = new WebContainerCallbackHandler( m_engine, request, authorizationMgr.getAuthorizer() );
            }
            catch ( WikiSecurityException e )
            {
                e.printStackTrace();
                throw new WikiSecurityException( e.getMessage() );
            }
            
            // Execute the container login module
            try
            {
                principals = authenticationMgr.doJAASLogin( WebContainerLoginModule.class, handler, options );
            }
            catch ( LoginException e )
            {
                // Container credentials not supplied in request. Ok, try the auth cookie!
            }
            
            // Execute the cookie authentication module (if allowed)
            if ( ( principals == null || principals.size() == 0 ) && authenticationMgr.allowsCookieAuthentication() )
            {
                try
                {
                    principals = authenticationMgr.doJAASLogin( CookieAuthenticationLoginModule.class, handler, options );
                }
                catch( LoginException e )
                {
                    // Authentication cookie not supplied in request. Ok, try the assertion cookie!
                }
            }
            
            // If the container logged the user in successfully, tell the WikiSession (and add all of the Principals)
            if ( principals != null && principals.size() > 0 )
            {
                fireEvent( WikiSecurityEvent.LOGIN_AUTHENTICATED, getLoginPrincipal( principals ), session );
                for ( Principal principal : principals )
                {
                    fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, principal, session );
                }
            }
        }

        // If user still not authenticated, check if assertion cookie was supplied
        if ( !session.isAuthenticated() && authenticationMgr.allowsCookieAssertions() )
        {
            // Execute the cookie assertion login module
            try
            {
                principals = authenticationMgr.doJAASLogin( CookieAssertionLoginModule.class, handler, options );
                if ( principals != null && principals.size() > 0 )
                {
                    fireEvent( WikiSecurityEvent.LOGIN_ASSERTED, getLoginPrincipal( principals ), session);
                }
            }
            catch( LoginException e )
            {
                // Assertion cookie not supplied in request. Ok, use the IP address!
            }
        }

        // If user still anonymous, use the remote address
        if ( session.isAnonymous() )
        {
            try
            {
                principals = authenticationMgr.doJAASLogin( AnonymousLoginModule.class, handler, options );
            }
            catch( LoginException e )
            {
                // If the anonymous login didn't succeed, we have a genuine configuration problem!
                e.printStackTrace();
                throw new WikiSecurityException( e.getMessage() );
            }
            if ( principals != null && principals.size() > 0 )
            {
                fireEvent( WikiSecurityEvent.LOGIN_ANONYMOUS, getLoginPrincipal( principals ), session );
                return true;
            }
        }
        
        // If by some unusual turn of events the Anonymous login module doesn't work, login failed!
        return false;
    }
    
    /**
     * Attempts to perform a WikiSession login for the given username/password
     * combination using JSPWiki's custom authentication mode, using the system default Locale.
     * The operation is otherwise identical to {@link #login(WikiSession, Locale, String, String)}.
     * @param session the current wiki session; may not be <code>null</code>.
     * @param username The user name
     * @param password the password
     * @return <code>true</code> if the username/password is valid; <code>false</code>
     *             if the LoginModule should be ignored, or the WikiSession was <code>null</code>
     * @throws LoginException
     *             if the LoginModule's <code>login()</code> or <code>commit()</code> phases
     *             failed for any reason, including invalid credentials.
     * @throws WikiSecurityException
     *             if the login failed for any other reason. The root-cause exception can
     *             be retrieved via {@link java.lang.Throwable#getCause()}
     */
    public final boolean login( WikiSession session, String username, String password ) throws WikiSecurityException, LoginException
    {
        return login( session, Locale.getDefault(), username, password );
    }

    /**
     * Attempts to perform a WikiSession login for the given username/password
     * combination using JSPWiki's custom authentication mode. In order to log in,
     * the JAAS LoginModule supplied by the WikiEngine property {@link #PROP_LOGIN_MODULE}
     * will be instantiated, and its
     * {@link javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)}
     * method will be invoked. By default, the {@link org.apache.wiki.auth.login.UserDatabaseLoginModule}
     * class will be used. When the LoginModule's <code>initialize</code> method is invoked,
     * an options Map populated by properties keys prefixed by {@link #PREFIX_LOGIN_MODULE_OPTIONS}
     * will be passed as a parameter.
     * @param session the current wiki session; may not be <code>null</code>.
     * @param username The user name. This is a login name, not a WikiName. In
     *            most cases they are the same, but in some cases, they might
     *            not be.
     * @param password the password
     * @return <code>true</code> if the username/password is valid; <code>false</code>
     *             if the LoginModule should be ignored, or the WikiSession was <code>null</code>
     * @throws LoginException
     *             if the LoginModule's <code>login()</code> or <code>commit()</code> phases
     *             failed for any reason, including invalid credentials.
     * @throws WikiSecurityException
     *             if the login failed for any other reason. The root-cause exception can
     *             be retrieved via {@link java.lang.Throwable#getCause()}
     */
    public final boolean login( WikiSession session, Locale locale, String username, String password ) throws WikiSecurityException, LoginException
    {
        if ( session == null )
        {
            log.error( "No wiki session provided, cannot log in." );
            return false;
        }

        // Protect against brute-force password guessing if configured to do so
        if ( m_throttleLogins )
        {
            delayLogin(username);
        }
        
        CallbackHandler handler = new WikiCallbackHandler(
                m_engine,
                locale,
                username,
                password );
        
        // Execute the user's specified login module
        Set<Principal> principals = doJAASLogin( m_loginModuleClass, handler, m_loginModuleOptions );
        if ( principals.size() > 0 )
        {
            fireEvent(WikiSecurityEvent.LOGIN_AUTHENTICATED, getLoginPrincipal( principals ), session );
            for ( Principal principal : principals )
            {
                fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, principal, session );
            }
            return true;
        }
        return false;
    }
    
    /**
     *  This method builds a database of login names that are being attempted, and will try to
     *  delay if there are too many requests coming in for the same username.
     *  <p>
     *  The current algorithm uses 2^loginattempts as the delay in milliseconds, i.e.
     *  at 10 login attempts it'll add 1.024 seconds to the login.
     *  
     *  @param username The username that is being logged in
     */
    private void delayLogin( String username )
    {
        try
        {
            m_lastLoginAttempts.cleanup( LASTLOGINS_CLEANUP_TIME );
            int count = m_lastLoginAttempts.count( username );
            
            long delay = Math.min( 1<<count, MAX_LOGIN_DELAY );
            log.debug( "Sleeping for "+delay+" ms to allow login." );
            Thread.sleep( delay );
            
            m_lastLoginAttempts.add( username );
        }
        catch( InterruptedException e )
        {
            // FALLTHROUGH is fine
        }
    }

    /**
     * Logs the user out by retrieving the WikiSession associated with the
     * HttpServletRequest and unbinding all of the Subject's Principals,
     * except for {@link Role#ALL}, {@link Role#ANONYMOUS}.
     * is a cheap-and-cheerful way to do it without invoking JAAS LoginModules.
     * The logout operation will also flush the JSESSIONID cookie from
     * the user's browser session, if it was set.
     * @param request the current HTTP request
     */
    public final void logout( HttpServletRequest request )
    {
        if( request == null )
        {
            log.error( "No HTTP reqest provided; cannot log out." );
            return;
        }

        HttpSession session = request.getSession();
        String sid = ( session == null ) ? "(null)" : session.getId();
        if( log.isDebugEnabled() )
        {
            log.debug( "Invalidating WikiSession for session ID=" + sid );
        }
        // Retrieve the associated WikiSession and clear the Principal set
        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        Principal originalPrincipal = wikiSession.getLoginPrincipal();
        wikiSession.invalidate();

        // Remove the wikiSession from the WikiSession cache
        WikiSession.removeWikiSession( m_engine, request );

        // We need to flush the HTTP session too
        if ( session != null )
        {
            session.invalidate();
        }

        // Log the event
        fireEvent( WikiSecurityEvent.LOGOUT, originalPrincipal, null );
    }

    /**
     * Determines whether this WikiEngine allows users to assert identities using
     * cookies instead of passwords. This is determined by inspecting
     * the WikiEngine property {@link #PROP_ALLOW_COOKIE_ASSERTIONS}.
     * @return <code>true</code> if cookies are allowed
     */
    public final boolean allowsCookieAssertions()
    {
        return m_allowsCookieAssertions;
    }

    /**
     *  Determines whether this WikiEngine allows users to authenticate using
     *  cookies instead of passwords. This is determined by inspecting
     * the WikiEngine property {@link #PROP_ALLOW_COOKIE_AUTH}.
     *  @return <code>true</code> if cookies are allowed for authentication
     *  @since 2.5.62
     */
    public final boolean allowsCookieAuthentication()
    {
        return m_allowsCookieAuthentication;
    }
    
    /**
     * Determines whether the supplied Principal is a "role principal".
     * @param principal the principal to test
     * @return <code>true</code> if the Principal is of type
     *         {@link GroupPrincipal} or
     *         {@link org.apache.wiki.auth.authorize.Role},
     *         <code>false</code> otherwise
     */
    public static final boolean isRolePrincipal( Principal principal )
    {
        return principal instanceof Role || principal instanceof GroupPrincipal;
    }

    /**
     * Determines whether the supplied Principal is a "user principal".
     * @param principal the principal to test
     * @return <code>false</code> if the Principal is of type
     *         {@link GroupPrincipal} or
     *         {@link org.apache.wiki.auth.authorize.Role},
     *         <code>true</code> otherwise
     */
    public static final boolean isUserPrincipal( Principal principal )
    {
        return !isRolePrincipal( principal );
    }

    /**
     * Instantiates and executes a single JAAS
     * {@link javax.security.auth.spi.LoginModule}, and returns a Set of
     * Principals that results from a successful login. The LoginModule is instantiated,
     * then its {@link javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)}
     * method is called. The parameters passed to <code>initialize</code> is a 
     * dummy Subject, an empty shared-state Map, and an options Map the caller supplies.
     * If login succeeds, this method will return the Set of Principals. If it fails for any reason,
     * including invalid credentials, it will throw a {@link javax.security.auth.login.LoginException}.
     * 
     * @param clazz
     *            the LoginModule class to instantiate
     * @param handler
     *            the callback handler to supply to the LoginModule
     * @param options
     *            a Map of key/value strings for initializing the LoginModule
     * @return the set of Principals returned by the JAAS method {@link Subject#getPrincipals()}
     * @throws LoginException
     *             if the LoginModule's <code>login()</code> or <code>commit()</code> phases
     *             failed for any reason, including invalid credentials.
     * @throws WikiSecurityException
     *             if the LoginModule could not be instantiated. The root-cause exception can
     *             be retrieved via {@link java.lang.Throwable#getCause()}
     */
    protected Set<Principal> doJAASLogin(Class<? extends LoginModule> clazz, CallbackHandler handler, Map<String,String> options) throws WikiSecurityException, LoginException
    {
        // Instantiate the login module
        LoginModule loginModule = null;
        try
        {
            loginModule = clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new WikiSecurityException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new WikiSecurityException( e.getMessage(), e );
        }

        // Initialize the LoginModule
        Subject subject = new Subject();
        loginModule.initialize( subject, handler, EMPTY_MAP, options );

        // Try to log in:
        boolean commitSucceeded = false;
        boolean loginSucceeded = loginModule.login();
        if ( loginSucceeded )
        {
            commitSucceeded = loginModule.commit();
        }

        // If we successfully logged in & committed, return all the principals
        if ( loginSucceeded && commitSucceeded )
        {
            return subject.getPrincipals();
        }
        return NO_PRINCIPALS;
    }

    /**
     * Looks up and obtains a configuration file inside the WEB-INF folder of a
     * wiki webapp.
     * @param engine the wiki engine
     * @param name the file to obtain, <em>e.g.</em>, <code>jspwiki.policy</code>
     * @return the URL to the file
     */
    protected static final URL findConfigFile( WikiEngine engine, String name )
    {
        // Try creating an absolute path first
        File defaultFile = null;
        if( engine.getRootPath() != null )
        {
            defaultFile = new File( engine.getRootPath() + "/WEB-INF/" + name );
        }
        if ( defaultFile != null && defaultFile.exists() )
        {
            try
            {
                return defaultFile.toURL();
            }
            catch ( MalformedURLException e)
            {
                // Shouldn't happen, but log it if it does
                log.warn( "Malformed URL: " + e.getMessage() );
            }

        }

        // Ok, the absolute path didn't work; try other methods
        ClassLoader cl = AuthenticationManager.class.getClassLoader();

        URL path = cl.getResource("/WEB-INF/"+name);

        if( path == null )
            path = cl.getResource("/"+name);

        if( path == null )
            path = cl.getResource(name);

        if( path == null && engine.getServletContext() != null )
        {
            try
            {
                path = engine.getServletContext().getResource("/WEB-INF/"+name);
            }
            catch( MalformedURLException e )
            {
                // This should never happen unless I screw up
                log.error("Your code is b0rked.  You are a bad person.");
            }
        }

        return path;
    }

    /**
     * Returns the first Principal in a set that isn't a {@link org.apache.wiki.auth.authorize.Role} or
     * {@link org.apache.wiki.auth.GroupPrincipal}.
     * @param principals the principal set
     * @return the login principal
     */
    protected Principal getLoginPrincipal(Set<Principal> principals)
    {
        for (Principal principal: principals )
        {
            if ( isUserPrincipal( principal ) )
            {
                return principal;
            }
        }
        return null;
    }

    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance.
     * This is a convenience method.
     * @param listener the event listener
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * This is a convenience method.
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     *  Fires a WikiSecurityEvent of the provided type, Principal and target Object
     *  to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiSecurityEvent
     * @param type       the event type to be fired
     * @param principal  the subject of the event, which may be <code>null</code>
     * @param target     the changed Object, which may be <code>null</code>
     */
    protected final void fireEvent( int type, Principal principal, Object target )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiSecurityEvent(this,type,principal,target));
        }
    }
    
    /**
     * Initializes the options Map supplied to the configured LoginModule every time it is invoked by
     * {@link #doLoginModule(Class, CallbackHandler)}. The properties and values extracted from
     * <code>jspwiki.properties</code> are of the form
     * <code>jspwiki.loginModule.options.<var>param</var> = <var>value</var>, where
     * <var>param</var> is the key name, and <var>value</var> is the value.
     * @param props the properties used to initialize JSPWiki
     * @throws IllegalArgumentException if any of the keys are duplicated
     */
    private void initLoginModuleOptions(Properties props)
    {
        for ( Object key : props.keySet() )
        {
            String propName = key.toString();
            if ( propName.startsWith( PREFIX_LOGIN_MODULE_OPTIONS ) )
            {
                // Extract the option name and value
                String optionKey = propName.substring( PREFIX_LOGIN_MODULE_OPTIONS.length() ).trim();
                if ( optionKey.length() > 0 )
                {
                    String optionValue = props.getProperty( propName );
                    
                    // Make sure the key is unique before stashing the key/value pair
                    if ( m_loginModuleOptions.containsKey( optionKey ) )
                    {
                        throw new IllegalArgumentException( "JAAS LoginModule key " + propName + " cannot be specified twice!" );
                    }
                    m_loginModuleOptions.put( optionKey, optionValue );
                }
            }
        }
    }

}
