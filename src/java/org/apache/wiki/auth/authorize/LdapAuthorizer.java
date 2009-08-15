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
import org.apache.wiki.auth.login.LdapLoginModule;

/**
 * <p>
 * Authorizer whose Roles are supplied by LDAP groups. This Authorizer requires
 * that {@link LdapLoginModule} be used for authentication. This can be done
 * either as part of the web container authentication configuration or (more
 * likely) as part of JSPWiki's own native authentication configuration.
 * </p>
 * <p>
 * When {@link #initialize(WikiEngine, Properties)} executes, a new instance of
 * {@link org.apache.wiki.auth.LdapConfig} is created and configured based on
 * the settings in <code>jspwiki.properties</code>. The properties that are
 * required in order for LdapAuthorizer to function correctly are
 * {@link LdapConfig#PROPERTY_CONNECTION_URL},
 * {@link LdapConfig#PROPERTY_ROLE_BASE},
 * {@link LdapConfig#PROPERTY_ROLE_PATTERN} and
 * {@link LdapConfig#PROPERTY_IS_IN_ROLE_PATTERN}. Additional properties that
 * can be set include {@link LdapConfig#PROPERTY_BIND_DN},
 * {@link LdapConfig#PROPERTY_AUTHENTICATION} and
 * {@link LdapConfig#PROPERTY_SSL}. See the documentation for that LdapConfig
 * for more details.
 * </p>
 */
public class LdapAuthorizer implements Authorizer
{
    private Hashtable<String, String> m_jndiEnv;

    private LdapConfig m_cfg = null;

    private String m_allRoleFinder = null;

    /**
     * {@inheritDoc}
     */
    public Principal findRole( String role )
    {
        try
        {
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            String roleFinder = m_cfg.rolePattern;
            roleFinder = roleFinder.replace( "{0}", role );
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, roleFinder, SEARCH_CONTROLS );
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
            NamingEnumeration<SearchResult> roles = ctx.search( m_cfg.roleBase, m_allRoleFinder, SEARCH_CONTROLS );
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
                                                                      LdapConfig.PROPERTY_ROLE_PATTERN,
                                                                      LdapConfig.PROPERTY_IS_IN_ROLE_PATTERN };

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
        m_allRoleFinder = m_cfg.rolePattern.replace( "{0}", "*" );

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
     * This implementation returns <code>true</code> when the user login
     * Principal contained in the WikiSession's Subject belongs to an LDAP group
     * found in the role-base DN. The login Principal is assumed to be a valid
     * DN. The scope searched is provided by
     * {@link LdapConfig#PROPERTY_ROLE_BASE}, and the filter to match roles is
     * provided by {@link LdapConfig#PROPERTY_IS_IN_ROLE_PATTERN}.
     * </p>
     * <p>
     * For example, consider a WikiSession whose subject contains three user
     * principals, the two built-in roles <code>ALL</code> and
     * <code>AUTHENTICATED</code>, and a group principal <code>MyGroup</code>:
     * </p>
     * <blockquote>
     * <code>WikiPrincipal.LOGIN_NAME "uid=biggie.smalls,ou=people,dc=jspwiki,dc=org"<br/>
     * WikiPrincipal.FULL_NAME "Biggie Smalls"<br/>
     * WikiPrincipal.WIKI_NAME "BiggieSmalls"<br/>
     * Role.ALL
     * Role.AUTHENTICATED
     * GroupPrincipal "MyGroup"</code></blockquote>
     * <p>
     * In this case, the DN
     * <code>uid=biggie.smalls,ou=people,dc=jspwiki,dc=org</code> would be
     * examined for membership in an LDAP group whose common name matches
     * <code>role</code>. Given an is-in-role pattern of
     * <code>(&(objectClass=groupOfUniqueNames)(cn={0})(uniqueMember={1}))</code>
     * , an LDAP search would be constructed to find objects whose
     * <code>objectClass</code> was of type <code>groupOfUniqueNames</code> and
     * whose <code>uniqueMember<code> attribute contained the value
     * <code>uid=biggie.smalls,ou=people,dc=jspwiki,dc=org</code>.
     * </p>
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        String loginName = session.getLoginPrincipal().getName();
        try
        {
            String dn = m_cfg.getUserDn( loginName );
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            String filter = m_cfg.isInRolePattern.replace( "{0}", role.getName() );
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
