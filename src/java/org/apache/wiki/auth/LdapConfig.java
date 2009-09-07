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
 * <p>
 * Immutable configuration object that is used to initialize
 * {@link org.apache.wiki.auth.authorize.LdapAuthorizer} and
 * {@link org.apache.wiki.auth.user.LdapUserDatabase}. LdapConfig contains
 * configuration information used to initialize connections to an LDAP server,
 * search specified base DNs for user and role (LDAP group) objects, and look up
 * user attributes.
 * </p>
 * <p>
 * LdapConfig objects are initialized via the static factory method
 * {@link #getInstance(Keychain, Map, String[])}. The parameters passed to the
 * factory method are:
 * </p>
 * <ul>
 * <li>a Map containing key/value String pairs that supply configuration
 * property values. Possible properties include all of the static members in
 * this class prefixed {@code PROPERTY_}, such as
 * {@link #PROPERTY_AUTHENTICATION}.</li>
 * <li>a {@link Keychain} object that optionally stores the password used for
 * binding to the LDAP server, if a "bind DN" property was set by
 * {@link #PROPERTY_BIND_DN}.</li>
 * <li>an array of String objects that supply the property names that must be
 * configured in order for the LdapConfig initialization to succeed. The
 * required properties are set by the calling program to account for the fact
 * that different classes need different things. For example, LdapAuthorizer
 * requires these properties to be set: {@link #PROPERTY_CONNECTION_URL},
 * {@link #PROPERTY_ROLE_BASE} and {@link #PROPERTY_IS_IN_ROLE_FILTER}.</li>
 * </ul>
 * <p>
 * <p>
 * For callers who need to set properties directly rather than use one of the
 * shortcut configs, some of the more important configuration properties
 * include:
 * </p>
 * <ul>
 * <li>{@link #PROPERTY_CONNECTION_URL} - the connection string for the LDAP
 * server, for example {@code ldap://ldap.jspwiki.org:389/}.</li>
 * <li>{@link #PROPERTY_LOGIN_ID_PATTERN} - optional string pattern indicating
 * how the login id should be formatted into a credential the LDAP server will
 * understand. The exact credential pattern varies by LDAP server. OpenLDAP
 * expects login IDs that match a distinguished name. Active Directory, on the
 * other hand, requires just the "short" login ID that is not in DN format. The
 * user ID supplied during the login will be substituted into the
 * {@code \{0\}} token in this pattern, and the user base will be 
 * substituted into the {@code \{1\}} token. Valid examples of login ID patterns
 * include {@code uid=\{0\},\{1\}} (for OpenLDAP) and
 * {@code \{0\}} (for Active Directory).</li>
 * <li>{@link #PROPERTY_USER_BASE} - the distinguished name of the base location
 * where user objects are located. This is generally an organizational unit (OU)
 * DN, such as {@code ou=people,dc=jspwiki,dc=org}. The user base and all
 * of its subtrees will be searched. For directories that contain multiple OUs
 * where users are located, use a higher-level base location (e.g.,
 * {@code dc=jspwiki,dc=org}).</li>
 * <li>{@link #PROPERTY_USER_FILTER} - an RFC 2254 search filter string used for
 * locating the actual user object within the user base. The user ID supplied
 * during the login will be substituted into the {@code \{0\}} token in this
 * filter, if it contains one. Only the first match will be selected, so it is
 * important that this filter selects unique objects. For example, if the user
 * filter is {@code (&(objectClass=inetOrgPerson)(uid=\{0\}))} and the user
 * name supplied during login is {@code fflintstone}, the the first object
 * within {@link #PROPERTY_USER_BASE} that matches the filter
 * {@code (&(objectClass=inetOrgPerson)(uid=fflintstone))} will be
 * selected. A suitable value for this property that works with Active Directory
 * 2000 and later is {@code (&(objectClass=person)(sAMAccountName=\{0\}))}.</li>
 * <li>{@link #PROPERTY_SSL} - Optional parameter that specifies whether to use
 * SSL when connecting to the LDAP server. Values like {@code true} or
 * {@code on} indicate that SSL should be used. If this parameter is not
 * supplied, SSL will not be used.</li>
 * <li>{@link #PROPERTY_AUTHENTICATION} - Optional parameter that specifies the
 * type of authentication method to be used. Valid values include
 * {@code simple} for plaintext username/password, and
 * {@code DIGEST-MD5} for digested passwords. Note that if SSL is not used,
 * for safety reasons this method will default to {@code DIGEST-MD5} to
 * prevent password interception.</li>
 * </ul>
 * <p>
 * LdapConfig objects are immutable and therefore thread-safe.
 * </p>
 */
public class LdapConfig
{
    /**
     * The name of the {@link Keychain} entry that supplies the password used by
     * the "bind DN", if one was specified by {@link #PROPERTY_BIND_DN}.
     */
    public static final String KEYCHAIN_BIND_DN_ENTRY = "ldap.bindDNPassword";

    /**
     * Property that specifies the JNDI authentication type. Valid values are
     * the same as those for {@link Context#SECURITY_AUTHENTICATION}: {@code
     * none}, {@code simple}, {@code strong} or {@code DIGEST-MD5}. The default
     * is {@code simple} if SSL is specified, and {@code DIGEST-MD5} otherwise.
     */
    public static final String PROPERTY_AUTHENTICATION = "ldap.authentication";

    /**
     * Property that supplies the DN used to bind to the directory when looking
     * up users and roles.
     */
    public static final String PROPERTY_BIND_DN = "ldap.bindDN";

    /**
     * Property that indicates what LDAP server configuration to use. Valid
     * values include {@code ad} for Active Directory and {@code openldap} for
     * OpenLDAP. If this value is set, the default settings for these
     * configurations will be loaded.
     */
    public static final String PROPERTY_CONFIG = "ldap.config";

    /**
     * Property that supplies the connection URL for the LDAP server, e.g.
     * {@code ldap://127.0.0.1:4890/}.
     */
    public static final String PROPERTY_CONNECTION_URL = "ldap.connectionURL";

    /**
     * Property that supplies the filter for finding users within the role base
     * that possess a given role, e.g. {@code
     * (&(objectClass=groupOfUniqueNames)(cn=\{0\})(uniqueMember=\{1\}))} .
     */
    public static final String PROPERTY_IS_IN_ROLE_FILTER = "ldap.isInRoleFilter";

    /**
     * Property that specifies the pattern for the username used to log in to
     * the LDAP server. This pattern maps the username supplied at login time by
     * the user to a username format the LDAP server can recognized. The Usually
     * this is a pattern that produces a full DN, for example {@code uid=\{0\}
     * ,\{1\}}. However, sometimes (as with Active
     * Directory 2003 and later) only the userid is used, in which case the
     * principal will simply be \{0\} . The default value if not supplied is
     * \{0\} .
     */
    public static final String PROPERTY_LOGIN_ID_PATTERN = "ldap.loginIdPattern";

    /**
     * Property that supplies the base DN where roles are contained, e.g.
     * {@code ou=roles,dc=jspwiki,dc=org}.
     */
    public static final String PROPERTY_ROLE_BASE = "ldap.roleBase";

    /**
     * Property that indicates whether to use SSL for connecting to the LDAP
     * server.
     */
    public static final String PROPERTY_SSL = "ldap.ssl";

    /**
     * Property that specifies the base DN where users are contained,
     * <em>e.g.,</em> {@code ou=people,dc=jspwiki,dc=org}. This DN is searched
     * recursively.
     */
    public static final String PROPERTY_USER_BASE = "ldap.userBase";

    /**
     * Property that specifies the login name attribute for user objects. By
     * default, this is {@code uid}.
     */
    public static final String PROPERTY_USER_LOGIN_NAME_ATTRIBUTE = "ldap.user.loginName";

    /**
     * Property that supplies the class name for user objects. By default, this
     * is {@code inetOrgPerson}.
     */
    public static final String PROPERTY_USER_OBJECT_CLASS = "ldap.user.objectClass";

    /**
     * Property that specifies the filter for finding users within the user
     * base, <em>e.g.,</em> {@code (&(objectClass=inetOrgPerson)(uid= 0}))}
     */
    public static final String PROPERTY_USER_FILTER = "ldap.userFilter";

    private static final Map<Default,LdapConfig> CONFIGS = new HashMap<Default,LdapConfig>();

    private static final SearchControls SEARCH_CONTROLS;

    static
    {
        SEARCH_CONTROLS = new SearchControls();
        SEARCH_CONTROLS.setSearchScope( SearchControls.SUBTREE_SCOPE );

        // Active Directory 2000+ defaults
        Map<String, String> options = new HashMap<String, String>();
        options.put( PROPERTY_IS_IN_ROLE_FILTER, "(&(&(objectClass=group)(cn={0}))(member={1}))" );
        options.put( PROPERTY_LOGIN_ID_PATTERN, "{0}" );
        options.put( PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, "sAMAccountName" );
        options.put( PROPERTY_USER_OBJECT_CLASS, "person" );
        options.put( PROPERTY_USER_FILTER, "(&(objectClass=person)(sAMAccountName={0}))" );
        LdapConfig config = new LdapConfig( null,options,new String[0] );
        CONFIGS.put( Default.ACTIVE_DIRECTORY, config );

        // OpenLDAP defaults
        options = new HashMap<String, String>();
        options.put( PROPERTY_IS_IN_ROLE_FILTER, "(&(&(objectClass=groupOfUniqueNames)(cn={0}))(uniqueMember={1}))" );
        options.put( PROPERTY_LOGIN_ID_PATTERN, "uid={0},{1}" );
        options.put( PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, "uid" );
        options.put( PROPERTY_USER_OBJECT_CLASS, "inetOrgPerson" );
        options.put( PROPERTY_USER_FILTER, "(&(objectClass=inetOrgPerson)(uid={0}))" );
        config = new LdapConfig( null,options,new String[0] );
        CONFIGS.put( Default.OPEN_LDAP, config );
    }

    /**
     * Escapes a string so that it conforms to an RFC2254-compliant
     * LDAP search filter. See
     * http://blogs.sun.com/shankar/entry/what_is_ldap_injection
     * 
     * @param dn the DN to escape
     * @return the escaped DN
     */
    public static String escapeFilterString( String dn )
    {
        StringBuilder s = new StringBuilder();
        for( char c : dn.toCharArray() )
        {
            switch( c )
            {
                case '=': {
                    s.append( '\\' );
                    s.append( '3' );
                    s.append( 'd' );
                    break;
                }
                case '(': {
                    s.append( '\\' );
                    s.append( '2' );
                    s.append( '8' );
                    break;
                }
                case ')': {
                    s.append( '\\' );
                    s.append( '2' );
                    s.append( '9' );
                    break;
                }
                case '&': {
                    s.append( '\\' );
                    s.append( '2' );
                    s.append( '6' );
                    break;
                }
                case '|': {
                    s.append( '\\' );
                    s.append( '7' );
                    s.append( 'c' );
                    break;
                }
                case '>': {
                    s.append( '\\' );
                    s.append( '3' );
                    s.append( 'e' );
                    break;
                }
                case '<': {
                    s.append( '\\' );
                    s.append( '3' );
                    s.append( 'c' );
                    break;
                }
                case '~': {
                    s.append( '\\' );
                    s.append( '7' );
                    s.append( 'e' );
                    break;
                }
                case '*': {
                    s.append( '\\' );
                    s.append( '2' );
                    s.append( 'a' );
                    break;
                }
                case '/': {
                    s.append( '\\' );
                    s.append( '2' );
                    s.append( 'f' );
                    break;
                }
                case '\\': {
                    s.append( '\\' );
                    s.append( '5' );
                    s.append( 'c' );
                    break;
                }
                default: {
                    s.append( c );
                }
            }
        }
        return s.toString();
    }

    /**
     * Typesafe enumeration indicating which configuration to use.
     */
    public enum Default { 
        /** Active Directory 2000 and higher. */
        ACTIVE_DIRECTORY, 
        /** OpenLDAP. */
        OPEN_LDAP;
    }
    
    /**
     * For a supplied LDAP user object, returns the user's equivalent JSPWiki
     * "full name." The full name will be equal to the user's first name (
     * {@code givenName}) + last name ({@code sn}) attributes, separated by a
     * space, <em>or</em> the user object's common name ({@code cn}) attribute,
     * in that order of preference.
     * 
     * @param attributes the attributes supplying the common name, surname
     *            and/or given name
     * @return the user's JSPWiki full name
     * @throws NamingException if the attributes cannot be retrieved for any
     *             reason
     */
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

    /**
     * Factory method that creates a new LdapConfig object.
     * 
     * @param keychain the Keychain that stores the password for the "bind DN",
     *            if one is used for this config
     * @param props the properties object containing the initialization
     *            parameters for the config
     * @param requiredProperties the properties that are must be set in order
     *            for configuration to succeed
     * @return the initialized LdapConfig object
     */
    public static LdapConfig getInstance( Keychain keychain, Map<? extends Object, ? extends Object> props,
                                          String[] requiredProperties )
    {
        return new LdapConfig( keychain, props, requiredProperties );
    }

    /**
     * The configured value that specifies the JNDI connection URL for the LDAP
     * server.
     * 
     * @see #PROPERTY_CONNECTION_URL
     */
    public final String connectionUrl;

    /**
     * The configured base DN to be searched for group objects that supply
     * roles.
     * 
     * @see #PROPERTY_ROLE_BASE
     */
    public final String roleBase;

    /**
     * The configured filter for finding whether a user belongs to a particular
     * group.
     * 
     * @link #PROPERTY_IS_IN_ROLE_FILTER
     */
    public final String isInRoleFilter;

    /**
     * The distinguished name used for connecting to the LDAP server.
     * 
     * @link #bindDN
     */
    public final String bindDN;

    /**
     * The configured value of the SSL property.
     * 
     * @see #PROPERTY_SSL
     */
    public final String ssl;

    /**
     * The configured value of the authentication protocol to be used.
     * 
     * @see #PROPERTY_AUTHENTICATION
     */
    public final String authentication;

    /**
     * The configured base DN to be searched for user objects.
     * 
     * @see #PROPERTY_USER_BASE
     */
    public final String userBase;

    /**
     * The configured pattern used to create login IDs for authenticating to
     * LDAP.
     * 
     * @see #PROPERTY_LOGIN_ID_PATTERN
     */
    public final String loginIdPattern;

    /**
     * The configured filter for finding user objects within the user base DN.
     * 
     * @see #PROPERTY_USER_FILTER
     */
    public final String userFilter;

    /**
     * The configured attribute name that supplies user login names.
     * 
     * @see #PROPERTY_USER_LOGIN_NAME_ATTRIBUTE
     */
    public final String userLoginNameAttribute;

    /**
     * The configured class name of the user object.
     * 
     * @see #PROPERTY_USER_OBJECT_CLASS
     */
    public final String userObjectClass;

    private final Set<String> m_configured = new HashSet<String>();

    private final Keychain m_keychain;

    private final Map<String, String> m_userDns = new HashMap<String, String>();

    /**
     * Private constructor that creates an immutable LdapConfig object.
     * 
     * @param keychain the Keychain that stores the password for the "bind DN",
     *            if one is used for this config
     * @param props the properties object containing the initialization
     *            parameters for the config
     * @param requiredProperties the properties that are must be set in order
     *            for configuration to succeed
     * @return the initialized LdapConfig object
     */
    private LdapConfig( Keychain keychain, Map<? extends Object, ? extends Object> props, String[] requiredProperties )
    {
        // Set defaults
        String defaultIsInRoleFilter = null;
        String defaultLoginIdPattern = "{0}";
        String defaultUserLoginNameAttribute = "uid";
        String defaultUserObjectClass = "inetOrgPerson";
        String defaultUserFilter = null;
        
        // Did user select a config shortcut for AD or OpenLdap?
        String config = (String) props.get( PROPERTY_CONFIG );
        if ( config != null )
        {
            try
            {
                Default configEnum = Default.valueOf( config ); 
                LdapConfig defaults = CONFIGS.get( configEnum );
                defaultIsInRoleFilter = defaults.isInRoleFilter;
                defaultLoginIdPattern = defaults.loginIdPattern;
                defaultUserLoginNameAttribute = defaults.userLoginNameAttribute;
                defaultUserObjectClass = defaults.userObjectClass;
                defaultUserFilter = defaults.userFilter;
            }
            catch( IllegalArgumentException e )
            {
                throw new IllegalArgumentException( "'" + config + 
                  "' is not a valid config value for " + PROPERTY_CONFIG + ".", e );
            }
        }

        // Basic connection properties
        connectionUrl = getProperty( props, PROPERTY_CONNECTION_URL, null );

        // Binding DN properties
        m_keychain = keychain;
        bindDN = getProperty( props, PROPERTY_BIND_DN, null );

        // User lookup properties
        userBase = getProperty( props, PROPERTY_USER_BASE, null );
        userFilter = getProperty( props, PROPERTY_USER_FILTER, defaultUserFilter );

        // Role lookup properties
        roleBase = getProperty( props, PROPERTY_ROLE_BASE, null );
        isInRoleFilter = getProperty( props, PROPERTY_IS_IN_ROLE_FILTER, defaultIsInRoleFilter );

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
        String parsedLoginIdPattern = getProperty( props, PROPERTY_LOGIN_ID_PATTERN, defaultLoginIdPattern );
        loginIdPattern = userBase == null ? parsedLoginIdPattern : parsedLoginIdPattern.replace( "{1}", userBase );

        // Optional user object attributes
        userObjectClass = getProperty( props, PROPERTY_USER_OBJECT_CLASS, defaultUserObjectClass );
        userLoginNameAttribute = getProperty( props, PROPERTY_USER_LOGIN_NAME_ATTRIBUTE, defaultUserLoginNameAttribute );

        // Validate everything
        for( String property : requiredProperties )
        {
            if( !m_configured.contains( property ) )
            {
                throw new IllegalArgumentException( "Property " + property + " is required!" );
            }
        }
    }

    public String getUserDn( String loginName ) throws NamingException
    {
        return getUserDn( loginName, null );
    }

    public synchronized String getUserDn( String loginName, DirContext ctx ) throws NamingException
    {
        String dn = m_userDns.get( loginName );
        if( dn == null )
        {
            String userFinder = userFilter.replace( "{0}", escapeFilterString( loginName ) );
            Hashtable<String, String> env = newJndiEnvironment();

            // Find the user
            if( ctx == null )
            {
                ctx = new InitialLdapContext( env, null );
            }
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
     * Builds a JNDI environment hashtable for performing an operation on the
     * LDAP server. The hashtable is built using the properties used to
     * initialize the LdapConfig object. If property {@link #PROPERTY_BIND_DN}
     * was set, that DN will be used as the authentication principal. The
     * password will be obtained from the Keychain.
     * 
     * @return the constructed hash table
     * @throws NamingException
     */
    public Hashtable<String, String> newJndiEnvironment() throws NamingException
    {
        // If we need a Bind DN and Keychain is loaded, get the bind DN and
        // password
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
     * Builds a JNDI environment hashtable for performing an operation on the
     * LDAP server. The hashtable is built using the properties used to
     * initialize the LdapConfig object. The username and password parameters
     * supply the LDAP credentials used with the connection.
     * 
     * @param username the user's distinguished name (DN), used for
     *            authentication
     * @param password the password
     * @return the constructed hash table
     */
    public Hashtable<String, String> newJndiEnvironment( String username, String password )
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        // Create fully qualified username
        if ( loginIdPattern != null && username != null )
        {
            username = loginIdPattern.replace( "{0}", username );
        }

        // LDAP server to authenticate to
        env.put( Context.PROVIDER_URL, connectionUrl );

        // Add credentials if supplied
        if( username != null )
        {
            env.put( Context.SECURITY_PRINCIPAL, username );
        }
        if ( password != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, password );
        }

        // Use SSL?
        env.put( Context.SECURITY_PROTOCOL, ssl );

        // Authentication type (simple, DIGEST-MD5, etc)
        env.put( Context.SECURITY_AUTHENTICATION, authentication );

        // Follow referrals
        env.put( Context.REFERRAL, "follow" );

        return env;
    }

    /**
     * Retrieves the password to be used with a bind DN.
     * 
     * @return the plaintext password
     * @throws KeyStoreException if the Keychain was not supplied during
     *             initialization, or if the lookup fails for any reason
     */
    private String getBindDNPassword() throws KeyStoreException
    {
        if( m_keychain == null )
        {
            throw new KeyStoreException( "LdapConfig was initialized without a keychain!" );
        }
        KeyStore.Entry password = m_keychain.getEntry( LdapConfig.KEYCHAIN_BIND_DN_ENTRY );
        if( password != null && password instanceof Keychain.Password )
        {
            return ((Keychain.Password) password).getPassword();
        }
        return null;
    }

    /**
     * Looks up and returns an initialization property.
     * 
     * @param props the Map used to initialize the LdapConfig object
     * @param property the property name to search for
     * @param defaultValue the default value if the property is not found
     * @return the property value if found, and the default if not
     */
    private String getProperty( Map<? extends Object, ? extends Object> props, String property, String defaultValue )
    {
        String shortProperty = property;
        String value = (String) props.get( property );
        if( value != null && value.length() > 0 )
        {
            m_configured.add( shortProperty );
            return props.get( property ).toString().trim();
        }
        
        // Return the default
        if ( defaultValue != null )
        {
            m_configured.add( shortProperty );
        }
        return defaultValue;
    }
}
