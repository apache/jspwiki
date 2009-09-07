/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

package org.apache.wiki.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.*;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.servlet.ServletContext;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.ajax.JavaScriptResolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Authorizer;
import org.apache.wiki.auth.LdapConfig;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.LdapAuthorizer;
import org.apache.wiki.auth.user.LdapUserDatabase;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.util.CommentedProperties;
import org.apache.wiki.util.CryptoUtil;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;
import org.freshcookies.security.Keychain;

@HttpCache( allow = false )
public class InstallActionBean extends AbstractActionBean implements ValidationErrorHandler
{
    /**
     * Wrapper class that encapsulates a properties file as a Stripes-friendly
     * Map with String keys and values. The internal representation of the
     * Properties that are to be loaded is a {@link CommentedProperties} object,
     * which preserves the key order and comments. So that Stripes can access
     * and set entries in the Properties file, the PropertiesMap object
     * implements the Map<String,String> interface, which serves as a facade for
     * the properties. The keys in this Map have had their periods (.) escaped
     * by colons (:); otherwise, all keys and values are identical to those in
     * the Properties file. For example, JSP Expression Language expressions and
     * {@link Validate} expressions would request the key {@code
     * jspwiki.baseURL} this way: {@code jspwiki:baseURL}.
     */
    public static class PropertiesMap<K, V> implements Map<String, String>
    {
        /**
         * Converts a key in normal properties form into one that Stripes can
         * use, and caches it for later use.
         * 
         * @param key the key, which may contain periods
         * @return the key, with all periods (.) replaced by underscores (_).
         */
        private static String escapedKey( String key )
        {
            String escapedKey = key.replace( ".", "_" );
            return escapedKey;
        }

        private final File m_file;

        private final CommentedProperties m_props;

        private final Map<String, String> m_settings;

        private Map<String, String> m_escapedKeys = new HashMap<String, String>();

        /**
         * Constructs a new PropertiesMap object, whose contents will be loaded
         * from a specified File location.
         * 
         * @param file the location to load the properties from
         */
        public PropertiesMap( File file )
        {
            m_file = file;
            m_props = new CommentedProperties();
            m_settings = new HashMap<String, String>();
        }

        /**
         * {@inheritDoc}
         */
        public void clear()
        {
            m_settings.clear();
            m_props.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsKey( Object key )
        {
            return m_settings.containsKey( key.toString() );
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsValue( Object value )
        {
            return m_settings.containsValue( value.toString() );
        }

        /**
         * {@inheritDoc}
         */
        public Set<Entry<String, String>> entrySet()
        {
            return m_settings.entrySet();
        }

        /**
         * {@inheritDoc}
         */
        public String get( Object key )
        {
            return m_settings.get( key.toString() );
        }

        /**
         * Returns the absolute path of the File that represents the Properties
         * object.
         * 
         * @return the file path
         */
        public String getPath()
        {
            return m_file.getAbsolutePath();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isEmpty()
        {
            return m_settings.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        public Set<String> keySet()
        {
            return m_settings.keySet();
        }

        /**
         * Loads the Properties object from disk and populates the settings Map
         * with copies of the keys and values. Each key's period (.) is replaced
         * with a colon (:).
         * 
         * @throws IOException if the Properties file cannot be read for any
         *             reason
         */
        public void load() throws IOException
        {
            // Load the file from disk
            FileInputStream in = new FileInputStream( m_file );
            try
            {
                // Load old properties from disk
                m_props.load( in );
            }
            finally
            {
                if( in != null )
                {
                    in.close();
                }
            }

            // Copy the properties to the settings map, escaping the periods.
            for( Map.Entry<Object, Object> entry : m_props.entrySet() )
            {
                String key = entry.getKey().toString();
                String escapedKey = escapedKey( key );
                Object value = entry.getValue();
                m_settings.put( escapedKey, value == null ? null : value.toString() );
                m_escapedKeys.put( escapedKey, key );
            }
        }

        /**
         * {@inheritDoc}
         */
        public String put( String key, String value )
        {
            String unescapedKey = unescapedKey( key );
            m_props.setProperty( unescapedKey, value );
            return m_settings.put( key, value );
        }

        /**
         * {@inheritDoc}
         */
        public void putAll( Map<? extends String, ? extends String> t )
        {
            for( Map.Entry<? extends String, ? extends String> entry : t.entrySet() )
            {
                put( entry.getKey(), entry.getValue() );
            }
        }

        /**
         * {@inheritDoc}
         */
        public String remove( Object key )
        {
            m_props.remove( unescapedKey( key.toString() ) );
            return m_settings.remove( key );
        }

        /**
         * {@inheritDoc}
         */
        public int size()
        {
            return m_settings.size();
        }

        public void store() throws IOException
        {
            // Write the file back to disk
            FileOutputStream out = new FileOutputStream( m_file );
            try
            {
                m_props.store( out, null );
            }
            finally
            {
                if( out != null )
                {
                    out.close();
                }
            }
        }

        /**
         * Returns the String representation of the the Properties object.
         */
        public String toString()
        {
            return m_props.toString();
        }

        /**
         * {@inheritDoc}
         */
        public Collection<String> values()
        {
            return m_settings.values();
        }

        /**
         * Simply sanitizes any path which contains backslashes (sometimes
         * Windows users may have them) by expanding them to double-backslashes
         * 
         * @param key the key of the setting to sanitize
         */
        private void sanitizePath( String key )
        {
            String s = m_settings.get( key );
            s = TextUtil.replaceString( s, "\\", "\\\\" );
            s = s.trim();
            m_settings.put( key, s );
        }

        /**
         * Simply sanitizes any URL which contains backslashes (sometimes
         * Windows users may have them)
         * 
         * @param key the key of the setting to sanitize
         */
        private void sanitizeURL( String key )
        {
            String s = m_settings.get( key );
            s = TextUtil.replaceString( s, "\\", "/" );
            s = s.trim();
            if( !s.endsWith( "/" ) )
            {
                s = s + "/";
            }
            m_settings.put( key, s );
        }

        /**
         * Converts a key from the format Stripes can use into normal properties
         * form.
         * 
         * @param key the key, which may contain underscores
         * @return
         */
        private String unescapedKey( String key )
        {
            if( m_escapedKeys.containsKey( key ) )
            {
                return m_escapedKeys.get( key );
            }
            String unescapedKey = key.replace( "_", "." );
            m_escapedKeys.put( key, unescapedKey );
            return unescapedKey;
        }
    }

    public static final String PROP_ADMIN_PASSWORD_HASH = "admin.passwordHash";

    private static final String CONFIG_LOG_FILE = "log4j_appender_FileLog_File";

    private static final String CONFIG_WORK_DIR = "jspwiki_workDir";

    private static final String CONFIG_PAGE_DIR = "jspwiki_fileSystemProvider_pageDir";

    private static final String CONFIG_LDAP_SSL = "ldap_ssl";

    private static final String CONFIG_BASE_URL = "jspwiki_baseURL";

    private static final String CONFIG_ADMIN_PASSWORD_HASH = "admin_passwordHash";

    public static void main( String[] params )
    {
        for( int i = 1; i < 255; i++ )
        {
            char ch = (char) i;
            System.out.print( i + ": " + ch + " " );
            System.out.println( Character.isJavaIdentifierPart( ch ) );
        }
    }

    private String m_bindDNpassword = null;

    private String m_adminPassword = null;

    private Keychain m_keychain = null;

    private File m_keychainPath = null;

    private String m_keychainPassword = null;

    /**
     * PropertiesMap object for configuring {@code jspwiki.properties}
     * properties.
     */
    private Map<String, PropertiesMap<String, String>> m_properties = null;

    /**
     * Returns {@code true} if the administrative user had been created
     * previously.
     * 
     * @return the result
     */
    public boolean getAdminExists()
    {
        // See if the admin password was set already
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        if( jspwiki != null )
        {
            return jspwiki.containsKey( CONFIG_ADMIN_PASSWORD_HASH );
        }
        return false;
    }

    /**
     * Returns the admin password.
     * 
     * @return the password
     */
    public String getAdminPassword()
    {
        return m_adminPassword;
    }

    /**
     * Returns the LDAP bind-DN password.
     * 
     * @return the password
     */
    public String getBindDNpassword()
    {
        return m_bindDNpassword;
    }

    public Map<String, PropertiesMap<String, String>> getProperties()
    {
        return m_properties;
    }

    /**
     * Intercepts any validation errors generated by the various AJAX "text"
     * methods and returns them to the client as an array of JSON-encoded
     * strings.
     */
    public Resolution handleValidationErrors( ValidationErrors errors ) throws Exception
    {
        String event = getContext().getEventName();
        if( event.startsWith( "test" ) && errors.size() > 0 )
        {
            Set<String> errorStrings = new HashSet<String>();
            for( Map.Entry<String, List<ValidationError>> errorList : errors.entrySet() )
            {
                for( ValidationError error : errorList.getValue() )
                {
                    errorStrings.add( error.getMessage( getContext().getLocale() ) );
                }
            }
            return new JavaScriptResolution( errorStrings.toArray( new String[errorStrings.size()] ) );
        }
        return null;
    }

    /**
     * Pre-action that loads the JSPWiki properties file before user-supplied
     * parameters are bound to the ActionBean.
     * 
     * @return always returns {@code null}
     */
    @Before( stages = LifecycleStage.BindingAndValidation )
    public Resolution init() throws IOException, NoSuchAlgorithmException
    {
        // Load jspwiki.properties
        ServletContext context = getContext().getServletContext();
        String path = context.getRealPath( "/" );

        if( m_properties == null )
        {
            m_properties = new HashMap<String, PropertiesMap<String, String>>();
        }

        // Load jspwiki.properties
        PropertiesMap<String, String> jspwiki;
        jspwiki = new PropertiesMap<String, String>( new File( path, PropertyReader.DEFAULT_PROPERTYFILE ) );
        jspwiki.load();
        m_properties.put( "jspwiki", jspwiki );

        // Load log4j.properties
        PropertiesMap<String, String> log4j;
        log4j = new PropertiesMap<String, String>( new File( path, "/WEB-INF/classes/log4j.properties" ) );
        log4j.load();
        m_properties.put( "log4j", log4j );

        // Create a new keychain with random password
        m_keychainPassword = TextUtil.generateRandomPassword() + TextUtil.generateRandomPassword();
        jspwiki.put( AuthenticationManager.PROP_KEYCHAIN_PATH, "keychain" );
        m_keychainPath = new File( path, "/WEB-INF/keychain" );
        m_keychain = new Keychain();
        m_keychain.load( null, m_keychainPassword.toCharArray() );

        // Set some sensible defaults
        if( !jspwiki.containsKey( CONFIG_WORK_DIR ) )
        {
            jspwiki.put( CONFIG_WORK_DIR, "/tmp/" );
        }
        if( !jspwiki.containsKey( PropertiesMap.escapedKey( WikiEngine.PROP_ENCODING ) ) )
        {
            jspwiki.put( PropertiesMap.escapedKey( WikiEngine.PROP_ENCODING ), "UTF-8" );
        }
        if( !jspwiki.containsKey( CONFIG_LDAP_SSL ) )
        {
            jspwiki.put( CONFIG_LDAP_SSL, "false" );
        }
        if( !jspwiki.containsKey( CONFIG_ADMIN_PASSWORD_HASH ) )
        {
            m_adminPassword = TextUtil.generateRandomPassword() + TextUtil.generateRandomPassword();
        }

        return null;
    }

    /**
     * Default Stripes event handler method that loads up the current
     * configuration settings and forwards the user to the display JSP.
     * 
     * @return always returns a ForwardResolution to {@code /admin/Install.jsp}
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "install" )
    @WikiRequestContext( "install" )
    public Resolution install()
    {
        // Has admin password been set?
        if( !getAdminExists() )
        {
            List<Message> messages = getContext().getMessages();
            messages.add( new LocalizableMessage( "install.jsp.install.msg.admin.notexists" ) );
        }

        return new ForwardResolution( "/admin/Install.jsp" );
    }

    @HandlesEvent( "save" )
    public Resolution save() throws Exception
    {
        // Sanitize any paths
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        jspwiki.sanitizeURL( CONFIG_BASE_URL );
        jspwiki.sanitizePath( CONFIG_PAGE_DIR );
        jspwiki.sanitizePath( CONFIG_WORK_DIR );
        PropertiesMap<String, String> log4j = m_properties.get( "log4j" );
        log4j.sanitizePath( CONFIG_LOG_FILE );

        // Hash the admin password
        String passwordHash = CryptoUtil.getSaltedPassword( m_adminPassword.getBytes() );
        jspwiki.put( CONFIG_ADMIN_PASSWORD_HASH, passwordHash );

        // Save the keychain
        m_keychain.store( new FileOutputStream( m_keychainPath ), m_keychainPassword.toCharArray() );
        jspwiki.put( AuthenticationManager.PROP_KEYCHAIN_PASSWORD, m_keychainPassword );

        // Save each properties file
        jspwiki.store();
        log4j.store();

        // Restart the WikiEngine
        WikiEngine engine = getContext().getEngine();
        engine.restart();

        return new RedirectResolution( "/admin/InstallSuccess.jsp" );
    }

    /**
     * Sets the admin password. Must be 16 characters or more.
     * 
     * @param password the admin password
     */
    @Validate( required = true, minlength = 16 )
    public void setAdminPassword( String password )
    {
        m_adminPassword = password;
    }

    /**
     * Sets the LDAP bind-DN password. Optional when LDAP is selected for
     * authentication and user/role storage.
     * 
     * @param password
     */
    @Validate( required = false )
    public void setBindDNpassword( String password ) throws KeyStoreException
    {
        m_bindDNpassword = password;
        if( password != null )
        {
            KeyStore.Entry keypass = new Keychain.Password( m_bindDNpassword );
            m_keychain.setEntry( LdapConfig.KEYCHAIN_BIND_DN_ENTRY, keypass );
        }
    }

    /**
     * @param properties
     */
    @ValidateNestedProperties( {
                                @Validate( field = "jspwiki.jspwiki_applicationName", required = true, on = "save", label = "install.installer.default.appname" ),
                                @Validate( field = "jspwiki.jspwiki_baseURL", required = true, on = "save", label = "install.installer.validate.baseurl" ),
                                @Validate( field = "jspwiki.jspwiki_fileSystemProvider_pageDir", required = true, on = "save", label = "install.installer.default.pagedir" ),
                                @Validate( field = "jspwiki.jspwiki_workDir", required = true, on = "save", label = "install.installer.validate.workdir" ),
                                @Validate( field = "log4j.log4j_appender_FileLog_File", required = true, on = "save", label = "install.installer.validate.logdir" ),
                                @Validate( field = "jspwiki.ldap_connectionURL", required = true, on = { "testLdapConnection",
                                                                                                        "testLdapAuthentication",
                                                                                                        "testLdapUsers",
                                                                                                        "testLdapRoles" }, label = "properties.jspwiki.ldap_connectionURL" ),
                                @Validate( field = "jspwiki.ldap_bindDN", required = true, on = "testLdapAuthentication", label = "properties.jspwiki.ldap_bindDN" ),
                                @Validate( field = "jspwiki.ldap_userBase", required = true, on = "testLdapUsers", label = "properties.jspwiki.ldap_userBase" ),
                                @Validate( field = "jspwiki.ldap_roleBase", required = true, on = "testLdapRoles", label = "properties.jspwiki.ldap_roleBase" ) } )
    public void setProperties( Map<String, PropertiesMap<String, String>> properties )
    {
        m_properties = properties;
    }

    /**
     * AJAX event method that tests LDAP authentication based on the bind-DN
     * settings, returning any results as an array of JavaScript strings.
     * 
     * @return the results
     * @throws WikiSecurityException
     */
    @HandlesEvent( "testLdapAuthentication" )
    public Resolution testLdapAuthentication() throws WikiSecurityException
    {
        // Call the main connection method (the bind-DN property is
        // required for this method, though, so we are guaranteed to
        // do this with a username/password
        return testLdapConnection();
    }

    /**
     * AJAX event method that tests the connection to the LDAP server, returning
     * any results as an array of JavaScript strings.
     * 
     * @return the results
     * @throws WikiSecurityException
     */
    @HandlesEvent( "testLdapConnection" )
    public Resolution testLdapConnection() throws WikiSecurityException
    {
        List<String> messages = new ArrayList<String>();
        try
        {
            initLdapConnection( messages );
            messages.add( "Success!" );
        }
        catch( Exception e )
        {
            messages.add( "Error: " + e.getMessage() );
            if( e.getCause() != null )
            {
                messages.add( " Cause: " + e.getCause().getMessage() );
            }
        }
        Resolution r = new JavaScriptResolution( messages.toArray( new String[messages.size()] ) );
        return r;
    }

    /**
     * AJAX event method that tests the LDAP role lookups based on the
     * configured user base.
     * 
     * @return the results
     * @throws WikiSecurityException
     */
    @HandlesEvent( "testLdapRoles" )
    public Resolution testLdapRoleLoolup() throws WikiSecurityException
    {
        WikiEngine engine = getContext().getEngine();
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        List<String> messages = new ArrayList<String>();
        try
        {
            // Make sure keychain is unlocked
            Keychain keychain = engine.getAuthenticationManager().getKeychain();
            if( !keychain.isLoaded() )
            {
                keychain.load( null, m_keychainPassword.toCharArray() );
                Keychain.Password password = new Keychain.Password( m_bindDNpassword );
                keychain.setEntry( LdapConfig.KEYCHAIN_BIND_DN_ENTRY, password );
            }

            // Initialize a new user authorizer
            Authorizer authorizer = new LdapAuthorizer();
            authorizer.initialize( engine, jspwiki.m_props );

            // Count roles
            Principal[] principals = authorizer.getRoles();
            messages.add( "Found " + principals.length + " roles." );

            // Get first role details
            if( principals.length > 0 )
            {
                messages.add( "Details for first role..." );
                messages.add( "Name: " + principals[0].getName() );
            }
        }
        catch( Exception e )
        {
            messages.add( "Error: " + e.getMessage() );
            if( e.getCause() != null )
            {
                messages.add( " Cause: " + e.getCause().getMessage() );
            }
        }
        return new JavaScriptResolution( messages.toArray( new String[messages.size()] ) );
    }

    /**
     * AJAX event method that tests the LDAP user lookups based on the
     * configured user base. server, returning any results as an array of
     * JavaScript strings.
     * 
     * @return the results
     * @throws WikiSecurityException
     */
    @HandlesEvent( "testLdapUsers" )
    public Resolution testLdapUserLookup()
    {
        WikiEngine engine = getContext().getEngine();
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        List<String> messages = new ArrayList<String>();
        try
        {
            // Make sure keychain is unlocked
            Keychain keychain = engine.getAuthenticationManager().getKeychain();
            if( !keychain.isLoaded() )
            {
                keychain.load( null, m_keychainPassword.toCharArray() );
                Keychain.Password password = new Keychain.Password( m_bindDNpassword );
                keychain.setEntry( LdapConfig.KEYCHAIN_BIND_DN_ENTRY, password );
            }

            // Initialize a new user database
            UserDatabase db = new LdapUserDatabase();
            db.initialize( engine, jspwiki.m_props );

            // Count users
            Principal[] principals = db.getWikiNames();
            messages.add( "Found " + principals.length + " users." );

            // Get first user details
            if( principals.length > 0 )
            {
                UserProfile user = db.findByWikiName( principals[0].getName() );
                messages.add( "Details for first user..." );
                messages.add( "Uid: " + user.getUid() );
                messages.add( "Login name: " + user.getLoginName() );
                messages.add( "Full name: " + user.getFullname() );
                messages.add( "Wiki name: " + user.getWikiName() );
                messages.add( "E-mail: " + user.getEmail() );
            }
        }
        catch( Exception e )
        {
            messages.add( "Error: " + e.getMessage() );
            if( e.getCause() != null )
            {
                messages.add( " Cause: " + e.getCause().getMessage() );
            }
        }
        return new JavaScriptResolution( messages.toArray( new String[messages.size()] ) );
    }

    /**
     * Initializes an LDAP connection, using the bind-DN username and password
     * if supplied.
     * 
     * @return the initialized connection
     * @throws NamingException if a connection cannot be made to the LDAP server
     *             for any reason
     */
    private LdapContext initLdapConnection( List<String> messages ) throws NamingException
    {
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        LdapConfig config = LdapConfig.getInstance( m_keychain, jspwiki.m_props, new String[0] );
        Hashtable<String, String> env;
        if( !jspwiki.containsKey( "ldap_bindDN" ) )
        {
            env = config.newJndiEnvironment();
            messages.add( "Binding as anonymous user." );
        }
        else
        {
            String username = jspwiki.get( "ldap_bindDN" );
            env = config.newJndiEnvironment( username, m_bindDNpassword );
            Object principal = env.get( Context.SECURITY_PRINCIPAL );
            messages.add( "Binding with principal: " + principal.toString() );
        }
        return new InitialLdapContext( env, null );
    }
}
