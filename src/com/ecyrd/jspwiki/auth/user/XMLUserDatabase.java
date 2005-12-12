/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.user;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
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
 * @version $Revision: 1.9 $ $Date: 2005-12-12 07:02:55 $
 * @since 2.3
 */
public class XMLUserDatabase extends AbstractUserDatabase
{

    /**
     * The jspwiki.properties property specifying the file system location of
     * the user database.
     */
    public static final String    PROP_USERDATABASE = "jspwiki.xmlUserDatabaseFile";
    
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

    private DateFormat          c_format          = DateFormat.getDateTimeInstance();

    private File                c_file            = null;

    /**
     * Persists database changes to disk.
     */
    public synchronized void commit() throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "User database doesn't exist in memory." );
        }

        File newFile = new File( c_file.getAbsolutePath() + ".new" );
        try
        {
            BufferedWriter io = new BufferedWriter( new FileWriter ( newFile ) );
            
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
     * Initializes the user database based on values from a Properties object.
     * The properties object must contain a file path to the XML database file
     * whose key is {@link #PROP_USERDATABASE}.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     * @throws NoRequiredPropertyException if the user database cannot be located, parsed, or opened
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException
    {
        File defaultFile = new File( engine.getRootPath() + "/WEB-INF/" + DEFAULT_USERDATABASE );

        // Get database file location
        String file = props.getProperty( PROP_USERDATABASE );
        if ( file == null )
        {
            log.error( "XML user database property " + PROP_USERDATABASE + " not found; trying " + defaultFile  );
            c_file = defaultFile;
        }
        else 
        {
            c_file = new File( file );
            if ( !c_file.exists() )
            {
                log.error( "XML user database " + file + " does not exist; trying " + defaultFile );
                if ( defaultFile.exists() )
                {
                    c_file = defaultFile;
                }
            }
        }

        // Read DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setExpandEntityReferences( false );
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        try
        {
            c_dom = factory.newDocumentBuilder().parse( c_file );
            log.info( "User database " + c_file + " successfully initialized." );
        }
        catch( ParserConfigurationException e )
        {
            log.error( "Configuration error: " + e.getMessage() );
        }
        catch( SAXException e )
        {
            log.error( "SAX error: " + e.getMessage() );
        }
        catch( IOException e )
        {
            log.error( "IO error: " + e.getMessage() );
        }
        if ( c_dom == null )
        {
            try
            {
                log.info( "Could not load file from disk: creating in-memory DOM." );
                c_dom = factory.newDocumentBuilder().newDocument();
            }
            catch( ParserConfigurationException e )
            {
                log.fatal( "Could not create in-memory DOM" );
            }
        }
    }

    /**
     * Saves a {@link UserProfile}to the user database, overwriting the
     * existing profile if it exists. The user name under which the profile
     * should be saved is returned by the supplied profile's
     * {@link UserProfile#getLoginName()}method.
     * @param profile the user profile to save
     * @throws WikiSecurityException if the profile cannot be saved
     */
    public void save( UserProfile profile ) throws WikiSecurityException
    {
        if ( c_dom == null )
        {
            log.fatal( "Could not save profile " + profile + " database does not exist" );
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        String index = profile.getLoginName();
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        Element user = null;
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element currentUser = (Element) users.item( i );
            if ( currentUser.getAttribute( LOGIN_NAME ).equals( index ) )
            {
                user = currentUser;
            }
        }
        if ( user == null )
        {
            log.info( "Creating new user " + index );
            user = c_dom.createElement( USER_TAG );
            c_dom.getDocumentElement().appendChild( user );
        }
        if ( profile.getCreated() != null ) {
            setAttribute( user, CREATED, c_format.format( profile.getCreated() ) );
        }
        if ( profile.getLastModified() != null ) {
            setAttribute( user, LAST_MODIFIED, c_format.format( profile.getLastModified() ) );
        }
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
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            if ( user.getAttribute( matchAttribute ).equals( index ) )
            {
                UserProfile profile = new DefaultUserProfile();
                profile.setLoginName( user.getAttribute( LOGIN_NAME ) );
                profile.setFullname( user.getAttribute( FULL_NAME ) );
                profile.setWikiName( user.getAttribute( WIKI_NAME ) );
                profile.setPassword( user.getAttribute( PASSWORD ) );
                profile.setEmail( user.getAttribute( EMAIL ) );
                String created = user.getAttribute( CREATED );
                String modified = user.getAttribute( LAST_MODIFIED );
                try 
                {
                    profile.setCreated( c_format.parse( created ) );                  
                    profile.setLastModified( c_format.parse( modified ) );                  
                }
                catch ( ParseException e )
                {
                    log.warn("Could not parse 'created' or 'lastModified' "
                        + "attribute for "
                        + " profile '" + profile.getLoginName() + "'."
                        + " It may have been tampered with." );
                }
                return profile;
            }
        }
        return null;
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