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

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.wiki.api.core.Engine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OidcAuthorizerTest {

    private OidcAuthorizer initialized( final Properties props ) {
        final OidcAuthorizer authorizer = new OidcAuthorizer();
        authorizer.initialize( Mockito.mock( Engine.class ), props );
        return authorizer;
    }

    @Test
    void declaresThatTheContainerHandlesTheLogin() {
        Assertions.assertTrue( initialized( new Properties() ).isContainerAuthorized() );
    }

    @Test
    void knowsAdminAndAuthenticatedByDefault() {
        final String names = Arrays.stream( initialized( new Properties() ).getRoles() ).map( r -> r.getName() ).sorted().collect( Collectors.joining( "," ) );
        Assertions.assertEquals( "Admin,Authenticated", names );
    }

    @Test
    void takesTheRolesFromTheProperty() {
        final Properties props = new Properties();
        props.setProperty( OidcAuthorizer.PROP_ROLES, " Editors , Admin ,," );
        final String names = Arrays.stream( initialized( props ).getRoles() ).map( r -> r.getName() ).sorted().collect( Collectors.joining( "," ) );
        Assertions.assertEquals( "Admin,Editors", names );
    }

}
