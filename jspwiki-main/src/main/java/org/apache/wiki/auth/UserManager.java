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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.ajax.AjaxUtil;
import org.apache.wiki.ajax.WikiAjaxDispatcherServlet;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
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
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.ui.InputValidator;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.workflow.Decision;
import org.apache.wiki.workflow.DecisionRequiredException;
import org.apache.wiki.workflow.Fact;
import org.apache.wiki.workflow.Step;
import org.apache.wiki.workflow.Workflow;
import org.apache.wiki.workflow.WorkflowBuilder;
import org.apache.wiki.workflow.WorkflowManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.WeakHashMap;


/**
 * Provides a facade for obtaining user information.
 * @since 2.3
 */
public class UserManager {

    private static final String USERDATABASE_PACKAGE = "org.apache.wiki.auth.user";
    private static final String SESSION_MESSAGES = "profile";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_FULLNAME = "fullname";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_LOGINNAME = "loginname";
    private static final String UNKNOWN_CLASS = "<unknown>";

    private WikiEngine m_engine;

    private static final Logger log = Logger.getLogger(UserManager.class);

    /** Message key for the "save profile" message. */
    private static final String PROP_DATABASE               = "jspwiki.userdatabase";

    public static final String JSON_USERS = "users";

    // private static final String  PROP_ACLMANAGER     = "jspwiki.aclManager";

    /** Associates wiki sessions with profiles */
    private final Map<WikiSession,UserProfile> m_profiles = new WeakHashMap<>();

    /** The user database loads, manages and persists user identities */
    private UserDatabase     m_database;

    /**
     * Initializes the engine for its nefarious purposes.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    public void initialize( final WikiEngine engine, final Properties props ) {
        m_engine = engine;

        // Attach the PageManager as a listener
        // TODO: it would be better if we did this in PageManager directly
        addWikiEventListener( engine.getPageManager() );

        //TODO: Replace with custom annotations. See JSPWIKI-566
        WikiAjaxDispatcherServlet.registerServlet( JSON_USERS, new JSONUserModule(this), new AllPermission(null));
    }

    /**
     * Returns the UserDatabase employed by this WikiEngine. The UserDatabase is lazily initialized by this method, if it does
     * not exist yet. If the initialization fails, this method will use the inner class DummyUserDatabase as a default (which
     * is enough to get JSPWiki running).
     *
     * @return the dummy user database
     * @since 2.3
     */
    public UserDatabase getUserDatabase() {
        if( m_database != null ) {
            return m_database;
        }

        String dbClassName = UNKNOWN_CLASS;

        try {
            dbClassName = TextUtil.getRequiredProperty( m_engine.getWikiProperties(), PROP_DATABASE );

            log.info( "Attempting to load user database class " + dbClassName );
            final Class<?> dbClass = ClassUtil.findClass( USERDATABASE_PACKAGE, dbClassName );
            m_database = (UserDatabase) dbClass.newInstance();
            m_database.initialize( m_engine, m_engine.getWikiProperties() );
            log.info("UserDatabase initialized.");
        } catch( final NoSuchElementException | NoRequiredPropertyException e ) {
            log.error( "You have not set the '"+PROP_DATABASE+"'. You need to do this if you want to enable user management by JSPWiki.", e );
        } catch( final ClassNotFoundException e ) {
            log.error( "UserDatabase class " + dbClassName + " cannot be found", e );
        } catch( final InstantiationException e ) {
            log.error( "UserDatabase class " + dbClassName + " cannot be created", e );
        } catch( final IllegalAccessException e ) {
            log.error( "You are not allowed to access this user database class", e );
        } catch( final WikiSecurityException e ) {
            log.error( "Exception initializing user database: " + e.getMessage(), e );
        } finally {
            if( m_database == null ) {
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
    public UserProfile getUserProfile( final WikiSession session ) {
        // Look up cached user profile
        UserProfile profile = m_profiles.get( session );
        boolean newProfile = profile == null;
        Principal user = null;

        // If user is authenticated, figure out if this is an existing profile
        if ( session.isAuthenticated() ) {
            user = session.getUserPrincipal();
            try {
                profile = getUserDatabase().find( user.getName() );
                newProfile = false;
            } catch( final NoSuchPrincipalException e ) { }
        }

        if ( newProfile ) {
            profile = getUserDatabase().newProfile();
            if ( user != null ) {
                profile.setLoginName( user.getName() );
            }
            if ( !profile.isNew() ) {
                throw new IllegalStateException( "New profile should be marked 'new'. Check your UserProfile implementation." );
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
    public void setUserProfile( WikiSession session, UserProfile profile ) throws DuplicateUserException, WikiException
    {
        // Verify user is allowed to save profile!
        final Permission p = new WikiPermission( m_engine.getApplicationName(), WikiPermission.EDIT_PROFILE_ACTION );
        if ( !m_engine.getAuthorizationManager().checkPermission( session, p ) )
        {
            throw new WikiSecurityException( "You are not allowed to save wiki profiles." );
        }

        // Check if profile is new, and see if container allows creation
        final boolean newProfile = profile.isNew();

        // Check if another user profile already has the fullname or loginname
        final UserProfile oldProfile = getUserProfile( session );
        final boolean nameChanged = ( oldProfile != null && oldProfile.getFullname() != null ) &&
                                    !( oldProfile.getFullname().equals( profile.getFullname() ) &&
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
        catch( final NoSuchPrincipalException e )
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
        catch( final NoSuchPrincipalException e )
        {
        }

        // For new accounts, create approval workflow for user profile save.
        if ( newProfile && oldProfile != null && oldProfile.isNew() )
        {
            startUserProfileCreationWorkflow(session, profile);

            // If the profile doesn't need approval, then just log the user in

            try
            {
                final AuthenticationManager mgr = m_engine.getAuthenticationManager();
                if ( newProfile && !mgr.isContainerAuthenticated() )
                {
                    mgr.login( session, null, profile.getLoginName(), profile.getPassword() );
                }
            }
            catch ( final WikiException e )
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
                final UserProfile[] profiles = new UserProfile[] { oldProfile, profile };
                fireEvent( WikiSecurityEvent.PROFILE_NAME_CHANGED, session, profiles );
            }
            else
            {
                // Fire an event that says we have new a new profile (new principals)
                fireEvent( WikiSecurityEvent.PROFILE_SAVE, session, profile );
            }
        }
    }

    public void startUserProfileCreationWorkflow( final WikiSession session, final UserProfile profile ) throws WikiException {
        final WorkflowBuilder builder = WorkflowBuilder.getBuilder( m_engine );
        final Principal submitter = session.getUserPrincipal();
        final Step completionTask = m_engine.getTasksManager().buildSaveUserProfileTask( m_engine, session.getLocale() );

        // Add user profile attribute as Facts for the approver (if required)
        final boolean hasEmail = profile.getEmail() != null;
        final Fact[] facts = new Fact[ hasEmail ? 4 : 3];
        facts[0] = new Fact( WorkflowManager.WF_UP_CREATE_SAVE_FACT_PREFS_FULL_NAME, profile.getFullname() );
        facts[1] = new Fact( WorkflowManager.WF_UP_CREATE_SAVE_FACT_PREFS_LOGIN_NAME, profile.getLoginName() );
        facts[2] = new Fact( WorkflowManager.WF_UP_CREATE_SAVE_FACT_SUBMITTER, submitter.getName() );
        if ( hasEmail )
        {
            facts[3] = new Fact( WorkflowManager.WF_UP_CREATE_SAVE_FACT_PREFS_EMAIL, profile.getEmail() );
        }
        final Workflow workflow = builder.buildApprovalWorkflow( submitter, 
                                                                 WorkflowManager.WF_UP_CREATE_SAVE_APPROVER, 
                                                                 null, 
                                                                 WorkflowManager.WF_UP_CREATE_SAVE_DECISION_MESSAGE_KEY, 
                                                                 facts, 
                                                                 completionTask, 
                                                                 null );

        workflow.setAttribute( WorkflowManager.WF_UP_CREATE_SAVE_ATTR_SAVED_PROFILE, profile );
        m_engine.getWorkflowManager().start(workflow);

        final boolean approvalRequired = workflow.getCurrentStep() instanceof Decision;

        // If the profile requires approval, redirect user to message page
        if ( approvalRequired )
        {
            throw new DecisionRequiredException( "This profile must be approved before it becomes active" );
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
    public UserProfile parseProfile( WikiContext context )
    {
        // Retrieve the user's profile (may have been previously cached)
        final UserProfile profile = getUserProfile( context.getWikiSession() );
        final HttpServletRequest request = context.getHttpRequest();

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
        // If authenticated, login name is always taken from container
        if ( m_engine.getAuthenticationManager().isContainerAuthenticated() &&
                context.getWikiSession().isAuthenticated() )
        {
            loginName = context.getWikiSession().getLoginPrincipal().getName();
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
    public void validateProfile( WikiContext context, UserProfile profile )
    {
        final boolean isNew = profile.isNew();
        final WikiSession session = context.getWikiSession();
        final InputValidator validator = new InputValidator( SESSION_MESSAGES, context );
        final ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE );

        //
        //  Query the SpamFilter first
        //
        final FilterManager fm = m_engine.getFilterManager();
        final List<PageFilter> ls = fm.getFilterList();
        for( final PageFilter pf : ls )
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
            final String password = profile.getPassword();
            if ( password == null )
            {
                if ( isNew )
                {
                    session.addMessage( SESSION_MESSAGES, rb.getString("security.error.blankpassword") );
                }
            }
            else
            {
                final HttpServletRequest request = context.getHttpRequest();
                final String password2 = ( request == null ) ? null : request.getParameter( "password2" );
                if ( !password.equals( password2 ) )
                {
                    session.addMessage( SESSION_MESSAGES, rb.getString("security.error.passwordnomatch") );
                }
            }
        }

        UserProfile otherProfile;
        final String fullName = profile.getFullname();
        final String loginName = profile.getLoginName();
        final String email = profile.getEmail();

        // It's illegal to use as a full name someone else's login name
        try
        {
            otherProfile = getUserDatabase().find( fullName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !fullName.equals( otherProfile.getFullname() ) )
            {
                final Object[] args = { fullName };
                session.addMessage( SESSION_MESSAGES, MessageFormat.format( rb.getString("security.error.illegalfullname"), args ) );
            }
        }
        catch ( final NoSuchPrincipalException e)
        { /* It's clean */ }

        // It's illegal to use as a login name someone else's full name
        try
        {
            otherProfile = getUserDatabase().find( loginName );
            if ( otherProfile != null && !profile.equals( otherProfile ) && !loginName.equals( otherProfile.getLoginName() ) )
            {
                final Object[] args = { loginName };
                session.addMessage( SESSION_MESSAGES, MessageFormat.format( rb.getString("security.error.illegalloginname"), args ) );
            }
        }
        catch ( final NoSuchPrincipalException e)
        { /* It's clean */ }

        // It's illegal to use multiple accounts with the same email
        try
        {
            otherProfile = getUserDatabase().findByEmail( email );
            if ( otherProfile != null
                && !profile.getUid().equals(otherProfile.getUid()) // Issue JSPWIKI-1042
                && !profile.equals( otherProfile ) && StringUtils.lowerCase( email ).equals( StringUtils.lowerCase(otherProfile.getEmail() ) ) )
            {
                final Object[] args = { email };
                session.addMessage( SESSION_MESSAGES, MessageFormat.format( rb.getString("security.error.email.taken"), args ) );
            }
        }
        catch ( final NoSuchPrincipalException e)
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
     * This is a database that gets used if nothing else is available. It does nothing of note - it just mostly throws
     * NoSuchPrincipalExceptions if someone tries to log in.
     */
    public static class DummyUserDatabase extends AbstractUserDatabase {

        /**
         * No-op.
         * @param loginName the login name to delete
         */
        @Override
        public void deleteByLoginName( String loginName ) {
            // No operation
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public UserProfile findByEmail(final String index) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public UserProfile findByFullName(final String index) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public UserProfile findByLoginName(final String index) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param uid the unique identifier to search for
         * @return the user profile
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public UserProfile findByUid( final String uid ) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }
        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param index the name to search for
         * @return the user profile
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public UserProfile findByWikiName(final String index) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op.
         * @return a zero-length array
         */
        @Override
        public Principal[] getWikiNames() {
            return new Principal[0];
        }

        /**
         * No-op.
         *
         * @param engine the wiki engine
         * @param props the properties used to initialize the wiki engine
         */
        @Override
        public void initialize(final WikiEngine engine, final Properties props) {
        }

        /**
         * No-op; always throws <code>NoSuchPrincipalException</code>.
         * @param loginName the login name
         * @param newName the proposed new login name
         * @throws NoSuchPrincipalException always...
         */
        @Override
        public void rename( final String loginName, final String newName ) throws NoSuchPrincipalException {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        /**
         * No-op.
         * @param profile the user profile
         */
        @Override
        public void save( final UserProfile profile ) {
        }

    }

    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance.
     * This is a convenience method.
     * @param listener the event listener
     */
    public synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * This is a convenience method.
     * @param listener the event listener
     */
    public synchronized void removeWikiEventListener( WikiEventListener listener )
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
    protected void fireEvent( int type, WikiSession session, Object profile )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiSecurityEvent(session,type,profile));
        }
    }

    /**
     *  Implements the JSON API for usermanager.
     *  <p>
     *  Even though this gets serialized whenever container shuts down/restarts, this gets reinstalled to the session when JSPWiki starts.
     *  This means that it's not actually necessary to save anything.
     */
    public static final class JSONUserModule implements WikiAjaxServlet {

		private volatile UserManager m_manager;

        /**
         *  Create a new JSONUserModule.
         *  @param mgr Manager
         */
        public JSONUserModule( UserManager mgr )
        {
            m_manager = mgr;
        }

        @Override
        public String getServletMapping() {
        	return JSON_USERS;
        }

        @Override
        public void service(HttpServletRequest req, HttpServletResponse resp, String actionName, List<String> params) throws ServletException, IOException {
        	try {
            	if( params.size() < 1 ) {
            		return;
            	}
        		final String uid = params.get(0);
	        	log.debug("uid="+uid);
	        	if (StringUtils.isNotBlank(uid)) {
		            final UserProfile prof = getUserInfo(uid);
		            resp.getWriter().write(AjaxUtil.toJson(prof));
	        	}
        	} catch (final NoSuchPrincipalException e) {
        		throw new ServletException(e);
        	}
        }

        /**
         *  Directly returns the UserProfile object attached to an uid.
         *
         *  @param uid The user id (e.g. WikiName)
         *  @return A UserProfile object
         *  @throws NoSuchPrincipalException If such a name does not exist.
         */
        public UserProfile getUserInfo( String uid ) throws NoSuchPrincipalException {
            if( m_manager != null ) {
                final UserProfile prof = m_manager.getUserDatabase().find( uid );
                return prof;
            }

            throw new IllegalStateException("The manager is offline.");
        }
    }

}
