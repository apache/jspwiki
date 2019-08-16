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

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Class for representing wiki user information, such as the login name, full
 * name, wiki name, and e-mail address. Note that since 2.6 the wiki name is
 * required to be automatically computed from the full name.
 * As of 2.8, user profiles can store custom key/value String/Serializable attributes, and store
 * a unique ID. Locks are checked by {@link org.apache.wiki.auth.AuthenticationManager};
 * if a profile is locked, the user cannot log with that profile.
 * @since 2.3
 */
public interface UserProfile extends Serializable
{

    /**
     * Returns the attributes associated with this profile as a Map of key/value pairs.
     * The Map should generally be a "live" Map; changes to the keys or values will be reflected
     * in the UserProfile.
     * @return the attributes
     */
    Map<String,Serializable> getAttributes();

    /**
     * Returns the creation date.
     * @return the creation date
     */
    Date getCreated();

    /**
     * Returns the user's e-mail address.
     * @return the e-mail address
     */
    String getEmail();

    /**
     * Returns the user's full name.
     * @return the full name
     */
    String getFullname();

    /**
     * Returns the last-modified date.
     * @return the date and time of last modification
     */
    Date getLastModified();

    /**
     * Returns the date/time of expiration of the profile's lock, if it has been
     * previously locked via {@link #setLockExpiry(Date)} and the lock is
     * still active. If the profile is unlocked, this method returns <code>null</code>.
     * Note that calling this method after the expiration date, <em>even if had previously
     * been set explicitly by {@link #setLockExpiry(Date)}</em>, will always return
     * <code>null</null>.
     * 
     * @return the lock expiration date
     */
    Date getLockExpiry();

    /**
     * Returns the user's login name.
     * @return the login name
     */
    String getLoginName();

    /**
     * Returns the user password for use with custom authentication. Note that
     * the password field is not meaningful for container authentication; the
     * user's private credentials are generally stored elsewhere. While it
     * depends on the {@link UserDatabase}implementation, in most cases the
     * value returned by this method will be a password hash, not the password
     * itself.
     * @return the password
     */
    String getPassword();

    /**
     * Returns the unique identifier for the user profile. If not previously
     * set, the value will be <code>null</code>.
     * @return the unique ID.
     */
    String getUid();
    
    /**
     * Returns the user's wiki name, based on the full name with all
     * whitespace removed.
     * @return the wiki name.
     */
    String getWikiName();

    /**
     * Returns
     * <code>true</code> if the profile is currently locked (disabled); <code>false</code> otherwise.
     * By default, profiles are created unlocked. Strictly speaking, calling this method is equivalent to calling {@link #getLockExpiry()}
     * and, if it returns a non-<code>null</code> value, checking if the date returned is later than the current time.
     * @return the result
     */
    boolean isLocked();

    /**
     * Returns <code>true</code> if the profile has never been
     * saved before. Implementing classes might check the
     * last modified date, for example, to determine this.
     * @return whether the profile is new
     */
    boolean isNew();

    /**
     * Sets the created date.
     * @param date the creation date
     */
    void setCreated( Date date );

    /**
     * Sets the user's e-mail address.
     * @param email the e-mail address
     */
    void setEmail( String email );

    /**
     * Sets the user's full name. For example, "Janne Jalkanen."
     * @param arg the full name
     */
    void setFullname( String arg );

    /**
     * Sets the last-modified date
     * @param date the last-modified date
     */
    void setLastModified( Date date );

    /**
     * Locks the profile until a specified lock expiration date.
     * 
     * @param expiry the date the lock expires; setting this value to <code>null</code>
     * will cause the lock to be cleared.
     */
    void setLockExpiry( Date expiry );
    
    /**
     * Sets the name by which the user logs in. The login name is used as the
     * username for custom authentication (see
     * {@link org.apache.wiki.auth.AuthenticationManager#login(WikiSession,HttpServletRequest, String, String)},
     * {@link org.apache.wiki.auth.login.UserDatabaseLoginModule}). The login
     * name is typically a short name ("jannej"). In contrast, the wiki name is
     * typically of type FirstnameLastName ("JanneJalkanen").
     * @param name the login name
     */
    void setLoginName( String name );

    /**
     * Sets the user's password for use with custom authentication. It is
     * <em>not</em> the responsibility of implementing classes to hash the
     * password; that responsibility is borne by the UserDatabase implementation
     * during save operations (see {@link UserDatabase#save(UserProfile)}).
     * Note that the password field is not meaningful for container
     * authentication; the user's private credentials are generally stored
     * elsewhere.
     * @param arg the password
     */
    void setPassword( String arg );

    /**
     * Sets the unique identifier for the user profile. Note that UserDatabase implementations
     * are required <em>not</em> to change the unique identifier after the initial save.
     * @param uid the unique identifier to set
     */
    void setUid( String uid );

    /**
     * Returns a string representation of this user profile.
     * @return the string
     */
    String toString();
}
