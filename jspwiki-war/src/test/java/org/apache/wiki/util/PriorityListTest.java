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

package org.apache.wiki.util;

import junit.framework.*;

public class PriorityListTest extends TestCase
{
    public PriorityListTest( String s )
    {
        super( s );
    }

    public void testInsert()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );

        assertEquals( "size", 2, p.size() );

        assertEquals( "Two", "Two", p.get(0) );
        assertEquals( "One", "One", p.get(1) );
    }

    /**
     *  Check that the priority in case two items are the same priority
     *  is "first goes first".
     */
    public void testInsertSame()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 1 );

        assertEquals( "size", 2, p.size() );

        assertEquals( "One", "One", p.get(0) );
        assertEquals( "Two", "Two", p.get(1) );
    }

    public void testInsertSame2()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );
        p.add( "Three", 3 );

        assertEquals( "size", 3, p.size() );

        assertEquals( "Three", "Three", p.get(0) );
        assertEquals( "Two", "Two", p.get(1) );
        assertEquals( "One", "One", p.get(2) );

        p.add( "TwoTwo", 2 );

        assertEquals( "2: size", 4, p.size() );

        assertEquals( "2: Three", "Three", p.get(0) );
        assertEquals( "2: Two", "Two", p.get(1) );
        assertEquals( "2: TwoTwo", "TwoTwo", p.get(2) );
        assertEquals( "2: One", "One", p.get(3) );
    }

    public void testInsertSame3()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );
        p.add( "Two2", 2 );
        p.add( "Two3", 2 );
        p.add( "Three", 3 );

        assertEquals( "size", 5, p.size() );

        assertEquals( "Three", "Three", p.get(0) );
        assertEquals( "Two", "Two", p.get(1) );
        assertEquals( "Two2", "Two2", p.get(2) );
        assertEquals( "Two3", "Two3", p.get(3) );
        assertEquals( "One", "One", p.get(4) );

        p.add( "TwoTwo", 2 );

        assertEquals( "2: size", 6, p.size() );

        assertEquals( "2: Three", "Three", p.get(0) );
        assertEquals( "2: Two", "Two", p.get(1) );
        assertEquals( "2: Two2", "Two2", p.get(2) );
        assertEquals( "2: Two3", "Two3", p.get(3) );
        assertEquals( "2: TwoTwo", "TwoTwo", p.get(4) );
        assertEquals( "2: One", "One", p.get(5) );
    }

    public static Test suite()
    {
        return new TestSuite( PriorityListTest.class );
    }
}


