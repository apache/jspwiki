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

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.auth.NoSuchPrincipalException;

import java.security.Principal;
import java.util.Properties;


/**
 * This is a database that gets used if nothing else is available. It does nothing of note - it just mostly throws
 * NoSuchPrincipalExceptions if someone tries to log in.
 */
public class DummyUserDatabase extends AbstractUserDatabase {

    /**
     * No-op.
     * @param loginName the login name to delete
     */
    @Override
    public void deleteByLoginName( final String loginName ) {
        // No operation
    }

    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param index the name to search for
     * @return the user profile
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public UserProfile findByEmail(final String index) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }

    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param index the name to search for
     * @return the user profile
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public UserProfile findByFullName(final String index) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }

    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param index the name to search for
     * @return the user profile
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public UserProfile findByLoginName(final String index) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }

    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param uid the unique identifier to search for
     * @return the user profile
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public UserProfile findByUid( final String uid ) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }
    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param index the name to search for
     * @return the user profile
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public UserProfile findByWikiName(final String index) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }

    /**
     * No-op.
     * @return a zero-length array
     */
    @Override
    public Principal[] getWikiNames() {
        return new Principal[0];
    }

    /**
     * No-op.
     *
     * @param engine the wiki engine
     * @param props the properties used to initialize the wiki engine
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) {
    }

    /**
     * No-op; always throws <code>NoSuchPrincipalException</code>.
     * @param loginName the login name
     * @param newName the proposed new login name
     * @throws NoSuchPrincipalException always...
     */
    @Override
    public void rename( final String loginName, final String newName ) throws NoSuchPrincipalException {
        throw new NoSuchPrincipalException("No user profiles available");
    }

    /**
     * No-op.
     * @param profile the user profile
     */
    @Override
    public void save( final UserProfile profile ) {
    }

}
