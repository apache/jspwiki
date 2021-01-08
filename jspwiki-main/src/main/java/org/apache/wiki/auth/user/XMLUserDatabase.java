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
package org.apache.wiki.auth.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.Serializer;
import org.apache.wiki.util.TextUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>Manages {@link DefaultUserProfile} objects using XML files for persistence. Passwords are hashed using SHA1. User entries are simple
 * <code>&lt;user&gt;</code> elements under the root. User profile properties are attributes of the element. For example:</p>
 * <blockquote><code>
 * &lt;users&gt;<br/>
 * &nbsp;&nbsp;&lt;user loginName="janne" fullName="Janne Jalkanen"<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;wikiName="JanneJalkanen" email="janne@ecyrd.com"<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;password="{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee"/&gt;<br/>
 * &lt;/users&gt;
 * </code></blockquote>
 * <p>In this example, the un-hashed password is <code>myP@5sw0rd</code>. Passwords are hashed without salt.</p>
 * @since 2.3
 */

// FIXME: If the DB is shared across multiple systems, it's possible to lose accounts
//        if two people add new accounts right after each other from different wikis.
public class XMLUserDatabase extends AbstractUserDatabase {

    /** The jspwiki.properties property specifying the file system location of the user database. */
    public static final String  PROP_USERDATABASE = "jspwiki.xmlUserDatabaseFile";
    private static final String DEFAULT_USERDATABASE = "userdatabase.xml";
    private static final String ATTRIBUTES_TAG    = "attributes";
    private static final String CREATED           = "created";
    private static final String EMAIL             = "email";
    private static final String FULL_NAME         = "fullName";
    private static final String LOGIN_NAME        = "loginName";
    private static final String LAST_MODIFIED     = "lastModified";
    private static final String LOCK_EXPIRY       = "lockExpiry";
    private static final String PASSWORD          = "password";
    private static final String UID               = "uid";
    private static final String USER_TAG          = "user";
    private static final String WIKI_NAME         = "wikiName";
    private static final String DATE_FORMAT       = "yyyy.MM.dd 'at' HH:mm:ss:SSS z";
    private Document            c_dom;
    private File                c_file;

    /** {@inheritDoc} */
    @Override
    public synchronized void deleteByLoginName( final String loginName ) throws NoSuchPrincipalException, WikiSecurityException {
        if( c_dom == null ) {
            throw new WikiSecurityException( "FATAL: database does not exist" );
        }

        final NodeList users = c_dom.getDocumentElement().getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ ) {
            final Element user = ( Element )users.item( i );
            if( user.getAttribute( LOGIN_NAME ).equals( loginName ) ) {
                c_dom.getDocumentElement().removeChild( user );

                // Commit to disk
                saveDOM();
                return;
            }
        }
        throw new NoSuchPrincipalException( "Not in database: " + loginName );
    }

    /** {@inheritDoc} */
    @Override
    public UserProfile findByEmail( final String index ) throws NoSuchPrincipalException {
        return findBy( EMAIL, index );
    }

    /** {@inheritDoc} */
    @Override
    public UserProfile findByFullName( final String index ) throws NoSuchPrincipalException {
        return findBy( FULL_NAME, index );
    }

    /** {@inheritDoc} */
    @Override
    public UserProfile findByLoginName( final String index ) throws NoSuchPrincipalException {
        return findBy( LOGIN_NAME, index );
    }

    /** {@inheritDoc} */
    @Override
    public UserProfile findByUid( final String uid ) throws NoSuchPrincipalException {
        return findBy( UID, uid );
    }

    /** {@inheritDoc} */
    @Override
    public UserProfile findByWikiName( final String index ) throws NoSuchPrincipalException {
        return findBy( WIKI_NAME, index );
    }

    public UserProfile findBy( final String attr, final String value ) throws NoSuchPrincipalException {
        final UserProfile profile = findByAttribute( attr, value );
        if ( profile != null ) {
            return profile;
        }
        throw new NoSuchPrincipalException( "Not in database: " + value );
    }

    /** {@inheritDoc} */
    @Override
    public Principal[] getWikiNames() throws WikiSecurityException {
        if ( c_dom == null ) {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        final SortedSet< Principal > principals = new TreeSet<>();
        final NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ ) {
            final Element user = (Element) users.item( i );
            final String wikiName = user.getAttribute( WIKI_NAME );
            if( wikiName == null ) {
                log.warn( "Detected null wiki name in XMLUserDataBase. Check your user database." );
            } else {
                final Principal principal = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
                principals.add( principal );
            }
        }
        return principals.toArray( new Principal[principals.size()] );
    }

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws NoRequiredPropertyException {
        final File defaultFile;
        if( engine.getRootPath() == null ) {
            log.warn( "Cannot identify JSPWiki root path" );
            defaultFile = new File( "WEB-INF/" + DEFAULT_USERDATABASE ).getAbsoluteFile();
        } else {
            defaultFile = new File( engine.getRootPath() + "/WEB-INF/" + DEFAULT_USERDATABASE );
        }

        // Get database file location
        final String file = TextUtil.getStringProperty( props, PROP_USERDATABASE, defaultFile.getAbsolutePath() );
        if( file == null ) {
            log.warn( "XML user database property " + PROP_USERDATABASE + " not found; trying " + defaultFile );
            c_file = defaultFile;
        } else {
            c_file = new File( file );
        }

        log.info( "XML user database at " + c_file.getAbsolutePath() );

        buildDOM();
        sanitizeDOM();
    }

    private void buildDOM() {
        // Read DOM
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setExpandEntityReferences( false );
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        try {
            c_dom = factory.newDocumentBuilder().parse( c_file );
            log.debug( "Database successfully initialized" );
            c_lastModified = c_file.lastModified();
            c_lastCheck = System.currentTimeMillis();
        } catch( final ParserConfigurationException e ) {
            log.error( "Configuration error: " + e.getMessage() );
        } catch( final SAXException e ) {
            log.error( "SAX error: " + e.getMessage() );
        } catch( final FileNotFoundException e ) {
            log.info( "User database not found; creating from scratch..." );
        } catch( final IOException e ) {
            log.error( "IO error: " + e.getMessage() );
        }
        if( c_dom == null ) {
            try {
                //  Create the DOM from scratch
                c_dom = factory.newDocumentBuilder().newDocument();
                c_dom.appendChild( c_dom.createElement( "users" ) );
            } catch( final ParserConfigurationException e ) {
                log.fatal( "Could not create in-memory DOM" );
            }
        }
    }

    private void saveDOM() throws WikiSecurityException {
        if( c_dom == null ) {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }

        final File newFile = new File( c_file.getAbsolutePath() + ".new" );
        try( final BufferedWriter io = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( newFile ), StandardCharsets.UTF_8 ) ) ) {

            // Write the file header and document root
            io.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
            io.write( "<users>\n" );

            // Write each profile as a <user> node
            final Element root = c_dom.getDocumentElement();
            final NodeList nodes = root.getElementsByTagName( USER_TAG );
            for( int i = 0; i < nodes.getLength(); i++ ) {
                final Element user = ( Element )nodes.item( i );
                io.write( "    <" + USER_TAG + " " );
                io.write( UID );
                io.write( "=\"" + user.getAttribute( UID ) + "\" " );
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
                io.write( LOCK_EXPIRY );
                io.write( "=\"" + user.getAttribute( LOCK_EXPIRY ) + "\" " );
                io.write( ">" );
                final NodeList attributes = user.getElementsByTagName( ATTRIBUTES_TAG );
                for( int j = 0; j < attributes.getLength(); j++ ) {
                    final Element attribute = ( Element )attributes.item( j );
                    final String value = extractText( attribute );
                    io.write( "\n        <" + ATTRIBUTES_TAG + ">" );
                    io.write( value );
                    io.write( "</" + ATTRIBUTES_TAG + ">" );
                }
                io.write( "\n    </" + USER_TAG + ">\n" );
            }
            io.write( "</users>" );
        } catch( final IOException e ) {
            throw new WikiSecurityException( e.getLocalizedMessage(), e );
        }

        // Copy new file over old version
        final File backup = new File( c_file.getAbsolutePath() + ".old" );
        if( backup.exists() ) {
            if( !backup.delete() ) {
                log.error( "Could not delete old user database backup: " + backup );
            }
        }
        if( !c_file.renameTo( backup ) ) {
            log.error( "Could not create user database backup: " + backup );
        }
        if( !newFile.renameTo( c_file ) ) {
            log.error( "Could not save database: " + backup + " restoring backup." );
            if( !backup.renameTo( c_file ) ) {
                log.error( "Restore failed. Check the file permissions." );
            }
            log.error( "Could not save database: " + c_file + ". Check the file permissions" );
        }
    }

    private long c_lastCheck;
    private long c_lastModified;

    private void checkForRefresh() {
        final long time = System.currentTimeMillis();
        if( time - c_lastCheck > 60 * 1000L ) {
            final long lastModified = c_file.lastModified();

            if( lastModified > c_lastModified ) {
                buildDOM();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.wiki.auth.user.UserDatabase#rename(String, String)
     */
    @Override
    public synchronized void rename( final String loginName, final String newName) throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException {
        if( c_dom == null ) {
            log.fatal( "Could not rename profile '" + loginName + "'; database does not exist" );
            throw new IllegalStateException( "FATAL: database does not exist" );
        }
        checkForRefresh();

        // Get the existing user; if not found, throws NoSuchPrincipalException
        final UserProfile profile = findByLoginName( loginName );

        // Get user with the proposed name; if found, it's a collision
        try {
            final UserProfile otherProfile = findByLoginName( newName );
            if( otherProfile != null ) {
                throw new DuplicateUserException( "security.error.cannot.rename", newName );
            }
        } catch( final NoSuchPrincipalException e ) {
            // Good! That means it's safe to save using the new name
        }

        // Find the user with the old login id attribute, and change it
        final NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ ) {
            final Element user = ( Element )users.item( i );
            if( user.getAttribute( LOGIN_NAME ).equals( loginName ) ) {
                final DateFormat c_format = new SimpleDateFormat( DATE_FORMAT );
                final Date modDate = new Date( System.currentTimeMillis() );
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

    /** {@inheritDoc} */
    @Override
    public synchronized void save( final UserProfile profile ) throws WikiSecurityException {
        if ( c_dom == null ) {
            log.fatal( "Could not save profile " + profile + " database does not exist" );
            throw new IllegalStateException( "FATAL: database does not exist" );
        }

        checkForRefresh();

        final DateFormat c_format = new SimpleDateFormat( DATE_FORMAT );
        final String index = profile.getLoginName();
        final NodeList users = c_dom.getElementsByTagName( USER_TAG );
        Element user = null;
        for( int i = 0; i < users.getLength(); i++ ) {
            final Element currentUser = ( Element )users.item( i );
            if( currentUser.getAttribute( LOGIN_NAME ).equals( index ) ) {
                user = currentUser;
                break;
            }
        }

        boolean isNew = false;

        final Date modDate = new Date( System.currentTimeMillis() );
        if( user == null ) {
            // Create new user node
            profile.setCreated( modDate );
            log.info( "Creating new user " + index );
            user = c_dom.createElement( USER_TAG );
            c_dom.getDocumentElement().appendChild( user );
            setAttribute( user, CREATED, c_format.format( profile.getCreated() ) );
            isNew = true;
        } else {
            // To update existing user node, delete old attributes first...
            final NodeList attributes = user.getElementsByTagName( ATTRIBUTES_TAG );
            for( int i = 0; i < attributes.getLength(); i++ ) {
                user.removeChild( attributes.item( i ) );
            }
        }

        setAttribute( user, UID, profile.getUid() );
        setAttribute( user, LAST_MODIFIED, c_format.format( modDate ) );
        setAttribute( user, LOGIN_NAME, profile.getLoginName() );
        setAttribute( user, FULL_NAME, profile.getFullname() );
        setAttribute( user, WIKI_NAME, profile.getWikiName() );
        setAttribute( user, EMAIL, profile.getEmail() );
        final Date lockExpiry = profile.getLockExpiry();
        setAttribute( user, LOCK_EXPIRY, lockExpiry == null ? "" : c_format.format( lockExpiry ) );

        // Hash and save the new password if it's different from old one
        final String newPassword = profile.getPassword();
        if( newPassword != null && !newPassword.equals( "" ) ) {
            final String oldPassword = user.getAttribute( PASSWORD );
            if( !oldPassword.equals( newPassword ) ) {
                setAttribute( user, PASSWORD, getHash( newPassword ) );
            }
        }

        // Save the attributes as as Base64 string
        if( profile.getAttributes().size() > 0 ) {
            try {
                final String encodedAttributes = Serializer.serializeToBase64( profile.getAttributes() );
                final Element attributes = c_dom.createElement( ATTRIBUTES_TAG );
                user.appendChild( attributes );
                final Text value = c_dom.createTextNode( encodedAttributes );
                attributes.appendChild( value );
            } catch( final IOException e ) {
                throw new WikiSecurityException( "Could not save user profile attribute. Reason: " + e.getMessage(), e );
            }
        }

        // Set the profile timestamps
        if( isNew ) {
            profile.setCreated( modDate );
        }
        profile.setLastModified( modDate );

        // Commit to disk
        saveDOM();
    }

    /**
     * Private method that returns the first {@link UserProfile}matching a &lt;user&gt; element's supplied attribute. This method will also
     * set the UID if it has not yet been set.
     *
     * @param matchAttribute
     * @param index
     * @return the profile, or <code>null</code> if not found
     */
    private UserProfile findByAttribute( final String matchAttribute, String index ) {
        if ( c_dom == null ) {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }

        checkForRefresh();
        final NodeList users = c_dom.getElementsByTagName( USER_TAG );
        if( users == null ) {
            return null;
        }

        // check if we have to do a case insensitive compare
        boolean caseSensitiveCompare = true;
        if (matchAttribute.equals(EMAIL)) {
            caseSensitiveCompare = false;
        }

        for( int i = 0; i < users.getLength(); i++ ) {
            final Element user = (Element) users.item( i );
            String userAttribute = user.getAttribute( matchAttribute );
            if( !caseSensitiveCompare ) {
                userAttribute = StringUtils.lowerCase(userAttribute);
                index = StringUtils.lowerCase(index);
            }
            if( userAttribute.equals( index ) ) {
                final UserProfile profile = newProfile();

                // Parse basic attributes
                profile.setUid( user.getAttribute( UID ) );
                if( profile.getUid() == null || profile.getUid().length() == 0 ) {
                    profile.setUid( generateUid( this ) );
                }
                profile.setLoginName( user.getAttribute( LOGIN_NAME ) );
                profile.setFullname( user.getAttribute( FULL_NAME ) );
                profile.setPassword( user.getAttribute( PASSWORD ) );
                profile.setEmail( user.getAttribute( EMAIL ) );

                // Get created/modified timestamps
                final String created = user.getAttribute( CREATED );
                final String modified = user.getAttribute( LAST_MODIFIED );
                profile.setCreated( parseDate( profile, created ) );
                profile.setLastModified( parseDate( profile, modified ) );

                // Is the profile locked?
                final String lockExpiry = user.getAttribute( LOCK_EXPIRY );
                if( lockExpiry == null || lockExpiry.length() == 0 ) {
                    profile.setLockExpiry( null );
                } else {
                    profile.setLockExpiry( new Date( Long.parseLong( lockExpiry ) ) );
                }

                // Extract all of the user's attributes (should only be one attributes tag, but you never know!)
                final NodeList attributes = user.getElementsByTagName( ATTRIBUTES_TAG );
                for( int j = 0; j < attributes.getLength(); j++ ) {
                    final Element attribute = ( Element )attributes.item( j );
                    final String serializedMap = extractText( attribute );
                    try {
                        final Map< String, ? extends Serializable > map = Serializer.deserializeFromBase64( serializedMap );
                        profile.getAttributes().putAll( map );
                    } catch( final IOException e ) {
                        log.error( "Could not parse user profile attributes!", e );
                    }
                }

                return profile;
            }
        }
        return null;
    }

    /**
     * Extracts all of the text nodes that are immediate children of an Element.
     *
     * @param element the base element
     * @return the text nodes that are immediate children of the base element, concatenated together
     */
    private String extractText( final Element element ) {
        StringBuilder text = new StringBuilder();
        if( element.getChildNodes().getLength() > 0 ) {
            final NodeList children = element.getChildNodes();
            for( int k = 0; k < children.getLength(); k++ ) {
                final Node child = children.item( k );
                if( child.getNodeType() == Node.TEXT_NODE ) {
                    text.append(((Text) child).getData());
                }
            }
        }
        return text.toString();
    }

    /**
     *  Tries to parse a date using the default format - then, for backwards compatibility reasons, tries the platform default.
     *
     *  @param profile
     *  @param date
     *  @return A parsed date, or null, if both parse attempts fail.
     */
    private Date parseDate( final UserProfile profile, final String date ) {
        try {
            final DateFormat c_format = new SimpleDateFormat( DATE_FORMAT );
            return c_format.parse( date );
        } catch( final ParseException e ) {
            try {
                return DateFormat.getDateTimeInstance().parse( date );
            } catch( final ParseException e2 ) {
                log.warn( "Could not parse 'created' or 'lastModified' attribute for profile '" + profile.getLoginName() + "'." +
                          " It may have been tampered with.", e2 );
            }
        }
        return null;
    }

    /**
     * After loading the DOM, this method sanity-checks the dates in the DOM and makes sure they are formatted properly. This is sort-of
     * hacky, but it should work.
     */
    private void sanitizeDOM() {
        if( c_dom == null ) {
            throw new IllegalStateException( "FATAL: database does not exist" );
        }

        final NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ ) {
            final Element user = ( Element )users.item( i );

            // Sanitize UID (and generate a new one if one does not exist)
            String uid = user.getAttribute( UID ).trim();
            if( uid == null || uid.length() == 0 || "-1".equals( uid ) ) {
                uid = String.valueOf( generateUid( this ) );
                user.setAttribute( UID, uid );
            }

            // Sanitize dates
            final String loginName = user.getAttribute( LOGIN_NAME );
            String created = user.getAttribute( CREATED );
            String modified = user.getAttribute( LAST_MODIFIED );
            final DateFormat c_format = new SimpleDateFormat( DATE_FORMAT );
            try {
                created = c_format.format( c_format.parse( created ) );
                modified = c_format.format( c_format.parse( modified ) );
                user.setAttribute( CREATED, created );
                user.setAttribute( LAST_MODIFIED, modified );
            } catch( final ParseException e ) {
                try {
                    created = c_format.format( DateFormat.getDateTimeInstance().parse( created ) );
                    modified = c_format.format( DateFormat.getDateTimeInstance().parse( modified ) );
                    user.setAttribute( CREATED, created );
                    user.setAttribute( LAST_MODIFIED, modified );
                } catch( final ParseException e2 ) {
                    log.warn( "Could not parse 'created' or 'lastModified' attribute for profile '" + loginName + "'."
                            + " It may have been tampered with." );
                }
            }
        }
    }

    /**
     * Private method that sets an attribute value for a supplied DOM element.
     *
     * @param element the element whose attribute is to be set
     * @param attribute the name of the attribute to set
     * @param value the desired attribute value
     */
    private void setAttribute( final Element element, final String attribute, final String value ) {
        if( value != null ) {
            element.setAttribute( attribute, value );
        }
    }

}