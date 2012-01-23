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

import org.apache.wiki.util.ClassUtil;

import junit.framework.*;

public class ClassUtilTest extends TestCase
{
    public ClassUtilTest( String s )
    {
        super( s );
    }

    /**
     *  Tries to find an existing class.
     */
    public void testFindClass()
        throws Exception
    {
        Class<?> foo = ClassUtil.findClass( "org.apache.wiki.api", "WikiPage" );

        assertEquals( foo.getName(), "org.apache.wiki.api.WikiPage" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    public void testFindClassNoClass()
        throws Exception
    {
        try
        {
            Class<?> foo = ClassUtil.findClass( "org.apache.jspwiki", "MubbleBubble" );
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


