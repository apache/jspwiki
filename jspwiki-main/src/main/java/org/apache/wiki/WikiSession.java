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
package org.apache.wiki;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.util.HttpUtil;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * <p>Default implementation for {@link Session}.</p>
 * <p>In addition to methods for examining individual <code>WikiSession</code> objects, this class also contains a number of static
 * methods for managing WikiSessions for an entire wiki. These methods allow callers to find, query and remove WikiSession objects, and
 * to obtain a list of the current wiki session users.</p>
 */
public final class WikiSession implements Session {

    private static final Logger log                   = Logger.getLogger( WikiSession.class );

    private static final String ALL                   = "*";

    private static final ThreadLocal< Session > c_guestSession = new ThreadLocal<>();

    private final Subject       m_subject             = new Subject();

    private final Map< String, Set< String > > m_messages  = new ConcurrentHashMap<>();

    /** The Engine that created this session. */
    private Engine              m_engine              = null;

    private String              m_status              = ANONYMOUS;

    private Principal           m_userPrincipal       = WikiPrincipal.GUEST;

    private Principal           m_loginPrincipal      = WikiPrincipal.GUEST;

    private Locale              m_cachedLocale        = Locale.getDefault();

    /**
     * Returns <code>true</code> if one of this WikiSession's user Principals can be shown to belong to a particular wiki group. If
     * the user is not authenticated, this method will always return <code>false</code>.
     *
     * @param group the group to test
     * @return the result
     */
    protected boolean isInGroup( final Group group ) {
        for( final Principal principal : getPrincipals() ) {
            if( isAuthenticated() && group.isMember( principal ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Private constructor to prevent WikiSession from being instantiated directly.
     */
    private WikiSession() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAsserted() {
        return m_subject.getPrincipals().contains( Role.ASSERTED );
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthenticated() {
        // If Role.AUTHENTICATED is in principals set, always return true.
        if ( m_subject.getPrincipals().contains( Role.AUTHENTICATED ) ) {
            return true;
        }

        // With non-JSPWiki LoginModules, the role may not be there, so we need to add it if the user really is authenticated.
        if ( !isAnonymous() && !isAsserted() ) {
            m_subject.getPrincipals().add( Role.AUTHENTICATED );
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAnonymous() {
        final Set< Principal > principals = m_subject.getPrincipals();
        return principals.contains( Role.ANONYMOUS ) ||
               principals.contains( WikiPrincipal.GUEST ) ||
               HttpUtil.isIPV4Address( getUserPrincipal().getName() );
    }

    /** {@inheritDoc} */
    @Override
    public Principal getLoginPrincipal() {
        return m_loginPrincipal;
    }

    /** {@inheritDoc} */
    @Override
    public Principal getUserPrincipal() {
        return m_userPrincipal;
    }

    /** {@inheritDoc} */
    @Override
    public Locale getLocale() {
        return m_cachedLocale;
    }

    /** {@inheritDoc} */
    @Override
    public void addMessage( final String message ) {
        addMessage( ALL, message );
    }

    /** {@inheritDoc} */
    @Override
    public void addMessage( final String topic, final String message ) {
        if ( topic == null ) {
            throw new IllegalArgumentException( "addMessage: topic cannot be null." );
        }
        final Set< String > messages = m_messages.computeIfAbsent( topic, k -> new LinkedHashSet<>() );
        messages.add( StringUtils.defaultString( message ) );
    }

    /** {@inheritDoc} */
    @Override
    public void clearMessages() {
        m_messages.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void clearMessages( final String topic ) {
        final Set< String > messages = m_messages.get( topic );
        if ( messages != null ) {
            m_messages.clear();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] getMessages() {
        return getMessages( ALL );
    }

    /** {@inheritDoc} */
    @Override
    public String[] getMessages( final String topic ) {
        final Set< String > messages = m_messages.get( topic );
        if( messages == null || messages.size() == 0 ) {
            return new String[ 0 ];
        }
        return messages.toArray( new String[ messages.size() ] );
    }

    /** {@inheritDoc} */
    @Override
    public Principal[] getPrincipals() {
        final ArrayList< Principal > principals = new ArrayList<>();

        // Take the first non Role as the main Principal
        for( final Principal principal : m_subject.getPrincipals() ) {
            if ( AuthenticationManager.isUserPrincipal( principal ) ) {
                principals.add( principal );
            }
        }

        return principals.toArray( new Principal[ principals.size() ] );
    }

    /** {@inheritDoc} */
    @Override
    public Principal[] getRoles() {
        final Set< Principal > roles = new HashSet<>();

        // Add all of the Roles possessed by the Subject directly
        roles.addAll( m_subject.getPrincipals( Role.class ) );

        // Add all of the GroupPrincipals possessed by the Subject directly
        roles.addAll( m_subject.getPrincipals( GroupPrincipal.class ) );

        // Return a defensive copy
        final Principal[] roleArray = roles.toArray( new Principal[ roles.size() ] );
        Arrays.sort( roleArray, WikiPrincipal.COMPARATOR );
        return roleArray;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPrincipal( final Principal principal ) {
        return m_subject.getPrincipals().contains( principal );
    }

    /**
     * Listens for WikiEvents generated by source objects such as the GroupManager, UserManager or AuthenticationManager. This method adds
     * Principals to the private Subject managed by the WikiSession.
     *
     * @see org.apache.wiki.event.WikiEventListener#actionPerformed(WikiEvent)
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if ( event instanceof WikiSecurityEvent ) {
            final WikiSecurityEvent e = (WikiSecurityEvent)event;
            if ( e.getTarget() != null ) {
                switch( e.getType() ) {
                case WikiSecurityEvent.GROUP_ADD:
                    final Group groupAdd = ( Group )e.getTarget();
                    if( isInGroup( groupAdd ) ) {
                        m_subject.getPrincipals().add( groupAdd.getPrincipal() );
                    }
                    break;
                case WikiSecurityEvent.GROUP_REMOVE:
                    final Group group = ( Group )e.getTarget();
                    m_subject.getPrincipals().remove( group.getPrincipal() );
                    break;
                case WikiSecurityEvent.GROUP_CLEAR_GROUPS:
                    m_subject.getPrincipals().removeAll( m_subject.getPrincipals( GroupPrincipal.class ) );
                    break;
                case WikiSecurityEvent.LOGIN_INITIATED:
                    // Do nothing
                    break;
                case WikiSecurityEvent.PRINCIPAL_ADD:
                    final WikiSession targetPA = ( WikiSession )e.getTarget();
                    if( this.equals( targetPA ) && m_status.equals( AUTHENTICATED ) ) {
                        final Set< Principal > principals = m_subject.getPrincipals();
                        principals.add( ( Principal )e.getPrincipal() );
                    }
                    break;
                case WikiSecurityEvent.LOGIN_ANONYMOUS:
                    final WikiSession targetLAN = ( WikiSession )e.getTarget();
                    if( this.equals( targetLAN ) ) {
                        m_status = ANONYMOUS;

                        // Set the login/user principals and login status
                        final Set< Principal > principals = m_subject.getPrincipals();
                        m_loginPrincipal = ( Principal )e.getPrincipal();
                        m_userPrincipal = m_loginPrincipal;

                        // Add the login principal to the Subject, and set the built-in roles
                        principals.clear();
                        principals.add( m_loginPrincipal );
                        principals.add( Role.ALL );
                        principals.add( Role.ANONYMOUS );
                    }
                    break;
                case WikiSecurityEvent.LOGIN_ASSERTED:
                    final WikiSession targetLAS = ( WikiSession )e.getTarget();
                    if( this.equals( targetLAS ) ) {
                        m_status = ASSERTED;

                        // Set the login/user principals and login status
                        final Set< Principal > principals = m_subject.getPrincipals();
                        m_loginPrincipal = ( Principal )e.getPrincipal();
                        m_userPrincipal = m_loginPrincipal;

                        // Add the login principal to the Subject, and set the built-in roles
                        principals.clear();
                        principals.add( m_loginPrincipal );
                        principals.add( Role.ALL );
                        principals.add( Role.ASSERTED );
                    }
                    break;
                case WikiSecurityEvent.LOGIN_AUTHENTICATED:
                    final WikiSession targetLAU = ( WikiSession )e.getTarget();
                    if( this.equals( targetLAU ) ) {
                        m_status = AUTHENTICATED;

                        // Set the login/user principals and login status
                        final Set< Principal > principals = m_subject.getPrincipals();
                        m_loginPrincipal = ( Principal )e.getPrincipal();
                        m_userPrincipal = m_loginPrincipal;

                        // Add the login principal to the Subject, and set the built-in roles
                        principals.clear();
                        principals.add( m_loginPrincipal );
                        principals.add( Role.ALL );
                        principals.add( Role.AUTHENTICATED );

                        // Add the user and group principals
                        injectUserProfilePrincipals();  // Add principals for the user profile
                        injectGroupPrincipals();  // Inject group principals
                    }
                    break;
                case WikiSecurityEvent.PROFILE_SAVE:
                    final WikiSession sourcePS = e.getSrc();
                    if( this.equals( sourcePS ) ) {
                        injectUserProfilePrincipals();  // Add principals for the user profile
                        injectGroupPrincipals();  // Inject group principals
                    }
                    break;
                case WikiSecurityEvent.PROFILE_NAME_CHANGED:
                    // Refresh user principals based on new user profile
                    final WikiSession sourcePNC = e.getSrc();
                    if( this.equals( sourcePNC ) && m_status.equals( AUTHENTICATED ) ) {
                        // To prepare for refresh, set the new full name as the primary principal
                        final UserProfile[] profiles = ( UserProfile[] )e.getTarget();
                        final UserProfile newProfile = profiles[ 1 ];
                        if( newProfile.getFullname() == null ) {
                            throw new IllegalStateException( "User profile FullName cannot be null." );
                        }

                        final Set< Principal > principals = m_subject.getPrincipals();
                        m_loginPrincipal = new WikiPrincipal( newProfile.getLoginName() );

                        // Add the login principal to the Subject, and set the built-in roles
                        principals.clear();
                        principals.add( m_loginPrincipal );
                        principals.add( Role.ALL );
                        principals.add( Role.AUTHENTICATED );

                        // Add the user and group principals
                        injectUserProfilePrincipals();  // Add principals for the user profile
                        injectGroupPrincipals();  // Inject group principals
                    }
                    break;

                //  No action, if the event is not recognized.
                default:
                    break;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate() {
        m_subject.getPrincipals().clear();
        m_subject.getPrincipals().add( WikiPrincipal.GUEST );
        m_subject.getPrincipals().add( Role.ANONYMOUS );
        m_subject.getPrincipals().add( Role.ALL );
        m_userPrincipal = WikiPrincipal.GUEST;
        m_loginPrincipal = WikiPrincipal.GUEST;
    }

    /**
     * Injects GroupPrincipal objects into the user's Principal set based on the groups the user belongs to. For Groups, the algorithm
     * first calls the {@link GroupManager#getRoles()} to obtain the array of GroupPrincipals the authorizer knows about. Then, the
     * method {@link GroupManager#isUserInRole(Session, Principal)} is called for each Principal. If the user is a member of the
     * group, an equivalent GroupPrincipal is injected into the user's principal set. Existing GroupPrincipals are flushed and replaced.
     * This method should generally be called after a user's {@link org.apache.wiki.auth.user.UserProfile} is saved. If the wiki session
     * is null, or there is no matching user profile, the method returns silently.
     */
    protected void injectGroupPrincipals() {
        // Flush the existing GroupPrincipals
        m_subject.getPrincipals().removeAll( m_subject.getPrincipals(GroupPrincipal.class) );

        // Get the GroupManager and test for each Group
        final GroupManager manager = m_engine.getManager( GroupManager.class );
        for( final Principal group : manager.getRoles() ) {
            if ( manager.isUserInRole( this, group ) ) {
                m_subject.getPrincipals().add( group );
            }
        }
    }

    /**
     * Adds Principal objects to the Subject that correspond to the logged-in user's profile attributes for the wiki name, full name
     * and login name. These Principals will be WikiPrincipals, and they will replace all other WikiPrincipals in the Subject. <em>Note:
     * this method is never called during anonymous or asserted sessions.</em>
     */
    protected void injectUserProfilePrincipals() {
        // Search for the user profile
        final String searchId = m_loginPrincipal.getName();
        if ( searchId == null ) {
            // Oh dear, this wasn't an authenticated user after all
            log.info("Refresh principals failed because WikiSession had no user Principal; maybe not logged in?");
            return;
        }

        // Look up the user and go get the new Principals
        final UserDatabase database = m_engine.getManager( UserManager.class ).getUserDatabase();
        if( database == null ) {
            throw new IllegalStateException( "User database cannot be null." );
        }
        try {
            final UserProfile profile = database.find( searchId );
            final Principal[] principals = database.getPrincipals( profile.getLoginName() );
            for( final Principal principal : principals ) {
                // Add the Principal to the Subject
                m_subject.getPrincipals().add( principal );

                // Set the user principal if needed; we prefer FullName, but the WikiName will also work
                final boolean isFullNamePrincipal = ( principal instanceof WikiPrincipal &&
                                                      ( ( WikiPrincipal )principal ).getType().equals( WikiPrincipal.FULL_NAME ) );
                if ( isFullNamePrincipal ) {
                   m_userPrincipal = principal;
                } else if ( !( m_userPrincipal instanceof WikiPrincipal ) ) {
                    m_userPrincipal = principal;
                }
            }
        } catch ( final NoSuchPrincipalException e ) {
            // We will get here if the user has a principal but not a profile
            // For example, it's a container-managed user who hasn't set up a profile yet
            log.warn("User profile '" + searchId + "' not found. This is normal for container-auth users who haven't set up a profile yet.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getStatus() {
        return m_status;
    }

    /** {@inheritDoc} */
    @Override
    public Subject getSubject() {
        return m_subject;
    }

    /**
     * Removes the wiki session associated with the user's HTTP request from the cache of wiki sessions, typically as part of a
     * logout process.
     *
     * @param engine the wiki engine
     * @param request the users's HTTP request
     */
    public static void removeWikiSession( final Engine engine, final HttpServletRequest request ) {
        if ( engine == null || request == null ) {
            throw new IllegalArgumentException( "Request or engine cannot be null." );
        }
        final SessionMonitor monitor = SessionMonitor.getInstance( engine );
        monitor.remove( request.getSession() );
        c_guestSession.remove();
    }

    /**
     * <p>Static factory method that returns the Session object associated with the current HTTP request. This method looks up
     * the associated HttpSession in an internal WeakHashMap and attempts to retrieve the WikiSession. If not found, one is created.
     * This method is guaranteed to always return a Session, although the authentication status is unpredictable until the user
     * attempts to log in. If the servlet request parameter is <code>null</code>, a synthetic {@link #guestSession(Engine)} is
     * returned.</p>
     * <p>When a session is created, this method attaches a WikiEventListener to the GroupManager, UserManager and AuthenticationManager,
     * so that changes to users, groups, logins, etc. are detected automatically.</p>
     *
     * @param engine the engine
     * @param request the servlet request object
     * @return the existing (or newly created) session
     */
    public static Session getWikiSession( final Engine engine, final HttpServletRequest request ) {
        if ( request == null ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Looking up WikiSession for NULL HttpRequest: returning guestSession()" );
            }
            return staticGuestSession( engine );
        }

        // Look for a WikiSession associated with the user's Http Session and create one if it isn't there yet.
        final HttpSession session = request.getSession();
        final SessionMonitor monitor = SessionMonitor.getInstance( engine );
        final WikiSession wikiSession = ( WikiSession )monitor.find( session );

        // Attach reference to wiki engine
        wikiSession.m_engine = engine;
        wikiSession.m_cachedLocale = request.getLocale();
        return wikiSession;
    }

    /**
     * Static factory method that creates a new "guest" session containing a single user Principal
     * {@link org.apache.wiki.auth.WikiPrincipal#GUEST}, plus the role principals {@link Role#ALL} and {@link Role#ANONYMOUS}. This
     * method also adds the session as a listener for GroupManager, AuthenticationManager and UserManager events.
     *
     * @param engine the wiki engine
     * @return the guest wiki session
     */
    public static Session guestSession( final Engine engine ) {
        final WikiSession session = new WikiSession();
        session.m_engine = engine;
        session.invalidate();

        // Add the session as listener for GroupManager, AuthManager, UserManager events
        final GroupManager groupMgr = engine.getManager( GroupManager.class );
        final AuthenticationManager authMgr = engine.getManager( AuthenticationManager.class );
        final UserManager userMgr = engine.getManager( UserManager.class );
        groupMgr.addWikiEventListener( session );
        authMgr.addWikiEventListener( session );
        userMgr.addWikiEventListener( session );

        return session;
    }

    /**
     *  Returns a static guest session, which is available for this thread only.  This guest session is used internally whenever
     *  there is no HttpServletRequest involved, but the request is done e.g. when embedding JSPWiki code.
     *
     *  @param engine Engine for this session
     *  @return A static WikiSession which is shared by all in this same Thread.
     */
    // FIXME: Should really use WeakReferences to clean away unused sessions.
    private static Session staticGuestSession( final Engine engine ) {
        Session session = c_guestSession.get();
        if( session == null ) {
            session = guestSession( engine );
            c_guestSession.set( session );
        }

        return session;
    }

    /**
     * Returns the total number of active wiki sessions for a particular wiki. This method delegates to the wiki's
     * {@link SessionMonitor#sessions()} method.
     *
     * @param engine the wiki session
     * @return the number of sessions
     * @deprecated use {@link SessionMonitor#sessions()} instead
     * @see SessionMonitor#sessions()
     */
    @Deprecated
    public static int sessions( final Engine engine ) {
        final SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.sessions();
    }

    /**
     * Returns Principals representing the current users known to a particular wiki. Each Principal will correspond to the
     * value returned by each WikiSession's {@link #getUserPrincipal()} method. This method delegates to
     * {@link SessionMonitor#userPrincipals()}.
     *
     * @param engine the wiki engine
     * @return an array of Principal objects, sorted by name
     * @deprecated use {@link SessionMonitor#userPrincipals()} instead
     * @see SessionMonitor#userPrincipals()
     */
    @Deprecated
    public static Principal[] userPrincipals( final Engine engine ) {
        final SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.userPrincipals();
    }

}
