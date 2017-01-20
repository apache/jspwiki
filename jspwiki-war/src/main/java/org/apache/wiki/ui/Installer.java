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
package org.apache.wiki.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.PageManager;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.util.TextUtil;

/**
 * Manages JSPWiki installation on behalf of <code>admin/Install.jsp</code>.
 * The contents of this class were previously part of <code>Install.jsp</code>.
 *
 * @since 2.4.20
 */
public class Installer
{
    public static final String ADMIN_ID = "admin";
    public static final String ADMIN_NAME = "Administrator";
    public static final String INSTALL_INFO = "Installer.Info";
    public static final String INSTALL_ERROR = "Installer.Error";
    public static final String INSTALL_WARNING = "Installer.Warning";
    public static final String APP_NAME = WikiEngine.PROP_APPNAME;
    public static final String STORAGE_DIR = BasicAttachmentProvider.PROP_STORAGEDIR;
    public static final String LOG_FILE = "log4j.appender.FileLog.File";
    public static final String PAGE_DIR = FileSystemProvider.PROP_PAGEDIR;
    public static final String WORK_DIR = WikiEngine.PROP_WORKDIR;
    public static final String ADMIN_GROUP = "Admin";
    public static final String PROPFILENAME = "jspwiki-custom.properties" ;
    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private final WikiSession m_session;
    private final File m_propertyFile;
    private final Properties m_props;
    private final WikiEngine m_engine;
    private HttpServletRequest m_request;
    private boolean m_validated;
    
    public Installer( HttpServletRequest request, ServletConfig config ) throws IOException {
        // Get wiki session for this user
        m_engine = WikiEngine.getInstance( config );
        m_session = WikiSession.getWikiSession( m_engine, request );
        
        // Get the file for properties
        m_propertyFile = new File(TMP_DIR, PROPFILENAME);
        m_props = new Properties();
        
        // Stash the request
        m_request = request;
        m_validated = false;
    }
    
    /**
     * Returns <code>true</code> if the administrative user had
     * been created previously.
     * @return the result
     */
    public boolean adminExists()
    {
        // See if the admin user exists already
        UserManager userMgr = m_engine.getUserManager();
        UserDatabase userDb = userMgr.getUserDatabase();
        
        try
        {
            userDb.findByLoginName( ADMIN_ID );
            return true;
        }
        catch ( NoSuchPrincipalException e )
        {
            return false;
        }
    }
    
    /**
     * Creates an administrative user and returns the new password.
     * If the admin user exists, the password will be <code>null</code>.
     * @return the password
     * @throws WikiSecurityException
     */
    public String createAdministrator() throws WikiSecurityException
    {
        if ( !m_validated )
        {
            throw new WikiSecurityException( "Cannot create administrator because one or more of the installation settings are invalid." );
        }
        
        if ( adminExists() )
        {
            return null;
        }
        
        // See if the admin user exists already
        UserManager userMgr = m_engine.getUserManager();
        UserDatabase userDb = userMgr.getUserDatabase();
        String password = null;
        
        try
        {
            userDb.findByLoginName( ADMIN_ID );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Create a random 12-character password
            password = TextUtil.generateRandomPassword();
            UserProfile profile = userDb.newProfile();
            profile.setLoginName( ADMIN_ID );
            profile.setFullname( ADMIN_NAME );
            profile.setPassword( password );
            userDb.save( profile );
        }
        
        // Create a new admin group
        GroupManager groupMgr = m_engine.getGroupManager();
        Group group = null;
        try
        {
            group = groupMgr.getGroup( ADMIN_GROUP );
            group.add( new WikiPrincipal( ADMIN_NAME ) );
        }
        catch ( NoSuchPrincipalException e )
        {
            group = groupMgr.parseGroup( ADMIN_GROUP, ADMIN_NAME, true );
        }
        groupMgr.setGroup( m_session, group );
        
        return password;
    }
    
    /**
     * Returns the properties as a "key=value" string separated by newlines
     * @return the string
     */
    public String getPropertiesList()
    {
        StringBuilder result = new StringBuilder();
        Set<String> keys = m_props.stringPropertyNames();
        for (String key:keys) {
            result.append(key + " = " + m_props.getProperty(key) + "\n");
        }
        return result.toString();
    }

    public String getPropertiesPath()
    {
        return m_propertyFile.getAbsolutePath();
    }

    /**
     * Returns a property from the WikiEngine's properties.
     * @param key the property key
     * @return the property value
     */
    public String getProperty( String key )
    {
        return m_props.getProperty( key );
    }
    
    public void parseProperties () throws Exception
    {
        ResourceBundle rb = ResourceBundle.getBundle( InternationalizationManager.CORE_BUNDLE,
                                                      m_session.getLocale() );
        m_validated = false;
        

        // Get application name
        String nullValue = m_props.getProperty( APP_NAME, rb.getString( "install.installer.default.appname" ) );
        parseProperty( APP_NAME, nullValue );

        // Get/sanitize page directory
        nullValue = m_props.getProperty( PAGE_DIR, rb.getString( "install.installer.default.pagedir" ) );
        parseProperty( PAGE_DIR, nullValue );
        sanitizePath( PAGE_DIR );
        
        // Get/sanitize log directory
        nullValue = m_props.getProperty( LOG_FILE, TMP_DIR + File.separator + "jspwiki.log" );
        parseProperty( LOG_FILE, nullValue );
        sanitizePath( LOG_FILE );
        
        // Get/sanitize work directory
        nullValue = m_props.getProperty( WORK_DIR, TMP_DIR );
        parseProperty( WORK_DIR, nullValue );
        sanitizePath( WORK_DIR );
        
        // Set a few more default properties, for easy setup
        m_props.setProperty( STORAGE_DIR, m_props.getProperty( PAGE_DIR ) );
        m_props.setProperty( PageManager.PROP_PAGEPROVIDER, "VersioningFileProvider" );
    }
    
    public void saveProperties()
    {
        ResourceBundle rb = ResourceBundle.getBundle( InternationalizationManager.CORE_BUNDLE, m_session.getLocale() );
        // Write the file back to disk
        try
        {
            OutputStream out = null;
            try
            {
                out = new FileOutputStream( m_propertyFile );
                m_props.store( out, null );
            }
            finally
            {
                if ( out != null )
                {
                    out.close();
                }
            }
            m_session.addMessage( INSTALL_INFO, MessageFormat.format(rb.getString("install.installer.props.saved"), m_propertyFile) );
        }
        catch( IOException e )
        {
            Object[] args = { e.getMessage(), m_props.toString() };
            m_session.addMessage( INSTALL_ERROR, MessageFormat.format( rb.getString( "install.installer.props.notsaved" ), args ) );
        }
    }
    
    public boolean validateProperties() throws Exception
    {
        ResourceBundle rb = ResourceBundle.getBundle( InternationalizationManager.CORE_BUNDLE, m_session.getLocale() );
        m_session.clearMessages( INSTALL_ERROR );
        parseProperties();
        validateNotNull( PAGE_DIR, rb.getString( "install.installer.validate.pagedir" ) );
        validateNotNull( APP_NAME, rb.getString( "install.installer.validate.appname" ) );
        validateNotNull( WORK_DIR, rb.getString( "install.installer.validate.workdir" ) );
        validateNotNull( LOG_FILE, rb.getString( "install.installer.validate.logfile" ) );
        
        if ( m_session.getMessages( INSTALL_ERROR ).length == 0 )
        {
            m_validated = true;
        }
        return m_validated;
    }
        
    /**
     * Sets a property based on the value of an HTTP request parameter.
     * If the parameter is not found, a default value is used instead.
     * @param param the parameter containing the value we will extract
     * @param defaultValue the default to use if the parameter was not passed
     * in the request
     */
    private void parseProperty( String param, String defaultValue )
    {
        String value = m_request.getParameter( param );
        if ( value == null )
        {
            value = defaultValue;
        }
        m_props.put(param, value);
    }
    
    /**
     * Simply sanitizes any path which contains backslashes (sometimes Windows
     * users may have them) by expanding them to double-backslashes
     * @param key the key of the property to sanitize
     */
    private void sanitizePath( String key )
    {
        String s = m_props.getProperty( key );
        s = TextUtil.replaceString(s, "\\", "\\\\" );
        s = s.trim();
        m_props.put( key, s );
    }
    
    /**
     * Simply sanitizes any URL which contains backslashes (sometimes Windows
     * users may have them)
     * @param key the key of the property to sanitize
     */
    private void sanitizeURL( String key )
    {
        String s = m_props.getProperty( key );
        s = TextUtil.replaceString( s, "\\", "/" );
        s = s.trim();
        if (!s.endsWith("/"))
        {
            s = s + "/";
        }
        m_props.put( key, s );
    }

    private void validateNotNull( String key, String message )
    {
        String value = m_props.getProperty( key );
        if ( value == null || value.length() == 0 )
        {
            m_session.addMessage( INSTALL_ERROR, message );
        }
    }
    
}
