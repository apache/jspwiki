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

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PageCommandTest {

    @Test
    public void testStaticCommand() {
        Command a = PageCommand.VIEW;
        Assertions.assertEquals( "view", a.getRequestContext() );
        Assertions.assertEquals( "Wiki.jsp", a.getJSP() );
        Assertions.assertEquals( "%uWiki.jsp?page=%n", a.getURLPattern() );
        Assertions.assertEquals( "PageContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, PageCommand.VIEW );

        a = PageCommand.EDIT;
        Assertions.assertEquals( "edit", a.getRequestContext() );
        Assertions.assertEquals( "Edit.jsp", a.getJSP() );
        Assertions.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assertions.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, PageCommand.EDIT );

        a = PageCommand.PREVIEW;
        Assertions.assertEquals( "preview", a.getRequestContext() );
        Assertions.assertEquals( "Preview.jsp", a.getJSP() );
        Assertions.assertEquals( "%uPreview.jsp?page=%n", a.getURLPattern() );
        Assertions.assertEquals( "PreviewContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, PageCommand.PREVIEW );
    }

    @Test
    public void testTargetedCommand() throws Exception {
        final TestEngine testEngine = TestEngine.build();
        testEngine.saveText( "TestPage", "This is a test." );
        final WikiPage testPage = testEngine.getPageManager().getPage( "TestPage" );

        // Get view command
        Command a = PageCommand.VIEW;

        // Combine with wiki page; make sure it's not equal to old command
        Command b = a.targetedCommand( testPage );
        Assertions.assertNotSame( a, b );
        Assertions.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assertions.assertEquals( a.getJSP(), b.getJSP() );
        Assertions.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assertions.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        Assertions.assertEquals( testPage, b.getTarget() );

        // Do the same with edit command
        a = PageCommand.EDIT;
        b = a.targetedCommand( testPage );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( PermissionFactory.getPagePermission( testPage, "edit" ), b.requiredPermission() );
        Assertions.assertEquals( testPage, b.getTarget() );

        // Do the same with delete command
        a = PageCommand.DELETE;
        b = a.targetedCommand( testPage );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( PermissionFactory.getPagePermission( testPage, "delete" ), b.requiredPermission() );
        Assertions.assertEquals( testPage, b.getTarget() );

        // Do the same with info command
        a = PageCommand.INFO;
        b = a.targetedCommand( testPage );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        Assertions.assertEquals( testPage, b.getTarget() );
    }

}
