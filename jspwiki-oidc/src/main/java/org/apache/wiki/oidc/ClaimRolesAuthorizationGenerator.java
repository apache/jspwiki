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

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;

import com.nimbusds.jwt.JWTParser;

/**
 * Takes the roles of the user from a claim of the profile or of the access token, as {@code roles} in Azure or
 * {@code realm_access.roles} in Keycloak.
 */
class ClaimRolesAuthorizationGenerator implements AuthorizationGenerator {

    private final String claim;
    private final Map< String, String > mapping;

    ClaimRolesAuthorizationGenerator( final String claim, final Map< String, String > mapping ) {
        this.claim = claim;
        this.mapping = mapping;
    }

    /** {@inheritDoc} */
    @Override
    public Optional< UserProfile > generate( final CallContext context, final UserProfile profile ) {
        final Map< String, Object > claims = new HashMap<>( profile.getAttributes() );
        if( profile instanceof OidcProfile && ( ( OidcProfile )profile ).getAccessToken() != null ) {
            try {
                claims.putAll( JWTParser.parse( ( ( OidcProfile )profile ).getAccessToken().getValue() ).getJWTClaimsSet().getClaims() );
            } catch( final ParseException e ) {
                // the access token is opaque, so the roles can only be in the profile
            }
        }
        profile.addRoles( rolesFrom( claims ) );
        return Optional.of( profile );
    }

    /**
     * Reads the roles from a set of claims.
     *
     * @param claims the claims of the profile and of the access token
     * @return the names of the roles of the wiki
     */
    Set< String > rolesFrom( final Map< String, Object > claims ) {
        final Set< String > roles = new LinkedHashSet<>();
        final Object value = lookup( claims, claim );
        if( value instanceof Collection ) {
            for( final Object item : ( Collection< ? > )value ) {
                add( roles, item );
            }
        } else {
            add( roles, value );
        }
        return roles;
    }

    private void add( final Set< String > roles, final Object value ) {
        if( value != null && !value.toString().isBlank() ) {
            final String text = value.toString().trim();
            roles.add( mapping.getOrDefault( text, text ) );
        }
    }

    /**
     * Looks a claim up by its name or, if there is none with that name, by a path of names separated by dots through nested claims.
     */
    static Object lookup( final Map< String, Object > claims, final String path ) {
        if( claims.containsKey( path ) ) {
            return claims.get( path );
        }
        Object current = claims;
        for( final String part : path.split( "\\." ) ) {
            if( !( current instanceof Map ) ) {
                return null;
            }
            current = ( ( Map< ?, ? > )current ).get( part );
        }
        return current;
    }

}
