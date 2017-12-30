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
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PageCommandTest
{
    TestEngine     testEngine;

    WikiPage       testPage;

    @Before
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        testEngine = new TestEngine( props );
        testEngine.saveText( "TestPage", "This is a test." );
        testPage = testEngine.getPage( "TestPage" );
    }

    @After
    public void tearDown() throws Exception
    {
        testEngine.deletePage( "TestPage" );
    }

    @Test
    public void testStaticCommand()
    {
        Command a = PageCommand.VIEW;
        Assert.assertEquals( "view", a.getRequestContext() );
        Assert.assertEquals( "Wiki.jsp", a.getJSP() );
        Assert.assertEquals( "%uWiki.jsp?page=%n", a.getURLPattern() );
        Assert.assertEquals( "PageContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, PageCommand.VIEW );

        a = PageCommand.EDIT;
        Assert.assertEquals( "edit", a.getRequestContext() );
        Assert.assertEquals( "Edit.jsp", a.getJSP() );
        Assert.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assert.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, PageCommand.EDIT );

        a = PageCommand.PREVIEW;
        Assert.assertEquals( "preview", a.getRequestContext() );
        Assert.assertEquals( "Preview.jsp", a.getJSP() );
        Assert.assertEquals( "%uPreview.jsp?page=%n", a.getURLPattern() );
        Assert.assertEquals( "PreviewContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, PageCommand.PREVIEW );
    }

    @Test
    public void testTargetedCommand()
    {
        // Get view command
        Command a = PageCommand.VIEW;

        // Combine with wiki page; make sure it's not equal to old command
        Command b = a.targetedCommand( testPage );
        Assert.assertNotSame( a, b );
        Assert.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assert.assertEquals( a.getJSP(), b.getJSP() );
        Assert.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assert.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        Assert.assertEquals( testPage, b.getTarget() );

        // Do the same with edit command
        a = PageCommand.EDIT;
        b = a.targetedCommand( testPage );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( PermissionFactory.getPagePermission( testPage, "edit" ), b.requiredPermission() );
        Assert.assertEquals( testPage, b.getTarget() );

        // Do the same with delete command
        a = PageCommand.DELETE;
        b = a.targetedCommand( testPage );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( PermissionFactory.getPagePermission( testPage, "delete" ), b.requiredPermission() );
        Assert.assertEquals( testPage, b.getTarget() );

        // Do the same with info command
        a = PageCommand.INFO;
        b = a.targetedCommand( testPage );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        Assert.assertEquals( testPage, b.getTarget() );
    }

}
