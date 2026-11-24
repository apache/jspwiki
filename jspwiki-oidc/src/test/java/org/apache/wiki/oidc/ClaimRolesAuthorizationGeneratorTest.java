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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClaimRolesAuthorizationGeneratorTest {

    @Test
    void readsAClaimWithAListOfRoles() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "roles", Map.of() );
        Assertions.assertEquals( Set.of( "Admin", "Editors" ), generator.rolesFrom( Map.of( "roles", List.of( "Admin", "Editors" ) ) ) );
    }

    @Test
    void readsAClaimWithASingleRole() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "roles", Map.of() );
        Assertions.assertEquals( Set.of( "Admin" ), generator.rolesFrom( Map.of( "roles", "Admin" ) ) );
    }

    @Test
    void followsADottedPathThroughNestedClaims() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "realm_access.roles", Map.of() );
        final Map< String, Object > claims = Map.of( "realm_access", Map.of( "roles", List.of( "Authenticated", "Admin" ) ) );
        Assertions.assertEquals( Set.of( "Authenticated", "Admin" ), generator.rolesFrom( claims ) );
    }

    @Test
    void prefersAClaimWhoseNameHasDots() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "https://example.org/roles", Map.of() );
        Assertions.assertEquals( Set.of( "Admin" ), generator.rolesFrom( Map.of( "https://example.org/roles", List.of( "Admin" ) ) ) );
    }

    @Test
    void mapsIdentifiersToRolesAndKeepsTheOtherValues() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "groups", Map.of( "1111-aaaa", "Admin" ) );
        Assertions.assertEquals( Set.of( "Admin", "Readers" ), generator.rolesFrom( Map.of( "groups", List.of( "1111-aaaa", "Readers" ) ) ) );
    }

    @Test
    void givesNoRolesWhenTheClaimIsMissingOrBlank() {
        final ClaimRolesAuthorizationGenerator generator = new ClaimRolesAuthorizationGenerator( "roles", Map.of() );
        Assertions.assertTrue( generator.rolesFrom( Map.of() ).isEmpty() );
        Assertions.assertTrue( generator.rolesFrom( Map.of( "roles", List.of( " ", "" ) ) ).isEmpty() );
        Assertions.assertTrue( generator.rolesFrom( Map.of( "other", "x" ) ).isEmpty() );
    }

}
