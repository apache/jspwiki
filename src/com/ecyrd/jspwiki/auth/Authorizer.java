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
package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * Interface for service providers of authorization information.
 * @author Andrew Jaquith
 * @since 2.3
 */
public interface Authorizer
{

    /**
     * Returns an array of role Principals this Authorizer knows about.
     * This method will always return an array; an implementing class may
     * choose to return an zero-length array if it has no ability to identify
     * the roles under its control.
     * @return an array of Principals representing the roles
     */
    public Principal[] getRoles();

    /**
     * Looks up and returns a role Principal matching a given String.
     * If a matching role cannot be found, this method returns <code>null</code>.
     * Note that it may not always be feasible for an Authorizer
     * implementation to return a role Principal.
     * @param role the name of the role to retrieve
     * @return the role Principal
     */
    public Principal findRole( String role );

    /**
     * Initializes the authorizer.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     * @throws WikiSecurityException if the Authorizer could not be initialized
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException;

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>.
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     * <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiSession session, Principal role );

}
