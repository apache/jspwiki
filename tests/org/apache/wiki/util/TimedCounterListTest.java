/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
