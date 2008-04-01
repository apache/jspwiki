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
package com.ecyrd.jspwiki.auth.user;

import java.io.*;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * <p>Manages {@link DefaultUserProfile} objects using XML files for persistence.
 * Passwords are hashed using SHA1. User entries are simple <code>&lt;user&gt;</code>
 * elements under the root. User profile properties are attributes of the
 * element. For example:</p>
 * <blockquote><code>
 * &lt;users&gt;<br/>
 * &nbsp;&nbsp;&lt;user loginName="janne" fullName="Janne Jalkanen"<br/> 
 * &nbsp;&nbsp;&nbsp;&nbsp;wikiName="JanneJalkanen" email="janne@ecyrd.com"<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;password="{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee"/&gt;<br/>
 * &lt;/users&gt;
 * </code></blockquote> 
 * <p>In this example, the un-hashed password is <code>myP@5sw0rd</code>. Passwords are hashed without salt.</p>
 * @author Andrew Jaquith
 * @since 2.3
 */

// FIXME: If the DB is shared across multiple systems, it's possible to lose accounts
//        if two people add new accounts right after each other from different wikis.
public class XMLUserDatabase extends AbstractUserDatabase
{

    /**
     * The jspwiki.properties property specifying the file system location of
     * the user database.
     */
    public static final String  PROP_USERDATABASE = "jspwiki.xmlUserDatabaseFile";
    
    private static final String DEFAULT_USERDATABASE = "userdatabase.xml";

    private static final String CREATED           = "created";
    
    private static final String EMAIL             = "email";

    private static final String FULL_NAME         = "fullName";

    private static final String LOGIN_NAME        = "loginName";

    private static final String LAST_MODIFIED     = "lastModified";
    
    private static final String PASSWORD          = "password";

    private static final String USER_TAG          = "user";

    private static final String WIKI_NAME         = "wikiName";

    private Document            c_dom             = null;

    private DateFormat          c_defaultFormat   = DateFormat.getDateTimeInstance();

    private DateFormat          c_format          = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss:SSS z");
    
    private File                c_file            = null;

    /**
     * Looks up and deletes the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param loginName the login name of the user profile that shall be deleted
     */
    public synchronized void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException
    {
        if ( c_dom == null )
        {
            throw new WikiSecurityException( "FATAL: database does not exist" );
        }
            
        NodeList users = c_dom.getDocumentElement().getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            if ( user.getAttribute( LOGIN_NAME ).equals( loginName ) )
            {
                c_dom.getDocumentElement().removeChild(user);
                
                // Commit to disk
                saveDOM();
                return;
            }
        }
        throw new NoSuchPrincipalException( "Not in database: " + loginName );
    }        

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given e-mail address. If the user
     * database does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the e-mail address of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByEmail(String)
     */
    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( EMAIL, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given full name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the fill name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( FULL_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the login name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( LOGIN_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given wiki name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the wiki name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( WIKI_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * Returns all WikiNames that are stored in the UserDatabase
     * as an array of WikiPrincipal objects. If the database does not
     * contain any profiles, this method will return a zero-length
     * array.
     * @return the WikiNames
     * @throws WikiSecurityException In case things fail.
     */
    public Principal[] getWikiNames() throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        SortedSet principals = new TreeSet();
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            String wikiName = user.getAttribute( WIKI_NAME );
            if ( wikiName == null )
            {
                log.warn( "Detected null wiki name in XMLUserDataBase. Check your user database." );
            }
            else
            {
                Principal principal = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
                principals.add( principal );
            }
        }
        return (Principal[])principals.toArray( new Principal[principals.size()] );
    }
    
    /**
     * Initializes the user database based on values from a Properties object.
     * The properties object must contain a file path to the XML database file
     * whose key is {@link #PROP_USERDATABASE}.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     * @throws NoRequiredPropertyException if the user database cannot be located, parsed, or opened
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException
    {
        File defaultFile = null;
        if( engine.getRootPath() == null )
        {
            log.warn( "Cannot identify JSPWiki root path"  );
            defaultFile = new File( "WEB-INF/" + DEFAULT_USERDATABASE ).getAbsoluteFile();
        }
        else
        {
            defaultFile = new File( engine.getRootPath() + "/WEB-INF/" + DEFAULT_USERDATABASE );
        }

        // Get database file location
        String file = props.getProperty( PROP_USERDATABASE );
        if( file == null )
        {
            log.error( "XML user database property " + PROP_USERDATABASE + " not found; trying " + defaultFile  );
            c_file = defaultFile;
        }
        else 
        {
            c_file = new File( file );
        }

        log.info("XML user database at "+c_file.getAbsolutePath());
        
        buildDOM();
        sanitizeDOM();
    }
    
    private void buildDOM()
    {
        // Read DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setExpandEntityReferences( false );
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        try
        {
            c_dom = factory.newDocumentBuilder().parse( c_file );
            log.debug( "Database successfully initialized" );
            c_lastModified = c_file.lastModified();
            c_lastCheck    = System.currentTimeMillis();
        }
        catch( ParserConfigurationException e )
        {
            log.error( "Configuration error: " + e.getMessage() );
        }
        catch( SAXException e )
        {
            log.error( "SAX error: " + e.getMessage() );
        }
        catch( FileNotFoundException e )
        {
            log.info("User database not found; creating from scratch...");
        }
        catch( IOException e )
        {
            log.error( "IO error: " + e.getMessage() );
        }
        if ( c_dom == null )
        {
            try
            {
                //
                //  Create the DOM from scratch
                //
                c_dom = factory.newDocumentBuilder().newDocument();
                c_dom.appendChild( c_dom.createElement( "users") );
            }
            catch( ParserConfigurationException e )
            {
                log.fatal( "Could not create in-memory DOM" );
            }
        }
    }
    
    private void saveDOM() throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "User database doesn't exist in memory." );
        }

        File newFile = new File( c_file.getAbsolutePath() + ".new" );
        try
        {
            BufferedWriter io = new BufferedWriter( new OutputStreamWriter ( 
                    new FileOutputStream( newFile ), "UTF-8" ) );
            
            // Write the file header and document root
            io.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            io.write("<users>\n");
            
            // Write each profile as a <user> node
            Element root = c_dom.getDocumentElement();
            NodeList nodes = root.getElementsByTagName( USER_TAG );  
            for( int i = 0; i < nodes.getLength(); i++ )
            {
                Element user = (Element)nodes.item( i );
                io.write( "<" + USER_TAG + " ");
                io.write( LOGIN_NAME );
                io.write( "=\"" + user.getAttribute( LOGIN_NAME ) + "\" " );
                io.write( WIKI_NAME );
                io.write( "=\"" + user.getAttribute( WIKI_NAME ) + "\" " );
                io.write( FULL_NAME );
                io.write( "=\"" + user.getAttribute( FULL_NAME ) + "\" " );
                io.write( EMAIL );
                io.write( "=\"" + user.getAttribute( EMAIL ) + "\" " );
                io.write( PASSWORD );
                io.write( "=\"" + user.getAttribute( PASSWORD ) + "\" " );
                io.write( CREATED );
                io.write( "=\"" + user.getAttribute( CREATED ) + "\" " );
                io.write( LAST_MODIFIED );
                io.write( "=\"" + user.getAttribute( LAST_MODIFIED ) + "\" " );
                io.write(" />\n");
            }
            io.write("</users>");
            io.close();
        }
        catch ( IOException e )
        {
            throw new WikiSecurityException( e.getLocalizedMessage() );
        }

        // Copy new file over old version
        File backup = new File( c_file.getAbsolutePath() + ".old" );
        if ( backup.exists() )
        {
            if ( !backup.delete() )
            {
                log.error( "Could not delete old user database backup: " + backup );
            }
        }
        if ( !c_file.renameTo( backup ) )
        {
            log.error( "Could not create user database backup: " + backup );
        }
        if ( !newFile.renameTo( c_file ) )
        {
            log.error( "Could not save database: " + backup + " restoring backup." );
            if ( !backup.renameTo( c_file ) )
            {
                log.error( "Restore failed. Check the file permissions." );
            }
            log.error( "Could not save database: " + c_file + ". Check the file permissions" );
        }
    }
    
    private long c_lastCheck    = 0;
    private long c_lastModified = 0;
    
    private void checkForRefresh()
    {
        long time = System.currentTimeMillis();
        
        if( time - c_lastCheck > 60*1000L )
        {
            long lastModified = c_file.lastModified();
            
            if( lastModified > c_lastModified )
            {
                buildDOM();
            }
        }
    }
    
    /**
     * Determines whether the user database shares user/password data with the
     * web container; always returns <code>false</code>.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#isSharedWithContainer()
     */
    public boolean isSharedWithContainer()
    {
        return false;
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#rename(String, String)
     */
    public synchronized void rename(String loginName, String newName) throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "Could not rename profile '" + loginName + "'; database does not exist" );
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        checkForRefresh();
        
        // Get the existing user; if not found, throws NoSuchPrincipalException
        UserProfile profile = findByLoginName( loginName );
        
        // Get user with the proposed name; if found, it's a collision
        try 
        {
            UserProfile otherProfile = findByLoginName( newName );
            if ( otherProfile != null )
            {
                throw ( new DuplicateUserException( "Cannot rename: the login name '" + newName + "' is already taken." ) );
            }
        }
        catch ( NoSuchPrincipalException e )
        {
            // Good! That means it's safe to save using the new name
        }
        
        // Find the user with the old login id attribute, and change it
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            if ( user.getAttribute( LOGIN_NAME ).equals( loginName ) )
            {
                Date modDate = new Date( System.currentTimeMillis() );
                setAttribute( user, LOGIN_NAME, newName );
                setAttribute( user, LAST_MODIFIED, c_format.format( modDate ) );
                profile.setLoginName( newName );
                profile.setLastModified( modDate );
                break;
            }
        }
        
        // Commit to disk
        saveDOM();
    }
    
    /**
     * Saves a {@link UserProfile}to the user database, overwriting the
     * existing profile if it exists. The user name under which the profile
     * should be saved is returned by the supplied profile's
     * {@link UserProfile#getLoginName()}method.
     * @param profile the user profile to save
     * @throws WikiSecurityException if the profile cannot be saved
     */
    public synchronized void save( UserProfile profile ) throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "Could not save profile " + profile + " database does not exist" );
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        
        checkForRefresh();
        
        String index = profile.getLoginName();
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        Element user = null;
        boolean isNew = true;
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element currentUser = (Element) users.item( i );
            if ( currentUser.getAttribute( LOGIN_NAME ).equals( index ) )
            {
                user = currentUser;
                isNew = false;
                break;
            }
        }
        Date modDate = new Date( System.currentTimeMillis() );
        if ( isNew )
        {
            profile.setCreated( modDate );
            log.info( "Creating new user " + index );
            user = c_dom.createElement( USER_TAG );
            c_dom.getDocumentElement().appendChild( user );
            setAttribute( user, CREATED, c_format.format( profile.getCreated() ) );
        }
        setAttribute( user, LAST_MODIFIED, c_format.format( modDate ) );
        setAttribute( user, LOGIN_NAME, profile.getLoginName() );
        setAttribute( user, FULL_NAME, profile.getFullname() );
        setAttribute( user, WIKI_NAME, profile.getWikiName() );
        setAttribute( user, EMAIL, profile.getEmail() );

        // Hash and save the new password if it's different from old one
        String newPassword = profile.getPassword();
        if ( newPassword != null && !newPassword.equals( "" ) )
        {
            String oldPassword = user.getAttribute( PASSWORD );
            if ( !oldPassword.equals( newPassword ) )
            {
                setAttribute( user, PASSWORD, SHA_PREFIX + getHash( newPassword ) );
            }
        }

        // Set the profile timestamps
        if ( isNew )
        {
            profile.setCreated( modDate );
        }
        profile.setLastModified( modDate );
        
        // Commit to disk
        saveDOM();
    }

    /**
     * Private method that returns the first {@link UserProfile}matching a
     * &lt;user&gt; element's supplied attribute.
     * @param matchAttribute
     * @param index
     * @return the profile, or <code>null</code> if not found
     */
    private UserProfile findByAttribute( String matchAttribute, String index )
    {
        if ( c_dom == null )
        {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        
        checkForRefresh();
        
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            if ( user.getAttribute( matchAttribute ).equals( index ) )
            {
                UserProfile profile = new DefaultUserProfile();
                profile.setLoginName( user.getAttribute( LOGIN_NAME ) );
                profile.setFullname( user.getAttribute( FULL_NAME ) );
                profile.setPassword( user.getAttribute( PASSWORD ) );
                profile.setEmail( user.getAttribute( EMAIL ) );
                String created = user.getAttribute( CREATED );
                String modified = user.getAttribute( LAST_MODIFIED );
                
                profile.setCreated( parseDate( profile, created ) );                  
                profile.setLastModified( parseDate( profile, modified ) );                  

                return profile;
            }
        }
        return null;
    }

    /**
     *  Tries to parse a date using the default format - then, for backwards
     *  compatibility reasons, tries the platform default.
     *  
     *  @param profile
     *  @param date
     *  @return A parsed date, or null, if both parse attempts fail.
     */
    private Date parseDate( UserProfile profile, String date )
    {
        try
        {
            return c_format.parse( date );
        }
        catch( ParseException e )
        {
            try
            {
                return c_defaultFormat.parse( date );
            }
            catch ( ParseException e2)
            {
                log.warn("Could not parse 'created' or 'lastModified' "
                    + "attribute for "
                    + " profile '" + profile.getLoginName() + "'."
                    + " It may have been tampered with." );
            }            
        }
        return null;
    }
    
    /**
     * After loading the DOM, this method sanity-checks the dates in the DOM and makes
     * sure they are formatted properly. This is sort-of hacky, but it should work.
     */
    private void sanitizeDOM()
    {
        if ( c_dom == null )
        {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            String loginName = user.getAttribute( LOGIN_NAME );
            String created = user.getAttribute( CREATED );
            String modified = user.getAttribute( LAST_MODIFIED );
            try
            {
                created = c_format.format( c_format.parse( created ) );
                modified = c_format.format( c_format.parse( modified ) );
                user.setAttribute( CREATED,  created );
                user.setAttribute( LAST_MODIFIED,  modified );
            }
            catch( ParseException e )
            {
                try
                {
                    created = c_format.format( c_defaultFormat.parse( created ) );
                    modified = c_format.format( c_defaultFormat.parse( modified ) );
                    user.setAttribute( CREATED,  created );
                    user.setAttribute( LAST_MODIFIED,  modified );
                }
                catch ( ParseException e2)
                {
                    log.warn("Could not parse 'created' or 'lastModified' "
                        + "attribute for "
                        + " profile '" + loginName + "'."
                        + " It may have been tampered with." );
                }            
            }
        }
    }
    
    /**
     * Private method that sets an attibute value for a supplied DOM element.
     * @param element the element whose attribute is to be set
     * @param attribute the name of the attribute to set
     * @param value the desired attribute value
     */
    private void setAttribute( Element element, String attribute, String value )
    {
        if ( value != null )
        {
            element.setAttribute( attribute, value );
        }
    }
}