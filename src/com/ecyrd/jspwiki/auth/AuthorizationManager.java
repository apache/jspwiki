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
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.acl.UnresolvedPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.event.EventSourceDelegate;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventSource;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 * <p>Manages all access control and authorization; determines what authenticated
 * users are allowed to do.</p>
 * <p>Privileges in JSPWiki are expressed as Java-standard {@link java.security.Permission}
 * classes. There are two types of permissions:</p>
 * <ul>
 *   <li>{@link com.ecyrd.jspwiki.auth.permissions.WikiPermission} - privileges that apply
 *   to an entire wiki instance: <em>e.g.,</em> editing user profiles, creating pages, creating groups</li>
 *   <li>{@link com.ecyrd.jspwiki.auth.permissions.PagePermission} - privileges that apply
 *   to a single wiki page or range of pages: <em>e.g.,</em> reading, editing, renaming
 * </ul>
 * <p>Calling classes determine whether they are entitled to perform a particular action
 * by constructing the appropriate permission first, then passing it and the current
 * {@link com.ecyrd.jspwiki.WikiSession} to the 
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
 * @author Andrew Jaquith
 * @version $Revision: 1.40 $ $Date: 2006-08-01 11:20:20 $
 * @since 2.3
 * @see AuthenticationManager
 */
public final class AuthorizationManager implements WikiEventSource
{
    private static final Logger log = Logger.getLogger( AuthorizationManager.class );
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

    /** Delegate for managing event listeners */
    private EventSourceDelegate               m_listeners       = new EventSourceDelegate();
    
    private boolean                           m_useJAAS         = true;
    
    /**
     * Constructs a new AuthorizationManager instance.
     */
    public AuthorizationManager()
    {
    }
    
    /**
     * @see com.ecyrd.jspwiki.event.WikiEventSource#addWikiEventListener(WikiEventListener)
     */
    public final void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.addWikiEventListener( listener );
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
     * <p>
     * If security not set to JAAS, will return true.
     * </p>
     * @param session the current wiki session
     * @param permission the Permission being checked
     * @see #hasRoleOrPrincipal(WikiSession, Principal)
     * @return the result of the Permission check
     */
    public final boolean checkPermission( WikiSession session, Permission permission )
    {
        if( !m_useJAAS ) 
        {
            //
            //  Nobody can login, if JAAS is turned off.
            //

            if( permission == null || permission.getActions().equals("login") )
                return false;
            
            return true;
        }
        
        //
        //  A slight sanity check.
        //
        if ( session == null || permission == null )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_DENIED, null, permission ) );
            return false;
        }
        Principal user = session.getLoginPrincipal();
        
        // Always allow the action if user has AllPermission
        Permission allPermission = new AllPermission( m_engine.getApplicationName() );
        boolean hasAllPermission = checkStaticPermission( session, allPermission );
        if ( hasAllPermission )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_ALLOWED, user, permission ) );
            return true;
        }
        
        // If the user doesn't have *at least* the permission
        // granted by policy, return false.
        boolean hasPolicyPermission = checkStaticPermission( session, permission );
        if ( !hasPolicyPermission )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_DENIED, user, permission ) );
            return false;
        }
        
        // If this isn't a PagePermission, it's allowed
        if ( ! ( permission instanceof PagePermission ) )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_ALLOWED, user, permission ) );
            return true;
        }

        //
        // If the page or ACL is null, it's allowed.
        String pageName = ((PagePermission)permission).getPage();
        WikiPage page = m_engine.getPage( pageName );
        Acl acl = ( page == null) ? null : m_engine.getAclManager().getPermissions( page );
        if ( page == null ||  acl == null )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_ALLOWED, user, permission ) );
            return true;
        }
        
        //
        //  Next, iterate through the Principal objects assigned
        //  this permission. If the context's subject possesses
        //  any of these, the action is allowed.

        Principal[] aclPrincipals = acl.findPrincipals( permission );

        log.debug( "Checking ACL entries..." );
        log.debug( "Acl for this page is: " + acl );
        log.debug( "Checking for principal: " + String.valueOf(aclPrincipals) );
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
                fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_ALLOWED, user, permission ) );
                return true;
            }
        }
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.ACCESS_DENIED, user, permission ) );
        return false;
    }
    
    /**
     * <p>Determines if the Subject associated with a 
     * supplied WikiSession contains a desired Role or GroupPrincipal. 
     * The algorithm simply checks to see if the Subject possesses 
     * the Role or GroupPrincipal it in its Principal set. Note that
     * any user (anyonymous, asserted, authenticated) can possess
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
     *            for, which must be non-<cor>null</code>. If <code>null</code>,
     *            the result of this method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role or GroupPrincipal, <code>false</code> otherwise
     */
    public final boolean isUserInRole( WikiSession session, Principal principal )
    {
        if ( session == null || principal == null ||
             AuthenticationManager.isUserPrincipal( principal ) )
        {
            return false;
        }

        // Any type of user can possess a built-in role
        if ( principal instanceof Role && Role.isBuiltInRole( ((Role)principal) ) )
        {
            return session.hasPrincipal( principal );
        }
        
        // Only authenticated users can posssess groups or custom roles
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
     * a {@link com.ecyrd.jspwiki.WikiException}.
     * @throws com.ecyrd.jspwiki.WikiException if the Authorizer could
     * not be initialized
     * @return the current Authorizer
     */
    public final Authorizer getAuthorizer() throws WikiSecurityException
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
     * @param session the current wiki session, which must be non-null. If null,
     *            the result of this method always returns <code>false</code>
     * @param principal the Principal (role, group, or user principal) to look
     *            for, which must be non-null. If null, the result of this
     *            method always returns <code>false</code>
     * @return <code>true</code> if the Subject supplied with the WikiContext
     *         posesses the Role, GroupPrincipal or desired
     *         user Principal, <code>false</code> otherwise
     */
    protected final boolean hasRoleOrPrincipal( WikiSession session, Principal principal )
    {
        // If either parameter is null, always deny
        if ( session == null || principal == null )
        {
            return false;
        }
        
        // If principal is role, delegate to isUserInRole
        if ( AuthenticationManager.isRolePrincipal( principal ) )
        {
            return isUserInRole( session, principal );
        }
        
        // We must be looking for a user principal, then. 
        // So just look for a name match.
        if ( AuthenticationManager.isUserPrincipal( principal ) )
        {
            String principalName = principal.getName();
            Principal[] userPrincipals = session.getPrincipals();
            for ( int i = 0; i < userPrincipals.length; i++ )
            {
                Principal userPrincipal = userPrincipals[i];
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
    public final void initialize( WikiEngine engine, Properties properties ) throws WikiException
    {
        m_engine = engine;
        
        m_useJAAS = AuthenticationManager.SECURITY_JAAS.equals( properties.getProperty(AuthenticationManager.PROP_SECURITY, AuthenticationManager.SECURITY_JAAS ) );
        
        if( !m_useJAAS ) return;
        
        //
        //  JAAS authorization continues
        //
        m_authorizer = getAuthorizerImplementation( properties );
        m_authorizer.initialize( engine, properties );
    }

    /**
     * @see com.ecyrd.jspwiki.event.EventSourceDelegate#fireEvent(com.ecyrd.jspwiki.event.WikiEvent)
     */
    protected final void fireEvent( WikiEvent event )
    {
        m_listeners.fireEvent( event );
    }
    
    /** 
     * Returns <code>true</code> if JSPWiki's JAAS authorization system 
     * is used for authorization in addition to container controls.
     * @return the result
     */
    protected boolean isJAASAuthorized()
    {
        return m_useJAAS;
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
    private final Authorizer getAuthorizerImplementation( Properties props ) throws WikiException
    {
        String authClassName = props.getProperty( PROP_AUTHORIZER, DEFAULT_AUTHORIZER );
        return (Authorizer) locateImplementation( authClassName );
    }

    private final Object locateImplementation( String clazz ) throws WikiException
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
     * @param session the WikiSession whose permission status is being queried
     * @param permission the Permission the Subject must possess
     * @return <code>true</code> if the Subject posesses the permission,
     *         <code>false</code> otherwise
     */
    protected final boolean checkStaticPermission( WikiSession session, final Permission permission )
    {
        if( !m_useJAAS ) return true;
        
        try
        {
            WikiSession.doPrivileged( session, new PrivilegedAction()
            {
                public Object run()
                {
                    AccessController.checkPermission( permission );
                    return null;
                }
            } );
            return true;
        }
        catch( AccessControlException e )
        {
            return false;
        }
    }
    
    /**
     * @see com.ecyrd.jspwiki.event.WikiEventSource#removeWikiEventListener(WikiEventListener)
     */
    public final void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.removeWikiEventListener( listener );
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
    public final Principal resolvePrincipal( String name ) 
    {  
        if( !m_useJAAS )
        {
            return new UnresolvedPrincipal(name);
        }
        
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

}