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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 */
public class PagePermissionTest
{

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    @Test
    public final void testEqualsObject()
    {
        PagePermission p1 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        PagePermission p2 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        PagePermission p3 = new PagePermission( "mywiki:Main", "delete,view,edit" );
        PagePermission p4 = new PagePermission( "mywiki:Main*", "delete,view,edit" );
        Assertions.assertEquals( p1, p2 );
        Assertions.assertEquals( p1, p3 );
        Assertions.assertFalse( p3.equals( p4 ) );
    }

    @Test
    public final void testCreateMask()
    {
        Assertions.assertEquals( 1, PagePermission.createMask( "view" ) );
        Assertions.assertEquals( 19, PagePermission.createMask( "view,edit,delete" ) );
        Assertions.assertEquals( 19, PagePermission.createMask( "edit,delete,view" ) );
        Assertions.assertEquals( 14, PagePermission.createMask( "edit,comment,upload" ) );
    }

    /*
     * Class under test for java.lang.String toString()
     */
    @Test
    public final void testToString()
    {
        PagePermission p;
        p = new PagePermission( "Main", "view,edit,delete" );
        Assertions.assertEquals( "(\"org.apache.wiki.auth.permissions.PagePermission\",\":Main\",\"delete,edit,view\")", p.toString() );
        p = new PagePermission( "mywiki:Main", "view,edit,delete" );
        Assertions.assertEquals( "(\"org.apache.wiki.auth.permissions.PagePermission\",\"mywiki:Main\",\"delete,edit,view\")", p.toString() );
    }

    /**
     * Tests wiki name support.
     */
    @Test
    public final void testWikiNames()
    {
        PagePermission p1;
        PagePermission p2;

        // Permissions without prepended wiki name should never imply themselves
        // or others
        p1 = new PagePermission( "Main", "edit" );
        p2 = new PagePermission( "Main", "edit" );
        Assertions.assertFalse( p1.implies( p1 ) );
        Assertions.assertFalse( p1.implies( p2 ) );

        // Permissions with a wildcard wiki should imply other wikis
        p1 = new PagePermission( "*:Main", "edit" );
        p2 = new PagePermission( "mywiki:Main", "edit" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        // Permissions that start with ":" are just like null
        p1 = new PagePermission( ":Main", "edit" );
        p2 = new PagePermission( "Main", "edit" );
        Assertions.assertFalse( p1.implies( p1 ) );
        Assertions.assertFalse( p1.implies( p2 ) );
    }

    @Test
    public final void testImpliesAttachments()
    {
        PagePermission p1;
        PagePermission p2;

        // A page should imply its attachment and vice-versa
        p1 = new PagePermission( "mywiki:Main", "view" );
        p2 = new PagePermission( "mywiki:Main/test.png", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p2.implies( p1 ) );
    }

    /*
     * Class under test for boolean implies(java.security.Permission)
     */
    @Test
    public final void testImpliesPermission()
    {
        PagePermission p1;
        PagePermission p2;
        PagePermission p3;

        // The same permission should imply itself
        p1 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        p2 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p2.implies( p1 ) );

        // The same permission should imply itself for wildcard wikis
        p1 = new PagePermission( "Main", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view,edit,delete" );
        p3 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        Assertions.assertFalse( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p1.implies( p3 ) );
        Assertions.assertTrue( p2.implies( p3 ) );
        Assertions.assertFalse( p3.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply permission for page with same
        // actions
        p1 = new PagePermission( "*:*", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view,edit,delete" );
        p3 = new PagePermission( "mywiki:Main", "view,edit,delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertTrue( p2.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Actions on single page should imply subset of those actions
        p1 = new PagePermission( "*:Main", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply subset of actions on single page
        p1 = new PagePermission( "*:*", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new PagePermission( "*:Mai*", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new PagePermission( "*:*in", "view,edit,delete" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Delete action on collection should imply modify/edit/upload/comment/view on
        // single page
        p1 = new PagePermission( "*:*in", "delete" );
        p2 = new PagePermission( "*:Main", "edit" );
        p3 = new PagePermission( "mywiki:Main", "edit" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "modify" );
        p3 = new PagePermission( "mywiki:Main", "modify" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "upload" );
        p3 = new PagePermission( "mywiki:Main", "upload" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "comment" );
        p3 = new PagePermission( "mywiki:Main", "comment" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Rename action on collection should imply edit on single page
        p1 = new PagePermission( "*:*in", "rename" );
        p2 = new PagePermission( "*:Main", "edit" );
        p3 = new PagePermission( "mywiki:Main", "edit" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Modify action on collection should imply upload/comment/view on single
        // page
        p1 = new PagePermission( "*:*in", "modify" );
        p2 = new PagePermission( "*:Main", "upload" );
        p3 = new PagePermission( "mywiki:Main", "upload" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "comment" );
        p3 = new PagePermission( "mywiki:Main", "comment" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Upload action on collection should imply view on single page
        p1 = new PagePermission( "*:*in", "upload" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Comment action on collection should imply view on single page
        p1 = new PagePermission( "*:*in", "comment" );
        p2 = new PagePermission( "*:Main", "view" );
        p3 = new PagePermission( "mywiki:Main", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // View action on wildcard collection SHOULD imply view on
        // GroupConfiguration page
        p1 = new PagePermission( "*:*", "view" );
        p2 = new PagePermission( "*:GroupConfiguration", "view" );
        p3 = new PagePermission( "mywiki:GroupConfiguration", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        // Pre- and post- wildcards should also be fine
        p1 = new PagePermission( "*:Group*", "view" );
        p2 = new PagePermission( "*:GroupConfiguration", "view" );
        p3 = new PagePermission( "mywiki:GroupConfiguration", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );

        p1 = new PagePermission( "*:*Configuration", "view" );
        p2 = new PagePermission( "*:GroupConfiguration", "view" );
        p3 = new PagePermission( "mywiki:GroupConfiguration", "view" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertTrue( p1.implies( p3 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
        Assertions.assertFalse( p3.implies( p1 ) );
    }

    @Test
    public final void testImplies()
    {
        Assertions.assertFalse(PagePermission.RENAME.implies( PagePermission.MODIFY ) );
        Assertions.assertTrue( PagePermission.RENAME.implies( PagePermission.EDIT ) );
        Assertions.assertFalse(PagePermission.RENAME.implies( PagePermission.UPLOAD ) );
        Assertions.assertTrue( PagePermission.RENAME.implies( PagePermission.COMMENT ) );
        Assertions.assertTrue( PagePermission.RENAME.implies( PagePermission.VIEW ) );

        Assertions.assertTrue( PagePermission.DELETE.implies( PagePermission.MODIFY ) );
        Assertions.assertTrue( PagePermission.DELETE.implies( PagePermission.EDIT ) );
        Assertions.assertTrue( PagePermission.DELETE.implies( PagePermission.UPLOAD ) );
        Assertions.assertTrue( PagePermission.DELETE.implies( PagePermission.COMMENT ) );
        Assertions.assertTrue( PagePermission.DELETE.implies( PagePermission.VIEW ) );

        Assertions.assertTrue( PagePermission.MODIFY.implies( PagePermission.EDIT ) );
        Assertions.assertTrue( PagePermission.MODIFY.implies( PagePermission.UPLOAD ) );
        Assertions.assertTrue( PagePermission.MODIFY.implies( PagePermission.COMMENT ) );
        Assertions.assertTrue( PagePermission.MODIFY.implies( PagePermission.VIEW ) );

        Assertions.assertTrue( PagePermission.EDIT.implies( PagePermission.VIEW ) );
        Assertions.assertTrue( PagePermission.EDIT.implies( PagePermission.COMMENT ) );

        Assertions.assertTrue( PagePermission.UPLOAD.implies( PagePermission.VIEW ) );

        Assertions.assertTrue( PagePermission.COMMENT.implies( PagePermission.VIEW ) );
    }

    @Test
    public final void testImpliedMask()
    {
        int result = ( PagePermission.DELETE_MASK | PagePermission.MODIFY_MASK | PagePermission.EDIT_MASK
                | PagePermission.COMMENT_MASK | PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.DELETE_MASK ) );

        result = ( PagePermission.RENAME_MASK | PagePermission.EDIT_MASK | PagePermission.COMMENT_MASK
                | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.RENAME_MASK ) );

        result = ( PagePermission.MODIFY_MASK | PagePermission.EDIT_MASK | PagePermission.COMMENT_MASK
                | PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.MODIFY_MASK ) );

        result = ( PagePermission.EDIT_MASK | PagePermission.COMMENT_MASK | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.EDIT_MASK ) );

        result = ( PagePermission.COMMENT_MASK | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.COMMENT_MASK ) );

        result = ( PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        Assertions.assertEquals( result, PagePermission.impliedMask( PagePermission.UPLOAD_MASK ) );
    }

    @Test
    public final void testGetName()
    {
        PagePermission p;
        p = new PagePermission( "Main", "view,edit,delete" );
        Assertions.assertEquals( "Main", p.getName() );
        p = new PagePermission( "mywiki:Main", "view,edit,delete" );
        Assertions.assertEquals( "mywiki:Main", p.getName() );
        Assertions.assertNotSame( "*:Main", p.getName() );
    }

    /*
     * Class under test for java.lang.String getActions()
     */
    @Test
    public final void testGetActions()
    {
        PagePermission p = new PagePermission( "Main", "VIEW,edit,delete" );
        Assertions.assertEquals( "delete,edit,view", p.getActions() );
    }

}
