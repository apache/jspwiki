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
package org.apache.wiki.auth.permissions;

import java.security.AccessControlException;
import java.security.Permission;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.Assert;

public class GroupPermissionTest
{

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    public final void testEqualsObject()
    {
        GroupPermission p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        GroupPermission p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        GroupPermission p3 = new GroupPermission( "mywiki:Test", "delete,view,edit" );
        GroupPermission p4 = new GroupPermission( "mywiki:Test*", "delete,view,edit" );
        Assert.assertEquals( p1, p2 );
        Assert.assertEquals( p1, p3 );
        Assert.assertFalse( p3.equals( p4 ) );
    }

    public final void testCreateMask()
    {
        Assert.assertEquals( 1, GroupPermission.createMask( "view" ) );
        Assert.assertEquals( 7, GroupPermission.createMask( "view,edit,delete" ) );
        Assert.assertEquals( 7, GroupPermission.createMask( "edit,delete,view" ) );
        Assert.assertEquals( 2, GroupPermission.createMask( "edit" ) );
        Assert.assertEquals( 6, GroupPermission.createMask( "edit,delete" ) );
    }

    /*
     * Class under test for java.lang.String toString()
     */
    public final void testToString()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        Assert.assertEquals( "(\"org.apache.wiki.auth.permissions.GroupPermission\",\"*:Test\",\"delete,edit,view\")", p
                .toString() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assert.assertEquals( "(\"org.apache.wiki.auth.permissions.GroupPermission\",\"mywiki:Test\",\"delete,edit,view\")", p
                .toString() );
    }

    /**
     * Tests wiki name support.
     */
    public final void testWikiNames()
    {
        GroupPermission p1;
        GroupPermission p2;

        // Permissions without prepended wiki name should imply themselves
        p1 = new GroupPermission( "Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        Assert.assertTrue( p1.implies( p1 ) );
        Assert.assertTrue( p1.implies( p2 ) );

        // Permissions with a wildcard wiki should imply other wikis
        p1 = new GroupPermission( "*:Test", "edit" );
        p2 = new GroupPermission( "mywiki:Test", "edit" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertFalse( p2.implies( p1 ) );

        // Permissions that start with ":" are just like "*:"
        p1 = new GroupPermission( "*:Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        Assert.assertTrue( p1.implies( p1 ) );
        Assert.assertTrue( p1.implies( p2 ) );
    }

    public final void testImpliesMember()
    {
        GroupPermission p1;
        Permission p2;
        Subject s;

        // <groupmember> implies TestGroup if Subject has GroupPermission("TestGroup")
        p1 = new GroupPermission( "*:<groupmember>", "view" );
        p2 = new GroupPermission ("*:TestGroup", "view" );
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "TestGroup" ) );
        Assert.assertTrue( subjectImplies( s, p1, p2 ) );

        // <groupmember> doesn't imply it if Subject has no GroupPermission("TestGroup")
        s = new Subject();
        s.getPrincipals().add( new WikiPrincipal( "TestGroup" ) );
        Assert.assertFalse( subjectImplies( s, p1, p2 ) );

        // <groupmember> doesn't imply it if Subject's GP doesn't match
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "FooGroup" ) );
        Assert.assertFalse( subjectImplies( s, p1, p2 ) );

        // <groupmember> doesn't imply it if p2 isn't GroupPermission type
        p2 = new PagePermission ("*:TestGroup", "view" );
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "TestGroup" ) );
        Assert.assertFalse( subjectImplies( s, p1, p2 ) );

        // <groupmember> implies TestGroup if not called with Subject combiner
        p1 = new GroupPermission( "*:<groupmember>", "view" );
        p2 = new GroupPermission ("*:TestGroup", "view" );
        Assert.assertFalse( p1.impliesMember( p2 ) );
    }


    /*
     * Class under test for boolean implies(java.security.Permission)
     */
    public final void testImpliesPermission()
    {
        GroupPermission p1;
        GroupPermission p2;
        GroupPermission p3;

        // The same permission should imply itself
        p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p2.implies( p1 ) );

        // The same permission should imply itself for wildcard wikis
        p1 = new GroupPermission( "Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p2.implies( p1 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertTrue( p2.implies( p3 ) );
        Assert.assertFalse( p3.implies( p1 ) );
        Assert.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply permission for group with same
        // actions
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertTrue( p2.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        // Actions on single group should imply subset of those actions
        p1 = new GroupPermission( "*:Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );
        Assert.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply subset of actions on single group
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:Tes*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*st", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        // Delete action on collection should imply edit/view on
        // single group
        p1 = new GroupPermission( "*:*st", "delete" );
        p2 = new GroupPermission( "*:Test", "edit" );
        p3 = new GroupPermission( "mywiki:Test", "edit" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        // Edit action on collection should imply view on single group
        p1 = new GroupPermission( "*:*st", "edit" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );


        // Pre- and post- wildcards should also be fine
        p1 = new GroupPermission( "*:Test*", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*Group", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        Assert.assertTrue( p1.implies( p2 ) );
        Assert.assertTrue( p1.implies( p3 ) );
        Assert.assertFalse( p2.implies( p1 ) );
        Assert.assertFalse( p3.implies( p1 ) );

        // Wildcards don't imply the <groupmember> target
        p1 = new GroupPermission( "*:*", "view" );
        p2 = new GroupPermission( "*:<groupmember>", "view" );
        Assert.assertFalse( p1.implies( p2 ) );
        Assert.assertFalse( p2.implies( p1 ) );

        p1 = new GroupPermission( "*:*ber>", "view" );
        Assert.assertFalse( p1.implies( p2 ) );
        Assert.assertFalse( p2.implies( p1 ) );
    }

    public final void testImplies()
    {
        Assert.assertTrue( GroupPermission.DELETE.implies( GroupPermission.EDIT ) );
        Assert.assertTrue( GroupPermission.DELETE.implies( GroupPermission.VIEW ) );
        Assert.assertTrue( GroupPermission.EDIT.implies( GroupPermission.VIEW ) );
    }

    public final void testImpliedMask()
    {
        int result = ( GroupPermission.DELETE_MASK | GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        Assert.assertEquals( result, GroupPermission.impliedMask( GroupPermission.DELETE_MASK ) );

        result = ( GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        Assert.assertEquals( result, GroupPermission.impliedMask( GroupPermission.EDIT_MASK ) );
    }

    public final void testGetName()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        Assert.assertEquals( "Test", p.getName() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assert.assertEquals( "mywiki:Test", p.getName() );
        Assert.assertNotSame( "*:Test", p.getName() );
    }

    /*
     * Class under test for java.lang.String getActions()
     */
    public final void testGetActions()
    {
        GroupPermission p = new GroupPermission( "Test", "VIEW,edit,delete" );
        Assert.assertEquals( "delete,edit,view", p.getActions() );
    }

    /**
     * Binds a Subject to the current AccessControlContext and calls
     * p1.implies(p2).
     * @param subject
     * @param p1
     * @param p2
     * @return
     */
    protected final boolean subjectImplies( final Subject subject, final GroupPermission p1, final Permission p2 )
    {
        try
        {
            Boolean result = (Boolean)Subject.doAsPrivileged( subject, new PrivilegedAction()
            {
                public Object run()
                {
                    return Boolean.valueOf( p1.impliesMember( p2 ) );
                }
            }, null );
            return result.booleanValue();
        }
        catch( AccessControlException e )
        {
            return false;
        }
    }

}
