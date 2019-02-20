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

import java.security.Principal;
import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;

/**
 * Defines an interface for loading, persisting and storing users.
 * @since 2.3
 */
public interface UserDatabase
{

    /**
     * Looks up and deletes the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}. This method is intended to be atomic;
     * results cannot be partially committed. If the commit fails, it should
     * roll back its state appropriately. Implementing classes that persist
     * to the file system may wish to make this method <code>synchronized</code>.
     * @param loginName the login name of the user profile that shall be deleted
     */
    void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException;

    /**
     * <p>
     * Looks up the Principals representing a user from the user database. These
     * are defined as a set of Principals manufactured from the login name, full
     * name, and wiki name. The order of the Principals returned is not
     * significant. If the user database does not contain a user with the
     * supplied identifier, throws a {@link NoSuchPrincipalException}.
     * </p>
     * <p>
     * Note that if an implememtation wishes to mark one of the returned
     * Principals as representing the user's common name, it should instantiate
     * this Principal using
     * {@link org.apache.wiki.auth.WikiPrincipal#WikiPrincipal(String, String)}
     * with the <code>type</code> parameter set to
     * {@link org.apache.wiki.auth.WikiPrincipal#WIKI_NAME}. The method
     * {@link org.apache.wiki.WikiSession#getUserPrincipal()} will return this
     * principal as the "primary" principal. Note that this method can also be
     * used to mark a WikiPrincipal as a login name or a wiki name.
     * </p>
     * @param identifier the name of the user to retrieve; this corresponds to
     *            value returned by the user profile's
     *            {@link UserProfile#getLoginName()} method.
     * @return the array of Principals representing the user's identities
     */
    Principal[] getPrincipals( String identifier ) throws NoSuchPrincipalException;

    /**
     * Returns all WikiNames that are stored in the UserDatabase
     * as an array of Principal objects. If the database does not
     * contain any profiles, this method will return a zero-length
     * array.
     * @return the WikiNames
     */
    Principal[] getWikiNames() throws WikiSecurityException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that whose login name, full name, or wiki name matches the supplied
     * string. This method provides a "forgiving" search algorithm for resolving
     * Principal names when the exact profile attribute that supplied the name
     * is unknown.
     * @param index the login name, full name, or wiki name
     */
    UserProfile find( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given e-mail address. If the user
     * database does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the e-mail address of the desired user profile
     * @return the user profile
     */
    UserProfile findByEmail( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the login name of the desired user profile
     * @return the user profile
     */
    UserProfile findByLoginName( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given unique ID (uid). If the user database
     * does not contain a user with a unique ID, it throws a
     * {@link NoSuchPrincipalException}.
     * @param uid the unique identifier of the desired user profile
     * @return the user profile
     * @since 2.8
     */
    UserProfile findByUid( String uid ) throws NoSuchPrincipalException;
    
    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given wiki name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the wiki name of the desired user profile
     * @return the user profile
     */
    UserProfile findByWikiName( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given full name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the fill name of the desired user profile
     * @return the user profile
     */
    UserProfile findByFullName( String index ) throws NoSuchPrincipalException;

    /**
     * Initializes the user database based on values from a Properties object.
     */
    void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException, WikiSecurityException;

    /**
     * Factory method that instantiates a new user profile.
     * The {@link UserProfile#isNew()} method of profiles created using
     * this method should return <code>true</code>.
     */
    UserProfile newProfile();

    /**
     * <p>Renames a {@link UserProfile} in the user database by changing
     * the profile's login name. Because the login name is the profile's unique
     * identifier, implementations should verify that the identifier is
     * "safe" to change before actually changing it. Specifically: the profile
     * with the supplied login name must already exist, and the proposed new
     * name must not be in use by another profile.</p>
     * <p>This method is intended to be atomic; results cannot be partially committed.
     * If the commit fails, it should roll back its state appropriately.
     * Implementing classes that persist to the file system may wish to make
     * this method <code>synchronized</code>.</p>
     * @param loginName the existing login name for the profile
     * @param newName the proposed new login name
     * @throws NoSuchPrincipalException if the user profile identified by
     * <code>loginName</code> does not exist
     * @throws DuplicateUserException if another user profile with the
     * proposed new login name already exists
     * @throws WikiSecurityException if the profile cannot be renamed for
     * any reason, such as an I/O error, database connection failure
     * or lack of support for renames.
     */
    void rename( String loginName, String newName ) throws NoSuchPrincipalException, DuplicateUserException, WikiSecurityException;

    /**
     * <p>
     * Saves a {@link UserProfile}to the user database, overwriting the
     * existing profile if it exists. The user name under which the profile
     * should be saved is returned by the supplied profile's
     * {@link UserProfile#getLoginName()} method.
     * </p>
     * <p>
     * The database implementation is responsible for detecting potential
     * duplicate user profiles; specifically, the login name, wiki name, and
     * full name must be unique. The implementation is not required to check for
     * validity of passwords or e-mail addresses. Special case: if the profile
     * already exists and the password is null, it should retain its previous
     * value, rather than being set to null.
     * </p>
     * <p>Implementations are <em>required</em> to time-stamp the creation
     * or modification fields of the UserProfile./p>
     * <p>This method is intended to be atomic; results cannot be partially committed.
     * If the commit fails, it should roll back its state appropriately.
     * Implementing classes that persist to the file system may wish to make
     * this method <code>synchronized</code>.</p>
     * @param profile the user profile to save
     * @throws WikiSecurityException if the profile cannot be saved
     */
    void save( UserProfile profile ) throws WikiSecurityException;

    /**
     * Determines whether a supplied user password is valid, given a login name
     * and password. It is up to the implementing class to determine how the
     * comparison should be made. For example, the password might be hashed
     * before comparing it to the value persisted in the back-end data store.
     * @param loginName the login name
     * @param password the password
     * @return <code>true</code> if the password is valid, <code>false</code>
     *         otherwise
     */
    boolean validatePassword( String loginName, String password );

}
