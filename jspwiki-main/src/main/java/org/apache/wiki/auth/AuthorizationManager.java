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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.AccessController;
import java.security.Permission;
import java.security.Principal;
import java.util.Properties;


/**
 * <p>Manages all access control and authorization; determines what authenticated users are allowed to do.</p>
 * <p>Privileges in JSPWiki are expressed as Java-standard {@link java.security.Permission} classes. There are two types of permissions:</p>
 * <ul>
 *   <li>{@link org.apache.wiki.auth.permissions.WikiPermission} - privileges that apply to an entire wiki instance: <em>e.g.,</em>
 *   editing user profiles, creating pages, creating groups</li>
 *   <li>{@link org.apache.wiki.auth.permissions.PagePermission} - privileges that apply to a single wiki page or range of pages:
 *   <em>e.g.,</em> reading, editing, renaming
 * </ul>
 * <p>Calling classes determine whether they are entitled to perform a particular action by constructing the appropriate permission first,
 * then passing it and the current {@link Session} to the {@link #checkPermission(Session, Permission)} method. If
 * the session's Subject possesses the permission, the action is allowed.</p>
 * <p>For WikiPermissions, the decision criteria is relatively simple: the caller either possesses the permission, as granted by the wiki
 * security policy -- or not.</p>
 * <p>For PagePermissions, the logic is exactly the same if the page being checked does not have an access control list. However, if the
 * page does have an ACL, the authorization decision is made based the <em>union</em> of the permissions granted in the ACL and in the
 * security policy. In other words, the user must be named in the ACL (or belong to a group or role that is named in the ACL) <em>and</em>
 * be granted (at least) the same permission in the security policy. We do this to prevent a user from gaining more permissions than they
 * already have, based on the security policy.</p>
 * <p>See the implementation on {@link #checkPermission(Session, Permission)} method for more information on the authorization logic.</p>
 *
 * @since 2.3
 * @see AuthenticationManager
 */
public interface AuthorizationManager {

    /** The default external Authorizer is the {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer} */
    String DEFAULT_AUTHORIZER = "org.apache.wiki.auth.authorize.WebContainerAuthorizer";

    /** Property that supplies the security policy file name, in WEB-INF. */
    String POLICY = "jspwiki.policy.file";

    /** Name of the default security policy file, in WEB-INF. */
    String DEFAULT_POLICY = "jspwiki.policy";

    /** The property name in jspwiki.properties for specifying the external {@link Authorizer}. */
    String PROP_AUTHORIZER = "jspwiki.authorizer";

    /**
     * Returns <code>true</code> or <code>false</code>, depending on whether a Permission is allowed for the Subject associated with
     * a supplied Session. The access control algorithm works this way:
     * <ol>
     * <li>The {@link org.apache.wiki.auth.acl.Acl} for the page is obtained</li>
     * <li>The Subject associated with the current {@link org.apache.wiki.api.core.Session} is obtained</li>
     * <li>If the Subject's Principal set includes the Role Principal that is the administrator group, always allow the Permission</li>
     * <li>For all permissions, check to see if the Permission is allowed according to the default security policy. If it isn't, deny
     * the permission and halt further processing.</li>
     * <li>If there is an Acl, get the list of Principals assigned this Permission in the Acl: these will be role, group or user Principals,
     * or {@link org.apache.wiki.auth.acl.UnresolvedPrincipal}s (see below). Then iterate through the Subject's Principal set and determine
     * whether the user (Subject) possesses any one of these specified Roles or Principals.</li>
     * </ol>
     * <p>
     * Note that when iterating through the Acl's list of authorized Principals, it is possible that one or more of the Acl's Principal
     * entries are of type <code>UnresolvedPrincipal</code>. This means that the last time the ACL was read, the Principal (user, built-in
     * Role, authorizer Role, or wiki Group) could not be resolved: the Role was not valid, the user wasn't found in the UserDatabase, or
     * the Group wasn't known to (e.g., cached) in the GroupManager. If an <code>UnresolvedPrincipal</code> is encountered, this method
     * will attempt to resolve it first <em>before</em> checking to see if the Subject possesses this principal, by calling
     * {@link #resolvePrincipal(String)}. If the (re-)resolution does not succeed, the access check for the principal will fail by
     * definition (the Subject should never contain UnresolvedPrincipals).
     * </p>
     * <p>
     * If security not set to JAAS, will return true.
     * </p>
     *
     * @param session the current wiki session
     * @param permission the Permission being checked
     * @return the result of the Permission check
     */
    boolean checkPermission( Session session, Permission permission );

    /**
     * <p>Determines if the Subject associated with a supplied Session contains a desired Role or GroupPrincipal. The algorithm
     * simply checks to see if the Subject possesses the Role or GroupPrincipal it in its Principal set. Note that any user (anonymous,
     * asserted, authenticated) can possess a built-in role. But a user <em>must</em> be authenticated to possess a role other than one
     * of the built-in ones. We do this to prevent privilege escalation.</p>
     * <p>For all other cases, this method returns <code>false</code>.</p>
     * <p>Note that this method does <em>not</em> consult the external Authorizer or GroupManager; it relies on the Principals that
     * have been injected into the user's Subject at login time, or after group creation/modification/deletion.</p>
     *
     * @param session the current wiki session, which must be non-null. If null, the result of this method always returns <code>false</code>
     * @param principal the Principal (role or group principal) to look for, which must be non-<code>null</code>. If <code>null</code>,
     *                  the result of this method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext posesses the Role or GroupPrincipal, <code>false</code> otherwise
     */
    default boolean isUserInRole( final Session session, final Principal principal ) {
        if ( session == null || principal == null || AuthenticationManager.isUserPrincipal( principal ) ) {
            return false;
        }

        // Any type of user can possess a built-in role
        if ( principal instanceof Role && Role.isBuiltInRole( (Role)principal ) ) {
            return session.hasPrincipal( principal );
        }

        // Only authenticated users can possess groups or custom roles
        if ( session.isAuthenticated() && AuthenticationManager.isRolePrincipal( principal ) ) {
            return session.hasPrincipal( principal );
        }
        return false;
    }

    /**
     * Returns the current external {@link Authorizer} in use. This method is guaranteed to return a properly-initialized Authorizer, unless
     * it could not be initialized. In that case, this method throws a {@link org.apache.wiki.auth.WikiSecurityException}.
     *
     * @throws org.apache.wiki.auth.WikiSecurityException if the Authorizer could not be initialized
     * @return the current Authorizer
     */
    Authorizer getAuthorizer() throws WikiSecurityException;

    /**
     * <p>Determines if the Subject associated with a supplied Session contains a desired user Principal or built-in Role principal,
     * OR is a member a Group or external Role. The rules are as follows:</p>
     * <ol>
     * <li>First, if desired Principal is a Role or GroupPrincipal, delegate to {@link #isUserInRole(Session, Principal)} and
     * return the result.</li>
     * <li>Otherwise, we're looking for a user Principal, so iterate through the Principal set and see if any share the same name as the
     * one we are looking for.</li>
     * </ol>
     * <p><em>Note: if the Principal parameter is a user principal, the session must be authenticated in order for the user to "possess it".
     * Anonymous or asserted sessions will never posseess a named user principal.</em></p>
     *
     * @param session the current wiki session, which must be non-null. If null, the result of this method always returns <code>false</code>
     * @param principal the Principal (role, group, or user principal) to look for, which must be non-null. If null, the result of this
     *                  method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext posesses the Role, GroupPrincipal or desired
     *         user Principal, <code>false</code> otherwise
     */
    boolean hasRoleOrPrincipal( Session session, Principal principal );

    /**
     * Checks whether the current user has access to the wiki context, by obtaining the required Permission ({@link Context#requiredPermission()})
     * and delegating the access check to {@link #checkPermission(Session, Permission)}. If the user is allowed, this method returns
     * <code>true</code>; <code>false</code> otherwise. If access is allowed, the wiki context will be added to the request as an attribute
     * with the key name {@link org.apache.wiki.api.core.Context#ATTR_CONTEXT}. Note that this method will automatically redirect the user to
     * a login or error page, as appropriate, if access fails. This is NOT guaranteed to be default behavior in the future.
     *
     * @param context wiki context to check if it is accesible
     * @param response the http response
     * @return the result of the access check
     * @throws IOException In case something goes wrong
     */
    default boolean hasAccess( final Context context, final HttpServletResponse response ) throws IOException {
        return hasAccess( context, response, true );
    }

    /**
     * Checks whether the current user has access to the wiki context (and
     * optionally redirects if not), by obtaining the required Permission ({@link Context#requiredPermission()})
     * and delegating the access check to {@link #checkPermission(Session, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. Also, the wiki context will be added to the request as attribute
     * with the key name {@link org.apache.wiki.api.core.Context#ATTR_CONTEXT}.
     *
     * @param context wiki context to check if it is accesible
     * @param response The servlet response object
     * @param redirect If true, makes an automatic redirect to the response
     * @return the result of the access check
     * @throws IOException If something goes wrong
     */
    boolean hasAccess( final Context context, final HttpServletResponse response, final boolean redirect ) throws IOException;

    /**
     * Initializes AuthorizationManager with an engine and set of properties. Expects to find property 'jspwiki.authorizer' with a valid
     * Authorizer implementation name to take care of role lookup operations.
     *
     * @param engine the wiki engine
     * @param properties the set of properties used to initialize the wiki engine
     * @throws WikiException if the AuthorizationManager cannot be initialized
     */
    void initialize( final Engine engine, final Properties properties ) throws WikiException;

    /**
     * Checks to see if the local security policy allows a particular static Permission.
     * Do not use this method for normal permission checks; use {@link #checkPermission(Session, Permission)} instead.
     *
     * @param principals the Principals to check
     * @param permission the Permission
     * @return the result
     */
    boolean allowedByLocalPolicy( Principal[] principals, Permission permission );

    /**
     * Determines whether a Subject possesses a given "static" Permission as defined in the security policy file. This method uses standard
     * Java 2 security calls to do its work. Note that the current access control context's <code>codeBase</code> is effectively <em>this
     * class</em>, not that of the caller. Therefore, this method will work best when what matters in the policy is <em>who</em> makes the
     * permission check, not what the caller's code source is. Internally, this method works by executing <code>Subject.doAsPrivileged</code>
     * with a privileged action that simply calls {@link AccessController#checkPermission(Permission)}.
     *
     * @see AccessController#checkPermission(Permission) . A caught exception (or lack thereof) determines whether the
     *       privilege is absent (or present).
     * @param session the Session whose permission status is being queried
     * @param permission the Permission the Subject must possess
     * @return <code>true</code> if the Subject possesses the permission, <code>false</code> otherwise
     */
    boolean checkStaticPermission( Session session, Permission permission );

    /**
     * <p>Given a supplied string representing a Principal's name from an Acl, this method resolves the correct type of Principal (role,
     * group, or user). This method is guaranteed to always return a Principal. The algorithm is straightforward:</p>
     * <ol>
     * <li>If the name matches one of the built-in {@link org.apache.wiki.auth.authorize.Role} names, return that built-in Role</li>
     * <li>If the name matches one supplied by the current {@link org.apache.wiki.auth.Authorizer}, return that Role</li>
     * <li>If the name matches a group managed by the current {@link org.apache.wiki.auth.authorize.GroupManager}, return that Group</li>
     * <li>Otherwise, assume that the name represents a user principal. Using the current {@link org.apache.wiki.auth.user.UserDatabase},
     * find the first user who matches the supplied name by calling {@link org.apache.wiki.auth.user.UserDatabase#find(String)}.</li>
     * <li>Finally, if a user cannot be found, manufacture and return a generic {@link org.apache.wiki.auth.acl.UnresolvedPrincipal}</li>
     * </ol>
     *
     * @param name the name of the Principal to resolve
     * @return the fully-resolved Principal
     */
    Principal resolvePrincipal( final String name );


    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     */
    void addWikiEventListener( WikiEventListener listener );

    /**
     * Un-registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     */
    void removeWikiEventListener( final WikiEventListener listener );

    /**
     * Fires a WikiSecurityEvent of the provided type, user, and permission to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiSecurityEvent
     * @param type        the event type to be fired
     * @param user        the user associated with the event
     * @param permission  the permission the subject must possess
     */
    default void fireEvent( final int type, final Principal user, final Object permission ) {
        if( WikiEventManager.isListening( this ) ) {
            WikiEventManager.fireEvent( this, new WikiSecurityEvent( this, type, user, permission ) );
        }
    }

}
