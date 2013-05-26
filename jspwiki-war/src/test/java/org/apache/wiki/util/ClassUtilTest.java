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

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ClassUtilTest extends TestCase
{
    public ClassUtilTest( String s )
    {
        super( s );
    }
    
    /**
     * tests various kinds of searches on classpath items
     */
    public void testClasspathSearch() throws Exception
    {
        List< String > jarSearch = ClassUtil.classpathEntriesUnder( "META-INF" );
        assertNotNull( jarSearch );
        assertTrue( jarSearch.size() > 0 );
        
        List< String > fileSearch = ClassUtil.classpathEntriesUnder( "templates" );
        assertNotNull( fileSearch );
        assertTrue( fileSearch.size() > 0 );
        
        List< String > nullSearch = ClassUtil.classpathEntriesUnder( "blurb" );
        assertNotNull( nullSearch );
        assertTrue( nullSearch.size() == 0 );
        
        List< String > nullInputSearch = ClassUtil.classpathEntriesUnder( null );
        assertNotNull( nullInputSearch );
        assertTrue( nullSearch.size() == 0 );
    }

    /**
     *  Tries to find an existing class.
     */
    public void testFindClass()
        throws Exception
    {
        Class foo = ClassUtil.findClass( "org.apache.wiki", "WikiPage" );

        assertEquals( foo.getName(), "org.apache.wiki.WikiPage" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    public void testFindClassNoClass()
        throws Exception
    {
        try
        {
            Class foo = ClassUtil.findClass( "org.apache.wiki", "MubbleBubble" );
            fail("Found class:"+foo);
        }
        catch( ClassNotFoundException e )
        {
            // Expected
        }
    }

    public static Test suite()
    {
        return new TestSuite( ClassUtilTest.class );
    }
}


