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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WikiCommandTest
{
    TestEngine testEngine;
    String wiki;

    @Before
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
        Assert.assertEquals( "createGroup", a.getRequestContext() );
        Assert.assertEquals( "NewGroup.jsp", a.getJSP() );
        Assert.assertEquals( "%uNewGroup.jsp", a.getURLPattern() );
        Assert.assertEquals( "NewGroupContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.CREATE_GROUP );

        a = WikiCommand.ERROR;
        Assert.assertEquals( "error", a.getRequestContext() );
        Assert.assertEquals( "Error.jsp", a.getJSP() );
        Assert.assertEquals( "%uError.jsp", a.getURLPattern() );
        Assert.assertEquals( "DisplayMessage.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.ERROR );

        a = WikiCommand.FIND;
        Assert.assertEquals( "find", a.getRequestContext() );
        Assert.assertEquals( "Search.jsp", a.getJSP() );
        Assert.assertEquals( "%uSearch.jsp", a.getURLPattern() );
        Assert.assertEquals( "FindContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.FIND );

        a = WikiCommand.INSTALL;
        Assert.assertEquals( "install", a.getRequestContext() );
        Assert.assertEquals( "Install.jsp", a.getJSP() );
        Assert.assertEquals( "%uInstall.jsp", a.getURLPattern() );
        Assert.assertNull( a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.INSTALL );

        a = WikiCommand.LOGIN;
        Assert.assertEquals( "login", a.getRequestContext() );
        Assert.assertEquals( "Login.jsp", a.getJSP() );
        Assert.assertEquals( "%uLogin.jsp?redirect=%n", a.getURLPattern() );
        Assert.assertEquals( "LoginContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.LOGIN );

        a = WikiCommand.LOGOUT;
        Assert.assertEquals( "logout", a.getRequestContext() );
        Assert.assertEquals( "Logout.jsp", a.getJSP() );
        Assert.assertEquals( "%uLogout.jsp", a.getURLPattern() );
        Assert.assertNull( a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.LOGOUT );

        a = WikiCommand.PREFS;
        Assert.assertEquals( "prefs", a.getRequestContext() );
        Assert.assertEquals( "UserPreferences.jsp", a.getJSP() );
        Assert.assertEquals( "%uUserPreferences.jsp", a.getURLPattern() );
        Assert.assertEquals( "PreferencesContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget() );
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, WikiCommand.PREFS );
    }

    @Test
    public void testTargetedCommand()
    {
        // Get view command
        Command a = WikiCommand.CREATE_GROUP;

        // Combine with wiki; make sure it's not equal to old command
        Command b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assert.assertEquals( a.getJSP(), b.getJSP() );
        Assert.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assert.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new WikiPermission( wiki, "createGroups" ), b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        // Do the same with other commands

        a = WikiCommand.ERROR;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNull( b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.FIND;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNull( b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.INSTALL;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNull( b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.LOGIN;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.LOGOUT;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );

        a = WikiCommand.PREFS;
        b = a.targetedCommand( wiki );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new WikiPermission( wiki, "editProfile" ), b.requiredPermission() );
        Assert.assertEquals( wiki, b.getTarget() );
    }

}
