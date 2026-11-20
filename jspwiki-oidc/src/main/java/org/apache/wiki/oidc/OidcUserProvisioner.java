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
package org.apache.wiki.oidc;

import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the profile of a user in the user database of the wiki the first time the user logs in with OpenID Connect, with the full
 * name and the e-mail address that the provider sends, so that the wiki shows the name of the person and can write to them. An
 * existing profile is left as it is, because the user may have changed it in the wiki.
 */
final class OidcUserProvisioner {

    private static final Logger LOG = LoggerFactory.getLogger( OidcUserProvisioner.class );

    private OidcUserProvisioner() {
    }

    /**
     * Creates the profile if the database has none for the login name.
     *
     * @param database the user database of the wiki
     * @param loginName the name the user has in the wiki
     * @param identity the profile that the provider sent
     * @return true if a profile was created
     */
    static boolean provision( final UserDatabase database, final String loginName, final org.pac4j.core.profile.UserProfile identity ) {
        if( loginName == null || loginName.isBlank() ) {
            return false;
        }
        try {
            database.findByLoginName( loginName );
            return false;
        } catch( final NoSuchPrincipalException e ) {
            // not there yet, created below
        }

        String fullName = fullName( identity, loginName );
        if( taken( database, fullName ) ) {
            fullName = loginName;
        }
        final String wikiName = fullName.replaceAll( "\\s+", "" );
        if( taken( database, fullName ) || isWikiNameTaken( database, wikiName ) ) {
            LOG.warn( "The profile of {} was not created, the name {} is already used by another user", loginName, fullName );
            return false;
        }

        final UserProfile profile = database.newProfile();
        profile.setLoginName( loginName );
        profile.setFullname( fullName );
        final Object email = identity.getAttribute( "email" );
        if( email != null && !email.toString().isBlank() ) {
            profile.setEmail( email.toString().trim() );
        }
        try {
            database.save( profile );
            LOG.info( "Created the profile of {} from the OpenID Connect login", loginName );
            return true;
        } catch( final WikiSecurityException e ) {
            LOG.error( "The profile of {} could not be saved", loginName, e );
            return false;
        }
    }

    /** The claim {@code name}, or else the given and the family names, or else the login name. */
    private static String fullName( final org.pac4j.core.profile.UserProfile identity, final String loginName ) {
        final String name = text( identity.getAttribute( "name" ) );
        if( name != null ) {
            return name;
        }
        final String given = text( identity.getAttribute( "given_name" ) );
        final String family = text( identity.getAttribute( "family_name" ) );
        final String joined = ( ( given == null ? "" : given ) + " " + ( family == null ? "" : family ) ).trim();
        return joined.isEmpty() ? loginName : joined;
    }

    private static String text( final Object value ) {
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static boolean taken( final UserDatabase database, final String fullName ) {
        try {
            database.findByFullName( fullName );
            return true;
        } catch( final NoSuchPrincipalException e ) {
            return false;
        }
    }

    private static boolean isWikiNameTaken( final UserDatabase database, final String wikiName ) {
        try {
            database.findByWikiName( wikiName );
            return true;
        } catch( final NoSuchPrincipalException e ) {
            return false;
        }
    }

}
