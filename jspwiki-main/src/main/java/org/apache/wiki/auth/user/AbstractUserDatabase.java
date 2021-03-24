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

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.util.ByteUtils;
import org.apache.wiki.util.CryptoUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

/**
 * Abstract UserDatabase class that provides convenience methods for finding profiles, building Principal collections and hashing passwords.
 *
 * @since 2.3
 */
public abstract class AbstractUserDatabase implements UserDatabase {

    protected static final Logger log = Logger.getLogger( AbstractUserDatabase.class );
    protected static final String SHA_PREFIX = "{SHA}";
    protected static final String SSHA_PREFIX = "{SSHA}";
    protected static final String SHA256_PREFIX = "{SHA-256}";

    /**
     * Looks up and returns the first {@link UserProfile} in the user database that whose login name, full name, or wiki name matches the
     * supplied string. This method provides a "forgiving" search algorithm for resolving principal names when the exact profile attribute
     * that supplied the name is unknown.
     *
     * @param index the login name, full name, or wiki name
     * @see org.apache.wiki.auth.user.UserDatabase#find(java.lang.String)
     */
    @Override
    public UserProfile find( final String index ) throws NoSuchPrincipalException {
        UserProfile profile = null;

        // Try finding by full name
        try {
            profile = findByFullName( index );
        } catch( final NoSuchPrincipalException e ) {
        }
        if( profile != null ) {
            return profile;
        }

        // Try finding by wiki name
        try {
            profile = findByWikiName( index );
        } catch( final NoSuchPrincipalException e ) {
        }
        if( profile != null ) {
            return profile;
        }

        // Try finding by login name
        try {
            profile = findByLoginName( index );
        } catch( final NoSuchPrincipalException e ) {
        }
        if( profile != null ) {
            return profile;
        }

        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByEmail(java.lang.String)
     */
    @Override
    public abstract UserProfile findByEmail( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    @Override
    public abstract UserProfile findByFullName( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    @Override
    public abstract UserProfile findByLoginName( String index ) throws NoSuchPrincipalException;

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    @Override
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
     * @throws NoSuchPrincipalException If the user database does not contain user with the supplied identifier
     */
    @Override
    public Principal[] getPrincipals( final String identifier ) throws NoSuchPrincipalException {
        final UserProfile profile = findByLoginName( identifier );
        final ArrayList< Principal > principals = new ArrayList<>();
        if( profile.getLoginName() != null && !profile.getLoginName().isEmpty() ) {
            principals.add( new WikiPrincipal( profile.getLoginName(), WikiPrincipal.LOGIN_NAME ) );
        }
        if( profile.getFullname() != null && !profile.getFullname().isEmpty() ) {
            principals.add( new WikiPrincipal( profile.getFullname(), WikiPrincipal.FULL_NAME ) );
        }
        if( profile.getWikiName() != null && !profile.getWikiName().isEmpty() ) {
            principals.add( new WikiPrincipal( profile.getWikiName(), WikiPrincipal.WIKI_NAME ) );
        }
        return principals.toArray( new Principal[0] );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.wiki.auth.user.UserDatabase#initialize(org.apache.wiki.api.core.Engine, java.util.Properties)
     */
    @Override
    public abstract void initialize( Engine engine, Properties props ) throws NoRequiredPropertyException, WikiSecurityException;

    /**
     * Factory method that instantiates a new DefaultUserProfile with a new, distinct unique identifier.
     * 
     * @return A new, empty profile.
     */
    @Override
    public UserProfile newProfile() {
        final UserProfile profile = new DefaultUserProfile();
        profile.setUid( AbstractUserDatabase.generateUid( this ) );
        return profile;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.auth.user.UserDatabase#save(org.apache.wiki.auth.user.UserProfile)
     */
    @Override
    public abstract void save( UserProfile profile ) throws WikiSecurityException;

    /**
     * Validates the password for a given user. If the user does not exist in the user database, this method always returns
     * <code>false</code>. If the user exists, the supplied password is compared to the stored password. Note that if the stored password's
     * value starts with <code>{SHA}</code>, the supplied password is hashed prior to the comparison.
     *
     * @param loginName the user's login name
     * @param password the user's password (obtained from user input, e.g., a web form)
     * @return <code>true</code> if the supplied user password matches the stored password
     * @see org.apache.wiki.auth.user.UserDatabase#validatePassword(java.lang.String, java.lang.String)
     */
    @Override
    public boolean validatePassword( final String loginName, final String password ) {
        String hashedPassword;
        try {
            final UserProfile profile = findByLoginName( loginName );
            String storedPassword = profile.getPassword();
            boolean verified = false;

            // If the password is stored as SHA-256 or SSHA, verify the hash
            if( storedPassword.startsWith( SHA256_PREFIX ) || storedPassword.startsWith( SSHA_PREFIX ) ) {
                verified = CryptoUtil.verifySaltedPassword( password.getBytes( StandardCharsets.UTF_8 ), storedPassword );
            }

            // Use older verification algorithm if password is stored as SHA
            if( storedPassword.startsWith( SHA_PREFIX ) ) {
                storedPassword = storedPassword.substring( SHA_PREFIX.length() );
                hashedPassword = getShaHash( password );
                verified = hashedPassword.equals( storedPassword );
            }

            // If in the old format and password verified, upgrade the hash to SSHA
            if( verified && !storedPassword.startsWith( SHA256_PREFIX ) ) {
                profile.setPassword( password );
                save( profile );
            }

            return verified;
        } catch( final NoSuchPrincipalException e ) {
        } catch( final NoSuchAlgorithmException e ) {
            log.error( "Unsupported algorithm: " + e.getMessage() );
        } catch( final WikiSecurityException e ) {
            log.error( "Could not upgrade SHA password to SSHA because profile could not be saved. Reason: " + e.getMessage(), e );
        }
        return false;
    }

    /**
     * Generates a new random user identifier (uid) that is guaranteed to be unique.
     * 
     * @param db The database for which the UID should be generated.
     * @return A random, unique UID.
     */
    protected static String generateUid( final UserDatabase db ) {
        // Keep generating UUIDs until we find one that doesn't collide
        String uid;
        boolean collision;
        
        do {
            uid = UUID.randomUUID().toString();
            collision = true;
            try {
                db.findByUid( uid );
            } catch ( final NoSuchPrincipalException e ) {
                collision = false;
            }
        } 
        while ( collision || uid == null );
        return uid;
    }
    
    /**
     * Private method that calculates the salted SHA-1 or SHA-256 hash of a given <code>String</code>. Note that as of JSPWiki 2.8, this method
     * calculates a <em>salted</em> hash rather than a plain hash.
     *
     * @param text the text to hash
     * @param text the algorithm used for the hash
     * @return the result hash
     */
    protected String getHash( final String text ) {
        try {
            return CryptoUtil.getSaltedPassword( text.getBytes(StandardCharsets.UTF_8), SHA256_PREFIX );
        } catch( final NoSuchAlgorithmException e ) {
            log.error( String.format( "Error creating salted password hash: %s", e.getMessage() ) );
            return text;
        }
    }

    /**
     * Private method that calculates the SHA-1 hash of a given <code>String</code>
     *
     * @param text the text to hash
     * @return the result hash
     * @deprecated this method is retained for backwards compatibility purposes; use {@link #getHash(String, String)} instead
     */
    @Deprecated
    String getShaHash(final String text ) {
        try {
            final MessageDigest md = MessageDigest.getInstance( "SHA" );
            md.update( text.getBytes( StandardCharsets.UTF_8 ) );
            final byte[] digestedBytes = md.digest();
            return ByteUtils.bytes2hex( digestedBytes );
        } catch( final NoSuchAlgorithmException e ) {
            log.error( "Error creating SHA password hash:" + e.getMessage() );
            return text;
        }
    }

    /**
     * Parses a long integer from a supplied string, or returns 0 if not parsable.
     *
     * @param value the string to parse
     * @return the value parsed
     */
    protected long parseLong( final String value ) {
        if( NumberUtils.isParsable( value ) ) {
            return Long.parseLong( value );
        } else {
            return 0L;
        }
    }

}
