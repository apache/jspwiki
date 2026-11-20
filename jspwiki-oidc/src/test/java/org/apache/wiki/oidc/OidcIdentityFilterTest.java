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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;

class OidcIdentityFilterTest {

    private CommonProfile profile() {
        final CommonProfile profile = new CommonProfile();
        profile.setId( "subject-1" );
        return profile;
    }

    @Test
    void usesTheConfiguredClaimAsTheName() {
        final CommonProfile profile = profile();
        profile.addAttribute( "preferred_username", "kcuser" );
        profile.addAttribute( "email", "kcuser@example.org" );
        Assertions.assertEquals( "kcuser", OidcIdentityFilter.userName( profile, "preferred_username" ) );
    }

    @Test
    void fallsBackToTheEmailAddress() {
        final CommonProfile profile = profile();
        profile.addAttribute( "email", "someone@example.org" );
        Assertions.assertEquals( "someone@example.org", OidcIdentityFilter.userName( profile, "preferred_username" ) );
    }

    @Test
    void fallsBackToTheIdentifierOfTheUser() {
        Assertions.assertEquals( "subject-1", OidcIdentityFilter.userName( profile(), "preferred_username" ) );
    }

}
