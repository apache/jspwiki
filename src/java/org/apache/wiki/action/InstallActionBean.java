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
import org.apache.wiki.auth.authorize.WebContainerAuthorizer;
import org.apache.wiki.auth.user.LdapUserDatabase;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.auth.user.XMLUserDatabase;
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

    private static final String CONFIG_PAGE_DIR = "priha_provider_defaultProvider_directory";

    private static final String CONFIG_USERDATABASE = "jspwiki_userdatabase";

    private static final String CONFIG_AUTHORIZER = "jspwiki_authorizer";

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

    private String m_bindPassword = null;

    private String m_adminPassword = null;

    private Keychain m_keychain = null;

    private File m_keychainPath = null;

    private String m_logDirectory = null;

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
     * Returns the LDAP binding password.
     * 
     * @return the password
     */
    public String getBindPassword()
    {
        return m_bindPassword;
    }

    /**
     * Returns the directory where log files are stored.
     * 
     * @return the directory
     */
    public String getLogDirectory()
    {
        return m_logDirectory;
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

        // Load priha.properties
        PropertiesMap<String, String> priha;
        priha = new PropertiesMap<String, String>( new File( path, "/WEB-INF/classes/priha.properties" ) );
        priha.load();
        m_properties.put( "priha", priha );

        // Get the log directory
        m_logDirectory = log4j.get( CONFIG_LOG_FILE );
        if( m_logDirectory == null )
        {
            m_logDirectory = System.getProperty( "java.io.tmpdir" );
        }
        File logs = new File( m_logDirectory );
        if( logs.exists() && !logs.isDirectory() )
        {
            logs = logs.getParentFile();
            if( logs == null )
            {
                logs = new File( System.getProperty( "java.io.tmpdir" ) );
            }
        }
        m_logDirectory = logs.getAbsolutePath();

        // Load the Keychain (or create new one with random password)
        initKeychain( path, jspwiki );

        // Set some sensible defaults
        if( !jspwiki.containsKey( CONFIG_USERDATABASE ) )
        {
            jspwiki.put( CONFIG_USERDATABASE, XMLUserDatabase.class.getName() );
        }
        if( !jspwiki.containsKey( CONFIG_WORK_DIR ) )
        {
            jspwiki.put( CONFIG_WORK_DIR, System.getProperty( "java.io.tmpdir" ) );
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
        return new ForwardResolution( "/admin/Install.jsp" );
    }

    /**
     * Saves the properties files with updated settings, and restarts the wiki.
     * 
     * @return if successful, always returns a {@link ForwardResolution} to
     *         {@code /admin/InstallSuccess.jsp}.
     * @throws Exception
     */
    @HandlesEvent( "save" )
    public Resolution save() throws Exception
    {
        // Sanitize any paths
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        jspwiki.put( CONFIG_BASE_URL, sanitizeURL( jspwiki.get( CONFIG_BASE_URL ) ) );
        jspwiki.put( CONFIG_WORK_DIR, sanitizeDir( jspwiki.get( CONFIG_WORK_DIR ) ) );
        PropertiesMap<String, String> log4j = m_properties.get( "log4j" );
        log4j.put( CONFIG_LOG_FILE, sanitizeDir( m_logDirectory ) + "jspwiki.log" );
        PropertiesMap<String, String> priha = m_properties.get( "priha" );
        priha.put( CONFIG_PAGE_DIR, sanitizeDir( priha.get( CONFIG_PAGE_DIR ) ) );

        // Set the correct userdatabase and authorizer
        String userdatabase = jspwiki.get( CONFIG_USERDATABASE );
        if( LdapUserDatabase.class.getName().equals( userdatabase ) )
        {
            jspwiki.put( CONFIG_AUTHORIZER, LdapAuthorizer.class.getName() );
        }
        else
        {
            jspwiki.put( CONFIG_AUTHORIZER, WebContainerAuthorizer.class.getName() );
        }

        // Hash the admin password
        String passwordHash = CryptoUtil.getSaltedPassword( m_adminPassword.getBytes() );
        jspwiki.put( CONFIG_ADMIN_PASSWORD_HASH, passwordHash );

        // Save the keychain
        String password = jspwiki.get( AuthenticationManager.PROP_KEYCHAIN_PASSWORD );
        if( password == null )
        {
            throw new WikiSecurityException( "Keychain password missing; this should not happen." );
        }
        m_keychain.store( new FileOutputStream( m_keychainPath ), password.toCharArray() );

        // Save each properties file
        jspwiki.store();
        log4j.store();
        priha.store();

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
    @Validate( required = true, on = "save", minlength = 16 )
    public void setAdminPassword( String password )
    {
        m_adminPassword = password;
    }

    /**
     * Sets the LDAP binding password. Optional when LDAP is selected for
     * authentication and user/role storage.
     * 
     * @param password
     */
    @Validate( required = false )
    public void setBindPassword( String password ) throws KeyStoreException
    {
        m_bindPassword = password;
        if( password != null )
        {
            KeyStore.Entry keypass = new Keychain.Password( m_bindPassword );
            m_keychain.setEntry( LdapConfig.KEYCHAIN_LDAP_BIND_PASSWORD, keypass );
        }
    }

    /**
     * Sets the directory where log files are stored.
     * 
     * @param dir the log directory
     */
    @Validate( required = true, on = "save" )
    public void setLogDirectory( String dir )
    {
        m_logDirectory = dir;
    }

    /**
     * @param properties
     */
    @ValidateNestedProperties( {
                                @Validate( field = "jspwiki.jspwiki_applicationName", required = true, on = "save" ),
                                @Validate( field = "jspwiki.jspwiki_baseURL", required = true, on = "save" ),
                                @Validate( field = "priha.priha_provider_defaultProvider_directory", required = true, on = "save" ),
                                @Validate( field = "jspwiki.jspwiki_workDir", required = true, on = "save" ),
                                @Validate( field = "jspwiki.jspwiki_userdatabase", required = true, on = "save" ),
                                @Validate( field = "jspwiki.ldap_connectionURL", required = true, on = { "testLdapConnection",
                                                                                                        "testLdapAuthentication",
                                                                                                        "testLdapUsers",
                                                                                                        "testLdapRoles" } ),
                                @Validate( field = "jspwiki.ldap_bindUser", required = true, on = "testLdapAuthentication" ),
                                @Validate( field = "jspwiki.ldap_userBase", required = true, on = "testLdapUsers" ),
                                @Validate( field = "jspwiki.ldap_roleBase", required = true, on = "testLdapRoles" ) } )
    public void setProperties( Map<String, PropertiesMap<String, String>> properties )
    {
        m_properties = properties;
    }

    /**
     * AJAX event method that tests LDAP authentication based on the bind-user
     * settings, returning any results as an array of JavaScript strings.
     * 
     * @return the results
     * @throws WikiSecurityException
     */
    @HandlesEvent( "testLdapAuthentication" )
    public Resolution testLdapAuthentication() throws WikiSecurityException
    {
        // Call the main connection method (the bindUser property is
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
    public Resolution testLdapRoleLookup() throws WikiSecurityException
    {
        WikiEngine engine = getContext().getEngine();
        PropertiesMap<String, String> jspwiki = m_properties.get( "jspwiki" );
        List<String> messages = new ArrayList<String>();
        try
        {
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
     * Initializes the Keychain by attempting to unlock it first based on
     * current {@code jspwiki.properties} settings. If this fails, a new
     * Keychain is created at {@code /WEB-INF/keychain}. It is not persisted to
     * disk until {@link #save()} is called.
     * 
     * @param path the webapp root path
     * @param jspwiki the PropertiesMap containing the current {@code
     *            jspwiki.properties}.
     * @throws IOException if the new Keychain cannot be loaded (this should
     *             never be thrown under normal conditions)
     * @throws NoSuchAlgorithmException if the new Keychain cannot be loaded
     *             (this should never be thrown under normal conditions)
     */
    private void initKeychain( String path, PropertiesMap<String, String> jspwiki ) throws IOException, NoSuchAlgorithmException
    {
        AuthenticationManager authMgr = getContext().getEngine().getAuthenticationManager();
        m_keychain = authMgr.getKeychain();
        boolean keychainUnlocked = false;
        String password = null;
        if( !m_keychain.isLoaded() )
        {
            password = jspwiki.get( AuthenticationManager.PROP_KEYCHAIN_PASSWORD );
            if( password != null )
            {
                try
                {
                    authMgr.unlockKeychain( password );
                    keychainUnlocked = true;
                }
                catch( WikiSecurityException e )
                {
                }
            }
        }
        if( !keychainUnlocked )
        {
            password = TextUtil.generateRandomPassword() + TextUtil.generateRandomPassword();
            m_keychain.load( null, password.toCharArray() );
        }
        jspwiki.put( AuthenticationManager.PROP_KEYCHAIN_PATH, "keychain" );
        jspwiki.put( AuthenticationManager.PROP_KEYCHAIN_PASSWORD, password );
        m_keychainPath = new File( path, "/WEB-INF/keychain" );
    }

    /**
     * Initializes an LDAP connection, using the bind-user and password if
     * supplied.
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
        if( !jspwiki.containsKey( "ldap_bindUser" ) )
        {
            env = config.newJndiEnvironment();
            messages.add( "Binding as anonymous user." );
        }
        else
        {
            String username = jspwiki.get( "ldap_bindUser" );
            env = config.newJndiEnvironment( username, m_bindPassword );
            Object principal = env.get( Context.SECURITY_PRINCIPAL );
            messages.add( "Binding with principal: " + principal.toString() );
        }
        return new InitialLdapContext( env, null );
    }

    /**
     * Simply sanitizes any path which contains backslashes (sometimes Windows
     * users may have them) by expanding them to double-backslashes
     * 
     * @param key the key of the setting to sanitize
     */
    private String sanitizeDir( String dir )
    {
        String s = dir;
        s = TextUtil.replaceString( s, "\\", "\\\\" );
        s = s.trim();
        if( !s.endsWith( "\\" ) || !s.endsWith( "/" ) )
        {
            s = s + "/";
        }
        return s;
    }

    /**
     * Simply sanitizes any URL which contains backslashes (sometimes Windows
     * users may have them)
     * 
     * @param key the key of the setting to sanitize
     */
    private String sanitizeURL( String url )
    {
        String s = url;
        s = TextUtil.replaceString( s, "\\", "/" );
        s = s.trim();
        if( !s.endsWith( "/" ) )
        {
            s = s + "/";
        }
        return s;
    }

}
