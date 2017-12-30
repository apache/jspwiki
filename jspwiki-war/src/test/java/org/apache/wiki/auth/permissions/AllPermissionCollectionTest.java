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
import java.security.Permission;
import java.util.Enumeration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class AllPermissionCollectionTest
{

    AllPermissionCollection c_all;

    /**
     *
     */
    @Before
    public void setUp() throws Exception
    {
        c_all = new AllPermissionCollection();
    }

    @Test
    public void testAddAllPermission()
    {
        AllPermission all1 = new AllPermission( "*" );
        AllPermission all2 = new AllPermission( "JSPWiki" );
        AllPermission all3 = new AllPermission( "myWiki" );
        AllPermission all4 = new AllPermission( "*" );

        c_all.add( all1 );
        Assert.assertEquals( 1, count( c_all ) );

        c_all.add( all2 );
        Assert.assertEquals( 2, count( c_all ) );

        c_all.add( all3 );
        Assert.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( all4 );
        Assert.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testAddPagePermission()
    {
        PagePermission p1 = new PagePermission( "JSPWiki:Main", "edit" );
        PagePermission p2 = new PagePermission( "JSPWiki:GroupAdmin", "edit" );
        PagePermission p3 = new PagePermission( "JSPWiki:Foobar", "delete" );
        PagePermission p4 = new PagePermission( "JSPWiki:Main", "edit" );

        c_all.add( p1 );
        Assert.assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        Assert.assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        Assert.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
        Assert.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testAddWikiPermission()
    {
        WikiPermission p1 = new WikiPermission( "JSPWiki", "login" );
        WikiPermission p2 = new WikiPermission( "JSPWiki", "createGroups" );
        WikiPermission p3 = new WikiPermission( "JSPWiki", "editPreferences" );
        WikiPermission p4 = new WikiPermission( "JSPWiki", "login" );

        c_all.add( p1 );
        Assert.assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        Assert.assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        Assert.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
        Assert.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testReadOnly()
    {
        AllPermission all1 = new AllPermission( "*" );
        AllPermission all2 = new AllPermission( "JSPWiki" );

        Assert.assertFalse( c_all.isReadOnly() );
        c_all.add( all1 );
        Assert.assertFalse( c_all.isReadOnly() );

        // Mark as read-only; isReadOnly should return "true", as we'll get an
        // error
        c_all.setReadOnly();
        Assert.assertTrue( c_all.isReadOnly() );
        boolean exception = false;
        try
        {
            c_all.add( all2 );
        }
        catch( SecurityException e )
        {
            exception = true;
        }

        if ( !exception )
        {
            // We should never get here
            Assert.assertTrue( false );
        }
    }

    @Test
    public void testImpliesAllPermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "myWiki" );
        AllPermission all3 = new AllPermission( "big*" );
        AllPermission all4 = new AllPermission( "*" );

        c_all.add( all1 );
        Assert.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assert.assertFalse( c_all.implies( new AllPermission( "myWiki" ) ) );

        c_all.add( all2 );
        Assert.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assert.assertFalse( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );

        c_all.add( all3 );
        Assert.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        Assert.assertFalse( c_all.implies( new AllPermission( "bittyWiki" ) ) );

        c_all.add( all4 );
        Assert.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        Assert.assertTrue( c_all.implies( new AllPermission( "bittyWiki" ) ) );
    }

    @Test
    public void testImpliesPagePermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );

        c_all.add( all2 );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );
    }

    @Test
    public void testImpliesWikiPermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );

        c_all.add( all2 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
    }

    @Test
    public void testImpliesMixedPermissions()
    {
        Permission p1 = new AllPermission( "JSPWiki" );
        Permission p2 = new WikiPermission( "myWiki", "editPreferences" );
        Permission p3 = new PagePermission( "bigTimeWiki:FooBar", "modify" );
        Permission p4 = new AllPermission( "*" );

        c_all.add( p1 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p2 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p3 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assert.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assert.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p4 );
        Assert.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assert.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );
    }

    private int count( AllPermissionCollection collection )
    {
        int i = 0;
        Enumeration< Permission > perms = collection.elements();
        while( perms.hasMoreElements() )
        {
            perms.nextElement();
            i++;
        }
        return i;
    }

}
