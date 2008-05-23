package com.ecyrd.jspwiki.util;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

public class Serializer
{
    
    /**
     * Deserializes a Base64-encoded String into a HashMap. Both the keys and values
     * must implement {@link java.io.Serializable}.
     * @param rawString the String contents containing the map to be deserialized
     * @return the attributes, parsed into a Properties object
     * @throws SQLException if the contents cannot be parsed for any reason
     */
    @SuppressWarnings("unchecked")
    public static Map<? extends Serializable,? extends Serializable> deserializeFromBase64( String rawString ) throws IOException
    {
        // Decode from Base64-encoded String to byte array
        byte[] decodedBytes = Base64.decodeBase64( rawString.getBytes() );
        
        // Deserialize from the input stream to the Map
        InputStream bytesIn = new ByteArrayInputStream( decodedBytes );
        ObjectInputStream in = new ObjectInputStream( bytesIn );
        HashMap<Serializable,Serializable> attributes;
        try
        {
            attributes = (HashMap<Serializable,Serializable>)in.readObject();
        }
        catch ( ClassNotFoundException e )
        {
            throw new IOException( "Could not deserialiaze user profile attributes. Reason: " + e.getMessage() );
        }
        finally
        {
            in.close();
        }
        return attributes;
    }

    /**
     * Serializes a Map and formats it into a Base64-encoded String. For ease of serialization, the Map contents
     * are first copied into a HashMap, then serialized into a byte array that is encoded as a Base64 String.
     * @param the Map to serialize
     * @return a String representing the serialized form of the Map
     */
    public static String serializeToBase64( Map<Serializable,Serializable> map ) throws IOException
    {
        // Load the Map contents into a defensive HashMap
        HashMap<Serializable,Serializable> serialMap = new HashMap<Serializable,Serializable>();
        serialMap.putAll( map );
        
        // Serialize the Map to an output stream
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( bytesOut );
        out.writeObject( serialMap );
        out.close();
        
        // Transform to Base64-encoded String
        byte[] result = Base64.encodeBase64( bytesOut.toByteArray() );
        return new String( result ) ;
    }

}
