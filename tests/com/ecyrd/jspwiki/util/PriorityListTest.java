
package com.ecyrd.jspwiki.util;

import junit.framework.*;
import java.util.*;

import org.apache.log4j.*;

public class PriorityListTest extends TestCase
{
    public PriorityListTest( String s )
    {
        super( s );
    }
    
    public void testInsert()
    {
        PriorityList p = new PriorityList();

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
        PriorityList p = new PriorityList();

        p.add( "One", 1 );
        p.add( "Two", 1 );
        
        assertEquals( "size", 2, p.size() );

        assertEquals( "One", "One", p.get(0) );
        assertEquals( "Two", "Two", p.get(1) );
    }

    public void testInsertSame2()
    {
        PriorityList p = new PriorityList();

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

    public static Test suite()
    {
        return new TestSuite( PriorityListTest.class );
    }
}


