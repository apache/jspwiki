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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PriorityListTest
{
    @Test
    public void testInsert()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );

        Assertions.assertEquals( 2, p.size(), "size" );

        Assertions.assertEquals( "Two", p.get(0), "Two" );
        Assertions.assertEquals( "One", p.get(1), "One" );
    }

    /**
     *  Check that the priority in case two items are the same priority
     *  is "first goes first".
     */
    @Test
    public void testInsertSame()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 1 );

        Assertions.assertEquals( 2, p.size(), "size" );

        Assertions.assertEquals( "One", p.get(0), "One" );
        Assertions.assertEquals( "Two", p.get(1), "Two" );
    }

    @Test
    public void testInsertSame2()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );
        p.add( "Three", 3 );

        Assertions.assertEquals( 3, p.size(), "size" );

        Assertions.assertEquals( "Three", p.get(0), "Three" );
        Assertions.assertEquals( "Two", p.get(1), "Two" );
        Assertions.assertEquals( "One", p.get(2), "One" );

        p.add( "TwoTwo", 2 );

        Assertions.assertEquals( 4, p.size(), "2: size" );

        Assertions.assertEquals( "Three", p.get(0), "2: Three" );
        Assertions.assertEquals( "Two", p.get(1), "2: Two" );
        Assertions.assertEquals( "TwoTwo", p.get(2), "2: TwoTwo" );
        Assertions.assertEquals( "One", p.get(3), "2: One" );
    }

    @Test
    public void testInsertSame3()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );
        p.add( "Two2", 2 );
        p.add( "Two3", 2 );
        p.add( "Three", 3 );

        Assertions.assertEquals( 5, p.size(), "size" );

        Assertions.assertEquals( "Three", p.get(0), "Three" );
        Assertions.assertEquals( "Two", p.get(1), "Two" );
        Assertions.assertEquals( "Two2", p.get(2), "Two2" );
        Assertions.assertEquals( "Two3", p.get(3), "Two3" );
        Assertions.assertEquals( "One", p.get(4), "One" );

        p.add( "TwoTwo", 2 );

        Assertions.assertEquals( 6, p.size(), "2: size" );

        Assertions.assertEquals( "Three", p.get(0), "2: Three" );
        Assertions.assertEquals( "Two", p.get(1), "2: Two" );
        Assertions.assertEquals( "Two2", p.get(2), "2: Two2" );
        Assertions.assertEquals( "Two3", p.get(3), "2: Two3" );
        Assertions.assertEquals( "TwoTwo", p.get(4), "2: TwoTwo" );
        Assertions.assertEquals( "One", p.get(5), "2: One" );
    }

}
