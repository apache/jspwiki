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

/**
 *  A collection of static byte utility methods.
 *  
 * @author Ichiro Furusato
 */
public class ByteUtils
{
    private static final char[] hexArray = "0123456789abcdef".toCharArray();
    
    
    /**
     *  byte[] array to hex conversion. Note that this provides
     *  no delimiters; the bytes are simply concatenated.
     */
    public static String bytes2hex( byte[] bytes )
    {
        char[] ca = new char[bytes.length * 2];
        for ( int i = 0; i < bytes.length; i++ ) {
            int v = bytes[i] & 0xff;
            ca[i * 2] = hexArray[v >>> 4];
            ca[i * 2 + 1] = hexArray[v & 0x0f];
        }
        return new String(ca);
    }
    
    
    /**
     *  byte to hex conversion.
     */
    public static String byte2hex( byte b )
    {
        return Integer.toHexString(b & 0xff);
    }

    
    /**
     *  Parses a hexadecimal string into its corresponding bytes.
     */
    public static byte[] parseHexBinary( String hex )
    {
        final int len = hex.length();
        // e.g., "111" is not a valid hex encoding.
        if( len%2 != 0 ) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: "+hex);
        }
        byte[] out = new byte[len/2];
        for( int i = 0; i < len; i+=2 ) {
            int h = hexToBin(hex.charAt(i));
            int l = hexToBin(hex.charAt(i+1));
            if ( h==-1 || l==-1 ) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: "+hex);
            }
            out[i/2] = (byte)(h*16+l);
        }
        return out;
    }
    
    
    /**
     *  Converts a single hex character into its integer equivalent.
     */
    public static int hexToBin( char c )
    {
        if ( '0'<=c && c<='9' ) return c-'0';
        if ( 'A'<=c && c<='F' ) return c-'A'+10;
        if ( 'a'<=c && c<='f' ) return c-'a'+10;
        return -1;
    }

    
    private ByteUtils() {}

}
