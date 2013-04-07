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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.permissions.PermissionFactory;

public class PageCommandTest extends TestCase
{
    TestEngine     testEngine;
    
    WikiPage       testPage;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
        testEngine.saveText( "TestPage", "This is a test." );
        testPage = testEngine.getPage( "TestPage" );
    }
    
    protected void tearDown() throws Exception
    {
        testEngine.deletePage( "TestPage" );
    }

    public void testStaticCommand()
    {
        Command a = PageCommand.VIEW;
        assertEquals( "view", a.getRequestContext() );
        assertEquals( "Wiki.jsp", a.getJSP() );
        assertEquals( "%uWiki.jsp?page=%n", a.getURLPattern() );
        assertEquals( "PageContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, PageCommand.VIEW );
        
        a = PageCommand.EDIT;
        assertEquals( "edit", a.getRequestContext() );
        assertEquals( "Edit.jsp", a.getJSP() );
        assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        assertEquals( "EditContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, PageCommand.EDIT );
        
        a = PageCommand.PREVIEW;
        assertEquals( "preview", a.getRequestContext() );
        assertEquals( "Preview.jsp", a.getJSP() );
        assertEquals( "%uPreview.jsp?page=%n", a.getURLPattern() );
        assertEquals( "PreviewContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, PageCommand.PREVIEW );
    }
    
    public void testTargetedCommand()
    {
        // Get view command
        Command a = PageCommand.VIEW;
        
        // Combine with wiki page; make sure it's not equal to old command
        Command b = a.targetedCommand( testPage );
        assertNotSame( a, b );
        assertEquals( a.getRequestContext(), b.getRequestContext() );
        assertEquals( a.getJSP(), b.getJSP() );
        assertEquals( a.getURLPattern(), b.getURLPattern() );
        assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        assertEquals( testPage, b.getTarget() );
        
        // Do the same with edit command
        a = PageCommand.EDIT;
        b = a.targetedCommand( testPage );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( PermissionFactory.getPagePermission( testPage, "edit" ), b.requiredPermission() );
        assertEquals( testPage, b.getTarget() );
        
        // Do the same with delete command
        a = PageCommand.DELETE;
        b = a.targetedCommand( testPage );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( PermissionFactory.getPagePermission( testPage, "delete" ), b.requiredPermission() );
        assertEquals( testPage, b.getTarget() );
        
        // Do the same with info command
        a = PageCommand.INFO;
        b = a.targetedCommand( testPage );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( PermissionFactory.getPagePermission( testPage, "view" ), b.requiredPermission() );
        assertEquals( testPage, b.getTarget() );
    }
    
    public static Test suite()
    {
        return new TestSuite( PageCommandTest.class );
    }
}
