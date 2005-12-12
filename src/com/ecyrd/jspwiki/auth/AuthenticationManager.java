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
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.WebContainerCallbackHandler;
import com.ecyrd.jspwiki.auth.login.WikiCallbackHandler;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * Manages authentication activities for a WikiEngine: user login, logout, and 
 * credential refreshes. This class uses JAAS to determine how users log in.
 * @author Andrew Jaquith
 * @author Janne Jalkanen
 * @author Erik Bunn
 * @version $Revision: 1.17 $ $Date: 2005-12-12 07:00:28 $
 * @since 2.3
 */
public class AuthenticationManager
{

    /** The name of the built-in cookie authentication module */
    public static final String                 COOKIE_MODULE       =  CookieAssertionLoginModule.class.getName();

    /** The JAAS application name for the web container authentication stack. */
    public static final String                 LOGIN_CONTAINER     = "JSPWiki-container";

    /** The JAAS application name for the JSPWiki custom authentication stack. */
    public static final String                 LOGIN_CUSTOM        = "JSPWiki-custom";

    /** If this jspwiki.properties property is <code>true</code>, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";

    static Logger                              log                 = Logger.getLogger( AuthenticationManager.class );
    
    /** Static Boolean for lazily-initializing the "allows assertions" flag */
    private static Boolean                     m_allowsAssertions  = null;

    private WikiEngine                         m_engine            = null;

    /** If not <code>null</code>, contains the name of the admin user */
    private String              m_admin             = null;

    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    private static final String                PROP_ADMIN_USER     = "jspwiki.admin.user";
    private static final String                PROP_JAAS_CONFIG    = "java.security.auth.login.config";
    private static final String                PROP_POLICY_CONFIG  = "java.security.policy";
    private static final String                DEFAULT_JAAS_CONFIG = "jspwiki.jaas";
    private static final String                DEFAULT_POLICY      = "jspwiki.policy";    
    
    private URL findConfigFile( String name )
    {
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
    
    static
    {
    }
    
    /**
     * Creates an AuthenticationManager instance for the given WikiEngine and
     * the specified set of properties. All initialization for the modules is
     * done here.
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );

        if (! PolicyLoader.isJaasConfigured() ) 
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
        
        if (! PolicyLoader.isSecurityPolicyConfigured() )
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

        // Initialize admin user, if found in the properties
        m_admin = props.getProperty( PROP_ADMIN_USER, null );
        if ( m_admin != null )
        {
           log.info( "Administrative user configured." );
        }
        else
        {
            log.info( "Administrative user property not present; NOT configured." );
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
    public boolean isContainerAuthenticated()
    {
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
    public boolean login( HttpServletRequest request )
    {
        if ( request == null )
        {
            throw new IllegalStateException( "Wiki context's HttpRequest may not be null" );
        }
        
        WikiSession wikiSession = WikiSession.getWikiSession( request );
        if ( wikiSession == null )
        {
            throw new IllegalStateException( "Wiki context's WikiSession may not be null" );
        }
        
        CallbackHandler handler = new WebContainerCallbackHandler( request, m_engine.getUserDatabase() );
        return doLogin( wikiSession, handler, LOGIN_CONTAINER );
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
    public boolean login( WikiSession session, String username, String password )
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
     * Reloads user Principals into the suppplied WikiSession's Subject.
     * Existing Role principals are preserved; all other Principal
     * types are flushed and replaced by those returned by
     * {@link com.ecyrd.jspwiki.auth.user.UserDatabase#getPrincipals(String)}.
     * This method should generally be called after a user's {@link com.ecyrd.jspwiki.auth.user.UserProfile}
     * is saved. If the wiki session is null, or there is no matching user profile, the
     * method returns silently.
     * @param wikiSession
     */
    public void refreshCredentials( WikiSession wikiSession ) 
    {
      // Get the database and wiki session Subject
      UserDatabase database = m_engine.getUserDatabase();
      if ( database == null )
      {
          throw new IllegalStateException( "User database cannot be null." );
      }
      Subject subject = wikiSession.getSubject();
      
      // Copy all Role principals into a temporary cache
      Set oldPrincipals = subject.getPrincipals();
      Set newPrincipals = new HashSet();
      for (Iterator it = oldPrincipals.iterator(); it.hasNext();)
      {
          Principal principal = (Principal)it.next();
          if (principal instanceof Role)
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
     * Determines whether a WikiSession's Subject posesses a login id that
     * matches the administrator id; if so, this method injects
     * {@link com.ecyrd.jspwiki.auth.authorize.Role#ADMIN} into the principal
     * set. To extract the login id Principal, this method delegates
     * to {@link com.ecyrd.jspwiki.WikiSession#getLoginPrincipal()}.
     * 
     * @param wikiSession the wiki session whose Subject is being examined
     */
    protected void checkForAdmin( WikiSession wikiSession )
    {
        Principal loginPrincipal = wikiSession.getLoginPrincipal();
        if ( m_admin != null && loginPrincipal.getName().equals( m_admin ) )
        {
            wikiSession.getSubject().getPrincipals().add( Role.ADMIN );
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
    private boolean doLogin( final WikiSession wikiSession, final CallbackHandler handler, final String application )
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
            loginContext.login();

            // Lastly, inject the ADMIN role if the user's id is privileged
            checkForAdmin( wikiSession );

            return true;
        }
        catch( FailedLoginException e )
        {
            //
            //  Just a mistyped password or a cracking attempt.  No need to worry
            //  and alert the admin
            //
            log.info("Failed login: "+e.getLocalizedMessage());
            return false;
        }
        catch( AccountExpiredException e )
        {
            log.info("Expired account: "+e.getLocalizedMessage());
            return false;
        }
        catch( CredentialExpiredException e )
        {
            log.info("Credentials expired: "+e.getLocalizedMessage());
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
     * Determines whether this WikiEngine allows users to assert identities using
     * cookies instead of passwords. This is determined by inspecting
     * the LoginConfiguration for application <code>JSPWiki-container</code>.
     * @return <code>true</code> if cookies are allowed
     */
    public static boolean allowsCookieAssertions()
    {
        // Lazily initialize
        if ( m_allowsAssertions == null )
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
     * Logs the user out by retrieving the WikiSession associated with the
     * HttpServletRequest and unbinding all of the Subject's Principals,
     * except for {@link Role#ALL}, {@link Role#
     * is a cheap-and-cheerful way to do it without invoking JAAS LoginModules.
     * The logout operation will also flush the JSESSIONID cookie from
     * the user's browser session, if it was set.
     * @param session the current HTTP session
     */
    public static void logout( HttpServletRequest request )
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
        WikiSession wikiSession = WikiSession.getWikiSession( request );
        wikiSession.invalidate();
        
        // We need to flush the HTTP session too
        session.invalidate();
    }

}