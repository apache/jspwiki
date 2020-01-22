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


import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.ClassUtil;
import org.freshcookies.security.policy.LocalPolicy;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.WeakHashMap;

/**
 * <p>Manages all access control and authorization; determines what authenticated
 * users are allowed to do.</p>
 * <p>Privileges in JSPWiki are expressed as Java-standard {@link java.security.Permission}
 * classes. There are two types of permissions:</p>
 * <ul>
 *   <li>{@link org.apache.wiki.auth.permissions.WikiPermission} - privileges that apply
 *   to an entire wiki instance: <em>e.g.,</em> editing user profiles, creating pages, creating groups</li>
 *   <li>{@link org.apache.wiki.auth.permissions.PagePermission} - privileges that apply
 *   to a single wiki page or range of pages: <em>e.g.,</em> reading, editing, renaming
 * </ul>
 * <p>Calling classes determine whether they are entitled to perform a particular action
 * by constructing the appropriate permission first, then passing it and the current
 * {@link org.apache.wiki.WikiSession} to the
 * {@link #checkPermission(WikiSession, Permission)} method. If the session's
 * Subject possesses the permission, the action is allowed.</p>
 * <p>For WikiPermissions, the decision criteria is relatively simple: the caller either
 * possesses the permission, as granted by the wiki security policy -- or not.</p>
 * <p>For PagePermissions, the logic is exactly the same if the page being checked
 * does not have an access control list. However, if the page does have an ACL, the
 * authorization decision is made based the <em>union</em> of the permissions
 * granted in the ACL and in the security policy. In other words, the user must
 * be named in the ACL (or belong to a group or role that is named in the ACL)
 * <em>and</em> be granted (at least) the same permission in the security policy. We
 * do this to prevent a user from gaining more permissions than they already
 * have, based on the security policy.</p>
 * <p>See the {@link #checkPermission(WikiSession, Permission)} and
 * {@link #hasRoleOrPrincipal(WikiSession, Principal)} methods for more information
 * on the authorization logic.</p>
 * @since 2.3
 * @see AuthenticationManager
 */
public class AuthorizationManager {

    private static final Logger log = Logger.getLogger( AuthorizationManager.class );
    /**
     * The default external Authorizer is the {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer}
     */
    public static final String                DEFAULT_AUTHORIZER = "org.apache.wiki.auth.authorize.WebContainerAuthorizer";

    /** Property that supplies the security policy file name, in WEB-INF. */
    protected static final String             POLICY      = "jspwiki.policy.file";

    /** Name of the default security policy file, in WEB-INF. */
    protected static final String             DEFAULT_POLICY      = "jspwiki.policy";

    /**
     * The property name in jspwiki.properties for specifying the external {@link Authorizer}.
     */
    public static final String                PROP_AUTHORIZER   = "jspwiki.authorizer";

    private Authorizer                        m_authorizer      = null;

    /** Cache for storing ProtectionDomains used to evaluate the local policy. */
    private Map<Principal, ProtectionDomain>                               m_cachedPds       = new WeakHashMap<Principal, ProtectionDomain>();

    private WikiEngine                        m_engine          = null;

    private LocalPolicy                       m_localPolicy     = null;

    /**
     * Constructs a new AuthorizationManager instance.
     */
    public AuthorizationManager()
    {
    }

    /**
     * Returns <code>true</code> or <code>false</code>, depending on
     * whether a Permission is allowed for the Subject associated with
     * a supplied WikiSession. The access control algorithm works this way:
     * <ol>
     * <li>The {@link org.apache.wiki.auth.acl.Acl} for the page is obtained</li>
     * <li>The Subject associated with the current
     * {@link org.apache.wiki.WikiSession} is obtained</li>
     * <li>If the Subject's Principal set includes the Role Principal that is
     * the administrator group, always allow the Permission</li>
     * <li>For all permissions, check to see if the Permission is allowed according
     * to the default security policy. If it isn't, deny the permission and halt
     * further processing.</li>
     * <li>If there is an Acl, get the list of Principals assigned this
     * Permission in the Acl: these will be role, group or user Principals, or
     * {@link org.apache.wiki.auth.acl.UnresolvedPrincipal}s (see below).
     * Then iterate through the Subject's Principal set and determine whether
     * the user (Subject) possesses any one of these specified Roles or
     * Principals. The matching process delegates to
     * {@link #hasRoleOrPrincipal(WikiSession, Principal)}.
     * </ol>
     * <p>
     * Note that when iterating through the Acl's list of authorized Principals,
     * it is possible that one or more of the Acl's Principal entries are of
     * type <code>UnresolvedPrincipal</code>. This means that the last time
     * the ACL was read, the Principal (user, built-in Role, authorizer Role, or
     * wiki Group) could not be resolved: the Role was not valid, the user
     * wasn't found in the UserDatabase, or the Group wasn't known to (e.g.,
     * cached) in the GroupManager. If an <code>UnresolvedPrincipal</code> is
     * encountered, this method will attempt to resolve it first <em>before</em>
     * checking to see if the Subject possesses this principal, by calling
     * {@link #resolvePrincipal(String)}. If the (re-)resolution does not
     * succeed, the access check for the principal will fail by definition (the
     * Subject should never contain UnresolvedPrincipals).
     * </p>
     * <p>
     * If security not set to JAAS, will return true.
     * </p>
     * @param session the current wiki session
     * @param permission the Permission being checked
     * @see #hasRoleOrPrincipal(WikiSession, Principal)
     * @return the result of the Permission check
     */
    public boolean checkPermission( WikiSession session, Permission permission )
    {
        //
        //  A slight sanity check.
        //
        if ( session == null || permission == null )
        {
            fireEvent( WikiSecurityEvent.ACCESS_DENIED, null, permission );
            return false;
        }

        Principal user = session.getLoginPrincipal();

        // Always allow the action if user has AllPermission
        Permission allPermission = new AllPermission( m_engine.getApplicationName() );
        boolean hasAllPermission = checkStaticPermission( session, allPermission );
        if ( hasAllPermission )
        {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        // If the user doesn't have *at least* the permission
        // granted by policy, return false.
        boolean hasPolicyPermission = checkStaticPermission( session, permission );
        if ( !hasPolicyPermission )
        {
            fireEvent( WikiSecurityEvent.ACCESS_DENIED, user, permission );
            return false;
        }

        // If this isn't a PagePermission, it's allowed
        if ( ! ( permission instanceof PagePermission ) )
        {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        //
        // If the page or ACL is null, it's allowed.
        //
        String pageName = ((PagePermission)permission).getPage();
        WikiPage page = m_engine.getPageManager().getPage( pageName );
        Acl acl = ( page == null) ? null : m_engine.getAclManager().getPermissions( page );
        if ( page == null ||  acl == null || acl.isEmpty() )
        {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        //
        //  Next, iterate through the Principal objects assigned
        //  this permission. If the context's subject possesses
        //  any of these, the action is allowed.

        Principal[] aclPrincipals = acl.findPrincipals( permission );

        log.debug( "Checking ACL entries..." );
        log.debug( "Acl for this page is: " + acl );
        log.debug( "Checking for principal: " + Arrays.toString( aclPrincipals ) );
        log.debug( "Permission: " + permission );

        for( Principal aclPrincipal : aclPrincipals )
        {
            // If the ACL principal we're looking at is unresolved,
            // try to resolve it here & correct the Acl
            if ( aclPrincipal instanceof UnresolvedPrincipal )
            {
                AclEntry aclEntry = acl.getEntry( aclPrincipal );
                aclPrincipal = resolvePrincipal( aclPrincipal.getName() );
                if ( aclEntry != null && !( aclPrincipal instanceof UnresolvedPrincipal ) )
                {
                    aclEntry.setPrincipal( aclPrincipal );
                }
            }

            if ( hasRoleOrPrincipal( session, aclPrincipal ) )
            {
                fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
                return true;
            }
        }
        fireEvent( WikiSecurityEvent.ACCESS_DENIED, user, permission );
        return false;
    }

    /**
     * <p>Determines if the Subject associated with a
     * supplied WikiSession contains a desired Role or GroupPrincipal.
     * The algorithm simply checks to see if the Subject possesses
     * the Role or GroupPrincipal it in its Principal set. Note that
     * any user (anonymous, asserted, authenticated) can possess
     * a built-in role. But a user <em>must</em> be authenticated to
     * possess a role other than one of the built-in ones.
     * We do this to prevent privilege escalation.</p>
     * <p>For all other cases, this method returns <code>false</code>.</p>
     * <p>Note that this method does <em>not</em> consult the external
     * Authorizer or GroupManager; it relies on the Principals that
     * have been injected into the user's Subject at login time, or
     * after group creation/modification/deletion.</p>
     * @param session the current wiki session, which must be non-null. If null,
     *            the result of this method always returns <code>false</code>
     * @param principal the Principal (role or group principal) to look
     *            for, which must be non-<code>null</code>. If <code>null</code>,
     *            the result of this method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role or GroupPrincipal, <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiSession session, Principal principal )
    {
        if ( session == null || principal == null ||
             AuthenticationManager.isUserPrincipal( principal ) )
        {
            return false;
        }

        // Any type of user can possess a built-in role
        if ( principal instanceof Role && Role.isBuiltInRole( (Role)principal ) )
        {
            return session.hasPrincipal( principal );
        }

        // Only authenticated users can possess groups or custom roles
        if ( session.isAuthenticated() && AuthenticationManager.isRolePrincipal( principal ) )
        {
            return session.hasPrincipal( principal );
        }
        return false;
    }

    /**
     * Returns the current external {@link Authorizer} in use. This method
     * is guaranteed to return a properly-initialized Authorizer, unless
     * it could not be initialized. In that case, this method throws
     * a {@link org.apache.wiki.auth.WikiSecurityException}.
     * @throws org.apache.wiki.auth.WikiSecurityException if the Authorizer could
     * not be initialized
     * @return the current Authorizer
     */
    public Authorizer getAuthorizer() throws WikiSecurityException
    {
        if ( m_authorizer != null )
        {
            return m_authorizer;
        }
        throw new WikiSecurityException( "Authorizer did not initialize properly. Check the logs." );
    }

    /**
     * <p>Determines if the Subject associated with a supplied WikiSession contains
     * a desired user Principal or built-in Role principal, OR is a member a
     * Group or external Role. The rules are as follows:</p>
     * <ol>
     * <li>First, if desired Principal is a Role or GroupPrincipal, delegate to
     * {@link #isUserInRole(WikiSession, Principal)} and
     * return the result.</li>
     * <li>Otherwise, we're looking for a user Principal,
     * so iterate through the Principal set and see if
     * any share the same name as the one we are looking for.</li>
     * </ol>
     * <p><em>Note: if the Principal parameter is a user principal, the session
     * must be authenticated in order for the user to "possess it". Anonymous
     * or asserted sessions will never posseess a named user principal.</em></p>
     * @param session the current wiki session, which must be non-null. If null,
     *            the result of this method always returns <code>false</code>
     * @param principal the Principal (role, group, or user principal) to look
     *            for, which must be non-null. If null, the result of this
     *            method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role, GroupPrincipal or desired
     *         user Principal, <code>false</code> otherwise
     */
    protected boolean hasRoleOrPrincipal( WikiSession session, Principal principal )
    {
        // If either parameter is null, always deny
        if( session == null || principal == null )
        {
            return false;
        }

        // If principal is role, delegate to isUserInRole
        if( AuthenticationManager.isRolePrincipal( principal ) )
        {
            return isUserInRole( session, principal );
        }

        // We must be looking for a user principal, assuming that the user
        // has been properly logged in.
        // So just look for a name match.
        if( session.isAuthenticated() && AuthenticationManager.isUserPrincipal( principal ) )
        {
            String principalName = principal.getName();
            Principal[] userPrincipals = session.getPrincipals();
            for( Principal userPrincipal : userPrincipals )
            {
                if( userPrincipal.getName().equals( principalName ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the current user has access to the wiki context,
     * by obtaining the required Permission ({@link WikiContext#requiredPermission()})
     * and delegating the access check to {@link #checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as an attribute
     * with the key name {@link org.apache.wiki.WikiContext#ATTR_CONTEXT}.
     * Note that this method will automatically redirect the user to
     * a login or error page, as appropriate, if access fails. This is
     * NOT guaranteed to be default behavior in the future.
     *
     * @param context wiki context to check if it is accesible
     * @param response the http response
     * @return the result of the access check
     * @throws IOException In case something goes wrong
     */
    public boolean hasAccess( WikiContext context, HttpServletResponse response ) throws IOException
    {
        return hasAccess( context, response, true );
    }

    /**
     * Checks whether the current user has access to the wiki context (and
     * optionally redirects if not), by obtaining the required Permission ({@link WikiContext#requiredPermission()})
     * and delegating the access check to {@link #checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. Also, the wiki context will be added to the request as attribute
     * with the key name {@link org.apache.wiki.WikiContext#ATTR_CONTEXT}.
     *
     * @param context wiki context to check if it is accesible
     * @param response The servlet response object
     * @param redirect If true, makes an automatic redirect to the response
     * @return the result of the access check
     * @throws IOException If something goes wrong
     */
    public boolean hasAccess( final WikiContext context, final HttpServletResponse response, final boolean redirect ) throws IOException {
        final boolean allowed = checkPermission( context.getWikiSession(), context.requiredPermission() );
        final ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE );

        // Stash the wiki context
        if ( context.getHttpRequest() != null && context.getHttpRequest().getAttribute( WikiContext.ATTR_CONTEXT ) == null ) {
            context.getHttpRequest().setAttribute( WikiContext.ATTR_CONTEXT, context );
        }

        // If access not allowed, redirect
        if( !allowed && redirect ) {
            final Principal currentUser  = context.getWikiSession().getUserPrincipal();
            final String pageurl = context.getPage().getName();
            if( context.getWikiSession().isAuthenticated() ) {
                log.info("User "+currentUser.getName()+" has no access - forbidden (permission=" + context.requiredPermission() + ")" );
                context.getWikiSession().addMessage( MessageFormat.format( rb.getString( "security.error.noaccess.logged" ),
                                                     context.getName()) );
            } else {
                log.info("User "+currentUser.getName()+" has no access - redirecting (permission=" + context.requiredPermission() + ")");
                context.getWikiSession().addMessage( MessageFormat.format( rb.getString("security.error.noaccess"), context.getName() ) );
            }
            response.sendRedirect( m_engine.getURL(WikiContext.LOGIN, pageurl, null ) );
        }
        return allowed;
    }

    /**
     * Initializes AuthorizationManager with an engine and set of properties.
     * Expects to find property 'jspwiki.authorizer' with a valid Authorizer
     * implementation name to take care of role lookup operations.
     * @param engine the wiki engine
     * @param properties the set of properties used to initialize the wiki engine
     * @throws WikiException if the AuthorizationManager cannot be initialized
     */
    public void initialize( final WikiEngine engine, final Properties properties ) throws WikiException {
        m_engine = engine;

        //
        //  JAAS authorization continues
        //
        m_authorizer = getAuthorizerImplementation( properties );
        m_authorizer.initialize( engine, properties );

        // Initialize local security policy
        try {
            final String policyFileName = properties.getProperty( POLICY, DEFAULT_POLICY );
            final URL policyURL = AuthenticationManager.findConfigFile( engine, policyFileName );

            if (policyURL != null) {
                final File policyFile = new File( policyURL.toURI().getPath() );
                log.info("We found security policy URL: " + policyURL + " and transformed it to file " + policyFile.getAbsolutePath());
                m_localPolicy = new LocalPolicy( policyFile, engine.getContentEncoding().displayName() );
                m_localPolicy.refresh();
                log.info( "Initialized default security policy: " + policyFile.getAbsolutePath() );
            } else {
                final String sb = "JSPWiki was unable to initialize the default security policy (WEB-INF/jspwiki.policy) file. " +
                                  "Please ensure that the jspwiki.policy file exists in the default location. " +
                		          "This file should exist regardless of the existance of a global policy file. " +
                                  "The global policy file is identified by the java.security.policy variable. ";
                final WikiSecurityException wse = new WikiSecurityException( sb );
                log.fatal( sb, wse );
                throw wse;
            }
        } catch ( final Exception e) {
            log.error("Could not initialize local security policy: " + e.getMessage() );
            throw new WikiException( "Could not initialize local security policy: " + e.getMessage(), e );
        }
    }

    /**
     * Attempts to locate and initialize a Authorizer to use with this manager.
     * Throws a WikiException if no entry is found, or if one fails to
     * initialize.
     * @param props jspwiki.properties, containing a
     *            'jspwiki.authorization.provider' class name
     * @return a Authorizer used to get page authorization information
     * @throws WikiException
     */
    private Authorizer getAuthorizerImplementation( Properties props ) throws WikiException {
        final String authClassName = props.getProperty( PROP_AUTHORIZER, DEFAULT_AUTHORIZER );
        return ( Authorizer )locateImplementation( authClassName );
    }

    private Object locateImplementation( final String clazz ) throws WikiException {
        if ( clazz != null ) {
            try {
                Class< ? > authClass = ClassUtil.findClass( "org.apache.wiki.auth.authorize", clazz );
                Object impl = authClass.newInstance();
                return impl;
            } catch( ClassNotFoundException e ) {
                log.fatal( "Authorizer " + clazz + " cannot be found", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be found", e );
            } catch( InstantiationException e ) {
                log.fatal( "Authorizer " + clazz + " cannot be created", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be created", e );
            } catch( IllegalAccessException e ) {
                log.fatal( "You are not allowed to access this authorizer class", e );
                throw new WikiException( "You are not allowed to access this authorizer class", e );
            }
        }

        throw new NoRequiredPropertyException( "Unable to find a " + PROP_AUTHORIZER + " entry in the properties.",
                                               PROP_AUTHORIZER );
    }

    /**
     * Checks to see if the local security policy allows a particular static Permission.
     * Do not use this method for normal permission checks; use
     * {@link #checkPermission(WikiSession, Permission)} instead.
     * @param principals the Principals to check
     * @param permission the Permission
     * @return the result
     */
    protected boolean allowedByLocalPolicy( Principal[] principals, Permission permission )
    {
        for ( Principal principal : principals )
        {
            // Get ProtectionDomain for this Principal from cache, or create new one
            ProtectionDomain pd = m_cachedPds.get( principal );
            if ( pd == null )
            {
                ClassLoader cl = this.getClass().getClassLoader();
                CodeSource cs = new CodeSource( null, (Certificate[])null );
                pd = new ProtectionDomain( cs, null, cl, new Principal[]{ principal } );
                m_cachedPds.put( principal, pd );
            }

            // Consult the local policy and get the answer
            if ( m_localPolicy.implies( pd, permission ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a Subject possesses a given "static" Permission as
     * defined in the security policy file. This method uses standard Java 2
     * security calls to do its work. Note that the current access control
     * context's <code>codeBase</code> is effectively <em>this class</em>,
     * not that of the caller. Therefore, this method will work best when what
     * matters in the policy is <em>who</em> makes the permission check, not
     * what the caller's code source is. Internally, this method works by
     * executing <code>Subject.doAsPrivileged</code> with a privileged action
     * that simply calls {@link java.security.AccessController#checkPermission(Permission)}.
     * @see AccessController#checkPermission(java.security.Permission) . A
     *       caught exception (or lack thereof) determines whether the privilege
     *       is absent (or present).
     * @param session the WikiSession whose permission status is being queried
     * @param permission the Permission the Subject must possess
     * @return <code>true</code> if the Subject possesses the permission,
     *         <code>false</code> otherwise
     */
    protected boolean checkStaticPermission( final WikiSession session, final Permission permission )
    {
        Boolean allowed = (Boolean) WikiSession.doPrivileged( session, new PrivilegedAction<Boolean>()
        {
            public Boolean run()
            {
                try
                {
                    // Check the JVM-wide security policy first
                    AccessController.checkPermission( permission );
                    return Boolean.TRUE;
                }
                catch( AccessControlException e )
                {
                    // Global policy denied the permission
                }

                // Try the local policy - check each Role/Group and User Principal
                if ( allowedByLocalPolicy( session.getRoles(), permission ) ||
                     allowedByLocalPolicy( session.getPrincipals(), permission ) )
                {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        } );
        return allowed.booleanValue();
    }

    /**
     * <p>Given a supplied string representing a Principal's name from an Acl, this
     * method resolves the correct type of Principal (role, group, or user).
     * This method is guaranteed to always return a Principal.
     * The algorithm is straightforward:</p>
     * <ol>
     * <li>If the name matches one of the built-in {@link org.apache.wiki.auth.authorize.Role} names,
     * return that built-in Role</li>
     * <li>If the name matches one supplied by the current
     * {@link org.apache.wiki.auth.Authorizer}, return that Role</li>
     * <li>If the name matches a group managed by the
     * current {@link org.apache.wiki.auth.authorize.GroupManager}, return that Group</li>
     * <li>Otherwise, assume that the name represents a user
     * principal. Using the current {@link org.apache.wiki.auth.user.UserDatabase}, find the
     * first user who matches the supplied name by calling
     * {@link org.apache.wiki.auth.user.UserDatabase#find(String)}.</li>
     * <li>Finally, if a user cannot be found, manufacture
     * and return a generic {@link org.apache.wiki.auth.acl.UnresolvedPrincipal}</li>
     * </ol>
     * @param name the name of the Principal to resolve
     * @return the fully-resolved Principal
     */
    public Principal resolvePrincipal( String name )
    {
        // Check built-in Roles first
        Role role = new Role(name);
        if ( Role.isBuiltInRole( role ) )
        {
            return role;
        }

        // Check Authorizer Roles
        Principal principal = m_authorizer.findRole( name );
        if ( principal != null )
        {
            return principal;
        }

        // Check Groups
        principal = m_engine.getGroupManager().findRole( name );
        if ( principal != null )
        {
            return principal;
        }

        // Ok, no luck---this must be a user principal
        Principal[] principals = null;
        UserProfile profile = null;
        UserDatabase db = m_engine.getUserManager().getUserDatabase();
        try
        {
            profile = db.find( name );
            principals = db.getPrincipals( profile.getLoginName() );
            for (int i = 0; i < principals.length; i++)
            {
                principal = principals[i];
                if ( principal.getName().equals( name ) )
                {
                    return principal;
                }
            }
        }
        catch( NoSuchPrincipalException e )
        {
            // We couldn't find the user...
        }
        // Ok, no luck---mark this as unresolved and move on
        return new UnresolvedPrincipal( name );
    }


    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     *  Fires a WikiSecurityEvent of the provided type, user,
     *  and permission to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiSecurityEvent
     * @param type        the event type to be fired
     * @param user        the user associated with the event
     * @param permission  the permission the subject must possess
     */
    protected void fireEvent( int type, Principal user, Object permission )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiSecurityEvent(this,type,user,permission));
        }
    }

}
