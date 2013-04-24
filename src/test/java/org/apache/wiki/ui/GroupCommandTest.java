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
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.permissions.GroupPermission;

public class GroupCommandTest extends TestCase
{
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        new TestEngine( props );
    }
    
    protected void tearDown() throws Exception
    {
    }

    public void testStaticCommand()
    {
        Command a;
        
        a = GroupCommand.VIEW_GROUP;
        assertEquals( "viewGroup", a.getRequestContext() );
        assertEquals( "Group.jsp", a.getJSP() );
        assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.VIEW_GROUP );
        
        a = GroupCommand.EDIT_GROUP;
        assertEquals( "editGroup", a.getRequestContext() );
        assertEquals( "EditGroup.jsp", a.getJSP() );
        assertEquals( "%uEditGroup.jsp?group=%n", a.getURLPattern() );
        assertEquals( "EditGroupContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.EDIT_GROUP );
        
        a = GroupCommand.DELETE_GROUP;
        assertEquals( "deleteGroup", a.getRequestContext() );
        assertEquals( "DeleteGroup.jsp", a.getJSP() );
        assertEquals( "%uDeleteGroup.jsp?group=%n", a.getURLPattern() );
        assertNull( null );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.DELETE_GROUP );
    }
    
    public void testTargetedCommand()
    {
        // Get view command
        Command a = GroupCommand.VIEW_GROUP;
        GroupPrincipal group = new GroupPrincipal( "Test" );
        
        // Combine with wiki group; make sure it's not equal to old command
        Command b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertEquals( a.getRequestContext(), b.getRequestContext() );
        assertEquals( a.getJSP(), b.getJSP() );
        assertEquals( a.getURLPattern(), b.getURLPattern() );
        assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "*:Test", "view" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
        
        // Do the same with edit command
        a = GroupCommand.EDIT_GROUP;
        b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "*:Test", "edit" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
        
        // Do the same with delete command
        a = GroupCommand.DELETE_GROUP;
        b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "*:Test", "delete" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
    }
    
    public static Test suite()
    {
        return new TestSuite( GroupCommandTest.class );
    }
}
