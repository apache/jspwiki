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
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.AclManager;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.pages.PageManager;
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
 * <p>Default implementation for {@link AuthorizationManager}</p>
 * {@inheritDoc}
 *
 * <p>See the {@link #checkPermission(WikiSession, Permission)} and {@link #hasRoleOrPrincipal(WikiSession, Principal)} methods for more
 * information on the authorization logic.</p>
 * @since 2.3
 * @see AuthenticationManager
 */
public class DefaultAuthorizationManager implements AuthorizationManager {

    private static final Logger log = Logger.getLogger( DefaultAuthorizationManager.class );

    private Authorizer m_authorizer = null;

    /** Cache for storing ProtectionDomains used to evaluate the local policy. */
    private Map< Principal, ProtectionDomain > m_cachedPds = new WeakHashMap<>();

    private Engine m_engine = null;

    private LocalPolicy m_localPolicy = null;

    /**
     * Constructs a new DefaultAuthorizationManager instance.
     */
    public DefaultAuthorizationManager() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkPermission( final Session session, final Permission permission ) {
        // A slight sanity check.
        if( session == null || permission == null ) {
            fireEvent( WikiSecurityEvent.ACCESS_DENIED, null, permission );
            return false;
        }

        final Principal user = session.getLoginPrincipal();

        // Always allow the action if user has AllPermission
        final Permission allPermission = new AllPermission( m_engine.getApplicationName() );
        final boolean hasAllPermission = checkStaticPermission( session, allPermission );
        if( hasAllPermission ) {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        // If the user doesn't have *at least* the permission granted by policy, return false.
        final boolean hasPolicyPermission = checkStaticPermission( session, permission );
        if( !hasPolicyPermission ) {
            fireEvent( WikiSecurityEvent.ACCESS_DENIED, user, permission );
            return false;
        }

        // If this isn't a PagePermission, it's allowed
        if( !( permission instanceof PagePermission ) ) {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        // If the page or ACL is null, it's allowed.
        final String pageName = ((PagePermission)permission).getPage();
        final WikiPage page = m_engine.getManager( PageManager.class ).getPage( pageName );
        final Acl acl = ( page == null) ? null : m_engine.getManager( AclManager.class ).getPermissions( page );
        if( page == null ||  acl == null || acl.isEmpty() ) {
            fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
            return true;
        }

        // Next, iterate through the Principal objects assigned this permission. If the context's subject possesses
        // any of these, the action is allowed.
        final Principal[] aclPrincipals = acl.findPrincipals( permission );

        log.debug( "Checking ACL entries..." );
        log.debug( "Acl for this page is: " + acl );
        log.debug( "Checking for principal: " + Arrays.toString( aclPrincipals ) );
        log.debug( "Permission: " + permission );

        for( Principal aclPrincipal : aclPrincipals ) {
            // If the ACL principal we're looking at is unresolved, try to resolve it here & correct the Acl
            if ( aclPrincipal instanceof UnresolvedPrincipal ) {
                final AclEntry aclEntry = acl.getEntry( aclPrincipal );
                aclPrincipal = resolvePrincipal( aclPrincipal.getName() );
                if ( aclEntry != null && !( aclPrincipal instanceof UnresolvedPrincipal ) ) {
                    aclEntry.setPrincipal( aclPrincipal );
                }
            }

            if ( hasRoleOrPrincipal( session, aclPrincipal ) ) {
                fireEvent( WikiSecurityEvent.ACCESS_ALLOWED, user, permission );
                return true;
            }
        }
        fireEvent( WikiSecurityEvent.ACCESS_DENIED, user, permission );
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Authorizer getAuthorizer() throws WikiSecurityException {
        if ( m_authorizer != null ) {
            return m_authorizer;
        }
        throw new WikiSecurityException( "Authorizer did not initialize properly. Check the logs." );
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRoleOrPrincipal( final Session session, final Principal principal ) {
        // If either parameter is null, always deny
        if( session == null || principal == null ) {
            return false;
        }

        // If principal is role, delegate to isUserInRole
        if( AuthenticationManager.isRolePrincipal( principal ) ) {
            return isUserInRole( session, principal );
        }

        // We must be looking for a user principal, assuming that the user has been properly logged in. So just look for a name match.
        if( session.isAuthenticated() && AuthenticationManager.isUserPrincipal( principal ) ) {
            final String principalName = principal.getName();
            final Principal[] userPrincipals = session.getPrincipals();
            for( final Principal userPrincipal : userPrincipals ) {
                if( userPrincipal.getName().equals( principalName ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAccess( final WikiContext context, final HttpServletResponse response, final boolean redirect ) throws IOException {
        final boolean allowed = checkPermission( context.getWikiSession(), context.requiredPermission() );
        final ResourceBundle rb = Preferences.getBundle( context, InternationalizationManager.CORE_BUNDLE );

        // Stash the wiki context
        if ( context.getHttpRequest() != null && context.getHttpRequest().getAttribute( Context.ATTR_CONTEXT ) == null ) {
            context.getHttpRequest().setAttribute( Context.ATTR_CONTEXT, context );
        }

        // If access not allowed, redirect
        if( !allowed && redirect ) {
            final Principal currentUser  = context.getWikiSession().getUserPrincipal();
            final String pageurl = context.getPage().getName();
            if( context.getWikiSession().isAuthenticated() ) {
                log.info( "User " + currentUser.getName() + " has no access - forbidden (permission=" + context.requiredPermission() + ")" );
                context.getWikiSession().addMessage( MessageFormat.format( rb.getString( "security.error.noaccess.logged" ),
                                                     context.getName()) );
            } else {
                log.info( "User " + currentUser.getName() + " has no access - redirecting (permission=" + context.requiredPermission() + ")" );
                context.getWikiSession().addMessage( MessageFormat.format( rb.getString("security.error.noaccess"), context.getName() ) );
            }
            response.sendRedirect( m_engine.getURL(WikiContext.LOGIN, pageurl, null ) );
        }
        return allowed;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws WikiException {
        m_engine = engine;

        //  JAAS authorization continues
        m_authorizer = getAuthorizerImplementation( properties );
        m_authorizer.initialize( engine, properties );

        // Initialize local security policy
        try {
            final String policyFileName = properties.getProperty( POLICY, DEFAULT_POLICY );
            final URL policyURL = engine.findConfigFile( policyFileName );

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
     * Attempts to locate and initialize a Authorizer to use with this manager. Throws a WikiException if no entry is found, or if one
     * fails to initialize.
     *
     * @param props jspwiki.properties, containing a 'jspwiki.authorization.provider' class name.
     * @return a Authorizer used to get page authorization information.
     * @throws WikiException if there are problems finding the authorizer implementation.
     */
    private Authorizer getAuthorizerImplementation( final Properties props ) throws WikiException {
        final String authClassName = props.getProperty( PROP_AUTHORIZER, DEFAULT_AUTHORIZER );
        return ( Authorizer )locateImplementation( authClassName );
    }

    private Object locateImplementation( final String clazz ) throws WikiException {
        if ( clazz != null ) {
            try {
                final Class< ? > authClass = ClassUtil.findClass( "org.apache.wiki.auth.authorize", clazz );
                return authClass.newInstance();
            } catch( final ClassNotFoundException e ) {
                log.fatal( "Authorizer " + clazz + " cannot be found", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be found", e );
            } catch( final InstantiationException e ) {
                log.fatal( "Authorizer " + clazz + " cannot be created", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be created", e );
            } catch( final IllegalAccessException e ) {
                log.fatal( "You are not allowed to access this authorizer class", e );
                throw new WikiException( "You are not allowed to access this authorizer class", e );
            }
        }

        throw new NoRequiredPropertyException( "Unable to find a " + PROP_AUTHORIZER + " entry in the properties.", PROP_AUTHORIZER );
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowedByLocalPolicy( final Principal[] principals, final Permission permission ) {
        for ( final Principal principal : principals ) {
            // Get ProtectionDomain for this Principal from cache, or create new one
            ProtectionDomain pd = m_cachedPds.get( principal );
            if ( pd == null ) {
                final ClassLoader cl = this.getClass().getClassLoader();
                final CodeSource cs = new CodeSource( null, (Certificate[])null );
                pd = new ProtectionDomain( cs, null, cl, new Principal[]{ principal } );
                m_cachedPds.put( principal, pd );
            }

            // Consult the local policy and get the answer
            if ( m_localPolicy.implies( pd, permission ) ) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkStaticPermission( final Session session, final Permission permission ) {
        return ( Boolean )Session.doPrivileged( session, ( PrivilegedAction< Boolean > )() -> {
            try {
                // Check the JVM-wide security policy first
                AccessController.checkPermission( permission );
                return Boolean.TRUE;
            } catch( final AccessControlException e ) {
                // Global policy denied the permission
            }

            // Try the local policy - check each Role/Group and User Principal
            if ( allowedByLocalPolicy( session.getRoles(), permission ) || allowedByLocalPolicy( session.getPrincipals(), permission ) ) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } );
    }

    /** {@inheritDoc} */
    @Override
    public Principal resolvePrincipal( final String name ) {
        // Check built-in Roles first
        final Role role = new Role(name);
        if ( Role.isBuiltInRole( role ) ) {
            return role;
        }

        // Check Authorizer Roles
        Principal principal = m_authorizer.findRole( name );
        if ( principal != null ) {
            return principal;
        }

        // Check Groups
        principal = m_engine.getManager( GroupManager.class ).findRole( name );
        if ( principal != null ) {
            return principal;
        }

        // Ok, no luck---this must be a user principal
        final Principal[] principals;
        final UserProfile profile;
        final UserDatabase db = m_engine.getManager( UserManager.class ).getUserDatabase();
        try {
            profile = db.find( name );
            principals = db.getPrincipals( profile.getLoginName() );
            for( final Principal value : principals ) {
                principal = value;
                if( principal.getName().equals( name ) ) {
                    return principal;
                }
            }
        } catch( final NoSuchPrincipalException e ) {
            // We couldn't find the user...
        }
        // Ok, no luck---mark this as unresolved and move on
        return new UnresolvedPrincipal( name );
    }


    // events processing .......................................................

    /** {@inheritDoc} */
    @Override
    public synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

}
