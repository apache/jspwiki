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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.WebContainerCallbackHandler;
import com.ecyrd.jspwiki.auth.login.WikiCallbackHandler;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.event.WikiEventListener;

/**
 * Manages authentication activities for a WikiEngine: user login, logout, and 
 * credential refreshes. This class uses JAAS to determine how users log in.
 * @author Andrew Jaquith
 * @author Janne Jalkanen
 * @author Erik Bunn
 * @version $Revision: 1.27 $ $Date: 2006-06-17 23:09:56 $
 * @since 2.3
 */
public final class AuthenticationManager
{

    /** The name of the built-in cookie authentication module */
    public static final String                 COOKIE_MODULE       =  CookieAssertionLoginModule.class.getName();

    /** The JAAS application name for the web container authentication stack. */
    public static final String                 LOGIN_CONTAINER     = "JSPWiki-container";

    /** The JAAS application name for the JSPWiki custom authentication stack. */
    public static final String                 LOGIN_CUSTOM        = "JSPWiki-custom";

    /** If this jspwiki.properties property is <code>true</code>, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";

    protected static final Logger                              log                 = Logger.getLogger( AuthenticationManager.class );
    
    /** Was JAAS login config already set before we startd up? */
    protected boolean m_isJaasConfiguredAtStartup = false;
    
    /** Was Java security policy already set before we startd up? */
    protected boolean m_isJavaPolicyConfiguredAtStartup = false;
    
    /** Static Boolean for lazily-initializing the "allows assertions" flag */
    private static Boolean                     m_allowsAssertions  = null;

    private WikiEngine                         m_engine            = null;

    /** Listeners for security events */
    private final Set        m_listeners = new HashSet();
    
    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    /** Value specifying that the user wants to use the container-managed security, just like
     *  in JSPWiki 2.2.
     */
    public static final String                SECURITY_CONTAINER= "container";

    /** Value specifying that the user wants to use the built-in JAAS-based system */
    public static final String                SECURITY_JAAS     = "jaas";

    /**
     *  This property determines whether we use JSPWiki authentication or not.
     *  Possible values are AUTH_JAAS or AUTH_CONTAINER.
     *  
     */
    
    public  static final String                PROP_SECURITY       = "jspwiki.security";

    private static final String                PROP_JAAS_CONFIG    = "java.security.auth.login.config";
    private static final String                PROP_POLICY_CONFIG  = "java.security.policy";
    private static final String                DEFAULT_JAAS_CONFIG = "jspwiki.jaas";
    private static final String                DEFAULT_POLICY      = "jspwiki.policy";    
    
    private static       boolean               c_useJAAS = true;
    
    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public synchronized final void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.add( listener );
    }

    /**
     * Creates an AuthenticationManager instance for the given WikiEngine and
     * the specified set of properties. All initialization for the modules is
     * done here.
     */
    public final void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );
        m_isJaasConfiguredAtStartup = PolicyLoader.isJaasConfigured();
        m_isJavaPolicyConfiguredAtStartup = PolicyLoader.isSecurityPolicyConfigured();

        c_useJAAS = SECURITY_JAAS.equals(props.getProperty( PROP_SECURITY, SECURITY_JAAS ));
        
        if( !c_useJAAS ) return;
        
        //
        //  The rest is JAAS implementation
        //
        
        log.info( "Checking JAAS configuration..." );

        if (! m_isJaasConfiguredAtStartup ) 
        {
            URL config = findConfigFile( DEFAULT_JAAS_CONFIG );
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
        
        log.info( "Checking security policy configuration..." );
        if (! m_isJavaPolicyConfiguredAtStartup )
        {
            URL policy = findConfigFile( DEFAULT_POLICY );
            log.info("Security policy not configured. Installing default policy: " + policy
                + ". Please set the "+PROP_POLICY_CONFIG+" system property, if you're not happy with the default.");
            try 
            { 
                PolicyLoader.setSecurityPolicy( policy );
            }
            catch ( SecurityException e)
            {
                log.error("Could not install security policy: " + e.getMessage());
            }
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
        
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        if ( authorizer != null && authorizer instanceof WebContainerAuthorizer )
        {
             return ( ( WebContainerAuthorizer )authorizer ).isContainerAuthorized();
        }
        return false;
    }

    /**
     * Logs in the user by attempting to populate a WikiSession Subject from 
     * a web servlet request. This method leverages container-managed authentication.
     * This method logs in the user if the user's status is "unknown" to the 
     * WikiSession, or if the Http servlet container's authentication status has 
     * changed. This method assumes that the HttpServletRequest is not null; otherwise,
     * an IllegalStateException is thrown. This method is a <em>privileged</em> action;
     * the caller must posess the (name here) permission.
     * @param request servlet request for this user
     * @throws IllegalStateException if the wiki context's
     *             <code>getHttpRequest</code> or <code>getWikiSession</code>
     *             methods return null
     * @throws IllegalArgumentException if the <code>context</code> parameter
     *             is null
     * @since 2.3
     */
    public final boolean login( HttpServletRequest request )
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
            Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
            CallbackHandler handler = new WebContainerCallbackHandler( request, m_engine.getUserDatabase(), authorizer );
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
     */
    public final boolean login( WikiSession session, String username, String password )
    {
        if ( session == null )
        {
            log.error( "No wiki session provided, cannot log in." );
            return false;
        }
        
        CallbackHandler handler = new WikiCallbackHandler( m_engine.getUserDatabase(), username, password );
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
        if ( request == null )
        {
            log.error( "No HTTP reqest provided; cannot log out." );
            return;
        }
        
        HttpSession session = request.getSession();
        String sid = ( session == null ) ? "(null)" : session.getId();
        if ( log.isDebugEnabled() )
        {
            log.debug( "Invalidating WikiSession for session ID=" + sid );
        }
        // Retrieve the associated WikiSession and clear the Principal set
        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        wikiSession.invalidate();
        
        // Remove the wikiSession from the WikiSession cache
        WikiSession.removeWikiSession( m_engine, request );
        
        // We need to flush the HTTP session too
        session.invalidate();
        
        // Log the event
        WikiSecurityEvent event = new WikiSecurityEvent( this, WikiSecurityEvent.LOGOUT, wikiSession.getLoginPrincipal(), null );
        fireEvent( event );
    }
    
    /**
     * Reloads user Principals into the suppplied WikiSession's Subject.
     * Existing Role principals are preserved; all other Principal
     * types are flushed and replaced by those returned by
     * {@link com.ecyrd.jspwiki.auth.user.UserDatabase#getPrincipals(String)}.
     * This method should generally be called after a user's {@link com.ecyrd.jspwiki.auth.user.UserProfile}
     * is saved. If the wiki session is null, or there is no matching user profile, the
     * method returns silently.
     * @param wikiSession
     */
    public final void refreshCredentials( WikiSession wikiSession ) 
    {
        // Get the database and wiki session Subject
        UserDatabase database = m_engine.getUserDatabase();
        if ( database == null )
        {
            throw new IllegalStateException( "User database cannot be null." );
        }
        Subject subject = wikiSession.getSubject();
      
        // Copy all Role and GroupPrincipal principals into a temporary cache
        Set oldPrincipals = subject.getPrincipals();
        Set newPrincipals = new HashSet();
        for (Iterator it = oldPrincipals.iterator(); it.hasNext();)
        {
            Principal principal = (Principal)it.next();
            if (principal instanceof Role || principal instanceof GroupPrincipal )
            {
                newPrincipals.add( principal );
            }
        }
        String searchId = wikiSession.getUserPrincipal().getName();
        if ( searchId == null )
        {
            // Oh dear, this wasn't an authenticated user after all
            log.info("Refresh principals failed because WikiSession had no user Principal; maybe not logged in?");
            return;
        }
      
        // Look up the user and go get the new Principals
        try 
        {
            UserProfile profile = database.find( searchId );
            Principal[] principals = database.getPrincipals( profile.getLoginName() );
            for (int i = 0; i < principals.length; i++)
            {
                newPrincipals.add( principals[i] );
            }
        
            // Replace the Subject's old Principals with the new ones
            oldPrincipals.clear();
            oldPrincipals.addAll( newPrincipals );
        }
        catch ( NoSuchPrincipalException e )
        {
            // It would be extremely surprising if we get here....
            log.error("Refresh principals failed because user profile matching '" + searchId + "' not found.");
        }
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.remove( listener );
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
        if( m_allowsAssertions == null )
        {
            m_allowsAssertions = Boolean.FALSE;
          
            // Figure out whether cookie assertions are allowed
            Configuration loginConfig = (Configuration)AccessController.doPrivileged(new PrivilegedAction()
              {
                  public Object run() {
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
                        m_allowsAssertions = Boolean.TRUE;
                    }
                }
            }
        }
        
        return m_allowsAssertions.booleanValue();
    }
    
    /**
     * Fires a wiki event to all registered listeners.
     * @param event the event
     */
    protected final void fireEvent( WikiSecurityEvent event )
    {
        for (Iterator it = m_listeners.iterator(); it.hasNext(); )
        {
            WikiEventListener listener = (WikiEventListener)it.next();
            listener.actionPerformed(event);
        }
    }
    
    /**
     * Injects GroupPrincipal objects into the user's Principal set
     * based on the groups the user belongs to. This method also
     * attaches a WikiEventListener to the GroupManager so that
     * changes to groups are detected automatically.
     * @param session the wiki session
     */
    protected final void injectGroupPrincipals( WikiSession session )
    {
        Subject subject = session.getSubject();
        
        // Get the GroupManager and test for each Group
        GroupManager manager = m_engine.getGroupManager();
        Principal[] groups = manager.getRoles();
        for ( int i = 0; i < groups.length; i++ )
        {
            if ( manager.isUserInRole( session, groups[i] ) )
            {
                Principal groupPrincipal = new GroupPrincipal( (Group)groups[i] );
                subject.getPrincipals().add( groupPrincipal );
            }
        }
        
        // Add the user's wiki session as a security event listener
        manager.addWikiEventListener( session );
    }
    
    /**
     * Injects Role Principals into the user's Principal set
     * based on the roles the user possesses, according to the
     * external {@link Authorizer}. This method is called during
     * once, during login. The algorithm first calls the 
     * {@link Authorizer#getRoles()} to obtain the array of
     * Principals the authorizer knows about. Then, the method
     * {@link Authorizer#isUserInRole(WikiSession, Principal)} is
     * called for each Principal. If the user possesses the role, 
     * an equivalent role Principal is injected into the user's
     * principal set.
     * 
     * @param session the wiki session
     */
    protected final void injectRolePrincipals( WikiSession session )
    {
        Subject subject = session.getSubject();
        
        // Get the authorizer's known roles, then test for each
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        Principal[] roles = authorizer.getRoles();
        for ( int i = 0; i < roles.length; i++ )
        {
            Principal role = roles[i];
            if ( authorizer.isUserInRole( session, role ) )
            {
                String roleName = role.getName();
                if ( !Role.isReservedName( roleName ) )
                {
                    subject.getPrincipals().add( new Role( roleName ) );
                }
            }
        }
    }
    
    /**
     * Log in to the application using a given JAAS LoginConfiguration.
     * @param wikiSession the current wiki session, to which the Subject will be associated
     * @param handler handles callbacks sent by the LoginModules in the configuration
     * @param application the name of the application whose LoginConfiguration should be used
     * @return the result of the login
     * @throws WikiSecurityException
     */
    private final boolean doLogin( final WikiSession wikiSession, final CallbackHandler handler, final String application )
    {
        try
        {
            LoginContext loginContext  = (LoginContext)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run() {
                    try
                    {
                        return new LoginContext( application, wikiSession.getSubject(), handler );
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
            }
            else
            {
                log.error("No login context.  Please double-check that JSPWiki found your 'jspwiki.jaas' file or the contents have been appended to your regular JAAS file.");
                return false;
            }
            
            // If the user authenticated, inject role and group principals and log the event
            if ( wikiSession.isAuthenticated() )
            {
                injectGroupPrincipals( wikiSession );
                injectRolePrincipals( wikiSession );
                WikiSecurityEvent event = new WikiSecurityEvent( this, WikiSecurityEvent.LOGIN_AUTHENTICATED, wikiSession.getLoginPrincipal(), null );
                fireEvent( event );
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
            WikiSecurityEvent event = new WikiSecurityEvent( this, WikiSecurityEvent.LOGIN_FAILED, wikiSession.getLoginPrincipal(), null );
            fireEvent( event );
            return false;
        }
        catch( AccountExpiredException e )
        {
            log.info("Expired account: "+e.getLocalizedMessage());
            WikiSecurityEvent event = new WikiSecurityEvent( this, WikiSecurityEvent.LOGIN_ACCOUNT_EXPIRED, wikiSession.getLoginPrincipal(), null );
            fireEvent( event );
            return false;
        }
        catch( CredentialExpiredException e )
        {
            log.info("Credentials expired: "+e.getLocalizedMessage());
            WikiSecurityEvent event = new WikiSecurityEvent( this, WikiSecurityEvent.LOGIN_CREDENTIAL_EXPIRED, wikiSession.getLoginPrincipal(), null );
            fireEvent( event );
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
    
    private final URL findConfigFile( String name )
    {
        // Try creating an absolute path first
        File defaultFile = null;
        if( m_engine.getRootPath() != null )
        {
            defaultFile = new File( m_engine.getRootPath() + "/WEB-INF/" + name );
        }
        if ( defaultFile != null && defaultFile.exists() ) 
        {
            try {
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
        
        if( path == null && m_engine.getServletContext() != null )
        {
            try
            {
                path = m_engine.getServletContext().getResource("/WEB-INF/"+name);
            }
            catch( MalformedURLException e )
            {
                // This should never happen unless I screw up
                log.fatal("Your code is b0rked.  You are a bad person.");
            }
        }

        return path;
    }

}