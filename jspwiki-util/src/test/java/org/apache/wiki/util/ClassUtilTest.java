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

import java.util.List;


public class ClassUtilTest
{

    /**
     * tests various kinds of searches on classpath items
     */
    @Test
    public void testClasspathSearch() {
        final List< String > jarSearch = ClassUtil.classpathEntriesUnder( "META-INF" );
        Assertions.assertNotNull( jarSearch );
        Assertions.assertTrue( jarSearch.size() > 0 );

        final List< String > fileSearch = ClassUtil.classpathEntriesUnder( "templates" );
        Assertions.assertNotNull( fileSearch );
        Assertions.assertTrue( fileSearch.size() > 0 );

        final List< String > nullSearch = ClassUtil.classpathEntriesUnder( "blurb" );
        Assertions.assertNotNull( nullSearch );
        Assertions.assertEquals( 0, nullSearch.size() );

        final List< String > nullInputSearch = ClassUtil.classpathEntriesUnder( null );
        Assertions.assertNotNull( nullInputSearch );
        Assertions.assertEquals( 0, nullSearch.size() );
    }

    /**
     *  Tries to find an existing class.
     */
    @Test
    public void testFindClass() throws Exception {
        final Class< List< ? > > foo = ClassUtil.findClass( "java.util", "List" );

        Assertions.assertEquals( foo.getName(), "java.util.List" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    @Test
    public void testFindClassNoClass() {
        Assertions.assertThrows( ClassNotFoundException.class, () ->  ClassUtil.findClass( "org.apache.wiki", "MubbleBubble" ) );
    }

    @Test
    public void testAssignable() {
        Assertions.assertTrue( ClassUtil.assignable( "java.util.ArrayList", "java.util.List" ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", "java.util.ArrayList" ) );
        Assertions.assertFalse( ClassUtil.assignable( null, "java.util.ArrayList" ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", null ) );
        Assertions.assertFalse( ClassUtil.assignable( "java.util.List", "java.util.HashMap" ) );
    }

    @Test
    public void testExists() {
        Assertions.assertTrue( ClassUtil.exists( "java.util.List" ) );
        Assertions.assertFalse( ClassUtil.exists( "org.apache.wiski.FrisFrus" ) );
    }

    @Test
    public void testBuildInstance() throws Exception {
        Assertions.assertTrue( ClassUtil.buildInstance( "java.util.ArrayList" ) instanceof List );
        Assertions.assertThrows( NoSuchMethodException.class, () -> ClassUtil.buildInstance( "java.util.List" ) );
    }

}
