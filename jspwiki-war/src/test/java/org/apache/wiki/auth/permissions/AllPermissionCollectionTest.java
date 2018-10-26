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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class AllPermissionCollectionTest
{

    AllPermissionCollection c_all;

    /**
     *
     */
    @BeforeEach
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
        Assertions.assertEquals( 1, count( c_all ) );

        c_all.add( all2 );
        Assertions.assertEquals( 2, count( c_all ) );

        c_all.add( all3 );
        Assertions.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( all4 );
        Assertions.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testAddPagePermission()
    {
        PagePermission p1 = new PagePermission( "JSPWiki:Main", "edit" );
        PagePermission p2 = new PagePermission( "JSPWiki:GroupAdmin", "edit" );
        PagePermission p3 = new PagePermission( "JSPWiki:Foobar", "delete" );
        PagePermission p4 = new PagePermission( "JSPWiki:Main", "edit" );

        c_all.add( p1 );
        Assertions.assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        Assertions.assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        Assertions.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
        Assertions.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testAddWikiPermission()
    {
        WikiPermission p1 = new WikiPermission( "JSPWiki", "login" );
        WikiPermission p2 = new WikiPermission( "JSPWiki", "createGroups" );
        WikiPermission p3 = new WikiPermission( "JSPWiki", "editPreferences" );
        WikiPermission p4 = new WikiPermission( "JSPWiki", "login" );

        c_all.add( p1 );
        Assertions.assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        Assertions.assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        Assertions.assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
        Assertions.assertEquals( 3, count( c_all ) );
    }

    @Test
    public void testReadOnly()
    {
        AllPermission all1 = new AllPermission( "*" );
        AllPermission all2 = new AllPermission( "JSPWiki" );

        Assertions.assertFalse( c_all.isReadOnly() );
        c_all.add( all1 );
        Assertions.assertFalse( c_all.isReadOnly() );

        // Mark as read-only; isReadOnly should return "true", as we'll get an
        // error
        c_all.setReadOnly();
        Assertions.assertTrue( c_all.isReadOnly() );
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
            Assertions.assertTrue( false );
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
        Assertions.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assertions.assertFalse( c_all.implies( new AllPermission( "myWiki" ) ) );

        c_all.add( all2 );
        Assertions.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assertions.assertFalse( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );

        c_all.add( all3 );
        Assertions.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        Assertions.assertFalse( c_all.implies( new AllPermission( "bittyWiki" ) ) );

        c_all.add( all4 );
        Assertions.assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        Assertions.assertTrue( c_all.implies( new AllPermission( "bittyWiki" ) ) );
    }

    @Test
    public void testImpliesPagePermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );

        c_all.add( all2 );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );
    }

    @Test
    public void testImpliesWikiPermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );

        c_all.add( all2 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
    }

    @Test
    public void testImpliesMixedPermissions()
    {
        Permission p1 = new AllPermission( "JSPWiki" );
        Permission p2 = new WikiPermission( "myWiki", "editPreferences" );
        Permission p3 = new PagePermission( "bigTimeWiki:FooBar", "modify" );
        Permission p4 = new AllPermission( "*" );

        c_all.add( p1 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p2 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p3 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assertions.assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assertions.assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p4 );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        Assertions.assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );
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
