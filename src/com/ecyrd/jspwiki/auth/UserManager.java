/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth;

import java.security.Permission;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.AbstractUserDatabase;
import com.ecyrd.jspwiki.auth.user.DuplicateUserException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.ui.InputValidator;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Provides a facade for user and group information.
 *  
 *  @author Janne Jalkanen
 *  @version $Revision: 1.45 $ $Date: 2006-05-20 23:56:17 $
 *  @since 2.3
 */
public class UserManager
{
    private WikiEngine m_engine;
    
    private static final Logger log = Logger.getLogger(UserManager.class);

    private static final String  PROP_USERDATABASE   = "jspwiki.userdatabase";

    // private static final String  PROP_ACLMANAGER     = "jspwiki.aclManager";

    /** Associateds wiki sessions with profiles */
    private Map              m_profiles     = new WeakHashMap(); 
    
    /** The user database loads, manages and persists user identities */
    private UserDatabase     m_database     = null;
    
    /** The group manager loads, manages and persists wiki groups */
    private GroupManager     m_groupManager = null;

    private boolean          m_useJAAS      = true;
    
    /**
     * Constructs a new UserManager instance.
     */
    public UserManager()
    {
    }
    
    /**
     *  Initializes the engine for its nefarious purposes.
     *  
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
        
        m_useJAAS = AuthenticationManager.SECURITY_JAAS.equals( props.getProperty(AuthenticationManager.PROP_SECURITY, AuthenticationManager.SECURITY_JAAS ) );
    }
    
    
    /**
     * Returns the GroupManager employed by this WikiEngine.
     * The GroupManager is lazily initialized.
     * @since 2.3
     */
    public GroupManager getGroupManager()
    {
        if( m_groupManager != null ) 
        {
            return m_groupManager;
        }
        
        if( !m_useJAAS )
        {
            m_groupManager = new DummyGroupManager();
            return m_groupManager;
        }
        
        String dbClassName = "<unknown>";
        String dbInstantiationError = null;
        Throwable cause = null;
        try 
        {
            Properties props = m_engine.getWikiProperties(); 
            dbClassName = props.getProperty( GroupManager.PROP_GROUPMANAGER );
            if( dbClassName == null ) 
            {
                dbClassName = DefaultGroupManager.class.getName();
            }
            log.info("Attempting to load group manager class " + dbClassName);
            Class dbClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.authorize", dbClassName );
            m_groupManager = (GroupManager) dbClass.newInstance();
            m_groupManager.initialize( m_engine, m_engine.getWikiProperties() );
            log.info("GroupManager initialized.");
        } 
        catch( ClassNotFoundException e ) 
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be found", e );
            dbInstantiationError = "Failed to locate GroupManager class " + dbClassName;
            cause = e;
        } 
        catch( InstantiationException e ) 
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be created", e );
            dbInstantiationError = "Failed to create GroupManager class " + dbClassName;
            cause = e;
        } 
        catch( IllegalAccessException e ) 
        {
            log.error( "You are not allowed to access user database class " + dbClassName, e );
            dbInstantiationError = "Access GroupManager class " + dbClassName + " denied";
            cause = e;
        }
        
        if( dbInstantiationError != null ) 
        {
            throw new RuntimeException( dbInstantiationError, cause );
        }
        
        return m_groupManager;
    }

    /**
     *  Returns the UserDatabase employed by this WikiEngine.
     *  The UserDatabase is lazily initialized by this method, if
     *  it does not exist yet.  If the initialization fails, this
     *  method will use the inner class DummyUserDatabase as
     *  a default (which is enough to get JSPWiki running).
     *  
     *  @since 2.3
     */
    
    // FIXME: Must not throw RuntimeException, but something else.
    public UserDatabase getUserDatabase()
    {
        if( m_database != null ) 
        {
            return m_database;
        }
        
        if( !m_useJAAS )
        {
            m_database = new DummyUserDatabase();
            return m_database;
        }
        
        String dbClassName = "<unknown>";
        
        try
        {
            dbClassName = WikiEngine.getRequiredProperty( m_engine.getWikiProperties(), 
                                                          PROP_USERDATABASE );

            log.info("Attempting to load user database class "+dbClassName);
            Class dbClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.user", dbClassName );
            m_database = (UserDatabase) dbClass.newInstance();
            m_database.initialize( m_engine, m_engine.getWikiProperties() );
            log.info("UserDatabase initialized.");
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "You have not set the '"+PROP_USERDATABASE+"'. You need to do this if you want to enable user management by JSPWiki." );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be found", e );
        }
        catch( InstantiationException e )
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be created", e );
        }
        catch( IllegalAccessException e )
        {
            log.error( "You are not allowed to access this user database class", e );
        }
        finally
        {
            if( m_database == null )
            {
                log.info("I could not create a database object you specified (or didn't specify), so I am falling back to a default.");
                m_database = new DummyUserDatabase();
            }
        }
        
        return m_database;
    }

    /**
     * Retrieves the {@link com.ecyrd.jspwiki.auth.user.UserProfile}for the
     * user in a wiki session. If the user is authenticated, the UserProfile
     * returned will be the one stored in the user database; if one does not
     * exist, a new one will be initialized and returned. If the user is
     * anonymous or asserted, the UserProfile will <i>always</i> be newly
     * initialized to prevent spoofing of identities. If a UserProfile needs to
     * be initialized, its
     * {@link com.ecyrd.jspwiki.auth.user.UserProfile#isNew()} method will
     * return <code>true</code>, and its login name will will be set
     * automatically if the user is authenticated. Note that this method does
     * not modify the retrieved (or newly created) profile otherwise; other
     * fields in the user profile may be <code>null</code>.
     * @param session the wiki session, which may not be <code>null</code>
     * @return the user's profile, which will be newly initialized if the user
     * is anonymous or asserted, or if the user cannot be found in the user
     * database
     * @throws WikiSecurityException if the database returns an exception
     * @throws IllegalStateException if a new UserProfile was created, but its
     * {@link com.ecyrd.jspwiki.auth.user.UserProfile#isNew()} method returns
     * <code>false</code>. This is meant as a quality check for UserDatabase
     * providers; it should only be thrown if the implementation is faulty.
     */
    public UserProfile getUserProfile( WikiSession session )
    {
        // Look up cached user profile
        UserProfile profile = (UserProfile)m_profiles.get( session );
        boolean newProfile = ( profile == null );
        Principal user = null;
        
        // If user is authenticated, figure out if this is an existing profile
        if ( session.isAuthenticated() )
        {
            user = session.getUserPrincipal();
            try
            {
                profile = m_database.find( user.getName() );
                newProfile = false;
            }
            catch( NoSuchPrincipalException e )
            {
            }
        }
        
        if ( newProfile ) 
        {
            profile = m_database.newProfile();
            if ( user != null )
            {
                profile.setLoginName( user.getName() );
            }
            if ( !profile.isNew() )
            {
                throw new IllegalStateException(
                        "New profile should be marked 'new'. Check your UserProfile implementation." );
            }
        }
        
        // Stash the profile for next time
        m_profiles.put( session, profile );
        return profile;
    }

    /**
     * Saves the {@link com.ecyrd.jspwiki.auth.user.UserProfile}for the user in
     * a wiki session. This method verifies that a user profile to be saved
     * doesn't collide with existing profiles; that is, the login name, wiki
     * name or full name is already used by another profile. If the profile
     * collides, a <code>DuplicateUserException</code> is thrown. After saving
     * the profile, the user database changes are committed, and the user's
     * credential set is refreshed; if custom authentication is used, this means
     * the user will be automatically be logged in.
     * @param session the wiki session, which may not be <code>null</code>
     * @param profile the user profile, which may not be <code>null</code>
     */
    public void setUserProfile( WikiSession session, UserProfile profile ) throws WikiSecurityException,
            DuplicateUserException
    {
        // Verify user is allowed to save profile!
        Permission p = new WikiPermission( m_engine.getApplicationName(), "editProfile" );
        if ( !m_engine.getAuthorizationManager().checkPermission( session, p ) )
        {
            throw new WikiSecurityException( "You are not allowed to save wiki profiles." );
        }

        // Check if profile is new, and see if container allows creation
        boolean newProfile = profile.isNew();

        // User profiles that may already have wikiname, fullname or loginname
        UserProfile oldProfile = getUserProfile( session );
        UserProfile otherProfile;
        try
        {
            otherProfile = m_database.findByLoginName( profile.getLoginName() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                throw new DuplicateUserException( "The login name '" + profile.getLoginName() + "' is already taken." );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
        try
        {
            otherProfile = m_database.findByFullName( profile.getFullname() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                throw new DuplicateUserException( "The full name '" + profile.getFullname() + "' is already taken." );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
        try
        {
            otherProfile = m_database.findByWikiName( profile.getWikiName() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                throw new DuplicateUserException( "The wiki name '" + profile.getWikiName() + "' is already taken." );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }

        // Save the profile (userdatabase will take care of timestamps for us)
        m_database.save( profile );
        m_database.commit();

        // Refresh the credential set
        AuthenticationManager mgr = m_engine.getAuthenticationManager();
        if ( newProfile && !mgr.isContainerAuthenticated() )
        {
            mgr.login( session, profile.getLoginName(), profile.getPassword() );
        }
        else
        {
            mgr.refreshCredentials( session );
        }
    }

    /**
     * <p> Extracts user profile parameters from the HTTP request and populates
     * a UserProfile with them. The UserProfile will either be a copy of the
     * user's existing profile (if one can be found), or a new profile (if not).
     * The rules for populating the profile as as follows: </p> <ul> <li>If the
     * <code>email</code> or <code>password</code> parameter values differ
     * from those in the existing profile, the passed parameters override the
     * old values.</li> <li>For new profiles, the user-supplied
     * <code>fullname</code> and <code>wikiname</code> parameters are always
     * used; for existing profiles the existing values are used, and whatever
     * values the user supplied are discarded.</li> <li>In all cases, the
     * created/last modified timestamps of the user's existing or new profile
     * always override whatever values the user supplied.</li> <li>If
     * container authentication is used, the login name property of the profile
     * is set to the name of
     * {@link com.ecyrd.jspwiki.WikiSession#getLoginPrincipal()}. Otherwise,
     * the value of the <code>loginname</code> parameter is used.</li> </ul>
     * @param context the current wiki context
     * @return a new, populated user profile
     */
    public UserProfile parseProfile( WikiContext context )
    {
        // Retrieve the user's profile (may have been previously cached)
        UserProfile profile = getUserProfile( context.getWikiSession() );
        HttpServletRequest request = context.getHttpRequest();
        
        // Extract values from request stream (cleanse whitespace as needed)
        String loginName = request.getParameter( "loginname" );
        String password = request.getParameter( "password" );
        String wikiname = request.getParameter( "wikiname" );
        String fullname = request.getParameter( "fullname" );
        String email = request.getParameter( "email" );
        loginName = InputValidator.isBlank( loginName ) ? null : loginName;
        password = InputValidator.isBlank( password ) ? null : password;
        wikiname = InputValidator.isBlank( wikiname ) ? null : wikiname.replaceAll( "\\s", "" );
        fullname = InputValidator.isBlank( fullname ) ? null : fullname;
        email = InputValidator.isBlank( email ) ? null : email;

        // A special case if we have container authentication
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated() )
        {
            // If authenticated, login name is always taken from container
            if ( context.getWikiSession().isAuthenticated() ) {
                loginName = context.getWikiSession().getLoginPrincipal().getName();
            }
        }
        
        if ( profile.isNew() )
        {
            // If new profile, use whatever values the user supplied
            profile.setLoginName( loginName );
            profile.setEmail( email );
            profile.setFullname( fullname );
            profile.setPassword( password );
            profile.setWikiName( wikiname );
        }
        else
        {
            // If modifying an existing profile, always override
            // the timestamp, login name, full name and wiki name properties
            profile.setEmail( email );
            profile.setPassword( password );
        }
        return profile;
    }

    /**
     * Validates a user profile, and appends any errors to the session errors
     * list. If the profile is new, the password will be checked to make sure it
     * isn't null. Otherwise, the password is checked for length and that it
     * matches the value of the 'password2' HTTP parameter. Note that we have a
     * special case when container-managed authentication is used and the user
     * is not authenticated; this will always cause validation to fail. Any
     * validation errors are added to the wiki session's messages collection
     * (see {@link WikiSession#getMessages()}.
     * @param context the current wiki context
     * @param profile the supplied UserProfile
     */
    public void validateProfile( WikiContext context, UserProfile profile )
    {
        boolean isNew = ( profile.isNew() );
        WikiSession session = context.getWikiSession();
        InputValidator validator = new InputValidator( "profile", session );
        
        // If container-managed auth and user not logged in, throw an error
        // unless we're allowed to add profiles to the container
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated()
             && !context.getWikiSession().isAuthenticated() 
             && !m_database.isSharedWithContainer() )
        {
            session.addMessage( "profile", "You must log in before creating a profile." );
        }

        validator.validateNotNull( profile.getLoginName(), "Login name" );
        validator.validateNotNull( profile.getWikiName(), "Wiki name" );
        validator.validateNotNull( profile.getFullname(), "Full name" );
        validator.validate( profile.getEmail(), "E-mail address", InputValidator.EMAIL );
        
        // If new profile, passwords must match and can't be null
        if ( !m_engine.getAuthenticationManager().isContainerAuthenticated() )
        {
            String password = profile.getPassword();
            if ( password == null )
            {
                if ( isNew )
                {
                    session.addMessage( "profile", "Password cannot be blank" );
                }
            }
            else 
            {
                HttpServletRequest request = context.getHttpRequest();
                String password2 = ( request == null ) ? null : request.getParameter( "password2" );
                if ( !password.equals( password2 ) )
                {
                    session.addMessage( "profile", "Passwords don't match" );
                }
            }
        }
    }

    /**
     * This is a database that gets used if nothing else is available. It does
     * nothing of note - it just mostly thorws NoSuchPrincipalExceptions if
     * someone tries to log in.
     * @author Janne Jalkanen
     */
    public class DummyUserDatabase extends AbstractUserDatabase
    {

        public void commit() throws WikiSecurityException
        {
            // No operation
        }

        public void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException
        {
            // No operation
        }

        public UserProfile findByEmail(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByFullName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByLoginName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByWikiName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public Principal[] getWikiNames() throws WikiSecurityException
        {
            return new Principal[0];
        }
        
        public void initialize(WikiEngine engine, Properties props) throws NoRequiredPropertyException
        {
        }
        
        public boolean isSharedWithContainer()
        {
            return false;
        }

        public void save( UserProfile profile ) throws WikiSecurityException
        {
        }
        
    }
    
    /**
     * Implements a simple GroupManager which does not simply
     * manage any groups.  This is used when jspwiki security is
     * turned off.
     * 
     * @author jalkanen
     *
     */
    public class DummyGroupManager implements GroupManager
    {

        public void add(Group group)
        {           
        }

        public void addWikiEventListener(WikiEventListener listener)
        {
        }

        public void commit()
        {
        }

        public boolean exists(Group group)
        {
            return false;
        }

        public void initialize(WikiEngine engine, Properties props)
        {
        }

        public void reload()
        {
        }

        public void remove(Group group)
        {
        }

        public void removeWikiEventListener(WikiEventListener listener)
        {
        }

        public Principal findRole(String role)
        {
            return null;
        }

        public Principal[] getRoles()
        {
            return null;
        }

        public boolean isUserInRole(WikiSession session, Principal role)
        {
            return false;
        }
        
    }
}
