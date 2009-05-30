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
package org.apache.wiki.auth.user;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.*;

import org.apache.wiki.NoRequiredPropertyException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.CryptoUtil;
import org.apache.wiki.util.HexUtils;


/**
 * Abstract UserDatabase class that provides convenience methods for finding
 * profiles, building Principal collections and hashing passwords.
 * @author Andrew R. Jaquith
 * @since 2.3
 */
public abstract class AbstractUserDatabase implements UserDatabase
{

    protected static final Logger log = LoggerFactory.getLogger( AbstractUserDatabase.class );
    protected static final String SHA_PREFIX = "{SHA}";
    protected static final String SSHA_PREFIX = "{SSHA}";

    /**
     * No-op method that in previous versions of JSPWiki was intended to
     * atomically commit changes to the user database. Now, the {@link #rename(String, String)},
     * {@link #save(UserProfile)} and {@link #deleteByLoginName(String)} methods
     * are atomic themselves.
     * @throws WikiSecurityException
     * @deprecated there is no need to call this method because the save, rename and
     * delete methods contain their own commit logic
     */
    @SuppressWarnings("deprecation")
    public synchronized void commit() throws WikiSecurityException
    { }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that whose login name, full name, or wiki name matches the supplied
     * string. This method provides a "forgiving" search algorithm for resolving
     * principal names when the exact profile attribute that supplied the name
     * is unknown.
     * @param index the login name, full name, or wiki name
     * @see org.apache.wiki.auth.user.UserDatabase#find(java.lang.String)
     */
    public UserProfile find( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = null;

        // Try finding by full name
        try
        {
            profile = findByFullName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }

        // Try finding by wiki name
        try
        {
            profile = findByWikiName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }

        // Try finding by login name
        try
        {
            profile = findByLoginName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }

        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByEmail(java.lang.String)
     */
    public abstract UserProfile findByEmail( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    public abstract UserProfile findByFullName( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    public abstract UserProfile findByLoginName( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    public abstract UserProfile findByWikiName( String index ) throws NoSuchPrincipalException;

    /**
     * <p>Looks up the Principals representing a user from the user database. These
     * are defined as a set of WikiPrincipals manufactured from the login name,
     * full name, and wiki name. If the user database does not contain a user
     * with the supplied identifier, throws a {@link NoSuchPrincipalException}.</p>
     * <p>When this method creates WikiPrincipals, the Principal containing
     * the user's full name is marked as containing the common name (see
     * {@link org.apache.wiki.auth.WikiPrincipal#WikiPrincipal(String, String)}).
     * @param identifier the name of the principal to retrieve; this corresponds to
     *            value returned by the user profile's
     *            {@link UserProfile#getLoginName()}method.
     * @return the array of Principals representing the user
     * @see org.apache.wiki.auth.user.UserDatabase#getPrincipals(java.lang.String)
     * @throws NoSuchPrincipalException {@inheritDoc}
     */
    public Principal[] getPrincipals( String identifier ) throws NoSuchPrincipalException
    {
        try
        {
            UserProfile profile = findByLoginName( identifier );
            ArrayList<Principal> principals = new ArrayList<Principal>();
            if ( profile.getLoginName() != null && profile.getLoginName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getLoginName(), WikiPrincipal.LOGIN_NAME ) );
            }
            if ( profile.getFullname() != null && profile.getFullname().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getFullname(), WikiPrincipal.FULL_NAME ) );
            }
            if ( profile.getWikiName() != null && profile.getWikiName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getWikiName(), WikiPrincipal.WIKI_NAME ) );
            }
            return principals.toArray( new Principal[principals.size()] );
        }
        catch( NoSuchPrincipalException e )
        {
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#initialize(org.apache.wiki.WikiEngine, java.util.Properties)
     */
    public abstract void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException;

    /**
     * Factory method that instantiates a new DefaultUserProfile with a new, distinct
     * unique identifier.
     * 
     * @return A new, empty profile.
     */
    public UserProfile newProfile()
    {
        return DefaultUserProfile.newProfile( this );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#save(org.apache.wiki.auth.user.UserProfile)
     */
    public abstract void save( UserProfile profile ) throws WikiSecurityException;

    /**
     * Validates the password for a given user. If the user does not exist in
     * the user database, this method always returns <code>false</code>. If
     * the user exists, the supplied password is compared to the stored
     * password. Note that if the stored password's value starts with
     * <code>{SHA}</code>, the supplied password is hashed prior to the
     * comparison.
     * @param loginName the user's login name
     * @param password the user's password (obtained from user input, e.g., a web form)
     * @return <code>true</code> if the supplied user password matches the
     * stored password
     * @throws NoSuchAlgorithmException 
     * @see org.apache.wiki.auth.user.UserDatabase#validatePassword(java.lang.String,
     *      java.lang.String)
     */
    public boolean validatePassword( String loginName, String password )
    {
        String hashedPassword;
        try
        {
            UserProfile profile = findByLoginName( loginName );
            String storedPassword = profile.getPassword();
            
            // Is the password stored as a salted hash (the new 2.8 format?)
            boolean newPasswordFormat = storedPassword.startsWith( SSHA_PREFIX );
            
            // If new format, verify the hash
            if ( newPasswordFormat )
            {
                hashedPassword = getHash( password );
                return CryptoUtil.verifySaltedPassword( password.getBytes("UTF-8"), storedPassword );
            }

            // If old format, verify using the old SHA verification algorithm
            if ( storedPassword.startsWith( SHA_PREFIX ) )
            {
                storedPassword = storedPassword.substring( SHA_PREFIX.length() );
            }
            hashedPassword = getOldHash( password );
            boolean verified = hashedPassword.equals( storedPassword ); 
            
            // If in the old format and password verified, upgrade the hash to SSHA
            if ( verified )
            {
                profile.setPassword( password );
                save( profile );
            }
            
            return verified;
        }
        catch( NoSuchPrincipalException e )
        {
        }
        catch( NoSuchAlgorithmException e )
        {
            log.error( "Unsupported algorithm: " + e.getMessage() );
        }
        catch( UnsupportedEncodingException e )
        {
            log.error( "You do not have UTF-8!?!" );
        }
        catch( WikiSecurityException e )
        {
            log.error( "Could not upgrade SHA password to SSHA because profile could not be saved. Reason: " + e.getMessage() );
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generates a new random user identifier (uid) that is guaranteed to be unique.
     * 
     * @param db The database for which the UID should be generated.
     * @return A random, unique UID.
     */
    protected static String generateUid( UserDatabase db )
    {
        // Keep generating UUIDs until we find one that doesn't collide
        String uid = null;
        boolean collision;
        
        do 
        {
            uid = UUID.randomUUID().toString();
            collision = true;
            try
            {
                db.findByUid( uid );
            }
            catch ( NoSuchPrincipalException e )
            {
                collision = false;
            }
        } 
        while ( collision || uid == null );
        return uid;
    }
    
    /**
     * Private method that calculates the salted SHA-1 hash of a given
     * <code>String</code>. Note that as of JSPWiki 2.8, this method calculates
     * a <em>salted</em> hash rather than a plain hash.
     * @param text the text to hash
     * @return the result hash
     */
    protected String getHash( String text )
    {
        String hash = null;
        try
        {
            hash = CryptoUtil.getSaltedPassword( text.getBytes("UTF-8") );
        }
        catch( NoSuchAlgorithmException e )
        {
            log.error( "Error creating salted SHA password hash:" + e.getMessage() );
            hash = text;
        }
        catch( UnsupportedEncodingException e )
        {
            log.error("You do not have UTF-8!?!");
        }
        return hash;
    }

    /**
     * Private method that calculates the SHA-1 hash of a given
     * <code>String</code>
     * @param text the text to hash
     * @return the result hash
     * @deprecated this method is retained for backwards compatibility purposes; use {@link #getHash(String)} instead
     */
    protected String getOldHash( String text )
    {
        String hash = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA" );
            md.update( text.getBytes("UTF-8") );
            byte[] digestedBytes = md.digest();
            hash = HexUtils.convert( digestedBytes );
        }
        catch( NoSuchAlgorithmException e )
        {
            log.error( "Error creating SHA password hash:" + e.getMessage() );
            hash = text;
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("UTF-8 not supported!?!");
        }
        return hash;
    }

    /**
     * Parses a long integer from a supplied string, or returns 0 if not parsable.
     * @param value the string to parse
     * @return the value parsed
     */
    protected long parseLong( String value )
    {
        if ( value == null || value.length() == 0 )
        {
            return 0;
        }
        try
        {
            return Long.parseLong( value );
        }
        catch ( NumberFormatException e )
        {
            return 0;
        }
    }

}
