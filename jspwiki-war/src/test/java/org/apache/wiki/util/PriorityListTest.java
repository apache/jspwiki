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

import org.junit.Assert;
import org.junit.Test;

public class PriorityListTest
{
    @Test
    public void testInsert()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );

        Assert.assertEquals( "size", 2, p.size() );

        Assert.assertEquals( "Two", "Two", p.get(0) );
        Assert.assertEquals( "One", "One", p.get(1) );
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

        Assert.assertEquals( "size", 2, p.size() );

        Assert.assertEquals( "One", "One", p.get(0) );
        Assert.assertEquals( "Two", "Two", p.get(1) );
    }

    @Test
    public void testInsertSame2()
    {
        PriorityList< String > p = new PriorityList< String >();

        p.add( "One", 1 );
        p.add( "Two", 2 );
        p.add( "Three", 3 );

        Assert.assertEquals( "size", 3, p.size() );

        Assert.assertEquals( "Three", "Three", p.get(0) );
        Assert.assertEquals( "Two", "Two", p.get(1) );
        Assert.assertEquals( "One", "One", p.get(2) );

        p.add( "TwoTwo", 2 );

        Assert.assertEquals( "2: size", 4, p.size() );

        Assert.assertEquals( "2: Three", "Three", p.get(0) );
        Assert.assertEquals( "2: Two", "Two", p.get(1) );
        Assert.assertEquals( "2: TwoTwo", "TwoTwo", p.get(2) );
        Assert.assertEquals( "2: One", "One", p.get(3) );
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

        Assert.assertEquals( "size", 5, p.size() );

        Assert.assertEquals( "Three", "Three", p.get(0) );
        Assert.assertEquals( "Two", "Two", p.get(1) );
        Assert.assertEquals( "Two2", "Two2", p.get(2) );
        Assert.assertEquals( "Two3", "Two3", p.get(3) );
        Assert.assertEquals( "One", "One", p.get(4) );

        p.add( "TwoTwo", 2 );

        Assert.assertEquals( "2: size", 6, p.size() );

        Assert.assertEquals( "2: Three", "Three", p.get(0) );
        Assert.assertEquals( "2: Two", "Two", p.get(1) );
        Assert.assertEquals( "2: Two2", "Two2", p.get(2) );
        Assert.assertEquals( "2: Two3", "Two3", p.get(3) );
        Assert.assertEquals( "2: TwoTwo", "TwoTwo", p.get(4) );
        Assert.assertEquals( "2: One", "One", p.get(5) );
    }

}
