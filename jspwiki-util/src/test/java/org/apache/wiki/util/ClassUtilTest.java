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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ClassUtilTest
{

    /**
     * tests various kinds of searches on classpath items
     */
    @Test
    public void testClasspathSearch() throws Exception
    {
        List< String > jarSearch = ClassUtil.classpathEntriesUnder( "META-INF" );
        Assertions.assertNotNull( jarSearch );
        Assertions.assertTrue( jarSearch.size() > 0 );

        List< String > fileSearch = ClassUtil.classpathEntriesUnder( "templates" );
        Assertions.assertNotNull( fileSearch );
        Assertions.assertTrue( fileSearch.size() > 0 );

        List< String > nullSearch = ClassUtil.classpathEntriesUnder( "blurb" );
        Assertions.assertNotNull( nullSearch );
        Assertions.assertTrue( nullSearch.size() == 0 );

        List< String > nullInputSearch = ClassUtil.classpathEntriesUnder( null );
        Assertions.assertNotNull( nullInputSearch );
        Assertions.assertTrue( nullSearch.size() == 0 );
    }

    /**
     *  Tries to find an existing class.
     */
    @Test
    public void testFindClass()
        throws Exception
    {
        Class< ? > foo = ClassUtil.findClass( "java.util", "List" );

        Assertions.assertEquals( foo.getName(), "java.util.List" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    @Test
    public void testFindClassNoClass()
        throws Exception
    {
        try
        {
            Class< ? > foo = ClassUtil.findClass( "org.apache.wiki", "MubbleBubble" );
            Assertions.fail( "Found class:" + foo );
        }
        catch( ClassNotFoundException e )
        {
            // Expected
        }
    }

    @Test
    public void testAssignable() {
        Assertions.assertTrue( ClassUtil.assignable( "java.util.ArrayList", "java.util.List" ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", "java.util.ArrayList" ) );
        Assertions.assertFalse( ClassUtil.assignable( null, "java.util.ArrayList" ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", null ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", "java.util.HashMap" ) );
    }

}
