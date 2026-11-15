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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Permission;

public class GroupPermissionTest
{

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    @Test
    public final void testEqualsObject()
    {
        final GroupPermission p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        final GroupPermission p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        final GroupPermission p3 = new GroupPermission( "mywiki:Test", "delete,view,edit" );
        final GroupPermission p4 = new GroupPermission( "mywiki:Test*", "delete,view,edit" );
        Assertions.assertEquals( p1, p2 );
        Assertions.assertEquals( p1, p3 );
        Assertions.assertNotEquals( p3, p4 );
    }

    @Test
    public final void testCreateMask()
    {
        Assertions.assertEquals( 1, GroupPermission.createMask( "view" ) );
        Assertions.assertEquals( 7, GroupPermission.createMask( "view,edit,delete" ) );
        Assertions.assertEquals( 7, GroupPermission.createMask( "edit,delete,view" ) );
        Assertions.assertEquals( 2, GroupPermission.createMask( "edit" ) );
        Assertions.assertEquals( 6, GroupPermission.createMask( "edit,delete" ) );
    }

    /*
     * Class under test for java.lang.String toString()
     */
    @Test
    public final void testToString()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        Assertions.assertEquals( "(\"org.apache.wiki.auth.permissions.GroupPermission\",\"*:Test\",\"delete,edit,view\")", p
                .toString() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assertions.assertEquals( "(\"org.apache.wiki.auth.permissions.GroupPermission\",\"mywiki:Test\",\"delete,edit,view\")", p
                .toString() );
    }

    /**
     * Tests wiki name support.
     */
    @Test
    public final void testWikiNames()
    {
        GroupPermission p1;
        GroupPermission p2;

        // Permissions without prepended wiki name should imply themselves
        p1 = new GroupPermission( "Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        Assertions.assertTrue( p1.implies( p1 ) );
        Assertions.assertTrue( p1.implies( p2 ) );

        // Permissions with a wildcard wiki should imply other wikis
        p1 = new GroupPermission( "*:Test", "edit" );
        p2 = new GroupPermission( "mywiki:Test", "edit" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        // Permissions that start with ":" are just like "*:"
        p1 = new GroupPermission( "*:Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        Assertions.assertTrue( p1.implies( p1 ) );
        Assertions.assertTrue( p1.implies( p2 ) );
    }

    @Test
    public final void testToMemberPermission() {
        final GroupPermission p = new GroupPermission( "mywiki:TestGroup", "edit" ).toMemberPermission();

        Assertions.assertEquals( "mywiki", p.getWiki() );
        Assertions.assertEquals( GroupPermission.MEMBER_TOKEN, p.getGroup() );
        Assertions.assertEquals( "edit", p.getActions() );
    }

    @Test
    public final void testToMemberPermissionKeepsTheActions() {
        Assertions.assertEquals( "delete,edit,view", new GroupPermission( "*:TestGroup", "view,edit,delete" ).toMemberPermission().getActions() );
    }

    @Test
    public final void testToMemberPermissionOfAGroupOfAnyWiki() {
        final GroupPermission p = new GroupPermission( "*:TestGroup", "view" ).toMemberPermission();

        Assertions.assertEquals( "*", p.getWiki() );
        Assertions.assertEquals( GroupPermission.MEMBER_TOKEN, p.getGroup() );
    }

    @Test
    public final void testToMemberPermissionOfAGroupWithoutWiki() {
        final GroupPermission p = new GroupPermission( ":TestGroup", "view" ).toMemberPermission();

        Assertions.assertNull( p.getWiki() );
        Assertions.assertEquals( GroupPermission.MEMBER_TOKEN, p.getGroup() );
    }

    @Test
    public final void testGroupMemberGrantImpliesTheMemberPermission() {
        final GroupPermission grant = new GroupPermission( "*:<groupmember>", "edit" );

        Assertions.assertTrue( grant.implies( new GroupPermission( "mywiki:TestGroup", "edit" ).toMemberPermission() ) );
        Assertions.assertTrue( grant.implies( new GroupPermission( "mywiki:TestGroup", "view" ).toMemberPermission() ) );
        Assertions.assertFalse( grant.implies( new GroupPermission( "mywiki:TestGroup", "delete" ).toMemberPermission() ) );
    }

    @Test
    public final void testGroupMemberGrantDoesNotImplyAGroupByItself() {
        // Who is a member is for the one who checks the permission to find out, see AuthorizationManager.checkStaticPermission
        final GroupPermission grant = new GroupPermission( "*:<groupmember>", "edit" );

        Assertions.assertFalse( grant.implies( new GroupPermission( "*:TestGroup", "view" ) ) );
        Assertions.assertFalse( grant.implies( new GroupPermission( "*:TestGroup", "edit" ) ) );
    }

    @Test
    public final void testWildcardGrantDoesNotImplyTheMemberPermission() {
        Assertions.assertFalse( new GroupPermission( "*:*", "edit" ).implies( new GroupPermission( "*:TestGroup", "edit" ).toMemberPermission() ) );
    }

    @Test
    public final void testGroupMemberGrantDoesNotImplyOtherTypesOfPermission() {
        Assertions.assertFalse( new GroupPermission( "*:<groupmember>", "view" ).implies( new PagePermission( "*:TestGroup", "view" ) ) );
    }

    /*
     * Class under test for boolean implies(java.security.Permission)
     */
    @Test
    public final void testImpliesPermission()
    {
        GroupPermission p1;
        GroupPermission p2;
        GroupPermission p3;

        // The same permission should imply itself
        p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p2.implies( p1 ) );

        // The same permission should imply itself for wildcard wikis
        p1 = new GroupPermission( "Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p2.implies( p1 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertTrue( p2.implies( p3 ) );
        Assertions.assertFalse( p3.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply permission for group with same
        // actions
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertTrue( p2.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Actions on single group should imply subset of those actions
        p1 = new GroupPermission( "*:Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply subset of actions on single group
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:Tes*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*st", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Delete action on collection should imply edit/view on
        // single group
        p1 = new GroupPermission( "*:*st", "delete" );
        p2 = new GroupPermission( "*:Test", "edit" );
        p3 = new GroupPermission( "mywiki:Test", "edit" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Edit action on collection should imply view on single group
        p1 = new GroupPermission( "*:*st", "edit" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );


        // Pre- and post- wildcards should also be fine
        p1 = new GroupPermission( "*:Test*", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*Group", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Wildcards don't imply the <groupmember> target
        p1 = new GroupPermission( "*:*", "view" );
        p2 = new GroupPermission( "*:<groupmember>", "view" );
        Assertions.assertFalse( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        p1 = new GroupPermission( "*:*ber>", "view" );
        Assertions.assertFalse( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
    }

    @Test
    public final void testImplies()
    {
        Assertions.assertTrue( GroupPermission.DELETE.implies( GroupPermission.EDIT ) );
        Assertions.assertTrue( GroupPermission.DELETE.implies( GroupPermission.VIEW ) );
        Assertions.assertTrue( GroupPermission.EDIT.implies( GroupPermission.VIEW ) );
    }

    @Test
    public final void testImpliedMask()
    {
        int result = ( GroupPermission.DELETE_MASK | GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        Assertions.assertEquals( result, GroupPermission.impliedMask( GroupPermission.DELETE_MASK ) );

        result = ( GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        Assertions.assertEquals( result, GroupPermission.impliedMask( GroupPermission.EDIT_MASK ) );
    }

    @Test
    public final void testGetName()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        Assertions.assertEquals( "Test", p.getName() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        Assertions.assertEquals( "mywiki:Test", p.getName() );
        Assertions.assertNotSame( "*:Test", p.getName() );
    }

    /*
     * Class under test for java.lang.String getActions()
     */
    @Test
    public final void testGetActions()
    {
        final GroupPermission p = new GroupPermission( "Test", "VIEW,edit,delete" );
        Assertions.assertEquals( "delete,edit,view", p.getActions() );
    }

}
