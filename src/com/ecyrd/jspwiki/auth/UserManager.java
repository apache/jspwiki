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

import java.security.Principal;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.user.DefaultUserProfile;
import com.ecyrd.jspwiki.auth.user.DuplicateUserException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Provides a facade for user and group information.
 *  
 *  @author Janne Jalkanen
 *  @version $Revision: 1.38 $ $Date: 2005-09-02 23:58:55 $
 *  @since 2.3
 */
public class UserManager
{
    private WikiEngine m_engine;
    
    private static final Logger log = Logger.getLogger(UserManager.class);

    private static final String  PROP_USERDATABASE   = "jspwiki.userdatabase";

    // private static final String  PROP_ACLMANAGER     = "jspwiki.aclManager";

    /** The user database loads, manages and persists user identities */
    private UserDatabase     m_database     = null;
    
    /** The group manager loads, manages and persists wiki groups */
    private GroupManager     m_groupManager = null;
    
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
     * returned will be the one stored in the user database; if one does
     * not exist, a new one will be initialized and returned. If the user is 
     * anonymous or asserted, the UserProfile will <i>always</i> be newly 
     * initialized to prevent spoofing of identities. If a UserProfile needs to
     * be initialized, its {@link com.ecyrd.jspwiki.auth.user.UserProfile#isNew()}
     * method will return <code>true</code>. Note that this method does 
     * not modify the retrieved (or newly created) profile in any way; any
     * and all fields in the user profile may be <code>null</code>.
     * @param session the wiki session, which may not be <code>null</code>
     * @return the user's profile, which will be newly initialized if the
     * user is anonymous or asserted, or if the user cannot be found in the
     * user database
     * @throws WikiSecurityException if the database returns an exception
     * @throws IllegalStateException if a new UserProfile was created, but its
     *             {@link com.ecyrd.jspwiki.auth.user.UserProfile#isNew()}
     *             method returns <code>false</code>. This is meant as a quality
     *             check for UserDatabase providers; it should only be thrown
     *             if the implementation is faulty.
     */
    public UserProfile getUserProfile( WikiSession session ) throws WikiSecurityException
    {
        boolean needsInitialization = true;
        UserProfile profile = null;
        Principal user;
        
        // Figure out if this is an existing profile
        if ( session.isAuthenticated() ) {
            user = session.getUserPrincipal();
            try
            {
                profile = m_database.find( user.getName() );
                needsInitialization = false;
            }
            catch( NoSuchPrincipalException e )
            {
            }
        }
        
        if ( needsInitialization ) 
        {
            profile = m_database.newProfile();
            if ( !profile.isNew() )
            {
                throw new IllegalStateException(
                        "New profile should be marked 'new'. Check your UserProfile implementation." );
            }
        }
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

        boolean newProfile = profile.isNew();
        UserProfile oldProfile = getUserProfile( session );

        // User profiles that may already have wikiname, fullname or loginname
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

        Date modDate = new Date();
        if ( newProfile )
        {
            profile.setCreated( modDate );
        }
        profile.setLastModified( modDate );
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
     * <p>
     * Extracts user profile parameters from the HTTP request and populates a
     * new UserProfile with them. The UserProfile will either be a copy of the
     * user's existing profile (if one can be found), or a new profile (if not).
     * The rules for populating the profile as as follows:
     * </p>
     * <ul>
     * <li>If the <code>email</code> or <code>password</code> parameter
     * values differ from those in the existing profile, the passed parameters
     * override the old values.</li>
     * <li>For new profiles, the <code>fullname</code> and
     * <code>wikiname</code> parameters are always used; otherwise they are
     * ignored.</li>
     * <li>In all cases, the created/last modified timestamps of the user's
     * existing/new profile are always used.</li>
     * <li>If container authentication is used, the login name property of the
     * profile is set to the name of
     * {@link com.ecyrd.jspwiki.WikiSession#getLoginPrincipal()}. Otherwise,
     * the value of the <code>loginname</code> parameter is used.</li>
     * </ul>
     * @param context the current wiki context
     * @return a new, populated user profile
     */
    public UserProfile parseProfile( WikiContext context )
    {
        // TODO:this class is probably the wrong place for this method...

        UserProfile profile = m_database.newProfile();
        UserProfile existingProfile = null;
        try
        {
            // Look up the existing profile, if it exists
            existingProfile = getUserProfile( context.getWikiSession() );
        }
        catch( WikiSecurityException e )
        {
        }
        HttpServletRequest request = context.getHttpRequest();

        // Set e-mail to user's supplied value; if null, use existing value
        String email = request.getParameter( "email" );
        email = isNotBlank( email ) ? email : existingProfile.getEmail();
        profile.setEmail( email );

        // Set password to user's supplied value; if null, skip it
        String password = request.getParameter( "password" );
        password = isNotBlank( password ) ? password : null;
        profile.setPassword( password );

        // For new profiles, we use what the user supplied for
        // full name and wiki name
        String fullname = request.getParameter( "fullname" );
        String wikiname = request.getParameter( "wikiname" );
        if ( existingProfile.isNew() )
        {
            fullname = isNotBlank( fullname ) ? fullname : null;
            wikiname = isNotBlank( wikiname ) ? wikiname : null;
        }
        else
        {
            fullname = existingProfile.getFullname();
            wikiname = existingProfile.getWikiName();
        }
        profile.setFullname( fullname );
        profile.setWikiName( wikiname );

        // For all profiles, we use the login name from the looked-up
        // profile. If the looked-up profile doesn't have a login
        // name, we either use the one from the session (if container auth)
        // or the one the user supplies (if custom auth).
        String loginName = existingProfile.getLoginName();
        if ( loginName == null )
        {
            if ( m_engine.getAuthenticationManager().isContainerAuthenticated() )
            {
                Principal principal = context.getWikiSession().getLoginPrincipal();
                loginName = principal.getName();
            }
            else
            {
                loginName = request.getParameter( "loginname" );
                wikiname = isNotBlank( loginName ) ? loginName : null;
            }
        }
        profile.setLoginName( loginName );

        // Always use existing profile's lastModified and Created properties
        profile.setCreated( existingProfile.getCreated() );
        profile.setLastModified( existingProfile.getLastModified() );

        return profile;
    }

    /**
     * Returns <code>true</code> if a supplied string is null or blank
     * @param parameter
     */
    private static boolean isNotBlank( String parameter )
    {
        return ( parameter != null && parameter.length() > 0 );
    }

    /**
     * Validates a user profile, and appends any errors to a user-supplied Set
     * of error strings. If the wiki request context is REGISTER, the password
     * will be checked to make sure it isn't null. Otherwise, the password
     * is checked.
     * @param context the current wiki context
     * @param profile the supplied UserProfile
     * @param errors the set of error strings
     */
    public void validateProfile( WikiContext context, UserProfile profile, Set errors )
    {
        // TODO:this class is probably the wrong place for this method...
        boolean isNew = ( WikiContext.REGISTER.equals( context.getRequestContext() ) );

        if ( profile.getFullname() == null )
        {
            errors.add( "Full name cannot be blank" );
        }
        if ( profile.getLoginName() == null )
        {
            errors.add( "Login name cannot be blank" );
        }
        if ( profile.getWikiName() == null )
        {
            errors.add( "Wiki name cannot be blank" );
        }
        if ( !m_engine.getAuthenticationManager().isContainerAuthenticated() && isNew && profile.getPassword() == null )
        {
            errors.add( "Password cannot be blank" );
        }
    }

    /**
     * This is a database that gets used if nothing else is available. It does
     * nothing of note - it just mostly thorws NoSuchPrincipalExceptions if
     * someone tries to log in.
     * @author jalkanen
     */
    public class DummyUserDatabase implements UserDatabase
    {

        public void commit() throws WikiSecurityException
        {
            // No operation
        }

        public UserProfile find(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
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

        public Principal[] getPrincipals(String identifier) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public void initialize(WikiEngine engine, Properties props) throws NoRequiredPropertyException
        {
        }

        public UserProfile newProfile()
        {
            return new DefaultUserProfile();
        }

        public void save( UserProfile profile ) throws WikiSecurityException
        {
        }

        public boolean validatePassword(String loginName, String password)
        {
            return false;
        }
        
    }
}
