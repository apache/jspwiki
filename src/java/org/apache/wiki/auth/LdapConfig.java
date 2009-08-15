package org.apache.wiki.auth;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.*;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.wiki.util.TextUtil;
import org.freshcookies.security.Keychain;

/**
 * Immutable holder for configuration of LDAP-related security modules.
 */
public class LdapConfig
{
    public static final Map<String,String> ACTIVE_DIRECTORY_CONFIG;
    
    public static final Map<String,String> OPEN_LDAP_CONFIG;

    public static final String KEYCHAIN_BIND_DN_ENTRY = "ldap.bindDNPassword";
    
    /**
     * Property that indicates what LDAP server configuration to use. Valid values include
     * <code>ad</code> for Active Directory and <code>openldap</code> for OpenLDAP.
     * If this value is set, the default settings for these configurations will be loaded.
     */
    public static final String PROPERTY_CONFIG = "ldap.config";
    
    /**
     * Property that indicates whether to use SSL for connecting to the LDAP
     * server. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    public static final String PROPERTY_SSL = "ldap.ssl";

    /**
     * Property that supplies the pattern for finding roles within the role base, e.g.
     * <code>(&(objectClass=groupOfUniqueNames)(cn={0}))</code>
     * .
     */
    public static final String PROPERTY_ROLE_PATTERN = "ldap.rolePattern";
    
    /**
     * Property that supplies the pattern for finding users within the role base
     * that possess a given role, e.g.
     * <code>(&(objectClass=groupOfUniqueNames)(cn={0})(uniqueMember={1}))</code>
     * .
     */
    public static final String PROPERTY_IS_IN_ROLE_PATTERN = "ldap.isInRolePattern";

    /**
     * Property that supplies the DN used to bind to the directory when looking
     * up users and roles.
     */
    public static final String PROPERTY_BIND_DN = "ldap.bindDN";

    /**
     * Property that supplies the connection URL for the LDAP server, e.g.
     * <code>ldap://127.0.0.1:4890/</code>. This property is also used by
     * {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    public static final String PROPERTY_CONNECTION_URL = "ldap.connectionURL";

    /**
     * Property that specifies the JNDI authentication type. Valid values are
     * the same as those for {@link Context#SECURITY_AUTHENTICATION}:
     * <code>none</code>, <code>simple</code>, <code>strong</code> or
     * <code>DIGEST-MD5</code>. The default is <code>simple</code> if SSL is
     * specified, and <code>DIGEST-MD5</code> otherwise. This property is also
     * used by {@link org.apache.wiki.auth.login.LdapLoginModule}.
     */
    public static final String PROPERTY_AUTHENTICATION = "ldap.authentication";
    
    /**
     * Property that specifies the login name attribute for user objects.
     * By default, this is <code>uid</code>.
     */
    public static final String PROPERTY_USER_LOGIN_NAME_ATTRIBUTE = "ldap.user.loginName";
    
    /**
     * Property that specifies the base DN where users are contained,
     * <em>e.g.,</em> <code>ou=people,dc=jspwiki,dc=org</code>. This DN is
     * searched recursively.
     */
    public static final String PROPERTY_USER_BASE = "ldap.userBase";
    
    /**
     * Property that specifies the DN pattern for finding users within the
     * user base, <em>e.g.,</em>
     * <code>(&(objectClass=inetOrgPerson)(uid={0}))</code>
     */
    public static final String PROPERTY_USER_PATTERN = "ldap.userPattern";

    
    /**
     * Property that specifies the pattern for the username used to log in to the
     * LDAP server. This pattern maps the username supplied at login time by the
     * user to a username format the LDAP server can recognized. Usually this is
     * a pattern that produces a full DN, for example
     * <code>uid={0},ou=people,dc=jspwiki,dc=org</code>. However, sometimes (as
     * with Active Directory 2003 and later) only the userid is used, in which
     * case the principal will simply be <code>{0}</code>. The default value
     * if not supplied is <code>{0}</code>.
     */
    public static final String PROPERTY_LOGIN_ID_PATTERN = "ldap.loginIdPattern";
    
    public static final String PROPERTY_USER_OBJECT_CLASS = "ldap.user.objectClass";
    
    /**
     * Property that supplies the base DN where roles are contained, e.g.
     * <code>ou=roles,dc=jspwiki,dc=org</code>.
     */
    public static final String PROPERTY_ROLE_BASE = "ldap.roleBase";

    public final String connectionUrl;

    public final String roleBase;
    
    public final String rolePattern;

    public final String isInRolePattern;

    public final String bindDN;

    public final String ssl;

    public final String authentication;
    
    public final String userBase;
    
    public final String loginIdPattern;
    
    public final String userPattern;
    
    public final String userLoginNameAttribute;

    public final String userObjectClass;
    
    private final Set<String> m_configured = new HashSet<String>();

    private Keychain m_keychain;
    
    static
    {
        // Active Directory 2000+ defaults
        Map<String,String> options = new HashMap<String,String>();
        options.put( PROPERTY_IS_IN_ROLE_PATTERN, "(&(objectClass=group)(cn={0})(member={1}))" );
        options.put( PROPERTY_LOGIN_ID_PATTERN, "{0}" );
        options.put( PROPERTY_ROLE_PATTERN, "(&(objectClass=group)(cn={0}))" );
        options.put( PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, "sAMAccountName" );
        options.put( PROPERTY_USER_OBJECT_CLASS, "person" );
        options.put( PROPERTY_USER_PATTERN, "(&(objectClass=person)(sAMAccountName={0}))" );
        ACTIVE_DIRECTORY_CONFIG = Collections.unmodifiableMap( options );
        
        // OpenLDAP defaults
        options = new HashMap<String,String>();
        options.put( PROPERTY_IS_IN_ROLE_PATTERN, "(&(&(objectClass=groupOfUniqueNames)(cn={0}))(uniqueMember={1}))" );
        options.put( PROPERTY_ROLE_PATTERN, "(&(objectClass=groupOfUniqueNames)(cn={0}))" );
        options.put( PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, "uid" );
        options.put( PROPERTY_USER_OBJECT_CLASS, "inetOrgPerson" );
        options.put( PROPERTY_USER_PATTERN, "(&(objectClass=inetOrgPerson)(uid={0}))" );
        OPEN_LDAP_CONFIG = Collections.unmodifiableMap( options );
    }

    private String getProperty( Map<? extends Object,? extends Object> props, String property, String defaultValue )
    {
        String shortProperty = property;
        String longProperty = AuthenticationManager.PREFIX_LOGIN_MODULE_OPTIONS + property;
        String value = (String)props.get( property );
        if ( value == null )
        {
            property = longProperty;
            value = (String)props.get( property );
        }
        if( value != null && value.length() > 0 )
        {
            m_configured.add( shortProperty );
            m_configured.add( longProperty );
            return props.get( property ).toString().trim();
        }
        return defaultValue;
    }
    
    public String getBindDNPassword() throws KeyStoreException
    {
        if( m_keychain == null )
        {
            throw new KeyStoreException( "LdapConfig was initialized without a keychain!" );
        }
        KeyStore.Entry password = m_keychain.getEntry( LdapConfig.KEYCHAIN_BIND_DN_ENTRY );
        if( password instanceof Keychain.Password )
        {
            return ((Keychain.Password) password).getPassword();
        }
        return null;
    }

    public static LdapConfig getInstance( Keychain keychain, Map<? extends Object,? extends Object> props, String[] requiredProperties )
    {
        return new LdapConfig( keychain, props, requiredProperties );
    }

    private LdapConfig( Keychain keychain, Map<? extends Object,? extends Object> props, String[] requiredProperties )
    {
        // Basic connection properties
        connectionUrl = getProperty( props, PROPERTY_CONNECTION_URL, null );

        // Binding DN properties
        m_keychain = keychain;
        bindDN = getProperty( props, PROPERTY_BIND_DN, null );
        
        // User lookup properties
        userBase = getProperty( props, PROPERTY_USER_BASE, null );
        userPattern = getProperty( props, PROPERTY_USER_PATTERN, null );

        // Role lookup properties
        roleBase = getProperty( props, PROPERTY_ROLE_BASE, null );
        rolePattern = getProperty( props, PROPERTY_ROLE_PATTERN, null );
        isInRolePattern = getProperty( props, PROPERTY_IS_IN_ROLE_PATTERN, null );

        // Optional security properties
        String parsedSsl = getProperty( props, PROPERTY_SSL, null );
        ssl = (parsedSsl != null && TextUtil.isPositive( parsedSsl )) ? "ssl" : "none";
        String parsedAuthentication = getProperty( props, PROPERTY_AUTHENTICATION, null );
        if( parsedAuthentication == null )
        {
            authentication = "ssl".equals( ssl ) ? "simple" : "DIGEST-MD5";
        }
        else
        {
            authentication = parsedAuthentication;
        }
        loginIdPattern = getProperty( props, PROPERTY_LOGIN_ID_PATTERN, "{0}" );

        // Optional user object attributes
        userObjectClass = getProperty( props, PROPERTY_USER_OBJECT_CLASS, "inetOrgPerson" );
        userLoginNameAttribute = getProperty( props, PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, "uid" );

        // Validate everything
        for( String property : requiredProperties )
        {
            if( !m_configured.contains( property ) )
            {
                throw new IllegalArgumentException( "Property " + property + " is required!" );
            }
        }
    }
    
    public Hashtable<String,String> newJndiEnvironment() throws NamingException
    {
        // If we need a Bind DN and Keychain is loaded, get the bind DN and password
        String username = bindDN;
        String password = null;
        if( username != null )
        {
            try
            {
                password = getBindDNPassword();
            }
            catch( KeyStoreException e )
            {
                e.printStackTrace();
                throw new NamingException( "Could not build JNDI environment: " + e.getMessage() );
            }
        }
        return newJndiEnvironment( username, password );
    }
    
    /**
     * Builds a JNDI environment hashtable for authenticating to the LDAP
     * server. The hashtable is built using information extracted from the
     * options map supplied to the LoginModule via
     * {@link #initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, Map, Map)}
     * . The username and password parameters supply the LDAP credentials.
     * 
     * @param username the user's distinguished name (DN), used for
     *            authentication
     * @param password the password
     * @return the constructed hash table
     */
    public Hashtable<String,String> newJndiEnvironment( String username, String password )
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        // LDAP server to authenticate to
        env.put( Context.PROVIDER_URL, connectionUrl );

        // Add credentials if supplied
        if ( username != null )
        {
            env.put( Context.SECURITY_PRINCIPAL, username );
            env.put( Context.SECURITY_CREDENTIALS, password );
        }
        else
        {
            
        }

        // Use SSL?
        env.put( Context.SECURITY_PROTOCOL, ssl );

        // Authentication type (simple, DIGEST-MD5, etc)
        env.put( Context.SECURITY_AUTHENTICATION, authentication );

        return env;
   }
    
    public static String getFullName( Attributes attributes ) throws NamingException
    {
        boolean hasCommonName = attributes.get( "cn" ) != null;
        boolean hasSurname = attributes.get( "sn" ) != null;
        boolean hasGivenName = attributes.get( "givenName" ) != null;

        String fullName = null;
        if( hasGivenName && hasSurname )
        {
            fullName = attributes.get( "givenName" ).get( 0 ) + " " + attributes.get( "sn" ).get( 0 );
        }
        else if( hasCommonName )
        {
            fullName = attributes.get( "cn" ).get( 0 ).toString();
        }
        else
        {
            throw new NamingException( "User did not have a givenName+sn or cn." );
        }
        return fullName;
    }

    private final Map<String, String> m_userDns = new HashMap<String, String>();

    private static final SearchControls SEARCH_CONTROLS;
    
    static
    {
        SEARCH_CONTROLS = new SearchControls();
        SEARCH_CONTROLS.setSearchScope( SearchControls.SUBTREE_SCOPE );
    }
    
    public String getUserDn( String loginName ) throws NamingException
    {
        String dn = m_userDns.get( loginName );
        if( dn == null )
        {
            loginName = sanitizeDn( loginName );
            String userFinder = userPattern.replace( "{0}", loginName );
            Hashtable<String, String> env = newJndiEnvironment();

            // Find the user
            DirContext ctx = new InitialLdapContext( env, null );
            NamingEnumeration<SearchResult> users = ctx.search( userBase, userFinder, SEARCH_CONTROLS );
            if( users.hasMore() )
            {
                dn = users.next().getNameInNamespace();
                m_userDns.put( loginName, dn );
            }
        }
        return dn;
    }

    /**
     * See http://blogs.sun.com/shankar/entry/what_is_ldap_injection
     * 
     * @param index
     * @return
     */
    private String sanitizeDn( String index )
    {
        index = index.replace( " ", " " );
        return index.replace( "=", "\\3D" );
    }
}
