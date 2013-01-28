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

import java.security.Permission;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wiki.NoRequiredPropertyException;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.auth.user.AbstractUserDatabase;
import org.apache.wiki.auth.user.DuplicateUserException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.rpc.RPCCallable;
import org.apache.wiki.rpc.json.JSONRPCManager;
import org.apache.wiki.ui.InputValidator;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.MailUtil;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.workflow.*;

/**
 * Provides a facade for obtaining user information.
 * @since 2.3
 */
public final class UserManager
{
    private static final String USERDATABASE_PACKAGE = "org.apache.wiki.auth.user";
    private static final String SESSION_MESSAGES = "profile";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_FULLNAME = "fullname";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_LOGINNAME = "loginname";
    private static final String UNKNOWN_CLASS = "<unknown>";

    private WikiEngine m_engine;

    private static Logger log = Logger.getLogger(UserManager.class);

    /** Message key for the "save profile" message. */
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

    /** Associates wiki sessions with profiles */
    private final Map<WikiSession,UserProfile> m_profiles = new WeakHashMap<WikiSession,UserProfile>();

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
    @SuppressWarnings("deprecation")
    public final void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;

        m_useJAAS = AuthenticationManager.SECURITY_JAAS.equals( props.getProperty(AuthenticationManager.PROP_SECURITY, AuthenticationManager.SECURITY_JAAS ) );

        // Attach the PageManager as a listener
        // TODO: it would be better if we did this in PageManager directly
        addWikiEventListener( engine.getPageManager() );

        JSONRPCManager.registerGlobalObject( "users", new JSONUserModule(this), new AllPermission(null) );
    }

    /**
     * Returns the UserDatabase employed by this WikiEngine. The UserDatabase is
     * lazily initialized by this method, if it does not exist yet. If the
     * initialization fails, this method will use the inner class
     * DummyUserDatabase as a default (which is enough to get JSPWiki running).
     * @return the dummy user database
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
            dbClassName = TextUtil.getRequiredProperty( m_engine.getWikiProperties(),
                                                          PROP_DATABASE );

            log.info("Attempting to load user database class "+dbClassName);
            Class<?> dbClass = ClassUtil.findClass( USERDATABASE_PACKAGE, dbClassName );
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
     * <p>Retrieves the {@link org.apache.wiki.auth.user.UserProfile}for the
     * user in a wiki session. If the user is authenticated, the UserProfile
     * returned will be the one stored in the user database; if one does not
     * exist, a new one will be initialized and returned. If the user is
     * anonymous or asserted, the UserProfile will <i>always</i> be newly
     * initialized to prevent spoofing of identities. If a UserProfile needs to
     * be initialized, its
     * {@link org.apache.wiki.auth.user.UserProfile#isNew()} method will
     * return <code>true</code>, and its login name will will be set
     * automatically if the user is authenticated. Note that this method does
     * not modify the retrieved (or newly created) profile otherwise; other
     * fields in the user profile may be <code>null</code>.</p>
     * <p>If a new UserProfile was created, but its
     * {@link org.apache.wiki.auth.user.UserProfile#isNew()} method returns
     * <code>false</code>, this method throws an {@link IllegalStateException}.
     * This is meant as a quality check for UserDatabase providers;
     * it should only be thrown if the implementation is faulty.</p>
     * @param session the wiki session, which may not be <code>null</code>
     * @return the user's profile, which will be newly initialized if the user
     * is anonymous or asserted, or if the user cannot be found in the user
     * database
     */
    public final UserProfile getUserProfile( WikiSession session )
    {
        // Look up cached user profile
        UserProfile profile = m_profiles.get( session );
        boolean newProfile = profile == null;
        Principal user = null;

        // If user is authenticated, figure out if this is an existing profile
        if ( session.isAuthenticated() )
        {
            user = session.getUserPrincipal();
            try
            {
                profile = getUserDatabase().find( user.getName() );
                newProfile = false;
            }
            catch( NoSuchPrincipalException e )
            {
            }
        }

        if ( newProfile )
        {
            profile = getUserDatabase().newProfile();
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
     * Saves the {@link org.apache.wiki.auth.user.UserProfile}for the user in
     * a wiki session. This method verifies that a user profile to be saved
     * doesn't collide with existing profiles; that is, the login name
     * or full name is already used by another profile. If the profile
     * collides, a <code>DuplicateUserException</code> is thrown. After saving
     * the profile, the user database changes are committed, and the user's
     * credential set is refreshed; if custom authentication is used, this means
     * the user will be automatically be logged in.
     * </p>
     * <p>
     * When the user's profile is saved successfully, this method fires a
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
     * @throws DuplicateUserException if the proposed profile's login name or full name collides with another
     * @throws WikiException if the save fails for some reason. If the current user does not have
     * permission to save the profile, this will be a {@link org.apache.wiki.auth.WikiSecurityException};
     * if if the user profile must be approved before it can be saved, it will be a
     * {@link org.apache.wiki.workflow.DecisionRequiredException}. All other WikiException
     * indicate a condition that is not normal is probably due to mis-configuration
     */
    public final void setUserProfile( WikiSession session, UserProfile profile ) throws DuplicateUserException, WikiException
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
            otherProfile = getUserDatabase().findByLoginName( profile.getLoginName() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                throw new DuplicateUserException( "security.error.login.taken", profile.getLoginName() );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }
        try
        {
            otherProfile = getUserDatabase().findByFullName( profile.getFullname() );
            if ( otherProfile != null && !otherProfile.equals( oldProfile ) )
            {
                throw new DuplicateUserException( "security.error.fullname.taken", profile.getFullname() );
            }
        }
        catch( NoSuchPrincipalException e )
        {
        }

        // For new accounts, create approval workflow for user profile save.
        if ( newProfile && oldProfile != null && oldProfile.isNew() )
        {
            WorkflowBuilder builder = WorkflowBuilder.getBuilder( m_engine );
            Principal submitter = session.getUserPrincipal();
            Task completionTask = new SaveUserProfileTask( m_engine, session.getLocale() );

            // Add user profile attribute as Facts for the approver (if required)
            boolean hasEmail = profile.getEmail() != null;
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

            boolean approvalRequired = workflow.getCurrentStep() instanceof Decision;

            // If the profile requires approval, redirect user to message page
            if ( approvalRequired )
            {
                throw new DecisionRequiredException( "This profile must be approved before it becomes active" );
            }

            // If the profile doesn't need approval, then just log the user in

            try
            {
                AuthenticationManager mgr = m_engine.getAuthenticationManager();
                if ( newProfile && !mgr.isContainerAuthenticated() )
                {
                    mgr.login( session, null, profile.getLoginName(), profile.getPassword() );
                }
            }
            catch ( WikiException e )
            {
                throw new WikiSecurityException( e.getMessage(), e );
            }

            // Alert all listeners that the profile changed...
            // ...this will cause credentials to be reloaded in the wiki session
            fireEvent( WikiSecurityEvent.PROFILE_SAVE, session, profile );
        }

        // For existing accounts, just save the profile
        else
        {
            // If login name changed, rename it first
            if ( nameChanged && oldProfile != null && !oldProfile.getLoginName().equals( profile.getLoginName() ) )
            {
                getUserDatabase().rename( oldProfile.getLoginName(), profile.getLoginName() );
            }

            // Now, save the profile (userdatabase will take care of timestamps for us)
            getUserDatabase().save( profile );

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
     * <code>fullname</code> parameter is always
     * used; for existing profiles the existing value is used, and whatever
     * value the user supplied is discarded. The wiki name is automatically
     * computed by taking the full name and extracting all whitespace.</li>
     * <li>In all cases, the
     * created/last modified timestamps of the user's existing or new profile
     * always override whatever values the user supplied.</li> <li>If
     * container authentication is used, the login name property of the profile
     * is set to the name of
     * {@link org.apache.wiki.WikiSession#getLoginPrincipal()}. Otherwise,
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
        InputValidator validator = new InputValidator( SESSION_MESSAGES, context );
        ResourceBundle rb = context.getBundle( InternationalizationManager.CORE_BUNDLE );

        //
        //  Query the SpamFilter first
        //
        FilterManager fm = m_engine.getFilterManager();
        List<PageFilter> ls = fm.getFilterList();
        for( PageFilter pf : ls )
        {
            if( pf instanceof SpamFilter )
            {
                if( ((SpamFilter)pf).isValidUserProfile( context, profile ) == false )
                {
                    session.addMessage( SESSION_MESSAGES, "Invalid userprofile" );
                    return;
                }
                break;
            }
        }
        
        // If container-managed auth and user not logged in, throw an error
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated()
             && !context.getWikiSession().isAuthenticated() )
        {
            session.addMessage( SESSION_MESSAGES, rb.getString("security.error.createprofilebeforelogin") );
        }

        validator.validateNotNull( profile.getLoginName(), rb.getString("security.user.loginname") );
        validator.validateNotNull( profile.getFullname(), rb.getString("security.user.fullname") );
        validator.validate( profile.getEmail(), rb.getString("security.user.email"), InputValidator.EMAIL );

        // If new profile, passwords must match and can't be null
        if ( !m_engine.getAuthenticationManager().isContainerAuthenticated() )
        {
            String password = profile.getPassword();
            if ( password == null )
            {
                if ( isNew )
                {
                    session.addMessage( SESSION_MESSAGES, rb.getString("security.error.blankpassword") );
                }
            }
            else
            {
                HttpServletRequest request = context.getHttpRequest();
                String password2 = ( request == null ) ? null : request.getParameter( "password2" );
                if ( !password.equals( password2 ) )
                {
                    session.addMessage( SESSION_MESSAGES, rb.getString("security.error.passwordnomatch") );
                }
            }
        }

        UserProfile otherProfile;
        String fullName = profile.getFullname();
        String loginName = profile.getLoginName();

        // It's illegal to use as a full name someone else's login name
        try
        {
            otherProfile = getUserDatabase().find( fullName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !fullName.equals( otherProfile.getFullname() ) )
            {
                Object[] args = { fullName };
                session.addMessage( SESSION_MESSAGES, MessageFormat.format( rb.getString("security.error.illegalfullname"),
                                                                            args ) );
            }
        }
        catch ( NoSuchPrincipalException e)
        { /* It's clean */ }

        // It's illegal to use as a login name someone else's full name
        try
        {
            otherProfile = getUserDatabase().find( loginName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !loginName.equals( otherProfile.getLoginName() ) )
            {
                Object[] args = { loginName };
                session.addMessage( SESSION_MESSAGES, MessageFormat.format( rb.getString("security.error.illegalloginname"),
                                                                            args ) );
            }
        }
        catch ( NoSuchPrincipalException e)
        { /* It's clean */ }
    }

    /**
     *  A helper method for returning all of the known WikiNames in this system.
     *  
     *  @return An Array of Principals
     *  @throws WikiSecurityException If for reason the names cannot be fetched
     */
    public Principal[] listWikiNames()
        throws WikiSecurityException
    {
        return getUserDatabase().getWikiNames();
    }

    /**
     * This is a database that gets used if nothing else is available. It does
     * nothing of note - it just mostly throws NoSuchPrincipalExceptions if
     * someone tries to log in.
     */
    public static class DummyUserDatabase extends AbstractUserDatabase
    {

        /**
         * No-op.
         * @throws WikiSecurityException never...
         */
        @SuppressWarnings("deprecation")
        public void commit() throws WikiSecurityException
        {
            // No operation
        }

        /**
         * No-op.
         * @param loginName the login name to delete
         * @throws WikiSecurityException never...
         */
        public void deleteByLoginName( String loginName ) throws WikiSecurityException
        {
            // No operation
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException never...
         */
        public UserProfile findByEmail(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException never...
         */
        public UserProfile findByFullName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException never...
         */
        public UserProfile findByLoginName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param uid the unique identifier to search for
         * @return the user profile
         * @throws NoSuchPrincipalException never...
         */
        public UserProfile findByUid( String uid ) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }
        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException never...
         */
        public UserProfile findByWikiName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op.
         * @return a zero-length array
         * @throws WikiSecurityException never...
         */
        public Principal[] getWikiNames() throws WikiSecurityException
        {
            return new Principal[0];
        }

        /**
         * No-op.
         * @param engine the wiki engine
         * @param props the properties used to initialize the wiki engine
         * @throws NoRequiredPropertyException never...
         */
        public void initialize(WikiEngine engine, Properties props) throws NoRequiredPropertyException
        {
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param loginName the login name
         * @param newName the proposed new login name
         * @throws DuplicateUserException never...
         * @throws WikiSecurityException never...
         */
        public void rename( String loginName, String newName ) throws DuplicateUserException, WikiSecurityException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op.
         * @param profile the user profile
         * @throws WikiSecurityException never...
         */
        public void save( UserProfile profile ) throws WikiSecurityException
        {
        }

    }

    // workflow task inner classes....................................................

    /**
     * Inner class that handles the actual profile save action. Instances
     * of this class are assumed to have been added to an approval workflow via
     * {@link org.apache.wiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, org.apache.wiki.workflow.Fact[], Task, String)};
     * they will not function correctly otherwise.
     *
     */
    public static class SaveUserProfileTask extends Task
    {
        private static final long serialVersionUID = 6994297086560480285L;
        private final UserDatabase m_db;
        private final WikiEngine m_engine;
        private final Locale m_loc;

        /**
         * Constructs a new Task for saving a user profile.
         * @param engine the wiki engine
         * @deprecated will be removed in 2.10 scope. Consider using 
         * {@link UserManager#SaveUserProfileTask(WikiEngine, Locale)} instead
         */
        @Deprecated
        public SaveUserProfileTask( WikiEngine engine )
        {
            super( SAVE_TASK_MESSAGE_KEY );
            m_engine = engine;
            m_db = engine.getUserManager().getUserDatabase();
            m_loc = null;
        }
        
        public SaveUserProfileTask( WikiEngine engine, Locale loc )
        {
            super( SAVE_TASK_MESSAGE_KEY );
            m_engine = engine;
            m_db = engine.getUserManager().getUserDatabase();
            m_loc = loc;
        }

        /**
         * Saves the user profile to the user database.
         * @return {@link org.apache.wiki.workflow.Outcome#STEP_COMPLETE} if the
         * task completed successfully
         * @throws WikiException if the save did not complete for some reason
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
                    InternationalizationManager i18n = m_engine.getInternationalizationManager();
                    i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc, "", "" );
                    String app = m_engine.getApplicationName();
                    String to = profile.getEmail();
                    String subject = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc, 
                                               "notification.createUserProfile.accept.subject", app );
                    
                    String content = i18n.get( InternationalizationManager.DEF_TEMPLATE, m_loc, 
                                               "notification.createUserProfile.accept.content", app, 
                                               profile.getLoginName(), 
                                               profile.getFullname(),
                                               profile.getEmail(),
                                               m_engine.getURL( WikiContext.LOGIN, null, null, true ) );
                    MailUtil.sendMessage( m_engine, to, subject, content);
                }
                catch ( AddressException e)
                {
                }
                catch ( MessagingException me )
                {
                    log.error( "Could not send registration confirmation e-mail. Is the e-mail server running?", me );
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
     * @see org.apache.wiki.event.WikiSecurityEvent
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

    /**
     *  Implements the JSON API for usermanager.
     *  <p>
     *  Even though this gets serialized whenever container shuts down/restarts,
     *  this gets reinstalled to the session when JSPWiki starts.  This means
     *  that it's not actually necessary to save anything.
     */
    public static final class JSONUserModule implements RPCCallable
    {
        private volatile UserManager m_manager;
        
        /**
         *  Create a new JSONUserModule.
         *  @param mgr Manager
         */
        public JSONUserModule( UserManager mgr )
        {
            m_manager = mgr;
        }
        
        /**
         *  Directly returns the UserProfile object attached to an uid.
         *
         *  @param uid The user id (e.g. WikiName)
         *  @return A UserProfile object
         *  @throws NoSuchPrincipalException If such a name does not exist.
         */
        public UserProfile getUserInfo( String uid )
            throws NoSuchPrincipalException
        {
            if( m_manager != null )
            {
                UserProfile prof = m_manager.getUserDatabase().find( uid );

                return prof;
            }
            
            throw new IllegalStateException("The manager is offline.");
        }
    }
}
