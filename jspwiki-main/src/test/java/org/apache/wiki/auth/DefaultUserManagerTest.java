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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.junit.jupiter.api.Disabled;

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

    
    @Test
    public void verifyPasswordReusePolicies() throws Exception {
     
        final HttpSession httpSession = mock(HttpSession.class);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("unitTestBob");
        when(request.getParameter("password")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password2")).thenReturn("myP@5sw0rd");
        when(request.getParameter("fullname")).thenReturn("unitTestBob" + " Smith");
        when(request.getParameter("email")).thenReturn("unitTestBob" + "@apache.org");
        when(request.getSession()).thenReturn(httpSession);
        Properties props = TestEngine.getTestProperties();
        props.put("jspwiki.credentials.reuseCount", "2");
        props.put("jspwiki.userdatabase", "org.apache.wiki.auth.user.XMLUserDatabase");

        
        final TestEngine engine = TestEngine.build(props);
        final DefaultUserManager userManager = (DefaultUserManager) engine.getManager(UserManager.class);

        final Session wikiSession = WikiSessionTest.authenticatedSession( engine, "unitTestBob", "myP@5sw0rd" );
        // Mock Context
        Context context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);

        // Call parseProfile
        final UserProfile profile = userManager.parseProfile(context);

        userManager.validateProfile(context, profile);
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        
        //this should save the profile
        userManager.setUserProfile(context, profile);
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));

        //change the password
        //note that the first password is not stored as a hash because
        //it's under a unit test context
        request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("unitTestBob");
        when(request.getParameter("fullname")).thenReturn("unitTestBob" + " Smith");
        when(request.getParameter("email")).thenReturn("unitTestBob" + "@apache.org");
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password2")).thenReturn("passwordA2!");
        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        userManager.validateProfile(context, profile);
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        //this should save the profile, changing the password
        userManager.setUserProfile(context, profile);
        Assertions.assertEquals(0, wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, StringUtils.join(wikiSession.getMessages()));


        //change it again
        request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("unitTestBob");
        when(request.getParameter("fullname")).thenReturn("unitTestBob" + " Smith");
        when(request.getParameter("email")).thenReturn("unitTestBob" + "@apache.org");
        when(request.getParameter("password0")).thenReturn("passwordA2!");
        when(request.getParameter("password2")).thenReturn("passwordA3!");
        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        userManager.validateProfile(context, profile);
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        userManager.setUserProfile(context, profile);
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));

        //change it back
        request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("unitTestBob");
        when(request.getParameter("fullname")).thenReturn("unitTestBob" + " Smith");
        when(request.getParameter("email")).thenReturn("unitTestBob" + "@apache.org");
        when(request.getParameter("password0")).thenReturn("passwordA3!");
        when(request.getParameter("password2")).thenReturn("passwordA2!");
        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        userManager.validateProfile(context, profile);
        System.out.println(StringUtils.join(wikiSession.getMessages()));
        Assertions.assertEquals(0, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        userManager.setUserProfile(context, profile);
        Assertions.assertEquals(1, 
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, 
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        


    }
    @Disabled
    @Test
    public void verifyPasswordReusePoliciesWithItOff() throws Exception {
         
        Properties props = TestEngine.getTestProperties();
        props.put("jspwiki.credentials.reuseCount", "-1");
        TestEngine engine = TestEngine.build(props);
    }
}
