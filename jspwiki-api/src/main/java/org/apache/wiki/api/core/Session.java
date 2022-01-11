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
package org.apache.wiki.api.core;

import org.apache.wiki.event.WikiEventListener;

import javax.security.auth.Subject;
import java.security.AccessControlException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Locale;


/**
 * <p>Represents a long-running wiki session, with an associated user Principal, user Subject, and authentication status. The sesion
 * is initialized with minimal, default-deny values: authentication is set to <code>false</code>, and the user principal is set to
 * <code>null</code>.</p>
 * <p>The Session allows callers to:</p>
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
 * <p>To keep track of the Principals each user posseses, each Session stores a JAAS Subject. Various login processes add or
 * remove Principals when users authenticate or log out.</p>
 * <p>Session extends the {@link org.apache.wiki.event.WikiEventListener} interface and listens for group add/change/delete
 * events fired by event sources the Session is registered with: {@link org.apache.wiki.auth.AuthenticationManager},
 * {@link org.apache.wiki.auth.UserManager} and {@link org.apache.wiki.auth.authorize.GroupManager}, so it can catch group events. Thus,
 * when a user is added to a {@link org.apache.wiki.auth.authorize.Group}, a corresponding {@link org.apache.wiki.auth.GroupPrincipal} is
 * injected into the Subject's Principal set. Likewise, when the user is removed from the Group or the Group is deleted, the
 * GroupPrincipal is removed from the Subject. The effect that this strategy produces is extremely beneficial: when someone adds a user
 * to a wiki group, that user <em>immediately</em> gains the privileges associated with that group; he or she does not need to
 * re-authenticate.
 * </p>
 * <p>In addition to methods for examining individual <code>Session</code> objects, this class also contains a number of static
 * methods for managing Sessions for an entire wiki. These methods allow callers to find, query and remove Session objects, and
 * to obtain a list of the current wiki session users.</p>
 */
public interface Session extends WikiEventListener {

    /** An anonymous user's session status. */
    String  ANONYMOUS = "anonymous";

    /** An asserted user's session status. */
    String  ASSERTED = "asserted";

    /** An authenticated user's session status. */
    String  AUTHENTICATED = "authenticated";

    /**
     * Returns <code>true</code> if the user is considered asserted via a session cookie; that is, the Subject contains the Principal
     * Role.ASSERTED.
     *
     * @return Returns <code>true</code> if the user is asserted
     */
    boolean isAsserted();

    /**
     * Returns the authentication status of the user's session. The user is considered authenticated if the Subject contains the
     * Principal Role.AUTHENTICATED. If this method determines that an earlier LoginModule did not inject Role.AUTHENTICATED, it
     * will inject one if the user is not anonymous <em>and</em> not asserted.
     *
     * @return Returns <code>true</code> if the user is authenticated
     */
    boolean isAuthenticated();

    /**
     * <p>Determines whether the current session is anonymous. This will be true if any of these conditions are true:</p>
     * <ul>
     *   <li>The session's Principal set contains {@link org.apache.wiki.auth.authorize.Role#ANONYMOUS}</li>
     *   <li>The session's Principal set contains {@link org.apache.wiki.auth.WikiPrincipal#GUEST}</li>
     *   <li>The Principal returned by {@link #getUserPrincipal()} evaluates to an IP address.</li>
     * </ul>
     * <p>The criteria above are listed in the order in which they are evaluated.</p>
     * @return whether the current user's identity is equivalent to an IP address
     */
    boolean isAnonymous();

    /**
     * <p> Returns the Principal used to log in to an authenticated session. The login principal is determined by examining the
     * Subject's Principal set for PrincipalWrappers or WikiPrincipals with type designator <code>LOGIN_NAME</code>; the first one
     * found is the login principal. If one is not found, this method returns the first principal that isn't of type Role or
     * GroupPrincipal. If neither of these conditions hold, this method returns
     * {@link org.apache.wiki.auth.WikiPrincipal#GUEST}.
     *
     * @return the login Principal. If it is a PrincipalWrapper containing an externally-provided Principal, the object returned is the
     * Principal, not the wrapper around it.
     */
    Principal getLoginPrincipal();

    /**
     * <p>Returns the primary user Principal associated with this session. The primary user principal is determined as follows:</p>
     * <ol>
     *     <li>If the Subject's Principal set contains WikiPrincipals, the first WikiPrincipal with type designator
     *         <code>WIKI_NAME</code> or (alternatively) <code>FULL_NAME</code> is the primary Principal.</li>
     *     <li>For all other cases, the first Principal in the Subject's principal collection that that isn't of type Role or
     *         GroupPrincipal is the primary.</li>
     * </ol>
     * If no primary user Principal is found, this method returns {@link org.apache.wiki.auth.WikiPrincipal#GUEST}.
     *
     * @return the primary user Principal
     */
    Principal getUserPrincipal();

    /**
     *  Returns a cached Locale object for this user.  It's better to use WikiContext's corresponding getBundle() method, since that
     *  will actually react if the user changes the locale in the middle, but if that's not available (or, for some reason, you need
     *  the speed), this method can also be used.  The Locale expires when the Session expires, and currently there is no way to
     *  reset the Locale.
     *
     *  @return A cached Locale object
     *  @since 2.5.96
     */
    Locale getLocale();

    /**
     * Adds a message to the generic list of messages associated with the session. These messages retain their order of insertion and
     * remain until the {@link #clearMessages()} method is called.
     *
     * @param message the message to add; if <code>null</code> it is ignored.
     */
    void addMessage( String message );

    /**
     * Adds a message to the specific set of messages associated with the session. These messages retain their order of insertion and
     * remain until the {@link #clearMessages()} method is called.
     *
     * @param topic the topic to associate the message to;
     * @param message the message to add
     */
    void addMessage( String topic, String message );

    /**
     * Clears all messages associated with this session.
     */
    void clearMessages();

    /**
     * Clears all messages associated with a session topic.
     *
     * @param topic the topic whose messages should be cleared.
     */
    void clearMessages( String topic );

    /**
     * Returns all generic messages associated with this session. The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages()}.
     *
     * @return the current messages.
     */
    String[] getMessages();

    /**
     * Returns all messages associated with a session topic. The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages(String)}.
     *
     * @return the current messages.
     * @param topic The topic
     */
    String[] getMessages( String topic );

    /**
     * Returns all user Principals associated with this session. User principals are those in the Subject's principal collection that
     * aren't of type Role or of type GroupPrincipal. This is a defensive copy.
     *
     * @return Returns the user principal
     * @see org.apache.wiki.auth.AuthenticationManager#isUserPrincipal(Principal)
     */
    Principal[] getPrincipals();

    /**
     * Returns an array of Principal objects that represents the groups and roles that the user associated with a Session possesses.
     * The array is built by iterating through the Subject's Principal set and extracting all Role and GroupPrincipal objects into a
     * list. The list is returned as an array sorted in the natural order implied by each Principal's <code>getName</code> method. Note
     * that this method does <em>not</em> consult the external Authorizer or GroupManager; it relies on the Principals that have been
     * injected into the user's Subject at login time, or after group creation/modification/deletion.
     *
     * @return an array of Principal objects corresponding to the roles the Subject possesses
     */
    Principal[] getRoles();

    /**
     * Returns <code>true</code> if the Session's Subject possess a supplied Principal. This method eliminates the need to externally
     * request and inspect the JAAS subject.
     *
     * @param principal the Principal to test
     * @return the result
     */
    boolean hasPrincipal( Principal principal );

    /** Invalidates the Session and resets its Subject's Principals to the equivalent of a "guest session". */
    void invalidate();

    /**
     * <p>Returns the status of the wiki session as a text string. Valid values are:</p>
     * <ul>
     *   <li>{@link #AUTHENTICATED}</li>
     *   <li>{@link #ASSERTED}</li>
     *   <li>{@link #ANONYMOUS}</li>
     * </ul>
     *
     * @return the user's session status
     */
    String getStatus();

    /**
     * Returns the {@link Subject} associated to the session.
     *
     * @return {@link Subject} associated to the session.
     */
    Subject getSubject();

    /**
     * Wrapper for {@link Subject#doAsPrivileged(Subject, PrivilegedAction, java.security.AccessControlContext)}
     * that executes an action with the privileges posssessed by a Session's Subject. The action executes with a <code>null</code>
     * AccessControlContext, which has the effect of running it "cleanly" without the AccessControlContexts of the caller.
     *
     * @param session the wiki session
     * @param action the privileged action
     * @return the result of the privileged action; may be <code>null</code>
     * @throws java.security.AccessControlException if the action is not permitted by the security policy
     */
    static Object doPrivileged( final Session session, final PrivilegedAction<?> action ) throws AccessControlException {
        return Subject.doAsPrivileged( session.getSubject(), action, null );
    }

}
