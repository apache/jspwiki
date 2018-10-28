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
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.permissions.GroupPermission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GroupCommandTest
{
    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        new TestEngine( props );
    }

    @Test
    public void testStaticCommand()
    {
        Command a;

        a = GroupCommand.VIEW_GROUP;
        Assertions.assertEquals( "viewGroup", a.getRequestContext() );
        Assertions.assertEquals( "Group.jsp", a.getJSP() );
        Assertions.assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        Assertions.assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, GroupCommand.VIEW_GROUP );

        a = GroupCommand.EDIT_GROUP;
        Assertions.assertEquals( "editGroup", a.getRequestContext() );
        Assertions.assertEquals( "EditGroup.jsp", a.getJSP() );
        Assertions.assertEquals( "%uEditGroup.jsp?group=%n", a.getURLPattern() );
        Assertions.assertEquals( "EditGroupContent.jsp", a.getContentTemplate() );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, GroupCommand.EDIT_GROUP );

        a = GroupCommand.DELETE_GROUP;
        Assertions.assertEquals( "deleteGroup", a.getRequestContext() );
        Assertions.assertEquals( "DeleteGroup.jsp", a.getJSP() );
        Assertions.assertEquals( "%uDeleteGroup.jsp?group=%n", a.getURLPattern() );
        Assertions.assertNull( null );
        Assertions.assertNull( a.getTarget());
        Assertions.assertNull( a.requiredPermission() );
        Assertions.assertEquals( a, GroupCommand.DELETE_GROUP );
    }

    @Test
    public void testTargetedCommand()
    {
        // Get view command
        Command a = GroupCommand.VIEW_GROUP;
        GroupPrincipal group = new GroupPrincipal( "Test" );

        // Combine with wiki group; make sure it's not equal to old command
        Command b = a.targetedCommand( group );
        Assertions.assertNotSame( a, b );
        Assertions.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assertions.assertEquals( a.getJSP(), b.getJSP() );
        Assertions.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assertions.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new GroupPermission( "*:Test", "view" ), b.requiredPermission() );
        Assertions.assertEquals( group, b.getTarget() );

        // Do the same with edit command
        a = GroupCommand.EDIT_GROUP;
        b = a.targetedCommand( group );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new GroupPermission( "*:Test", "edit" ), b.requiredPermission() );
        Assertions.assertEquals( group, b.getTarget() );

        // Do the same with delete command
        a = GroupCommand.DELETE_GROUP;
        b = a.targetedCommand( group );
        Assertions.assertNotSame( a, b );
        Assertions.assertNotNull( b.getTarget() );
        Assertions.assertNotNull( b.requiredPermission() );
        Assertions.assertEquals( new GroupPermission( "*:Test", "delete" ), b.requiredPermission() );
        Assertions.assertEquals( group, b.getTarget() );
    }

}
