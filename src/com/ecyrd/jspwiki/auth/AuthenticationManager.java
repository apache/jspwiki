/*
 * JSPWiki - a JSP-based WikiWiki clone. Copyright (C) 2001-2003 Janne Jalkanen
 * (Janne.Jalkanen@iki.fi) This program is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.ecyrd.jspwiki.auth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.CookieAuthenticationLoginModule;
import com.ecyrd.jspwiki.auth.login.WebContainerCallbackHandler;
import com.ecyrd.jspwiki.auth.login.WikiCallbackHandler;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventManager;
import com.ecyrd.jspwiki.event.WikiSecurityEvent;

/**
 * Manages authentication activities for a WikiEngine: user login, logout, and
 * credential refreshes. This class uses JAAS to determine how users log in.
 * @author Andrew Jaquith
 * @author Janne Jalkanen
 * @author Erik Bunn
 * @since 2.3
 */
public final class AuthenticationManager
{

    /** The name of the built-in cookie assertion module */
    public static final String                 COOKIE_MODULE       =  CookieAssertionLoginModule.class.getName();

    /** The name of the built-in cookie authentication module */
    public static final String                 COOKIE_AUTHENTICATION_MODULE =  CookieAuthenticationLoginModule.class.getName();

    /** The JAAS application name for the web container authentication stack. */
    public static final String                 LOGIN_CONTAINER     = "JSPWiki-container";

    /** The JAAS application name for the JSPWiki custom authentication stack. */
    public static final String                 LOGIN_CUSTOM        = "JSPWiki-custom";

    /** If this jspwiki.properties property is <code>true</code>, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";

    protected static final Logger              log                 = Logger.getLogger( AuthenticationManager.class );

    /** Was JAAS login config already set before we startd up? */
    protected boolean m_isJaasConfiguredAtStartup = false;

    /** Static Boolean for lazily-initializing the "allows assertions" flag */
    private static Boolean                     c_allowsAssertions  = null;

    /** Static Boolean for lazily-initializing the "allows cookie authentication" flag */
    private static Boolean                     c_allowsAuthentication = null;

    private WikiEngine                         m_engine            = null;

    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    /** Value specifying that the user wants to use the container-managed security, just like
     *  in JSPWiki 2.2.
     */
    public static final String                SECURITY_OFF      = "off";

    /** Just to provide compatibility with the old versions.  The same
     *  as SECURITY_OFF.
     *
     *  @deprecated
     */
    protected static final String             SECURITY_CONTAINER = "container";

    /** Value specifying that the user wants to use the built-in JAAS-based system */
    public static final String                SECURITY_JAAS     = "jaas";

    /**
     *  This property determines whether we use JSPWiki authentication or not.
     *  Possible values are AUTH_JAAS or AUTH_CONTAINER.
     *
     */

    public  static final String                PROP_SECURITY       = "jspwiki.security";
    private static final String                PROP_JAAS_CONFIG    = "java.security.auth.login.config";
    private static final String                DEFAULT_JAAS_CONFIG = "jspwiki.jaas";

    private static       boolean               c_useJAAS = true;

    /**
     * Creates an AuthenticationManager instance for the given WikiEngine and
     * the specified set of properties. All initialization for the modules is
     * done here.
     * @param engine the wiki engine
     * @param props the properties used to initialize the wiki engine
     * @throws WikiException if the AuthenticationManager cannot be initialized
     */
    public final void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );
        m_isJaasConfiguredAtStartup = PolicyLoader.isJaasConfigured();

        // Yes, writing to a static field is done here on purpose.
        c_useJAAS = SECURITY_JAAS.equals(props.getProperty( PROP_SECURITY, SECURITY_JAAS ));

        if( !c_useJAAS ) return;

        //
        //  The rest is JAAS implementation
        //

        log.info( "Checking JAAS configuration..." );

        if (! m_isJaasConfiguredAtStartup )
        {
            URL config = findConfigFile( engine, DEFAULT_JAAS_CONFIG );
            log.info("JAAS not configured. Installing default configuration: " + config
                + ". You can set the "+PROP_JAAS_CONFIG+" system property to point to your "
                + "jspwiki.jaas file, or add the entries from jspwiki.jaas to your own "
                + "JAAS configuration file.");
            try
            {
                PolicyLoader.setJaasConfiguration( config );
            }
            catch ( SecurityException e)
            {
                log.error("Could not configure JAAS: " + e.getMessage());
            }
        }
        else
        {
            log.info("JAAS already configured by some other application (leaving it alone...)");
        }
    }

    /**
     * Returns true if this WikiEngine uses container-managed authentication.
     * This method is used primarily for cosmetic purposes in the JSP tier, and
     * performs no meaningful security function per se. Delegates to
     * {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer#isContainerAuthorized()},
     * if used as the external authorizer; otherwise, returns <code>false</code>.
     * @return <code>true</code> if the wiki's authentication is managed by
     *         the container, <code>false</code> otherwise
     */
    public final boolean isContainerAuthenticated()
    {
        if( !c_useJAAS ) return true;

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
     * a web servlet request. This method leverages container-managed authentication.
     * This method logs in the user if the user's status is "unknown" to the
     * WikiSession, or if the Http servlet container's authentication status has
     * changed. This method assumes that the HttpServletRequest is not null; otherwise,
     * an IllegalStateException is thrown. This method is a <em>privileged</em> action;
     * the caller must posess the (name here) permission.</p>
     * <p>If <code>request</code> is <code>null</code>, or the WikiSession
     * cannot be located for this request, this method throws an {@link IllegalStateException}.</p>
     *             methods return null
     * @param request servlet request for this user
     * @return the result of the login operation: <code>true</code> if the user logged in
     * successfully; <code>false</code> otherwise
     * @throws com.ecyrd.jspwiki.auth.WikiSecurityException if the Authorizer or UserManager cannot be obtained
     * @since 2.3
     */
    public final boolean login( HttpServletRequest request ) throws WikiSecurityException
    {
        if ( request == null )
        {
            throw new IllegalStateException( "Wiki context's HttpRequest may not be null" );
        }

        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        if ( wikiSession == null )
        {
            throw new IllegalStateException( "Wiki context's WikiSession may not be null" );
        }

        // If using JAAS, try to log in; otherwise logins "always" succeed
        boolean login = true;
        if( c_useJAAS )
        {
            AuthorizationManager authMgr = m_engine.getAuthorizationManager();
            CallbackHandler handler = new WebContainerCallbackHandler(
                    m_engine,
                    request,
                    authMgr.getAuthorizer() );
            login = doLogin( wikiSession, handler, LOGIN_CONTAINER );
        }
        return login;
    }

    /**
     * Attempts to perform a WikiSession login for the given username/password
     * combination. This is custom authentication.
     * @param session the current wiki session; may not be null.
     * @param username The user name. This is a login name, not a WikiName. In
     *            most cases they are the same, but in some cases, they might
     *            not be.
     * @param password The password
     * @return true, if the username/password is valid
     * @throws com.ecyrd.jspwiki.auth.WikiSecurityException if the Authorizer or UserManager cannot be obtained
     */
    public final boolean login( WikiSession session, String username, String password ) throws WikiSecurityException
    {
        if ( session == null )
        {
            log.error( "No wiki session provided, cannot log in." );
            return false;
        }

        UserManager userMgr = m_engine.getUserManager();
        CallbackHandler handler = new WikiCallbackHandler(
                userMgr.getUserDatabase(),
                username,
                password );
        return doLogin( session, handler, LOGIN_CUSTOM );
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
     * the LoginConfiguration for application <code>JSPWiki-container</code>.
     * @return <code>true</code> if cookies are allowed
     */
    public static final boolean allowsCookieAssertions()
    {
        if( !c_useJAAS ) return true;

        // Lazily initialize
        if( c_allowsAssertions == null )
        {
            c_allowsAssertions = Boolean.FALSE;

            // Figure out whether cookie assertions are allowed
            Configuration loginConfig = (Configuration)AccessController.doPrivileged(new PrivilegedAction()
              {
                  public Object run()
                  {
                      return Configuration.getConfiguration();
                  }
              });

            if (loginConfig != null)
            {
                AppConfigurationEntry[] configs = loginConfig.getAppConfigurationEntry( LOGIN_CONTAINER );
                for ( int i = 0; i < configs.length; i++ )
                {
                    AppConfigurationEntry config = configs[i];
                    if ( COOKIE_MODULE.equals( config.getLoginModuleName() ) )
                    {
                        c_allowsAssertions = Boolean.TRUE;
                    }
                }
            }
        }

        return c_allowsAssertions.booleanValue();
    }

    /**
     *  Determines whether this WikiEngine allows users to authenticate using
     *  cookies instead of passwords. This is determined by inspecting
     *  the LoginConfiguration for application <code>JSPWiki-container</code>.
     *  @return <code>true</code> if cookies are allowed for authentication
     *  @since 2.5.62
     */
    public static final boolean allowsCookieAuthentication()
    {
        if( !c_useJAAS ) return true;

        // Lazily initialize
        if( c_allowsAuthentication == null )
        {
            c_allowsAuthentication = Boolean.FALSE;

            // Figure out whether cookie assertions are allowed
            Configuration loginConfig = (Configuration)AccessController.doPrivileged(new PrivilegedAction()
              {
                  public Object run()
                  {
                      return Configuration.getConfiguration();
                  }
              });

            if (loginConfig != null)
            {
                AppConfigurationEntry[] configs = loginConfig.getAppConfigurationEntry( LOGIN_CONTAINER );
                for ( int i = 0; i < configs.length; i++ )
                {
                    AppConfigurationEntry config = configs[i];
                    if ( COOKIE_AUTHENTICATION_MODULE.equals( config.getLoginModuleName() ) )
                    {
                        c_allowsAuthentication = Boolean.TRUE;
                    }
                }
            }
        }

        return c_allowsAuthentication.booleanValue();
    }
    /**
     * Determines whether the supplied Principal is a "role principal".
     * @param principal the principal to test
     * @return <code>true</code> if the Principal is of type
     *         {@link GroupPrincipal} or
     *         {@link com.ecyrd.jspwiki.auth.authorize.Role},
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
     *         {@link com.ecyrd.jspwiki.auth.authorize.Role},
     *         <code>true</code> otherwise
     */
    public static final boolean isUserPrincipal( Principal principal )
    {
        return !isRolePrincipal( principal );
    }

    /**
     * Log in to the application using a given JAAS LoginConfiguration. Any
     * configuration error
     * @param wikiSession the current wiki session, to which the Subject will be associated
     * @param handler handles callbacks sent by the LoginModules in the configuration
     * @param application the name of the application whose LoginConfiguration should be used
     * @return the result of the login
     * @throws WikiSecurityException
     */
    private final boolean doLogin( final WikiSession wikiSession, final CallbackHandler handler, final String application ) throws WikiSecurityException
    {
        try
        {
            LoginContext loginContext  = (LoginContext)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        return wikiSession.getLoginContext( application, handler );
                    }
                    catch( LoginException e )
                    {
                        log.error( "Couldn't retrieve login configuration.\nMessage="
                                   + e.getLocalizedMessage() );
                        return null;
                    }
                }
            });

            if( loginContext != null )
            {
                loginContext.login();
                fireEvent( WikiSecurityEvent.LOGIN_INITIATED, null, wikiSession );
            }
            else
            {
                log.error("No login context.  Please double-check that JSPWiki found your 'jspwiki.jaas' file or the contents have been appended to your regular JAAS file.");
                return false;
            }

            // Fire event for the correct authentication event
            if ( wikiSession.isAnonymous() )
            {
                fireEvent( WikiSecurityEvent.LOGIN_ANONYMOUS, wikiSession.getLoginPrincipal(), wikiSession );
            }
            else if ( wikiSession.isAsserted() )
            {
                fireEvent( WikiSecurityEvent.LOGIN_ASSERTED, wikiSession.getLoginPrincipal(), wikiSession );
            }
            else if ( wikiSession.isAuthenticated() )
            {
                fireEvent( WikiSecurityEvent.LOGIN_AUTHENTICATED, wikiSession.getLoginPrincipal(), wikiSession );
            }

            return true;
        }
        catch( FailedLoginException e )
        {
            //
            //  Just a mistyped password or a cracking attempt.  No need to worry
            //  and alert the admin
            //
            log.info("Failed login: "+e.getLocalizedMessage());
            fireEvent( WikiSecurityEvent.LOGIN_FAILED, wikiSession.getLoginPrincipal(), wikiSession );
            return false;
        }
        catch( AccountExpiredException e )
        {
            log.info("Expired account: "+e.getLocalizedMessage());
            fireEvent( WikiSecurityEvent.LOGIN_ACCOUNT_EXPIRED, wikiSession.getLoginPrincipal(), wikiSession );
            return false;
        }
        catch( CredentialExpiredException e )
        {
            log.info("Credentials expired: "+e.getLocalizedMessage());
            fireEvent( WikiSecurityEvent.LOGIN_CREDENTIAL_EXPIRED, wikiSession.getLoginPrincipal(), wikiSession );
            return false;
        }
        catch( LoginException e )
        {
            //
            //  This should only be caught if something unforeseen happens,
            //  so therefore we can log it as an error.
            //
            log.error( "Couldn't log in.\nMessage="
                       + e.getLocalizedMessage() );
            return false;
        }
        catch( SecurityException e )
        {
            log.error( "Could not log in.  Please check that your jaas.config file is found.", e );
            return false;
        }
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
                log.fatal("Your code is b0rked.  You are a bad person.");
            }
        }

        return path;
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
     * @see com.ecyrd.jspwiki.event.WikiSecurityEvent
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

}
