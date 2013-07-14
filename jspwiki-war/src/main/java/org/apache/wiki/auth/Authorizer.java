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
package org.apache.wiki.auth;

import java.security.Principal;
import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;

/**
 * Interface for service providers of authorization information. After a user
 * successfully logs in, the
 * {@link org.apache.wiki.auth.AuthenticationManager} consults the configured
 * Authorizer to determine which additional
 * {@link org.apache.wiki.auth.authorize.Role} principals should be added to
 * the user's WikiSession. To determine which roles should be injected, the
 * Authorizer is queried for the roles it knows about by calling
 * {@link org.apache.wiki.auth.Authorizer#getRoles()}. Then, each role
 * returned by the Authorizer is tested by calling
 * {@link org.apache.wiki.auth.Authorizer#isUserInRole(WikiSession, Principal)}.
 * If this check fails, and the Authorizer is of type WebAuthorizer,
 * AuthenticationManager checks the role again by calling
 * {@link org.apache.wiki.auth.authorize.WebAuthorizer#isUserInRole(javax.servlet.http.HttpServletRequest, Principal)}).
 * Any roles that pass the test are injected into the Subject by firing
 * appropriate authentication events.
 * 
 * @since 2.3
 */
public interface Authorizer
{

    /**
     * Returns an array of role Principals this Authorizer knows about. This
     * method will always return an array; an implementing class may choose to
     * return an zero-length array if it has no ability to identify the roles
     * under its control.
     * 
     * @return an array of Principals representing the roles
     */
    Principal[] getRoles();

    /**
     * Looks up and returns a role Principal matching a given String. If a
     * matching role cannot be found, this method returns <code>null</code>.
     * Note that it may not always be feasible for an Authorizer implementation
     * to return a role Principal.
     * 
     * @param role the name of the role to retrieve
     * @return the role Principal
     */
    Principal findRole( String role );

    /**
     * Initializes the authorizer.
     * 
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     * @throws WikiSecurityException if the Authorizer could not be initialized
     */
    void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException;

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>.
     * 
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    boolean isUserInRole( WikiSession session, Principal role );

}
