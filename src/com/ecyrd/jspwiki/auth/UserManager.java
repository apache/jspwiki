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

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.auth.user.AbstractUserDatabase;
import com.ecyrd.jspwiki.auth.user.DuplicateUserException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventManager;
import com.ecyrd.jspwiki.event.WikiSecurityEvent;
import com.ecyrd.jspwiki.filters.RedirectException;
import com.ecyrd.jspwiki.ui.InputValidator;
import com.ecyrd.jspwiki.util.ClassUtil;
import com.ecyrd.jspwiki.util.MailUtil;
import com.ecyrd.jspwiki.workflow.*;

/**
 * Provides a facade for obtaining user information.
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @since 2.3
 */
public final class UserManager
{
    private static final String USERDATABASE_PACKAGE = "com.ecyrd.jspwiki.auth.user";
    private static final String SESSION_MESSAGES = "profile";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_FULLNAME = "fullname";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_LOGINNAME = "loginname";
    private static final String UNKNOWN_CLASS = "<unknown>";

    private WikiEngine m_engine;

    private static Logger log = Logger.getLogger(UserManager.class);

    public  static final String SAVE_APPROVER               = "workflow.createUserProfile";
    private static final String PROP_DATABASE               = "jspwiki.userdatabase";
    protected static final String SAVE_TASK_MESSAGE_KEY     = "task.createUserProfile";
    protected static final String SAVED_PROFILE             = "userProfile";
    protected static final String SAVE_DECISION_MESSAGE_KEY = "decision.createUserProfile";
    protected static final String FACT_SUBMITTER            = "fact.submitter";
    protected static final String PREFS_LOGIN_NAME          = "prefs.loginname";
    protected static final String PREFS_FULL_NAME           = "prefs.fullname";
    protected static final String PREFS_EMAIL               = "prefs.email";

    // private static final String  PROP_ACLMANAGER     = "jspwiki.aclManager";

    /** Associateds wiki sessions with profiles */
    private final Map        m_profiles     = new WeakHashMap();

    /** The user database loads, manages and persists user identities */
    private UserDatabase     m_database;

    private boolean          m_useJAAS      = true;

    /**
     * Constructs a new UserManager instance.
     */
    public UserManager()
    {
    }

    /**
     * Initializes the engine for its nefarious purposes.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    public final void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;

        m_useJAAS = AuthenticationManager.SECURITY_JAAS.equals( props.getProperty(AuthenticationManager.PROP_SECURITY, AuthenticationManager.SECURITY_JAAS ) );

        // Attach the PageManager as a listener
        // TODO: it would be better if we did this in PageManager directly
        addWikiEventListener( engine.getPageManager() );
    }

    /**
     * Returns the UserDatabase employed by this WikiEngine. The UserDatabase is
     * lazily initialized by this method, if it does not exist yet. If the
     * initialization fails, this method will use the inner class
     * DummyUserDatabase as a default (which is enough to get JSPWiki running).
     * @since 2.3
     */
    public final UserDatabase getUserDatabase()
    {
        // FIXME: Must not throw RuntimeException, but something else.
        if( m_database != null )
        {
            return m_database;
        }

        if( !m_useJAAS )
        {
            m_database = new DummyUserDatabase();
            return m_database;
        }

        String dbClassName = UNKNOWN_CLASS;

        try
        {
            dbClassName = WikiEngine.getRequiredProperty( m_engine.getWikiProperties(),
                                                          PROP_DATABASE );

            log.info("Attempting to load user database class "+dbClassName);
            Class dbClass = ClassUtil.findClass( USERDATABASE_PACKAGE, dbClassName );
            m_database = (UserDatabase) dbClass.newInstance();
            m_database.initialize( m_engine, m_engine.getWikiProperties() );
            log.info("UserDatabase initialized.");
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "You have not set the '"+PROP_DATABASE+"'. You need to do this if you want to enable user management by JSPWiki." );
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
    public final UserProfile getUserProfile( WikiSession session )
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
     * <p>
     * Saves the {@link com.ecyrd.jspwiki.auth.user.UserProfile}for the user in
     * a wiki session. This method verifies that a user profile to be saved
     * doesn't collide with existing profiles; that is, the login name
     * or full name is already used by another profile. If the profile
     * collides, a <code>DuplicateUserException</code> is thrown. After saving
     * the profile, the user database changes are committed, and the user's
     * credential set is refreshed; if custom authentication is used, this means
     * the user will be automatically be logged in.
     * </p>
     * <p>
     * When the user's profile is saved succcessfully, this method fires a
     * {@link WikiSecurityEvent#PROFILE_SAVE} event with the WikiSession as the
     * source and the UserProfile as target. For existing profiles, if the
     * user's full name changes, this method also fires a "name changed"
     * event ({@link WikiSecurityEvent#PROFILE_NAME_CHANGED}) with the
     * WikiSession as the source and an array containing the old and new
     * UserProfiles, respectively. The <code>NAME_CHANGED</code> event allows
     * the GroupManager and PageManager can change group memberships and
     * ACLs if needed.
     * </p>
     * <p>
     * Note that WikiSessions normally attach event listeners to the
     * UserManager, so changes to the profile will automatically cause the
     * correct Principals to be reloaded into the current WikiSession's Subject.
     * </p>
     * @param session the wiki session, which may not be <code>null</code>
     * @param profile the user profile, which may not be <code>null</code>
     * @throws RedirectException if the user profile must be approved before it can be saved
     * @throws DuplicateUserException if the proposed profile's login name or full name collides with another
     * @throws WikiSecurityException if the current user does not have permission to save the profile
     * @throws WikiException if approval is required, but this method cannot create a workflow for
     * some reason; this is not normal, and indicates mis-configuration
     * or name collision with another user
     */
    public final void setUserProfile( WikiSession session, UserProfile profile ) throws WikiSecurityException,
            DuplicateUserException, WikiException
    {
        // Verify user is allowed to save profile!
        Permission p = new WikiPermission( m_engine.getApplicationName(), WikiPermission.EDIT_PROFILE_ACTION );
        if ( !m_engine.getAuthorizationManager().checkPermission( session, p ) )
        {
            throw new WikiSecurityException( "You are not allowed to save wiki profiles." );
        }

        // Check if profile is new, and see if container allows creation
        boolean newProfile = profile.isNew();

        // Check if another user profile already has the fullname or loginname
        UserProfile oldProfile = getUserProfile( session );
        boolean nameChanged = ( oldProfile == null  || oldProfile.getFullname() == null )
            ? false
            : !( oldProfile.getFullname().equals( profile.getFullname() ) &&
                 oldProfile.getLoginName().equals( profile.getLoginName() ) );
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

        // For new accounts, create approval workflow for user profile save.
        if ( newProfile && oldProfile.isNew() )
        {
            WorkflowBuilder builder = WorkflowBuilder.getBuilder( m_engine );
            Principal submitter = session.getUserPrincipal();
            Task completionTask = new SaveUserProfileTask( m_engine );

            // Add user profile attribute as Facts for the approver (if required)
            boolean hasEmail = ( profile.getEmail() != null );
            Fact[] facts = new Fact[ hasEmail ? 4 : 3];
            facts[0] = new Fact( PREFS_FULL_NAME, profile.getFullname() );
            facts[1] = new Fact( PREFS_LOGIN_NAME, profile.getLoginName() );
            facts[2] = new Fact( FACT_SUBMITTER, submitter.getName() );
            if ( hasEmail )
            {
                facts[3] = new Fact( PREFS_EMAIL, profile.getEmail() );
            }
            Workflow workflow = builder.buildApprovalWorkflow( submitter,
                                                               SAVE_APPROVER,
                                                               null,
                                                               SAVE_DECISION_MESSAGE_KEY,
                                                               facts,
                                                               completionTask,
                                                               null );

            workflow.setAttribute( SAVED_PROFILE, profile );
            m_engine.getWorkflowManager().start(workflow);

            boolean approvalRequired = ( workflow.getCurrentStep() instanceof Decision );

            // If the profile requires approval, redirect user to message page
            if ( approvalRequired )
            {
                throw new RedirectException( "Approval required.",
                                             m_engine.getURL(WikiContext.VIEW,"ApprovalRequiredForUserProfiles",null,true) );
            }

            // If the profile doesn't need approval, then just log the user in

            try
            {
                AuthenticationManager mgr = m_engine.getAuthenticationManager();
                if ( newProfile && !mgr.isContainerAuthenticated() )
                {
                    mgr.login( session, profile.getLoginName(), profile.getPassword() );
                }
            }
            catch ( WikiException e )
            {
                throw new WikiSecurityException( e.getMessage() );
            }

            // Alert all listeners that the profile changed...
            // ...this will cause credentials to be reloaded in the wiki session
            fireEvent( WikiSecurityEvent.PROFILE_SAVE, session, profile );
        }

        // For existing accounts, just save the profile
        else
        {
            // If login name changed, rename it first
            if ( nameChanged && !oldProfile.getLoginName().equals( profile.getLoginName() ) )
            {
                m_database.rename( oldProfile.getLoginName(), profile.getLoginName() );
            }

            // Now, save the profile (userdatabase will take care of timestamps for us)
            m_database.save( profile );

            if ( nameChanged )
            {
                // Fire an event if the login name or full name changed
                UserProfile[] profiles = new UserProfile[] { oldProfile, profile };
                fireEvent( WikiSecurityEvent.PROFILE_NAME_CHANGED, session, profiles );
            }
            else
            {
                // Fire an event that says we have new a new profile (new principals)
                fireEvent( WikiSecurityEvent.PROFILE_SAVE, session, profile );
            }
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
     * <code>fullname</code parameter is always
     * used; for existing profiles the existing value is used, and whatever
     * value the user supplied is discarded. The wiki name is automatically
     * computed by taking the full name and extracting all whitespace.</li>
     * <li>In all cases, the
     * created/last modified timestamps of the user's existing or new profile
     * always override whatever values the user supplied.</li> <li>If
     * container authentication is used, the login name property of the profile
     * is set to the name of
     * {@link com.ecyrd.jspwiki.WikiSession#getLoginPrincipal()}. Otherwise,
     * the value of the <code>loginname</code> parameter is used.</li> </ul>
     * @param context the current wiki context
     * @return a new, populated user profile
     */
    public final UserProfile parseProfile( WikiContext context )
    {
        // Retrieve the user's profile (may have been previously cached)
        UserProfile profile = getUserProfile( context.getWikiSession() );
        HttpServletRequest request = context.getHttpRequest();

        // Extract values from request stream (cleanse whitespace as needed)
        String loginName = request.getParameter( PARAM_LOGINNAME );
        String password = request.getParameter( PARAM_PASSWORD );
        String fullname = request.getParameter( PARAM_FULLNAME );
        String email = request.getParameter( PARAM_EMAIL );
        loginName = InputValidator.isBlank( loginName ) ? null : loginName;
        password = InputValidator.isBlank( password ) ? null : password;
        fullname = InputValidator.isBlank( fullname ) ? null : fullname;
        email = InputValidator.isBlank( email ) ? null : email;

        // A special case if we have container authentication
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated() )
        {
            // If authenticated, login name is always taken from container
            if ( context.getWikiSession().isAuthenticated() )
            {
                loginName = context.getWikiSession().getLoginPrincipal().getName();
            }
        }

        // Set the profile fields!
        profile.setLoginName( loginName );
        profile.setEmail( email );
        profile.setFullname( fullname );
        profile.setPassword( password );
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
    public final void validateProfile( WikiContext context, UserProfile profile )
    {
        boolean isNew = profile.isNew();
        WikiSession session = context.getWikiSession();
        InputValidator validator = new InputValidator( SESSION_MESSAGES, session );

        // If container-managed auth and user not logged in, throw an error
        // unless we're allowed to add profiles to the container
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated()
             && !context.getWikiSession().isAuthenticated()
             && !m_database.isSharedWithContainer() )
        {
            session.addMessage( SESSION_MESSAGES, "You must log in before creating a profile." );
        }

        validator.validateNotNull( profile.getLoginName(), "Login name" );
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
                    session.addMessage( SESSION_MESSAGES, "Password cannot be blank" );
                }
            }
            else
            {
                HttpServletRequest request = context.getHttpRequest();
                String password2 = ( request == null ) ? null : request.getParameter( "password2" );
                if ( !password.equals( password2 ) )
                {
                    session.addMessage( SESSION_MESSAGES, "Passwords don't match" );
                }
            }
        }

        UserProfile otherProfile;
        String fullName = profile.getFullname();
        String loginName = profile.getLoginName();

        // It's illegal to use as a full name someone else's login name
        try
        {
            otherProfile = m_database.find( fullName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !fullName.equals( otherProfile.getFullname() ) )
            {
                session.addMessage( SESSION_MESSAGES, "Full name '" + fullName + "' is illegal" );
            }
        }
        catch ( NoSuchPrincipalException e)
        { /* It's clean */ }

        // It's illegal to use as a login name someone else's full name
        try
        {
            otherProfile = m_database.find( loginName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !loginName.equals( otherProfile.getLoginName() ) )
            {
                session.addMessage( SESSION_MESSAGES, "Login name '" + loginName + "' is illegal" );
            }
        }
        catch ( NoSuchPrincipalException e)
        { /* It's clean */ }
    }

    /**
     * This is a database that gets used if nothing else is available. It does
     * nothing of note - it just mostly thorws NoSuchPrincipalExceptions if
     * someone tries to log in.
     * @author Janne Jalkanen
     */
    public static class DummyUserDatabase extends AbstractUserDatabase
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

        public void rename( String loginName, String newName ) throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public void save( UserProfile profile ) throws WikiSecurityException
        {
        }

    }

    // workflow task inner classes....................................................

    /**
     * Inner class that handles the actual profile save action. Instances
     * of this class are assumed to have been added to an approval workflow via
     * {@link com.ecyrd.jspwiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, com.ecyrd.jspwiki.workflow.Fact[], Task, String)};
     * they will not function correctly otherwise.
     *
     * @author Andrew Jaquith
     */
    public static class SaveUserProfileTask extends Task
    {
        private final UserDatabase m_db;
        private final WikiEngine m_engine;

        /**
         * Constructs a new Task for saving a user profile.
         * @param engine the wiki engine
         */
        public SaveUserProfileTask( WikiEngine engine )
        {
            super( SAVE_TASK_MESSAGE_KEY );
            m_engine = engine;
            m_db = engine.getUserManager().getUserDatabase();
        }

        /**
         * Saves the user profile to the user database. The
         */
        public Outcome execute() throws WikiException
        {
            // Retrieve user profile
            UserProfile profile = (UserProfile) getWorkflow().getAttribute( SAVED_PROFILE );

            // Save the profile (userdatabase will take care of timestamps for us)
            m_db.save( profile );

            // Send e-mail if user supplied an e-mail address
            if ( profile.getEmail() != null )
            {
                try
                {
                    String app = m_engine.getApplicationName();
                    String to = profile.getEmail();
                    String subject = "Welcome to " + app;
                    String content = "Congratulations! Your new profile on "
                        + app + " has been created. Your profile details are as follows: \n\n"
                        + "Login name: " + profile.getLoginName() + "\n"
                        + "Your name : " + profile.getFullname() + "\n"
                        + "E-mail    : " + profile.getEmail() + "\n\n"
                        + "If you forget your password, you can re-set it at "
                        + m_engine.getBaseURL() + "\\LostPassword.jsp";
                    MailUtil.sendMessage( m_engine, to, subject, content);
                }
                catch ( AddressException e)
                {
                }
                catch ( MessagingException e )
                {
                    log.error( "Could not send registration confirmation e-mail. Is the e-mail server running?" );
                }
            }

            return Outcome.STEP_COMPLETE;
        }
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
     * @param session    the wiki session supporting the event
     * @param profile    the user profile (or array of user profiles), which may be <code>null</code>
     */
    protected final void fireEvent( int type, WikiSession session, Object profile )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiSecurityEvent(session,type,profile));
        }
    }

}