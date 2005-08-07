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

import java.security.Principal;
import java.util.Properties;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * Defines an interface for loading, persisting and storing users.
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.4 $ $Date: 2005-08-07 21:15:19 $
 * @since 2.3
 */
public interface UserDatabase
{

    /**
     * Persists the current state of the user database to back-end storage. This
     * method is intended to be atomic; results cannot be partially committed.
     * If the commit fails, it should roll back its state appropriately.
     * Implementing classes that persist to the file system may wish to make
     * this method <code>synchronized</code>.
     * @throws WikiSecurityException
     */
    public void commit() throws WikiSecurityException;

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
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#WikiPrincipal(String, String)}
     * with the <code>type</code> parameter set to
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#FULL_NAME}. The method
     * {@link com.ecyrd.jspwiki.WikiSession#getUserPrincipal()} will return this
     * principal as the "primary" principal. Note that this method can also be
     * used to mark a WikiPrincipal as a login name or a wiki name.
     * </p>
     * @param identifier the name of the user to retrieve; this corresponds to
     *            value returned by the user profile's
     *            {@link UserProfile#getLoginName()} method.
     * @return the array of Principals representing the user's identities
     */
    public Principal[] getPrincipals( String identifier ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that whose login name, full name, or wiki name matches the supplied
     * string. This method provides a "forgiving" search algorithm for resolving
     * Principal names when the exact profile attribute that supplied the name
     * is unknown.
     * @param index the login name, full name, or wiki name
     */
    public UserProfile find( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given e-mail address. If the user
     * database does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the e-mail address of the desired user profile
     * @return the user profile
     */
    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the login name of the desired user profile
     * @return the user profile
     */
    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given wiki name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the wiki name of the desired user profile
     * @return the user profile
     */
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException;

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given full name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the fill name of the desired user profile
     * @return the user profile
     */
    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException;

    /**
     * Initializes the user database based on values from a Properties object.
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException;

    /**
     * Factory method that instantiates a new user profile.
     * The {@link UserProfile#isNew()} method of profiles created using
     * this method should return <code>true</code>.
     */
    public UserProfile newProfile();

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
     * <p>Implementations are not required to time-stamp the creation
     * or modification fields of the UserProfile; this should be
     * handled by the calling class (such as UserManager)./p>
     * <p>
     * Calling classes should generally also call
     * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#refreshCredentials(WikiSession)} after
     * calling this method to ensure that Principals are reloaded into the
     * current WikiSession's Subject.
     * </p>
     * @param profile the user profile to save
     * @throws WikiSecurityException if the profile cannot be saved
     */
    public void save( UserProfile profile ) throws WikiSecurityException;

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
    public boolean validatePassword( String loginName, String password );

}