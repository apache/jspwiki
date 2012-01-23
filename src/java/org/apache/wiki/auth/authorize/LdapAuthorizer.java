package org.apache.wiki.auth.authorize;

import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.Authorizer;
import org.apache.wiki.auth.LdapConfig;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.LdapUserDatabase;

/**
 * <p>
 * Authorizer whose Roles are supplied by LDAP groups. This Authorizer is often
 * used in conjunction with {@link LdapUserDatabase} for authentication. This
 * can be done either as part of the web container authentication configuration
 * or (more likely) as part of JSPWiki's own native authentication
 * configuration.
 * </p>
 * <p>
 * When {@link #initialize(WikiEngine, Properties)} executes, a new instance of
 * {@link org.apache.wiki.auth.LdapConfig} is created and configured based on
 * the settings in {@code jspwiki.properties}. The properties that are required
 * in order for LdapAuthorizer to function correctly are
 * {@link LdapConfig#PROPERTY_CONNECTION_URL},
 * {@link LdapConfig#PROPERTY_ROLE_BASE} and
 * {@link LdapConfig#PROPERTY_IS_IN_ROLE_FILTER}. Additional properties that can
 * be set include {@link LdapConfig#PROPERTY_BIND_USER},
 * {@link LdapConfig#PROPERTY_AUTHENTICATION} and
 * {@link LdapConfig#PROPERTY_SSL}. See the documentation for that LdapConfig
 * for more details.
 * </p>
 */
public class LdapAuthorizer implements Authorizer
{
    private Hashtable<String, String> m_jndiEnv;

    private LdapConfig m_cfg = null;

    /**
     * Finds all of the LDAP group objects in the role base with at least one
     * member.
     */
    private String m_allRolesFilter = null;

    /**
     * Finds an LDAP group object matching a supplied role name.
     */
    private String m_roleFilter = null;

    /**
     * Finds all of the LDAP group objects that contain a supplied user as a
     * member.
     */
    private String m_userRolesFilter = null;

    /**
     * {@inheritDoc}
     */
    public Principal findRole( String role )
    {
        try
        {
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            String filter = m_roleFilter;
            filter = filter.replace( "{0}", LdapConfig.escapeFilterString( role ) );
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, filter, SEARCH_CONTROLS );
            if( roles.hasMore() )
            {
                return new Role( role );
            }
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Principal[] getRoles()
    {
        Set<Role> foundRoles = new HashSet<Role>();
        try
        {
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, m_allRolesFilter, SEARCH_CONTROLS );
            while ( roles.hasMore() )
            {
                SearchResult foundRole = roles.next();
                String roleName = (String) foundRole.getAttributes().get( "cn" ).get( 0 );
                foundRoles.add( new Role( roleName ) );
            }
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return foundRoles.toArray( new Role[foundRoles.size()] );
    }

    /**
     * {@inheritDoc}
     */
    public Role[] findRoles( WikiSession session ) throws WikiSecurityException
    {
        String loginName = session.getLoginPrincipal().getName();
        Set<Role> foundRoles = new HashSet<Role>();
        try
        {
            String dn = m_cfg.getUserDn( loginName );
            dn = LdapConfig.escapeFilterString( dn );
            String filter = m_userRolesFilter.replace( "{1}", dn );
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, filter, SEARCH_CONTROLS );
            while ( roles.hasMore() )
            {
                SearchResult foundRole = roles.next();
                String roleName = (String) foundRole.getAttributes().get( "cn" ).get( 0 );
                foundRoles.add( new Role( roleName ) );
            }
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return foundRoles.toArray( new Role[foundRoles.size()] );
    }

    private static final String[] REQUIRED_PROPERTIES = new String[] { LdapConfig.PROPERTY_CONNECTION_URL,
                                                                      LdapConfig.PROPERTY_ROLE_BASE,
                                                                      LdapConfig.PROPERTY_IS_IN_ROLE_FILTER };

    private static final SearchControls SEARCH_CONTROLS;

    static
    {
        SEARCH_CONTROLS = new SearchControls();
        SEARCH_CONTROLS.setSearchScope( SearchControls.SUBTREE_SCOPE );
    }

    /**
     * {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException
    {
        m_cfg = LdapConfig.getInstance( engine.getAuthenticationManager().getKeychain(), props, REQUIRED_PROPERTIES );
        m_roleFilter = m_cfg.isInRoleFilter.replace( "{1}", "*" );
        m_userRolesFilter = m_cfg.isInRoleFilter.replace( "{0}", "*" );
        m_allRolesFilter = m_userRolesFilter.replace( "{1}", "*" );

        // Do a quick connection test, and fail-fast if needed
        try
        {
            m_jndiEnv = m_cfg.newJndiEnvironment();
            new InitialLdapContext( m_jndiEnv, null );
        }
        catch( NamingException e )
        {
            throw new WikiSecurityException( "Could not start LdapAuthorizer! Cause: " + e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns {@code true} when the user login Principal
     * contained in the WikiSession's Subject belongs to an LDAP group found in
     * the role-base DN. The login Principal is assumed to be a valid JSPWiki
     * login name and is transformed into a full DN before the search by
     * consulting {@link LdapConfig#getUserDn(String)}. The scope searched is
     * provided by {@link LdapConfig#PROPERTY_ROLE_BASE}, and the filter to
     * match roles is provided by {@link LdapConfig#PROPERTY_IS_IN_ROLE_FILTER}.
     * </p>
     * <p>
     * For example, consider a WikiSession whose subject contains three user
     * principals, the two built-in roles {@code ALL} and {@code AUTHENTICATED},
     * and a group principal {@code MyGroup}. We assume the user names are
     * stored in the user-base DN {@code ou=people,dc=jspwiki,dc=org}:
     * </p>
     * <blockquote> {@code WikiPrincipal.LOGIN_NAME "biggie.smalls"<br/>
     * WikiPrincipal.FULL_NAME "Biggie Smalls"<br/>
     * WikiPrincipal.WIKI_NAME "BiggieSmalls"<br/>
     * Role.ALL Role.AUTHENTICATED GroupPrincipal "MyGroup"}</blockquote>
     * <p>
     * In this case, the DN {@code
     * uid=biggie.smalls,ou=people,dc=jspwiki,dc=org} would be examined for
     * membership in an LDAP group whose common name matches {@code role}. Given
     * an is-in-role filter of {@code (&(objectClass=groupOfUniqueNames)(cn=\
     * 0\})(uniqueMember=\{1\}))}, an LDAP search would be constructed to find
     * objects whose {@code objectClass} was of type {@code groupOfUniqueNames}
     * and whose {@code uniqueMember} attribute contained the value {@code
     * uid=biggie.smalls,ou=people,dc=jspwiki,dc=org}.
     * </p>
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        String loginName = session.getLoginPrincipal().getName();
        try
        {
            String dn = m_cfg.getUserDn( loginName );
            dn = LdapConfig.escapeFilterString( dn );
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            String roleName = LdapConfig.escapeFilterString( role.getName() );
            String filter = m_cfg.isInRoleFilter.replace( "{0}", roleName );
            filter = filter.replace( "{1}", dn );
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, filter, SEARCH_CONTROLS );
            boolean isMember = roles.hasMore();
            return isMember;
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return false;
    }
}
