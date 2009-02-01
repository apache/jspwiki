/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.api;

import java.security.Principal;
import java.util.Locale;

/**
 * <p>Represents a long-running wiki session, with an associated user Principal,
 * user Subject, and authentication status. This class is initialized with
 * minimal, default-deny values: authentication is set to <code>false</code>,
 * and the user principal is set to <code>null</code>.</p>
 * <p>The WikiSession class allows callers to:</p>
 * <ul>
 *   <li>Obtain the authentication status of the user via
 *     {@link #isAnonymous()} and {@link #isAuthenticated()}</li>
 *   <li>Query the session for Principals representing the
 *     user's identity via {@link #getLoginPrincipal()},
 *     {@link #getUserPrincipal()} and {@link #getPrincipals()}</li>
 *   <li>Store, retrieve and clear UI messages via
 *     {@link #addMessage(String)}, {@link #getMessages(String)}
 *     and {@link #clearMessages(String)}</li>
 * </ul>
 * <p>To keep track of the Principals each user posseses, each WikiSession
 * stores a JAAS Subject. Various login processes add or remove Principals
 * when users authenticate or log out.</p>
 * <p>WikiSession implements the {@link org.apache.wiki.event.WikiEventListener}
 * interface and listens for group add/change/delete events fired by
 * event sources the WikiSession is registered with. Normally,
 * {@link org.apache.wiki.auth.AuthenticationManager} registers each WikiSession
 * with the {@link org.apache.wiki.auth.authorize.GroupManager}
 * so it can catch group events. Thus, when a user is added to a
 * {@link org.apache.wiki.auth.authorize.Group}, a corresponding
 * {@link org.apache.wiki.auth.GroupPrincipal} is injected into
 * the Subject's Principal set. Likewise, when the user is removed from
 * the Group or the Group is deleted, the GroupPrincipal is removed
 * from the Subject. The effect that this strategy produces is extremely
 * beneficial: when someone adds a user to a wiki group, that user
 * <em>immediately</em> gains the privileges associated with that
 * group; he or she does not need to re-authenticate.
 * </p>
 * <p>In addition to methods for examining individual <code>WikiSession</code>
 * objects, this class also contains a number of static methods for
 * managing WikiSessions for an entire wiki. These methods allow callers
 * to find, query and remove WikiSession objects, and
 * to obtain a list of the current wiki session users.</p>
 * <p>WikiSession encloses a protected static class, {@link org.apache.wiki.auth.SessionMonitor},
 * to keep track of WikiSessions registered with each wiki.</p>
 * @author Andrew R. Jaquith
 */
public interface WikiSession
{

    /**
     *  Returns true, if the current user has administrative permissions (i.e. the omnipotent
     *  AllPermission).
     *
     *  @since 2.4.46
     *  @return true, if the user has all permissions.
     */
    // NEW: moved from WikiContext.hasAdminPermissions()
    public boolean isAdmin();

    /**
     * Returns <code>true</code> if the user is considered asserted via
     * a session cookie; that is, the Subject contains the Principal
     * Role.ASSERTED.
     * @return Returns <code>true</code> if the user is asserted
     */
    public boolean isAsserted();

    /**
     * Returns the authentication status of the user's session. The user is
     * considered authenticated if the Subject contains the Principal
     * Role.AUTHENTICATED. If this method determines that an earlier
     * LoginModule did not inject Role.AUTHENTICATED, it will inject one
     * if the user is not anonymous <em>and</em> not asserted.
     * @return Returns <code>true</code> if the user is authenticated
     */
    public boolean isAuthenticated();

    /**
     * <p>Determines whether the current session is anonymous. This will be
     * true if any of these conditions are true:</p>
     * <ul>
     *   <li>The session's Principal set contains
     *       {@link org.apache.wiki.auth.authorize.Role#ANONYMOUS}</li>
     *   <li>The session's Principal set contains
     *       {@link org.apache.wiki.auth.WikiPrincipal#GUEST}</li>
     *   <li>The Principal returned by {@link #getUserPrincipal()} evaluates
     *       to an IP address.</li>
     * </ul>
     * <p>The criteria above are listed in the order in which they are
     * evaluated.</p>
     * @return whether the current user's identity is equivalent to an IP
     * address
     */
    public boolean isAnonymous();

    /**
     * <p> Returns the Principal used to log in to an authenticated session. The
     * login principal is determined by examining the Subject's Principal set
     * for PrincipalWrappers or WikiPrincipals with type designator
     * <code>LOGIN_NAME</code>; the first one found is the login principal.
     * If one is not found, this method returns the first principal that isn't
     * of type Role or GroupPrincipal. If neither of these conditions hold, this method returns
     * {@link org.apache.wiki.auth.WikiPrincipal#GUEST}.
     * @return the login Principal. If it is a PrincipalWrapper containing an
     * externally-provided Principal, the object returned is the Principal, not
     * the wrapper around it.
     */
    public Principal getLoginPrincipal();

    /**
     * <p>Returns the primary user Principal associated with this session. The
     * primary user principal is determined as follows:</p> <ol> <li>If the
     * Subject's Principal set contains WikiPrincipals, the first WikiPrincipal
     * with type designator <code>WIKI_NAME</code> or (alternatively)
     * <code>FULL_NAME</code> is the primary Principal.</li>
     *   <li>For all other cases, the first Principal in the Subject's principal
     *       collection that isn't of type Role or GroupPrincipal is the primary.</li>
     * </ol>
     * If no primary user Principal is found, this method returns
     * {@link org.apache.wiki.auth.WikiPrincipal#GUEST}.
     * @return the primary user Principal
     */
    public Principal getUserPrincipal();

    /**
     *  Returns a cached Locale object for this user.  It's better to use
     *  WikiContext's corresponding getBundle() method, since that will actually
     *  react if the user changes the locale in the middle, but if that's not
     *  available (or, for some reason, you need the speed), this method can
     *  also be used.  The Locale expires when the WikiSession expires, and
     *  currently there is no way to reset the Locale.
     *
     *  @return A cached Locale object
     *  @since 2.5.96
     */
    public Locale getLocale();

    /**
     * Adds a message to the generic list of messages associated with the
     * session. These messages retain their order of insertion and remain until
     * the {@link #clearMessages()} method is called.
     * @param message the message to add; if <code>null</code> it is ignored.
     */
    public void addMessage(String message);


    /**
     * Adds a message to the specific set of messages associated with the
     * session. These messages retain their order of insertion and remain until
     * the {@link #clearMessages()} method is called.
     * @param topic the topic to associate the message to;
     * @param message the message to add
     */
    public void addMessage(String topic, String message);

    /**
     * Clears all messages associated with this session.
     */
    public void clearMessages();

    /**
     * Clears all messages associated with a session topic.
     * @param topic the topic whose messages should be cleared.
     */
    public void clearMessages( String topic );

    /**
     * Returns all generic messages associated with this session.
     * The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages()}.
     * @return the current messages.
     */
    public String[] getMessages();

    /**
     * Returns all messages associated with a session topic.
     * The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages(String)}.
     * @return the current messages.
     * @param topic The topic
     */
    public String[] getMessages( String topic );

    /**
     * Returns all user Principals associated with this session. User principals
     * are those in the Subject's principal collection that aren't of type Role or
     * of type GroupPrincipal. This is a defensive copy.
     * @return Returns the user principal
     * @see org.apache.wiki.auth.AuthenticationManager#isUserPrincipal(Principal)
     */
    public Principal[] getPrincipals();

    /**
     * Returns an array of Principal objects that represents the groups and
     * roles that the user associated with a WikiSession possesses. The array is
     * built by iterating through the Subject's Principal set and extracting all
     * Role and GroupPrincipal objects into a list. The list is returned as an
     * array sorted in the natural order implied by each Principal's
     * <code>getName</code> method. Note that this method does <em>not</em>
     * consult the external Authorizer or GroupManager; it relies on the
     * Principals that have been injected into the user's Subject at login time,
     * or after group creation/modification/deletion.
     * @return an array of Principal objects corresponding to the roles the
     *         Subject possesses
     */
    public Principal[] getRoles();

    /**
     * Returns <code>true</code> if the WikiSession's Subject
     * possesses the supplied Principal. This method eliminates the need
     * to externally request and inspect the JAAS subject.
     * @param principal the Principal to test
     * @return the result
     */
    public boolean hasPrincipal( Principal principal );

    /**
     * <p>Returns the status of the wiki session as a text string. Valid values are:</p>
     * <ul>
     *   <li>{@link org.apache.wiki.WikiSession#AUTHENTICATED}</li>
     *   <li>{@link org.apache.wiki.WikiSession#ASSERTED}</li>
     *   <li>{@link org.apache.wiki.WikiSession#ANONYMOUS}</li>
     * </ul>
     * @return the user's session status
     */
    public String getStatus();
}
