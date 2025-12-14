/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.user.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public abstract class AbstractPasswordReuseTest {

    public abstract Properties getTestProps() throws Exception;

    @Test
    public void verifyPasswordReusePolicies() throws Exception {
        Properties props = getTestProps();

        final HttpSession httpSession = mock(HttpSession.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("unitTestBob");
        when(request.getParameter("password")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password2")).thenReturn("myP@5sw0rd");
        when(request.getParameter("fullname")).thenReturn("unitTestBob" + " Smith");
        when(request.getParameter("email")).thenReturn("unitTestBob" + "@apache.org");
        when(request.getSession()).thenReturn(httpSession);
        props.put("jspwiki.credentials.reuseCount", "2");
       // props.put("jspwiki.userdatabase", "org.apache.wiki.auth.user.XMLUserDatabase");

        final TestEngine engine = TestEngine.build(props);
        final DefaultUserManager userManager = (DefaultUserManager) engine.getManager(UserManager.class);

        final Session wikiSession = WikiSessionTest.anonymousSession(engine);
        // Mock Context
        Context context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);

        // Call parseProfile
        UserProfile profile = userManager.parseProfile(context);
        profile.setCreated(null);
        profile.setLastModified(null);
        userManager.validateProfile(context, profile);
        Assertions.assertEquals(0,
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length,
                userManager.getUserDatabase().getClass().getName() + " " +
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
        when(request.getParameter("password")).thenReturn("myP@5sw0rd");

        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        when(request.getParameter("password")).thenReturn("passwordA2!");

        //the proposed password
        profile.setPassword("passwordA2!");
        //old one
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        //new confirm
        when(request.getParameter("password2")).thenReturn("passwordA2!");

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
        when(request.getParameter("password")).thenReturn("passwordA2!");

        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        profile = userManager.parseProfile(context);
        //proposed
        profile.setPassword("passwordA3!");
        //existing
        when(request.getParameter("password0")).thenReturn("passwordA2!");
        //new pass
        when(request.getParameter("password2")).thenReturn("passwordA3!");
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
        when(request.getParameter("password")).thenReturn("passwordA3!");
        when(request.getParameter("password2")).thenReturn("passwordA2!");
        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        profile = userManager.parseProfile(context);
        profile.setPassword("passwordA2!");

        userManager.validateProfile(context, profile);
        System.out.println(StringUtils.join(wikiSession.getMessages()));
        Assertions.assertEquals(1,
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length,
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));

    }

    @Test
    public void verifyPasswordReusePoliciesWithItOff() throws Exception {

        Properties props = getTestProps();

        final HttpSession httpSession = mock(HttpSession.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("loginname")).thenReturn("verifyPasswordReusePoliciesWithItOff");
        when(request.getParameter("password")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        when(request.getParameter("password2")).thenReturn("myP@5sw0rd");
        when(request.getParameter("fullname")).thenReturn("verifyPasswordReusePoliciesWithItOff" + " Smith");
        when(request.getParameter("email")).thenReturn("verifyPasswordReusePoliciesWithItOff" + "@apache.org");
        when(request.getSession()).thenReturn(httpSession);
        props.put("jspwiki.credentials.reuseCount", "-1");
        //props.put("jspwiki.userdatabase", "org.apache.wiki.auth.user.XMLUserDatabase");

        final TestEngine engine = TestEngine.build(props);
        final DefaultUserManager userManager = (DefaultUserManager) engine.getManager(UserManager.class);

        final Session wikiSession = WikiSessionTest.anonymousSession(engine);
        //engine, "verifyPasswordReusePoliciesWithItOff", "myP@5sw0rd");
        // Mock Context
        Context context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);

        // Call parseProfile
        UserProfile profile = userManager.parseProfile(context);
        profile.setCreated(null);
        profile.setLastModified(null);
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
        when(request.getParameter("loginname")).thenReturn("verifyPasswordReusePoliciesWithItOff");
        when(request.getParameter("fullname")).thenReturn("verifyPasswordReusePoliciesWithItOff" + " Smith");
        when(request.getParameter("email")).thenReturn("verifyPasswordReusePoliciesWithItOff" + "@apache.org");
        when(request.getParameter("password")).thenReturn("myP@5sw0rd");

        when(request.getSession()).thenReturn(httpSession);
        context = mock(Context.class);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getEngine()).thenReturn(engine);
        when(context.getWikiSession()).thenReturn(wikiSession);
        when(request.getParameter("password")).thenReturn("passwordA2!");

        //the proposed password
        profile.setPassword("passwordA2!");
        //old one
        when(request.getParameter("password0")).thenReturn("myP@5sw0rd");
        //new confirm
        when(request.getParameter("password2")).thenReturn("passwordA2!");

        userManager.validateProfile(context, profile);
        Assertions.assertEquals(0,
                wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length,
                StringUtils.join(wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES)));
        //this should save the profile, changing the password
        userManager.setUserProfile(context, profile);
        Assertions.assertEquals(0, wikiSession.getMessages(DefaultUserManager.SESSION_MESSAGES).length, StringUtils.join(wikiSession.getMessages()));

    }
}
