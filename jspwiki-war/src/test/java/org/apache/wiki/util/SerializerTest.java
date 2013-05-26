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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SerializerTest extends TestCase
{

    public static Test suite()
    {
        return new TestSuite( SerializerTest.class );
    }

    public void testSerializeMap() throws Exception
    {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put( "attribute1", "some random value" );
        map.put( "attribute2", "another value" );
        String serializedForm = Serializer.serializeToBase64( map );

        Map<String, ? extends Serializable> newMap = Serializer.deserializeFromBase64( serializedForm );
        assertEquals( 2, newMap.size() );
        assertTrue( newMap.containsKey( "attribute1" ) );
        assertTrue( newMap.containsKey( "attribute2" ) );
        assertEquals( "some random value", newMap.get( "attribute1" ) );
        assertEquals( "another value", newMap.get( "attribute2" ) );
    }
}
