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
/*
 * (C) Janne Jalkanen 2005
 *
 */
package org.apache.wiki.ui;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WikiCommandTest
{
    TestEngine testEngine;
    String wiki;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        testEngine = new TestEngine( props );
        wiki = testEngine.getApplicationName();
    }

    @Test
    public void testStaticCommand()
    {
        Command a = WikiCommand.CREATE_GROUP;
        Assertions.assertEquals( "createGroup", a.getRequestContext() );
        Assertions.assertEquals( "NewGroup.jsp", a.getJSP() );
        Assertions.assertEquals( "%uNewGroup.jsp", a.getURLPattern() );
        Assertions.assertEquals( "NewGroupContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.CREATE_GROUP );

        a = WikiCommand.ERROR;
        Assertions.assertEquals( "error", a.getRequestContext() );
        Assertions.assertEquals( "Error.jsp", a.getJSP() );
        Assertions.assertEquals( "%uError.jsp", a.getURLPattern() );
        Assertions.assertEquals( "DisplayMessage.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.ERROR );

        a = WikiCommand.FIND;
        Assertions.assertEquals( "find", a.getRequestContext() );
        Assertions.assertEquals( "Search.jsp", a.getJSP() );
        Assertions.assertEquals( "%uSearch.jsp", a.getURLPattern() );
        Assertions.assertEquals( "FindContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.FIND );

        a = WikiCommand.INSTALL;
        Assertions.assertEquals( "install", a.getRequestContext() );
        Assertions.assertEquals( "Install.jsp", a.getJSP() );
        Assertions.assertEquals( "%uInstall.jsp", a.getURLPattern() );
        Assertions.assertNull( a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.INSTALL );

        a = WikiCommand.LOGIN;
        Assertions.assertEquals( "login", a.getRequestContext() );
        Assertions.assertEquals( "Login.jsp", a.getJSP() );
        Assertions.assertEquals( "%uLogin.jsp?redirect=%n", a.getURLPattern() );
        Assertions.assertEquals( "LoginContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.LOGIN );

        a = WikiCommand.LOGOUT;
        Assertions.assertEquals( "logout", a.getRequestContext() );
        Assertions.assertEquals( "Logout.jsp", a.getJSP() );
        Assertions.assertEquals( "%uLogout.jsp", a.getURLPattern() );
        Assertions.assertNull( a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.LOGOUT );

        a = WikiCommand.PREFS;
        Assertions.assertEquals( "prefs", a.getRequestContext() );
        Assertions.assertEquals( "UserPreferences.jsp", a.getJSP() );
        Assertions.assertEquals( "%uUserPreferences.jsp", a.getURLPattern() );
        Assertions.assertEquals( "PreferencesContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget() );
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, WikiCommand.PREFS );
    }

    @Test
    public void testTargetedCommand()
    {
        // Get view command
        Command a = WikiCommand.CREATE_GROUP;

        // Combine with wiki; make sure it's not equal to old command
        Command b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assertions.assertEquals( a.getJSP(), b.getJSP() );
        Assertions.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assertions.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new WikiPermission( wiki, "createGroups" ), b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        // Do the same with other commands

        a = WikiCommand.ERROR;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNull( b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.FIND;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNull( b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.INSTALL;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNull( b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.LOGIN;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.LOGOUT;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.PREFS;
        b = a.targetedCommand( wiki );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new WikiPermission( wiki, "editProfile" ), b.requiredPermission() );
        Assertions.assertEquals( wiki, b.getTarget() );
    }

}
