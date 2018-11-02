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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 *  Provides static helper functions for serializing different objects.
 *  
 *  @since 2.8
 */
public final class Serializer
{
    /**
     * Prefix used to indicated that a serialized item was encoded with Base64.
     */
    protected static final String BASE64_PREFIX = "base64 ";

    /**
     *  Prevent instantiation.
     */
    private Serializer()
    {}
    
    /**
     * Deserializes a Base64-encoded String into a HashMap. Both the keys and values
     * must implement {@link java.io.Serializable}.
     * @param rawString the String contents containing the map to be deserialized
     * @return the attributes, parsed into a Map
     * @throws IOException if the contents cannot be parsed for any reason
     */
    @SuppressWarnings("unchecked")
    public static Map< String, ? extends Serializable > deserializeFromBase64( String rawString ) throws IOException {
        // Decode from Base64-encoded String to byte array
        byte[] decodedBytes = Base64.getDecoder().decode( rawString.getBytes( StandardCharsets.UTF_8 ) );
        
        // Deserialize from the input stream to the Map
        InputStream bytesIn = new ByteArrayInputStream( decodedBytes );
        try( ObjectInputStream in = new ObjectInputStream( bytesIn ) ) {
            return ( HashMap< String, Serializable > )in.readObject();
        } catch ( ClassNotFoundException e ) {
            throw new IOException( "Could not deserialiaze user profile attributes. Reason: " + e.getMessage() );
        }
    }

    /**
     * Serializes a Map and formats it into a Base64-encoded String. For ease of serialization, the Map contents
     * are first copied into a HashMap, then serialized into a byte array that is encoded as a Base64 String.
     * @param map the Map to serialize
     * @return a String representing the serialized form of the Map
     * @throws IOException If serialization cannot be done
     */
    public static String serializeToBase64( Map< String, Serializable > map ) throws IOException {
        // Load the Map contents into a defensive HashMap
        Map< String, Serializable > serialMap = new HashMap<>();
        serialMap.putAll( map );
        
        // Serialize the Map to an output stream
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( bytesOut );
        out.writeObject( serialMap );
        out.close();
        
        // Transform to Base64-encoded String
        byte[] result = Base64.getEncoder().encode( bytesOut.toByteArray() );
        return new String( result ) ;
    }

}
