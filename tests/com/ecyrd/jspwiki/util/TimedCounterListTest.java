package com.ecyrd.jspwiki.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TimedCounterListTest extends TestCase
{
    TimedCounterList<String> m_list = new TimedCounterList<String>();
    
    public void setUp()
    {
        m_list.add( "Foo" );
        m_list.add( "Foo" );
        m_list.add( "Foo" );
        m_list.add( "Bar" );
    }
    
    public void testCount()
    {
        assertEquals( "Foo", 3, m_list.count( "Foo" ) );
        assertEquals( "Bar", 1, m_list.count( "Bar" ) );
        assertEquals( "Baz", 0, m_list.count( "Baz" ) );
    }
    
    public void testCleanup()
    {
        try
        {
            Thread.sleep(110);
            
            m_list.cleanup(100);
            
            assertEquals( "Foo", 0, m_list.count( "Foo" ) );
            assertEquals( "Bar", 0, m_list.count( "Foo" ) );
            assertEquals( "Baz", 0, m_list.count( "Foo" ) );
            
            assertEquals( "size", 0, m_list.size() );
        }
        catch( InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    public static Test suite()
    {
        return new TestSuite( TimedCounterListTest.class );
    }

}
