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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.user.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultUserManagerTest {

    @Test
    void testParseProfileTrimsFields() {
        // Mock HttpServletRequest
        final HttpServletRequest request = mock( HttpServletRequest.class );
        when( request.getParameter( "loginname" ) ).thenReturn( "  admin  " );
        when( request.getParameter( "password" ) ).thenReturn( "password" );
        when( request.getParameter( "fullname" ) ).thenReturn( "  Administrator  " );
        when( request.getParameter( "email" ) ).thenReturn( "  admin@example.com  " );

        // Mock Engine and its dependencies
        final AuthenticationManager aMgr = mock( AuthenticationManager.class );
        when( aMgr.isContainerAuthenticated() ).thenReturn( false );
        final Properties props = new Properties();
        props.put( "jspwiki.userdatabase", "org.apache.wiki.auth.user.XMLUserDatabase" );
        final Engine engine = mock( Engine.class );
        when( engine.getManager( AuthenticationManager.class ) ).thenReturn( aMgr );
        when( engine.getWikiProperties() ).thenReturn( props );

        // Mock Context
        final Context context = mock( Context.class );
        when( context.getHttpRequest() ).thenReturn( request );

        // Mock Session and ensure it's authenticated
        final Session session = mock( Session.class );
        when( session.isAuthenticated() ).thenReturn( true );
        when( session.getUserPrincipal() ).thenReturn( () -> "admin" );
        when( context.getWikiSession() ).thenReturn( session );

        // Call parseProfile
        final DefaultUserManager userManager = new DefaultUserManager();
        userManager.initialize( engine, engine.getWikiProperties() );
        final UserProfile profile = userManager.parseProfile( context );

        // Verify fields are trimmed
        Assertions.assertEquals( "admin", profile.getLoginName(), "Login name should be trimmed" );
        Assertions.assertEquals( "Administrator", profile.getFullname(), "Full name should be trimmed" );
        Assertions.assertEquals( "admin@example.com", profile.getEmail(), "Email should be trimmed" );
    }

}
