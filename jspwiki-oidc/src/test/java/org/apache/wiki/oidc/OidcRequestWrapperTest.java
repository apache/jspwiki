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

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

class OidcRequestWrapperTest {

    private final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
    private final OidcRequestWrapper wrapper = new OidcRequestWrapper( request, "kcuser", Set.of( "Authenticated" ) );

    @Test
    void presentsTheLoggedInUserAsContainerUser() {
        Assertions.assertEquals( "kcuser", wrapper.getRemoteUser() );
        Assertions.assertEquals( "kcuser", wrapper.getUserPrincipal().getName() );
    }

    @Test
    void grantsTheRolesOfTheIdentityProvider() {
        Assertions.assertTrue( wrapper.isUserInRole( "Authenticated" ) );
        Assertions.assertFalse( wrapper.isUserInRole( "Admin" ) );
    }

    @Test
    void keepsTheRolesOfTheWrappedRequest() {
        Mockito.when( request.isUserInRole( "Admin" ) ).thenReturn( true );
        Assertions.assertTrue( wrapper.isUserInRole( "Admin" ) );
    }

}
