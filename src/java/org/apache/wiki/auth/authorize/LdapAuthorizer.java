package org.apache.wiki.auth.authorize;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

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
import org.freshcookies.security.Keychain;

/**
 * Authorizer whose Roles are supplied by LDAP groups.
 */
public class LdapAuthorizer implements Authorizer
{
    private Hashtable<String, String> m_jndiEnv;

    private String m_roleBase = null;

    /**
     * Finds all roles; based on m_rolePattern
     */
    private String m_roleFinder = null;

    private String m_authentication = null;

    private Keychain m_keychain = null;

    private String m_connectionUrl = null;

    private String m_ssl = null;

    private String m_rolePattern = null;

    private String m_userLoginIdPattern = null;

    private String m_bindDN = null;

    /**
     * {@inheritDoc}
     */
    public Principal findRole( String role )
    {
        try
        {
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
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
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[] { "cn" } );
            NamingEnumeration<SearchResult> roles = ctx.search( m_roleBase, m_roleFinder, searchControls );
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
     * Property that specifies the JNDI authentication type. Valid values are
     * the same as those for {@link Context#SECURITY_AUTHENTICATION}:
     * <code>none</code>, <code>simple</code>, <code>strong</code> or
     * <code>DIGEST-MD5</code>. The default is <code>simple</code> if SSL is
     * specified, and <code>DIGEST-MD5</code> otherwise. This property is also
     * used by {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_AUTHENTICATION = "jspwiki.loginModule.options.ldap.authentication";

    /**
     * Property that supplies the connection URL for the LDAP server, e.g.
     * <code>ldap://127.0.0.1:4890/</code>. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_CONNECTION_URL = "jspwiki.loginModule.options.ldap.connectionURL";

    /**
     * Property that supplies the DN used to bind to the directory when looking
     * up users and roles.
     */
    protected static final String PROPERTY_BIND_DN = "jspwiki.ldap.bindDN";

    /**
     * Property that supplies the base DN where roles are contained, e.g.
     * <code>ou=roles,dc=jspwiki,dc=org</code>.
     */
    protected static final String PROPERTY_ROLE_BASE = "jspwiki.ldap.roleBase";

    /**
     * Property that supplies the pattern for finding users within the role
     * base, e.g.
     * <code>(&(objectClass=groupOfUniqueNames)(cn={0})(uniqueMember={1}))</code>
     * .
     */
    protected static final String PROPERTY_ROLE_PATTERN = "jspwiki.ldap.rolePattern";

    /**
     * Property that indicates whether to use SSL for connecting to the LDAP
     * server. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_SSL = "jspwiki.loginModule.options.ldap.ssl";

    /**
     * Property that specifies the pattern for the username used to log in to
     * the LDAP server. Usually this is a full DN, for example
     * <code>uid={0},ou=people,dc=jspwiki,dc=org</code>. However, sometimes (as
     * with Active Directory 2003 and later) only the userid is used, in which
     * case the principal will simply be <code>{0}</code>. This property is also
     * used by {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    protected static final String PROPERTY_LOGIN_ID_PATTERN = "jspwiki.loginModule.options.ldap.userPattern";

    private static final String[] REQUIRED_PROPERTIES = new String[] { PROPERTY_CONNECTION_URL, PROPERTY_LOGIN_ID_PATTERN,
                                                                      PROPERTY_ROLE_BASE, PROPERTY_ROLE_PATTERN };

    protected static final String KEYCHAIN_BIND_DN_ENTRY = "LdapAuthorizer.BindDN.Password";

    /**
     * {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException
    {
        // Make sure all required properties are here
        for( String prop : REQUIRED_PROPERTIES )
        {
            if( !props.containsKey( prop ) )
            {
                throw new WikiSecurityException( "Property " + prop + " is required!" );
            }
        }

        // Figure out LDAP environment settings
        m_connectionUrl = props.getProperty( PROPERTY_CONNECTION_URL ).trim();
        m_userLoginIdPattern = props.getProperty( PROPERTY_LOGIN_ID_PATTERN ).trim();
        m_roleBase = props.getProperty( PROPERTY_ROLE_BASE ).trim();
        m_rolePattern = props.getProperty( PROPERTY_ROLE_PATTERN ).trim();
        m_roleFinder = m_rolePattern.replaceAll( "\\{[0-1]\\}", "\\*" );
        m_keychain = engine.getAuthenticationManager().getKeychain();

        // Figure out optional properties
        String ssl = (String) props.get( PROPERTY_SSL );
        m_ssl = (ssl != null && TextUtil.isPositive( ssl )) ? "ssl" : "none";
        String authentication = (String) props.get( PROPERTY_AUTHENTICATION );
        if( authentication == null || authentication.length() == 0 )
        {
            m_authentication = "ssl".equals( m_ssl ) ? "simple" : "DIGEST-MD5";
        }
        else
        {
            m_authentication = authentication;
        }
        String bindDN = props.getProperty( PROPERTY_BIND_DN );
        if( bindDN != null && bindDN.length() > 0 )
        {
            m_bindDN = bindDN.trim();
        }

        // Do a quick connection test, and fail-fast if needed
        buildJndiEnvironment();
        try
        {
            new InitialLdapContext( m_jndiEnv, null );
        }
        catch( NamingException e )
        {
            throw new WikiSecurityException( "Could not start LdapAuthorizer! Cause: " + e.getMessage(), e );
        }
    }

    private void buildJndiEnvironment() throws WikiSecurityException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, m_connectionUrl );
        env.put( Context.SECURITY_PROTOCOL, m_ssl );
        env.put( Context.SECURITY_AUTHENTICATION, m_authentication );

        // If we need and Bind DN and Keychain is loaded, get the bind DN and
        // password
        if( m_bindDN != null && m_keychain.isLoaded() )
        {
            try
            {
                KeyStore.Entry password = m_keychain.getEntry( KEYCHAIN_BIND_DN_ENTRY );
                if( password instanceof Keychain.Password )
                {
                    env.put( Context.SECURITY_PRINCIPAL, m_bindDN );
                    env.put( Context.SECURITY_CREDENTIALS, ((Keychain.Password) password).getPassword() );
                }
            }
            catch( KeyStoreException e )
            {
                e.printStackTrace();
                throw new WikiSecurityException( "Could not build JNDI environment. ", e );
            }
        }

        // Spill all the information if debugging
        if( log.isDebugEnabled() )
        {
            log.debug( "Built JNDI environment for LDAP login.", env );
        }

        m_jndiEnv = env;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns <code>true</code> when at least one of the
     * user login Principals contained in the WikiSession's Subject belongs to
     * an LDAP group contained in the role-base DN. The user DN is constructed
     * from the user Principal, where the <code>uid</code> of the user's DN is
     * the Principal name, and the rest of the DN is determined by
     * {@link #PROPERTY_LOGIN_ID_PATTERN}.
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
        String dn = m_userLoginIdPattern.replace( "{0}", uid ).trim();
        dn = dn.replace( "=", "\\3D" );

        try
        {
            DirContext ctx = new InitialLdapContext( m_jndiEnv, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            searchControls.setReturningAttributes( new String[0] );
            String filter = m_rolePattern.replace( "{0}", role.getName() );
            filter = filter.replace( "{1}", dn );
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
