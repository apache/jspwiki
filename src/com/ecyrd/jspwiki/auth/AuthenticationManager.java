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
import java.security.Permission;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.WebContainerCallbackHandler;
import com.ecyrd.jspwiki.auth.login.WikiCallbackHandler;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * Manages authentication activities for a WikiEngine: user login, logout, and 
 * credential refreshes. This class uses JAAS to determine how users log in.
 * @author Andrew Jaquith
 * @author Janne Jalkanen
 * @author Erik Bunn
 * @version $Revision: 1.6 $ $Date: 2005-07-23 20:55:00 $
 * @since 2.3
 */
public class AuthenticationManager
{

    public static final String                 COOKIE_MODULE       =  CookieAssertionLoginModule.class.getName();
    
    public static final String                 LOGIN_CONTAINER     = "JSPWiki-container";

    public static final String                 LOGIN_CUSTOM        = "JSPWiki-custom";

    /** If true, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";

    public static final String                 PROP_USE_CMS_AUTH   = "jspwiki.useContainerAuth";

    static Logger                              log                 = Logger.getLogger( AuthenticationManager.class );
    
    private boolean                            m_containerAuth     = true;

    private WikiEngine                         m_engine            = null;

    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    private static final String                PROP_JAAS_CONFIG    = "java.security.auth.login.config";
    private static final String                PROP_POLICY_CONFIG  = "java.security.policy";
    
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
        m_containerAuth  = TextUtil.getBooleanProperty( props, PROP_USE_CMS_AUTH, m_containerAuth );
        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );

        //
        //  Give user some helpful hints.
        //
        AppConfigurationEntry[] jspwikiConfig = null;
        
        try
        {
            Configuration jaasconfig = Configuration.getConfiguration();
        
            jspwikiConfig = jaasconfig.getAppConfigurationEntry(LOGIN_CONTAINER);
        }
        catch( SecurityException e )
        {
            log.info("Unable to get security configuration.  Falling back on default.");
            log.debug("Exception is",e);
        }
        
        if( jspwikiConfig == null )
        {
            log.warn("Your JAAS configuration file does not contain an entry for '"+LOGIN_CONTAINER+
                     "'. You need to set the "+PROP_JAAS_CONFIG+" system property to point at the jspwiki.jaas -file,"+ 
                     "or add the entries from jspwiki.jaas to your own JAAS configuration file.");
            
            URL guessedJaasPath = findConfigFile( "jspwiki.jaas" );
            
            if( guessedJaasPath != null )
            {
                log.info("I'm falling back on the default, found in "+guessedJaasPath.toString());
                System.setProperty( PROP_JAAS_CONFIG, guessedJaasPath.toString() );
         
                //
                //  Set also jspwiki.policy
                //
                
                if( System.getProperty( PROP_POLICY_CONFIG ) == null )
                {
                    URL guessedPolicyPath = findConfigFile("jspwiki.policy");
                
                    if( guessedPolicyPath != null )
                    {
                        log.info("I am also assuming you have not added jspwiki.policy to the path");
                        log.info("Please set the "+PROP_POLICY_CONFIG+" system property, if you're not happy with the default.");
                        log.info("Set default to "+guessedPolicyPath.toString());
                        
                        System.setProperty( PROP_POLICY_CONFIG, guessedPolicyPath.toString() );
                    }
                    else
                    {
                        log.warn("I cannot locate your 'jspwiki.policy' file.  Please copy one from the default distribution.");
                    }
                }
                else
                {
                    log.warn("It may be that your java.policy is broken - please make sure you have jspwiki properties in your java.policy file");
                }
            }
            else
            {
                log.warn("Cannot locate jspwiki.jaas.  Please copy one from the default jspwiki installation.");
            }
        }        
    }
    
    /**
     * Returns true if this WikiEngine uses container-managed authentication.
     * This method is used primarily for cosmetic purposes in the JSP tier, and
     * performs no meaningful security function per se. Defaults to true unless
     * property {@link #PROP_USE_CMS_AUTH}was set.
     * @return <code>true</code> if the wiki's authentication is managed by
     *         the container, <code>false</code> otherwise
     */
    public boolean isContainerAuthenticated()
    {
        return m_containerAuth;
    }

    /**
     * Logs in the user by attempting to obtain a Subject from web container via
     * the currrent wiki context. This method logs in the user if the
     * user's status is "unknown" to the WikiSession, or if the Http servlet
     * container's authentication status has changed. This method assumes that
     * the WikiContext has been previously initialized with an
     * HttpServletRequest and a WikiSession. If neither of these conditions are
     * true, an IllegalStateException is thrown. This method is a
     * <em>privileged</em> action; the caller must posess the (name here)
     * permission.
     * @param context wiki context for this user.
     * @throws IllegalStateException if the wiki context's
     *             <code>getHttpRequest</code> or <code>getWikiSession</code>
     *             methods return null
     * @throws IllegalArgumentException if the <code>context</code> parameter
     *             is null
     * @since 2.3
     */
    public boolean loginContainer( WikiContext context )
    {
        // TODO: this should be a privileged action

        // If the WikiSession's Subject doesn't have any principals,
        // do a "login".
        if ( context == null )
        {
            throw new IllegalArgumentException( "Context may not be null" );
        }
        
        WikiSession wikiSession = context.getWikiSession();
        HttpServletRequest request = context.getHttpRequest();
        
        if ( wikiSession == null )
        {
            throw new IllegalStateException( "Wiki context's WikiSession may not be null" );
        }
        
        if ( request == null )
        {
            throw new IllegalStateException( "Wiki context's HttpRequest may not be null" );
        }
        
        CallbackHandler handler = new WebContainerCallbackHandler( request, m_engine.getUserDatabase() );
        return doLogin( wikiSession, handler, LOGIN_CONTAINER );
    }

    /**
     * Attempts to perform a login for the given username/password combination.
     * @param username The user name. This is a login name, not a WikiName. In
     *            most cases they are the same, but in some cases, they might
     *            not be.
     * @param password The password
     * @return true, if the username/password is valid
     */
    public boolean loginCustom( String username, String password, HttpServletRequest request )
    {
        if ( request == null )
        {
            log.error( "No Http request provided, cannot log in." );
            return false;
        }
        
        WikiSession wikiSession = WikiSession.getWikiSession( request );
        Subject subject = wikiSession.getSubject();
        subject.getPrincipals().clear();
        CallbackHandler handler = new WikiCallbackHandler( m_engine.getUserDatabase(), username, password );
        
        return doLogin( wikiSession, handler, LOGIN_CUSTOM );
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
     * Determines whether authentication is required to view wiki pages. This is
     * done by checking for the PagePermission.VIEW permission using a null
     * WikiContext. It delegates the check to
     * {@link AuthorizationManager#checkPermission(WikiContext, Permission)}.
     * @return <code>true</code> if logins are required
     */
    public boolean strictLogins()
    {
        return ( m_engine.getAuthorizationManager().checkPermission( null, PagePermission.VIEW ) );
    }

    /**
     * Log in to the application using a given JAAS LoginConfiguration.
     * @param wikiSession the current wiki session, to which the Subject will be associated
     * @param handler handles callbacks sent by the LoginModules in the configuration
     * @param application the name of the application whose LoginConfiguration should be used
     * @return the result of the login
     * @throws WikiSecurityException
     */
    private boolean doLogin( WikiSession wikiSession, CallbackHandler handler, String application )
    {
        try
        {
            LoginContext loginContext = new LoginContext( application, handler );
            loginContext.login();
            Subject subject = loginContext.getSubject();
            wikiSession.setSubject( subject );
            
            // TODO: Inject Role.ADMIN if user is named admin or part of role
            
            return true;
        }
        catch( LoginException e )
        {
            log.error( "Couldn't log in. Is something wrong with your jaas.config file?\nMessage="
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
        Configuration loginConfig = Configuration.getConfiguration();
        AppConfigurationEntry[] configs = loginConfig.getAppConfigurationEntry( LOGIN_CONTAINER );
        for ( int i = 0; i < configs.length; i++ )
        {
            AppConfigurationEntry config = configs[i];
            if ( COOKIE_MODULE.equals( config.getLoginModuleName() ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs the user out by invalidating the Http session associated with the
     * Wiki context. As a consequence, this will also automatically unbind the
     * WikiSession, and with it all of its Principal objects and Subject. This
     * is a cheap-and-cheerful way to do it without invoking JAAS LoginModules.
     * The logout operation will also remove the JSESSIONID cookie from
     * the user's browser session, if it was set.
     * @param session the current HTTP session
     */
    public static void logout( HttpSession session )
    {
        if ( session == null )
        {
            log.error( "No HTTP session provided; cannot log out." );
            return;
        }
        // Clear the session
        session.invalidate();

        // Remove JSESSIONID in case it is still kicking around
        Cookie sessionCookie = new Cookie("JSESSIONID", null);
        sessionCookie.setMaxAge(0);
    }

}