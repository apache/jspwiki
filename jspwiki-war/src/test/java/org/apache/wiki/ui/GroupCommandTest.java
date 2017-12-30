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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GroupCommandTest
{
    @Before
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
        Assert.assertEquals( "viewGroup", a.getRequestContext() );
        Assert.assertEquals( "Group.jsp", a.getJSP() );
        Assert.assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        Assert.assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, GroupCommand.VIEW_GROUP );

        a = GroupCommand.EDIT_GROUP;
        Assert.assertEquals( "editGroup", a.getRequestContext() );
        Assert.assertEquals( "EditGroup.jsp", a.getJSP() );
        Assert.assertEquals( "%uEditGroup.jsp?group=%n", a.getURLPattern() );
        Assert.assertEquals( "EditGroupContent.jsp", a.getContentTemplate() );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, GroupCommand.EDIT_GROUP );

        a = GroupCommand.DELETE_GROUP;
        Assert.assertEquals( "deleteGroup", a.getRequestContext() );
        Assert.assertEquals( "DeleteGroup.jsp", a.getJSP() );
        Assert.assertEquals( "%uDeleteGroup.jsp?group=%n", a.getURLPattern() );
        Assert.assertNull( null );
        Assert.assertNull( a.getTarget());
        Assert.assertNull( a.requiredPermission() );
        Assert.assertEquals( a, GroupCommand.DELETE_GROUP );
    }

    @Test
    public void testTargetedCommand()
    {
        // Get view command
        Command a = GroupCommand.VIEW_GROUP;
        GroupPrincipal group = new GroupPrincipal( "Test" );

        // Combine with wiki group; make sure it's not equal to old command
        Command b = a.targetedCommand( group );
        Assert.assertNotSame( a, b );
        Assert.assertEquals( a.getRequestContext(), b.getRequestContext() );
        Assert.assertEquals( a.getJSP(), b.getJSP() );
        Assert.assertEquals( a.getURLPattern(), b.getURLPattern() );
        Assert.assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new GroupPermission( "*:Test", "view" ), b.requiredPermission() );
        Assert.assertEquals( group, b.getTarget() );

        // Do the same with edit command
        a = GroupCommand.EDIT_GROUP;
        b = a.targetedCommand( group );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new GroupPermission( "*:Test", "edit" ), b.requiredPermission() );
        Assert.assertEquals( group, b.getTarget() );

        // Do the same with delete command
        a = GroupCommand.DELETE_GROUP;
        b = a.targetedCommand( group );
        Assert.assertNotSame( a, b );
        Assert.assertNotNull( b.getTarget() );
        Assert.assertNotNull( b.requiredPermission() );
        Assert.assertEquals( new GroupPermission( "*:Test", "delete" ), b.requiredPermission() );
        Assert.assertEquals( group, b.getTarget() );
    }

}
