package org.apache.wiki.auth.authorize;

import java.security.Principal;
import java.util.*;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.Authorizer;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * Authorizer whose Roles are supplied by LDAP groups.
 */
public class LdapAuthorizer implements Authorizer
{
    private Hashtable<String, String> m_env;

    private String m_roleBase = null;

    private String m_userPattern = null;

    /**
     * {@inheritDoc}
     */
    public Principal findRole( String role )
    {
        try
        {
            DirContext ctx = new InitialLdapContext( m_env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[0] );
            NamingEnumeration<SearchResult> roles = ctx.search( m_roleBase, "(cn=" + role + ")", searchControls );
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
            DirContext ctx = new InitialLdapContext( m_env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[] { "cn" } );
            NamingEnumeration<SearchResult> roles = ctx.search( m_roleBase, "(objectClass=groupOfUniqueNames)", searchControls );
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

    private static final Logger log = LoggerFactory.getLogger( LdapAuthorizer.class );

    /**
     * Property that supplies the connection URL for the LDAP server, e.g.
     * <code>ldap://127.0.0.1:4890/</code>. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_CONNECTION_URL = "jspwiki.loginModule.options.ldap.connectionURL";

    /**
     * Property that indicates whether to use SSL for connecting to the LDAP
     * server. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_SSL = "jspwiki.loginModule.options.ldap.ssl";

    /**
     * Property that supplies the DN pattern for finding users, e.g.
     * <code>uid={0},ou=people,dc=jspwiki,dc=org</code> This property is also
     * used by {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_USER_PATTERN = "jspwiki.loginModule.options.ldap.userPattern";

    /**
     * Property that supplies the DN pattern for finding roles, e.g.
     * <code>ou=roles,dc=jspwiki,dc=org</code>
     */
    protected static final String PROPERTY_ROLE_BASE = "jspwiki.ldap.roleBase";

    /**
     * {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );

        // LDAP server to search
        String option = (String) props.get( PROPERTY_CONNECTION_URL );
        if( option != null && option.trim().length() > 0 )
        {
            env.put( Context.PROVIDER_URL, option.trim() );
        }

        // Use SSL?
        option = (String) props.get( PROPERTY_SSL );
        boolean ssl = TextUtil.isPositive( option );
        env.put( Context.SECURITY_PROTOCOL, ssl ? "ssl" : "none" );
        m_env = env;

        if( log.isDebugEnabled() )
        {
            log.debug( "Built JNDI environment for LDAP search.", m_env );
        }

        // DN pattern for finding users
        option = (String) props.get( PROPERTY_USER_PATTERN );
        if( option != null && option.trim().length() > 0 )
        {
            m_userPattern = option.trim();
        }
        else
        {
            throw new WikiSecurityException( PROPERTY_USER_PATTERN + " not supplied." );
        }

        // DN pattern for finding roles
        option = (String) props.get( PROPERTY_ROLE_BASE );
        if( option != null && option.trim().length() > 0 )
        {
            m_roleBase = option.trim();
        }
        else
        {
            throw new WikiSecurityException( PROPERTY_ROLE_BASE + " not supplied." );
        }
        
        // Do a quick connection test, and fail-fast if needed
        try
        {
            new InitialLdapContext( m_env, null );
        }
        catch( NamingException e )
        {
            throw new WikiSecurityException( "Could not start LdapAuthorizer! Cause: " + e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns <code>true</code> when at least one of the
     * user login Principals contained in the WikiSession's Subject belongs to
     * an LDAP group contained in the role-base DN. The user DN is constructed
     * from the user Principal, where the <code>uid</code> of the user's DN is
     * the Principal name, and the rest of the DN is determined by
     * {@link #PROPERTY_USER_PATTERN}.
     * </p>
     * <p>
     * To make an accurate search, Principals that are of type {@link Role},
     * {@link org.apache.wiki.auth.GroupPrincipal} are excluded from
     * consideration. So are {@link org.apache.wiki.auth.WikiPrincipal} whose
     * type are {@link org.apache.wiki.auth.WikiPrincipal#FULL_NAME} or
     * {@link org.apache.wiki.auth.WikiPrincipal#WIKI_NAME}.
     * </p>
     * <p>
     * For example, consider an LDAP user base of
     * <code>ou=people,dc=jspwiki,dc=org</code>, and a WikiSession whose subject
     * contains three user principals, the two built-in roles <code>ALL</code>
     * and <code>AUTHENTICATED</code>, and a group principal
     * <code>MyGroup</code>:
     * </p>
     * <blockquote><code>WikiPrincipal.LOGIN_NAME "biggie.smalls"<br/>
     * WikiPrincipal.FULL_NAME "Biggie Smalls"<br/>
     * WikiPrincipal.WIKI_NAME "BiggieSmalls"<br/>
     * Role.ALL
     * Role.AUTHENTICATED
     * GroupPrincipal "MyGroup"</code></blockquote>
     * <p>
     * In this case, only WikiPrincipal.LOGIN_NAME "biggie.smalls" would be
     * examined. An LDAP search would be constructed that searched the LDAP role
     * base for an object whose <code>objectClass</code> was of type
     * <code>groupOfUniqueNames</code> and whose
     * <code>uniqueMember<code> attribute contained the value
     * <code>uid=biggie.smalls,ou=people,dc=jspwiki,dc=org</code>.
     * </p>
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        // Build DN
        String uid = session.getLoginPrincipal().getName();
        String dn = m_userPattern.replace( "{0}", uid ).trim();
        dn = dn.replace( "=", "\\3D" );

        try
        {
            DirContext ctx = new InitialLdapContext( m_env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[0] );
            String filter = "(&(objectClass=groupOfUniqueNames)(cn=" + role.getName() + ")(uniqueMember=" + dn + "))";
            NamingEnumeration<SearchResult> roles = ctx.search( m_roleBase, filter, searchControls );
            return roles.hasMore();
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return false;
    }

}
