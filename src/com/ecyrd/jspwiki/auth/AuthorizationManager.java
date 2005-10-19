/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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


import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.acl.UnresolvedPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 * Manages all access control and authorization; determines what authenticated
 * users are allowed to do.
 * @author Andrew Jaquith
 * @version $Revision: 1.29 $ $Date: 2005-10-19 04:10:24 $
 * @since 2.3
 * @see AuthenticationManager
 */
public class AuthorizationManager
{
    static Logger log = Logger.getLogger( AuthorizationManager.class );
    /**
     * The default external Authorizer is the {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer}
     */
    public static final String                DEFAULT_AUTHORIZER = "com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer";

    /**
     * The property name in jspwiki.properties for specifying the external {@link Authorizer}.
     */
    public static final String                PROP_AUTHORIZER   = "jspwiki.authorizer";

    private Authorizer                        m_authorizer      = null;

    private WikiEngine                        m_engine          = null;

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
     * <li>The {@link com.ecyrd.jspwiki.auth.acl.Acl} for the page is obtained</li>
     * <li>The Subject associated with the current
     * {@link com.ecyrd.jspwiki.WikiSession} is obtained</li>
     * <li>If the Subject's Principal set includes the Role Principal that is
     * the administrator group, always allow the Permission</li>
     * <li>For all permissions, check to see if the Permission is allowed according
     * to the default security policy. If it isn't, deny the permission and halt 
     * further processing.</li>
     * <li>If there is an Acl, get the list of Principals assigned this
     * Permission in the Acl: these will be role, group or user Principals, or
     * {@link com.ecyrd.jspwiki.auth.acl.UnresolvedPrincipal}s (see below).
     * Then iterate through the Subject's Principal set and determine whether
     * the user (Subject) posesses any one of these specified Roles or
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
            return false;
        }
        Subject subject = session.getSubject();
        
        // Always allow the action if it's the Admin
        if ( subject.getPrincipals().contains( Role.ADMIN ) )
        {
            return true;
        }
        
        // If the user doesn't have *at least* the permission
        // granted by policy, return false.
        boolean hasPolicyPermission = checkStaticPermission( subject, permission );
        if ( !hasPolicyPermission )
        {
            return false;
        }

        // If this isn't a PagePermission, it's allowed
        if ( ! ( permission instanceof PagePermission ) )
        {
            return true;
        }

        //
        // If the page or ACL is null, it's allowed.
        String pageName = ((PagePermission)permission).getPage();
        WikiPage page = m_engine.getPage( pageName );
        Acl acl = ( page == null) ? null : m_engine.getAclManager().getPermissions( page );
        if ( page == null ||  acl == null )
        {
            return true;
        }
        
        //
        //  Next, iterate through the Principal objects assigned
        //  this permission. If the context's subject possesses
        //  any of these, the action is allowed.

        Principal[] aclPrincipals = acl.findPrincipals( permission );

        log.debug( "Checking ACL entries..." );
        log.debug( "Acl for this page is: " + acl );
        log.debug( "Checking for principal: " + aclPrincipals );
        log.debug( "Permission: " + permission );

        for( int i = 0; i < aclPrincipals.length; i++ )
        {
            Principal aclPrincipal = aclPrincipals[i];
            
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
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns an array of Principal objects that represents the roles that
     * the user associated with a WikiSession possesses. The array is built
     * by iterating through the Subject's Principal set and extracting
     * all Role objects into a list. The external Authorizer and GroupManager 
     * are also consulted; in each case, the Principal[] array returned by 
     * <code>getRoles()</code> is examined. If the Subject posseses this
     * role, it is added to the list. The list is returned as an array
     * sorted in the natural order implied by each Principal's 
     * <code>getName</code> method.
     * @param session the wiki session
     * @return an array of Principal objects corresponding to the 
     * roles the Subject possesses, across all Authorizers
     */
    public Principal[] getRoles( WikiSession session )
    {
        Set roles = new HashSet();
        Subject subject = session.getSubject();
        
        // Add all of the Roles possessed by the Subject directly
        roles.addAll( subject.getPrincipals( Role.class ) );
        
        // Get the GroupManager and test for each Group
        GroupManager manager = m_engine.getGroupManager();
        Principal[] groups = manager.getRoles();
        for ( int i = 0; i < groups.length; i++ )
        {
            if ( manager.isUserInRole( session, groups[i] ) )
            {
                roles.add( groups[i] );
            }
        }
        
        // Get the external Authorizer and test for each Role
        Principal[] externalRoles = m_authorizer.getRoles();
        for ( int i = 0; i < externalRoles.length; i++ )
        {
            if ( m_authorizer.isUserInRole( session, externalRoles[i] ) )
            {
                roles.add( externalRoles[i] );
            }
        }
        
        // Return a defensive copy
        Principal[] roleArray = ( Principal[] )roles.toArray( new Principal[roles.size()] );
        Arrays.sort( roleArray, WikiPrincipal.COMPARATOR );
        return roleArray;
    }
    
    /**
     * Wrapper method that determines if the Subject associated with a 
     * supplied WikiSession contains a desired built-in Role principal, 
     * OR is a member a Group or external Role. The rules as as follows:
     * <ul>
     * <li>If the desired Principal is a built-in Role, the algorithm simply
     * checks to see if the Subject possesses it in its Principal set</li>
     * <li>If the desired Principal is a Role but <em>not</em> built-in, the
     * external Authorizer's <code>isInRole</code> method is called</li>
     * <li>If the desired principal is a Group, the GroupManager's group
     * authorizer <code>isInRole</code> method is called</li>
     * </ul>
     * @param session the current wiki session, which must be non-null. If null,
     *            the result of this method always returns <code>false</code>
     * @param principal the Principal (role or group principal) to look
     *            for, which must be non-null. If null, the result of this
     *            method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role or is a member of the Group,
     *         <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiSession session, Principal principal )
    // TODO: write unit tests
    {
        if (session == null || principal == null)
        {
            return false;
        }
        Subject subject = session.getSubject();
        if ( principal instanceof Role)
        {
            Role role = (Role) principal;
            // If built-in role, check to see if user possesses it.
            if ( Role.isBuiltInRole( role ) && subject.getPrincipals().contains( role ) )
            {
                return true;
            }
            // No luck; try the external authorizer (e.g., container)
            return (m_authorizer.isUserInRole( session, role ) );
        }
        else if ( principal instanceof Group )
        {
            Group group = (Group) principal;
            return m_engine.getGroupManager().isUserInRole( session, group );
        }
        return false;
    }
    
    /**
     * Determines if the Subject associated with a supplied WikiSession contains
     * a desired user Principal or built-in Role principal, OR is a member a
     * Group or external Role. The rules as as follows:
     * <ul>
     * <li>Delegate the initial check to {@link #isUserInRole(WikiSession, Principal)}.</li>
     * <li>If this check fails, delegate to
     * {@link #hasUserPrincipal(Subject, Principal)} to determine whether the
     * Subject posesses the desired Principal in its Principal set.</li>
     * </ul>
     * @param session the current wiki session, which must be non-null. If null,
     *            the result of this method always returns <code>false</code>
     * @param principal the Principal (role, group, or user principal) to look
     *            for, which must be non-null. If null, the result of this
     *            method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role, is a member of the Group, or contains the
     *         desired Principal, <code>false</code> otherwise
     */
    protected boolean hasRoleOrPrincipal( WikiSession session, Principal principal )
    {
        if (session == null || principal == null)
        {
            return false;
        }
        if ( principal instanceof Role || principal instanceof Group )
        {
            return isUserInRole( session, principal );
        }
        Subject subject = session.getSubject();
        return hasUserPrincipal( subject, principal );
    }

    /**
     * Determines whether any of the user Principals posessed by a Subject have
     * the same name as a supplied Principal. Principals in the subject's
     * principal set that are of types Role or Group are <em>not</em>
     * considered in the comparison, since this would otherwise introduce the
     * potential for spoofing.
     * @param subject the Subject whose Principal set will be inspected
     * @param principal the desired Principal
     * @return <code>true</code> if any of the Subject's Principals have the
     *         same name as the supplied Principal, otherwise <code>false</code>
     */
    protected boolean hasUserPrincipal( Subject subject, Principal principal )
    {
        String principalName = principal.getName();
        for( Iterator it = subject.getPrincipals().iterator(); it.hasNext(); )
        {
            Principal userPrincipal = (Principal) it.next();
            if ( !( userPrincipal instanceof Role || userPrincipal instanceof Group ) )
            {
                if ( userPrincipal.getName().equals( principalName ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initializes AuthorizationManager with an engine and set of properties.
     * Expects to find property 'jspwiki.authorizer' with a valid Authorizer
     * implementation name to take care of group lookup operations.
     */
    public void initialize( WikiEngine engine, Properties properties ) throws WikiException
    {
        m_engine = engine;
        m_authorizer = getAuthorizerImplementation( properties );
        m_authorizer.initialize( engine, properties );
    }

    /**
     * Returns the current external {@link Authorizer} in use, which may be 
     * <code>null<code>.
     * @return the current Authorizer
     */
    protected Authorizer getAuthorizer()
    {
        return m_authorizer;
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
    private Authorizer getAuthorizerImplementation( Properties props ) throws WikiException
    {
        String authClassName = props.getProperty( PROP_AUTHORIZER, DEFAULT_AUTHORIZER );
        return (Authorizer) locateImplementation( authClassName );
    }

    private Object locateImplementation( String clazz ) throws WikiException
    {
        if ( clazz != null )
        {
            try
            {
                Class authClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.authorize", clazz );
                Object impl = authClass.newInstance();
                return impl;
            }
            catch( ClassNotFoundException e )
            {
                log.fatal( "Authorizer " + clazz + " cannot be found", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be found" );
            }
            catch( InstantiationException e )
            {
                log.fatal( "Authorizer " + clazz + " cannot be created", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be created" );
            }
            catch( IllegalAccessException e )
            {
                log.fatal( "You are not allowed to access this authorizer class", e );
                throw new WikiException( "You are not allowed to access this authorizer class" );
            }
        }

        throw new NoRequiredPropertyException( "Unable to find a " + PROP_AUTHORIZER + " entry in the properties.",
                                               PROP_AUTHORIZER );
    }

    /**
     * Determines whether a Subject posesses a given "static" Permission as
     * defined in the security policy file. This method uses standard Java 2
     * security calls to do its work. Note that the current access control
     * context's <code>codeBase</code> is effectively <em>this class</em>,
     * not that of the caller. Therefore, this method will work best when what
     * matters in the policy is <em>who</em> makes the permission check, not
     * what the caller's code source is. Internally, this method works by
     * excuting <code>Subject.doAsPrivileged</code> with a privileged action
     * that simply calls {@link java.security.AccessController#checkPermission(Permission)}.
     * @link AccessController#checkPermission(java.security.Permission). A
     *       caught exception (or lack thereof) determines whether the privilege
     *       is absent (or present).
     * @param subject the Subject whose permission status is being queried
     * @param permission the Permission the Subject must possess
     * @return <code>true</code> if the Subject posesses the permission,
     *         <code>false</code> otherwise
     */
    protected final boolean checkStaticPermission( final Subject subject, final Permission permission )
    {
        try
        {
            Subject.doAsPrivileged( subject, new PrivilegedAction()
            {
                public Object run()
                {
                    AccessController.checkPermission( permission );
                    return null;
                }
            }, null );
            return true;
        }
        catch( AccessControlException e )
        {
            return false;
        }
    }
    
    /**
     * <p>Given a supplied string representing a Principal's name from an Acl, this
     * method resolves the correct type of Principal (role, group, or user).
     * This method is guaranteed to always return a Principal.
     * The algorithm is straightforward:</p>
     * <ol>
     * <li>If the name matches one of the built-in {@link com.ecyrd.jspwiki.auth.authorize.Role} names,
     * return that built-in Role</li>
     * <li>If the name matches one supplied by the current
     * {@link com.ecyrd.jspwiki.auth.Authorizer}, return that Role</li>
     * <li>If the name matches a group managed by the 
     * current {@link com.ecyrd.jspwiki.auth.authorize.GroupManager}, return that Group</li>
     * <li>Otherwise, assume that the name represents a user
     * principal. Using the current {@link com.ecyrd.jspwiki.auth.user.UserDatabase}, find the
     * first user who matches the supplied name by calling
     * {@link com.ecyrd.jspwiki.auth.user.UserDatabase#find(String)}.</li>
     * <li>Finally, if a user cannot be found, manufacture
     * and return a generic {@link com.ecyrd.jspwiki.auth.acl.UnresolvedPrincipal}</li>
     * </ol>
     * @param name the name of the Principal to resolve
     * @return the fully-resolved Principal
     */
    public Principal resolvePrincipal( String name ) 
    {    
        // Check built-in Roles first
        Role role = new Role(name);
        if (Role.isBuiltInRole(role)) {
            return role;
        }
        
        // Check Authorizer Roles
        Principal principal = m_authorizer.findRole( name );
        if (principal != null) 
        {
            return principal;
        }
        
        // Check Groups
        principal = m_engine.getGroupManager().findRole( name );
        if (principal != null)
        {
            return principal;
        }
        
        // Ok, no luck---this must be a user principal
        Principal[] principals = null;
        UserProfile profile = null;
        UserDatabase db = m_engine.getUserDatabase();
        try
        {
            profile = db.find( name );
            principals = db.getPrincipals( profile.getLoginName() );
            for (int i = 0; i < principals.length; i++) 
            {
                principal = principals[i];
                if (principal.getName().equals( name ))
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

}