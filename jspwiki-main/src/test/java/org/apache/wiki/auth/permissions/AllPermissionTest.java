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

/**
 */
public class AllPermissionTest
{

    /*
     * Class under test for boolean equals(Object)
     */
    @Test
    public void testEqualsObject()
    {
        AllPermission p1 = new AllPermission( "*" );
        AllPermission p2 = new AllPermission( "*" );
        AllPermission p3 = new AllPermission( "myWiki" );
        Assertions.assertTrue( p1.equals( p2 ) );
        Assertions.assertTrue( p2.equals( p1 ) );
        Assertions.assertFalse( p1.equals( p3 ) );
        Assertions.assertFalse( p3.equals( p1 ) );
    }

    @Test
    public void testImpliesAllPermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        AllPermission p2 = new AllPermission( "*" );
        Assertions.assertTrue( p1.equals( p2 ) );
        Assertions.assertTrue( p2.equals( p1 ) );

        p2 = new AllPermission( "myWiki" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
    }

    @Test
    public void testImpliesPagePermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        PagePermission p2 = new PagePermission( "*:TestPage", "delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        p2 = new PagePermission( "myWiki:TestPage", "delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        p2 = new PagePermission( "*:GroupTest", "delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        p2 = new PagePermission( "myWiki:GroupTest", "delete" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
    }

    @Test
    public void testImpliesWikiPermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        WikiPermission p2 = new WikiPermission( "*", "createPages" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );

        p2 = new WikiPermission( "myWiki", "createPages" );
        Assertions.assertTrue( p1.implies( p2 ) );
        Assertions.assertFalse( p2.implies( p1 ) );
    }

    /*
     * Class under test for String toString()
     */
    @Test
    public void testToString()
    {
        AllPermission p = new AllPermission( "myWiki" );
        String result = "(\"org.apache.wiki.auth.permissions.AllPermission\",\"myWiki\")";
        Assertions.assertEquals( result, p.toString() );

        p = new AllPermission( "*" );
        result = "(\"org.apache.wiki.auth.permissions.AllPermission\",\"*\")";
        Assertions.assertEquals( result, p.toString() );
    }

}
