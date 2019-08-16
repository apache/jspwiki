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
package org.apache.wiki.auth;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebAuthorizer;
import org.apache.wiki.auth.authorize.WebContainerAuthorizer;
import org.apache.wiki.auth.login.AnonymousLoginModule;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.login.CookieAuthenticationLoginModule;
import org.apache.wiki.auth.login.UserDatabaseLoginModule;
import org.apache.wiki.auth.login.WebContainerCallbackHandler;
import org.apache.wiki.auth.login.WebContainerLoginModule;
import org.apache.wiki.auth.login.WikiCallbackHandler;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.util.TimedCounterList;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
 * @since 2.3
 */
public class AuthenticationManager {

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
    
    /** Whether logins should be throttled to limit brute-forcing attempts. Defaults to true. */
    public static final String                 PROP_LOGIN_THROTTLING = "jspwiki.login.throttling";

    protected static final Logger              log                 = Logger.getLogger( AuthenticationManager.class );

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

    /** The default {@link javax.security.auth.spi.LoginModule} class name to use for custom authentication. */
    private static final String                 DEFAULT_LOGIN_MODULE = "org.apache.wiki.auth.login.UserDatabaseLoginModule";
    
    /** Empty principal set. */
    private static final Set<Principal> NO_PRINCIPALS = new HashSet<>();

    /** Static Boolean for lazily-initializing the "allows assertions" flag */
    private boolean                     m_allowsCookieAssertions  = true;

    private boolean                     m_throttleLogins = true;

    /** Static Boolean for lazily-initializing the "allows cookie authentication" flag */
    private boolean                     m_allowsCookieAuthentication = false;

    private WikiEngine                         m_engine            = null;
    
    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

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
    public void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );

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
            log.error( e.getMessage(), e );
            throw new WikiException( "Could not instantiate LoginModule class.", e );
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
    public boolean isContainerAuthenticated()
    {
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
    public boolean login( HttpServletRequest request ) throws WikiSecurityException
    {
        HttpSession httpSession = request.getSession();
        WikiSession session = SessionMonitor.getInstance(m_engine).find( httpSession );
        AuthenticationManager authenticationMgr = m_engine.getAuthenticationManager();
        AuthorizationManager authorizationMgr = m_engine.getAuthorizationManager();
        CallbackHandler handler = null;
        Map<String,String> options = EMPTY_MAP;

        // If user not authenticated, check if container logged them in, or if
        // there's an authentication cookie
        if ( !session.isAuthenticated() )
        {
            // Create a callback handler
            handler = new WebContainerCallbackHandler( m_engine, request );
            
            // Execute the container login module, then (if that fails) the cookie auth module
            Set<Principal> principals = authenticationMgr.doJAASLogin( WebContainerLoginModule.class, handler, options );
            if ( principals.size() == 0 && authenticationMgr.allowsCookieAuthentication() )
            {
                principals = authenticationMgr.doJAASLogin( CookieAuthenticationLoginModule.class, handler, options );
            }
            
            // If the container logged the user in successfully, tell the WikiSession (and add all of the Principals)
            if ( principals.size() > 0 )
            {
                fireEvent( WikiSecurityEvent.LOGIN_AUTHENTICATED, getLoginPrincipal( principals ), session );
                for ( Principal principal : principals )
                {
                    fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, principal, session );
                }
                
                // Add all appropriate Authorizer roles
                injectAuthorizerRoles( session, authorizationMgr.getAuthorizer(), request );
            }
        }

        // If user still not authenticated, check if assertion cookie was supplied
        if ( !session.isAuthenticated() && authenticationMgr.allowsCookieAssertions() )
        {
            // Execute the cookie assertion login module
            Set<Principal> principals = authenticationMgr.doJAASLogin( CookieAssertionLoginModule.class, handler, options );
            if ( principals.size() > 0 )
            {
                fireEvent( WikiSecurityEvent.LOGIN_ASSERTED, getLoginPrincipal( principals ), session);
            }
        }

        // If user still anonymous, use the remote address
        if (session.isAnonymous() )
        {
            Set<Principal> principals = authenticationMgr.doJAASLogin( AnonymousLoginModule.class, handler, options );
            if ( principals.size() > 0 )
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
     * combination using JSPWiki's custom authentication mode. In order to log in,
     * the JAAS LoginModule supplied by the WikiEngine property {@link #PROP_LOGIN_MODULE}
     * will be instantiated, and its
     * {@link javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)}
     * method will be invoked. By default, the {@link org.apache.wiki.auth.login.UserDatabaseLoginModule}
     * class will be used. When the LoginModule's <code>initialize</code> method is invoked,
     * an options Map populated by properties keys prefixed by {@link #PREFIX_LOGIN_MODULE_OPTIONS}
     * will be passed as a parameter.
     * @param session the current wiki session; may not be <code>null</code>.
     * @param request the user's HTTP request. This parameter may be <code>null</code>, but the configured
     * LoginModule will not have access to the HTTP request in this case.
     * @param username The user name. This is a login name, not a WikiName. In
     *            most cases they are the same, but in some cases, they might
     *            not be.
     * @param password the password
     * @return true, if the username/password is valid
     * @throws org.apache.wiki.auth.WikiSecurityException if the Authorizer or UserManager cannot be obtained
     */
    public boolean login( WikiSession session, HttpServletRequest request, String username, String password ) throws WikiSecurityException
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
        
        CallbackHandler handler = new WikiCallbackHandler( m_engine, null, username, password );
        
        // Execute the user's specified login module
        Set<Principal> principals = doJAASLogin( m_loginModuleClass, handler, m_loginModuleOptions );
        if (principals.size() > 0)
        {
            fireEvent(WikiSecurityEvent.LOGIN_AUTHENTICATED, getLoginPrincipal( principals ), session );
            for ( Principal principal : principals )
            {
                fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, principal, session );
            }
            
            // Add all appropriate Authorizer roles
            injectAuthorizerRoles( session, m_engine.getAuthorizationManager().getAuthorizer(), null );
            
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
    public void logout( HttpServletRequest request )
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
    public boolean allowsCookieAssertions()
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
    public boolean allowsCookieAuthentication()
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
    public static boolean isRolePrincipal( final Principal principal ) {
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
    public static boolean isUserPrincipal( final Principal principal ) {
        return !isRolePrincipal( principal );
    }

    /**
     * Instantiates and executes a single JAAS
     * {@link javax.security.auth.spi.LoginModule}, and returns a Set of
     * Principals that results from a successful login. The LoginModule is instantiated,
     * then its {@link javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)}
     * method is called. The parameters passed to <code>initialize</code> is a 
     * dummy Subject, an empty shared-state Map, and an options Map the caller supplies.
     * 
     * @param clazz the LoginModule class to instantiate
     * @param handler the callback handler to supply to the LoginModule
     * @param options a Map of key/value strings for initializing the LoginModule
     * @return the set of Principals returned by the JAAS method {@link Subject#getPrincipals()}
     * @throws WikiSecurityException if the LoginModule could not be instantiated for any reason
     */
    protected Set<Principal> doJAASLogin(Class<? extends LoginModule> clazz, CallbackHandler handler, Map<String,String> options) throws WikiSecurityException
    {
        // Instantiate the login module
        final LoginModule loginModule;
        try {
            loginModule = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new WikiSecurityException(e.getMessage(), e );
        }

        // Initialize the LoginModule
        Subject subject = new Subject();
        loginModule.initialize( subject, handler, EMPTY_MAP, options );

        // Try to log in:
        boolean loginSucceeded = false;
        boolean commitSucceeded = false;
        try
        {
            loginSucceeded = loginModule.login();
            if (loginSucceeded)
            {
                commitSucceeded = loginModule.commit();
            }
        }
        catch (LoginException e)
        {
            // Login or commit failed! No principal for you!
        }

        // If we successfully logged in & committed, return all the principals
        if (loginSucceeded && commitSucceeded)
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
    protected static URL findConfigFile( WikiEngine engine, String name )
    {
        log.info( "looking for " + name + " inside WEB-INF " );
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
                return defaultFile.toURI().toURL();
            }
            catch ( MalformedURLException e)
            {
                // Shouldn't happen, but log it if it does
                log.warn( "Malformed URL: " + e.getMessage() );
            }

        }

        
        // Ok, the absolute path didn't work; try other methods

        URL path = null;
        
        if( engine.getServletContext() != null ) {
            final File tmpFile;
        	try {
                tmpFile = File.createTempFile( "temp." + name, "" );
            } catch( final IOException e ) {
        	    log.error( "unable to create a temp file to load onto the policy", e );
        	    return null;
            }
            tmpFile.deleteOnExit();
            log.info( "looking for /" + name + " on classpath" );
            //  create a tmp file of the policy loaded as an InputStream and return the URL to it
            try( final InputStream is = AuthenticationManager.class.getResourceAsStream( "/" + name );
                 final OutputStream os = new FileOutputStream( tmpFile )  ) {
                if( is == null ) {
                    throw new FileNotFoundException( name + " not found" );
                }
            	final URL url = engine.getServletContext().getResource( "/WEB-INF/" + name );
            	if( url != null ) {
            		return url;
            	}
            	
                final byte[] buff = new byte[1024];
                int bytes;
                while( ( bytes = is.read( buff ) ) != -1 ) {
                    os.write( buff, 0, bytes );
                }

                path = tmpFile.toURI().toURL();
            } catch( final MalformedURLException e ) {
                // This should never happen unless I screw up
                log.fatal( "Your code is b0rked.  You are a bad person.", e );
            } catch( final IOException e ) {
               log.error( "failed to load security policy from file " + name + ",stacktrace follows", e );
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
    protected Principal getLoginPrincipal( final Set< Principal > principals ) {
        for( final Principal principal : principals ) {
            if ( isUserPrincipal( principal ) ) {
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
    public synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * This is a convenience method.
     * @param listener the event listener
     */
    public synchronized void removeWikiEventListener( final WikiEventListener listener ) {
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
    protected void fireEvent( final int type, final Principal principal, final Object target ) {
        if ( WikiEventManager.isListening( this ) ) {
            WikiEventManager.fireEvent( this, new WikiSecurityEvent( this, type, principal, target ) );
        }
    }
    
    /**
     * Initializes the options Map supplied to the configured LoginModule every time it is invoked.
     * The properties and values extracted from
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
    
    /**
     * After successful login, this method is called to inject authorized role Principals into the WikiSession.
     * To determine which roles should be injected, the configured Authorizer
     * is queried for the roles it knows about by calling  {@link org.apache.wiki.auth.Authorizer#getRoles()}.
     * Then, each role returned by the authorizer is tested by calling {@link org.apache.wiki.auth.Authorizer#isUserInRole(WikiSession, Principal)}.
     * If this check fails, and the Authorizer is of type WebAuthorizer, the role is checked again by calling
     * {@link org.apache.wiki.auth.authorize.WebAuthorizer#isUserInRole(javax.servlet.http.HttpServletRequest, Principal)}).
     * Any roles that pass the test are injected into the Subject by firing appropriate authentication events.
     * @param session the user's current WikiSession
     * @param authorizer the WikiEngine's configured Authorizer
     * @param request the user's HTTP session, which may be <code>null</code>
     */
    private void injectAuthorizerRoles( WikiSession session, Authorizer authorizer, HttpServletRequest request )
    {
        // Test each role the authorizer knows about
        for ( Principal role : authorizer.getRoles() )
        {
            // Test the Authorizer
            if ( authorizer.isUserInRole( session, role ) )
            {
                fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, role, session );
                if ( log.isDebugEnabled() )
                {
                    log.debug("Added authorizer role " + role.getName() + "." );
                }
            }
            
            // If web authorizer, test the request.isInRole() method also
            else if ( request != null && authorizer instanceof WebAuthorizer )
            {
                WebAuthorizer wa = (WebAuthorizer)authorizer;
                if ( wa.isUserInRole( request, role ) )
                {
                    fireEvent( WikiSecurityEvent.PRINCIPAL_ADD, role, session );
                    if ( log.isDebugEnabled() )
                    {
                        log.debug("Added container role " + role.getName() + "." );
                    }
                }
            }
        }
    }

}
