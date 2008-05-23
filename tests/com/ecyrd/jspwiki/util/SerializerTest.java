package com.ecyrd.jspwiki.util;

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
        Map<Serializable, Serializable> map = new HashMap<Serializable, Serializable>();
        map.put( "attribute1", "some random value" );
        map.put( "attribute2", "another value" );
        String serializedForm = Serializer.serializeToBase64( map );

        Map<? extends Serializable, ? extends Serializable> newMap = Serializer.deserializeFromBase64( serializedForm );
        assertEquals( 2, newMap.size() );
        assertTrue( newMap.containsKey( "attribute1" ) );
        assertTrue( newMap.containsKey( "attribute2" ) );
        assertEquals( "some random value", newMap.get( "attribute1" ) );
        assertEquals( "another value", newMap.get( "attribute2" ) );
    }
}
