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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * some useful methods for properties
 *
 * @version 1.0
 */
public final class PropertiesUtils {

    private static final String OTHER_WHITESPACE = "\t\r\n\014";
    private static final char[] HEXDIGIT = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Private constructor to prevent instantiation. */
    private PropertiesUtils()
    {}

    /**
     * <p>
     * like Properties.store, but stores the properties in sorted order
     * </p>
     *
     * @param properties the properties object
     * @return String the properties, nicely formatted 
     */
    public static String toSortedString( Properties properties )
    {
        @SuppressWarnings( { "unchecked", "rawtypes" } )
		TreeMap< String, String > treemap = new TreeMap( properties );
        String string = "";
        Iterator< Map.Entry< String, String > > iterator = treemap.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry< String, String > entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue() == null ? "null" : entry.getValue();
            string += toLine( key, value ) + '\n';
        }
        return string;
    }

    /**
     * Generates a property file line from a supplied key and value.
     * @param key the property's key
     * @param value the property's value
     * @return the converted string
     */
    public static String toLine( String key, String value )
    {
        return saveConvert( key, true ) + '=' + saveConvert( value, false );
    }

    /**
     * Encodes a property file string from a supplied key/value line.
     * @param string the string to encode
     * @param encodeWhiteSpace <code>true</code> if whitespace should be encoded also
     * @return the converted string
     */
    public static String saveConvert( String string, boolean encodeWhiteSpace )
    {
        int i = string.length();
        StringBuilder stringbuffer = new StringBuilder( i * 2 );
        for( int i3 = 0; i3 < i; i3++ )
        {
            char c = string.charAt( i3 );
            switch( c )
            {
                case ' ':
                    if( i3 == 0 || encodeWhiteSpace )
                    {
                        stringbuffer.append( '\\' );
                    }
                    stringbuffer.append( ' ' );
                    break;
                case '\\':
                    stringbuffer.append( '\\' );
                    stringbuffer.append( '\\' );
                    break;
                case '\t':
                    stringbuffer.append( '\\' );
                    stringbuffer.append( 't' );
                    break;
                case '\n':
                    stringbuffer.append( '\\' );
                    stringbuffer.append( 'n' );
                    break;
                case '\r':
                    stringbuffer.append( '\\' );
                    stringbuffer.append( 'r' );
                    break;
                case '\014':
                    stringbuffer.append( '\\' );
                    stringbuffer.append( 'f' );
                    break;
                default:
                    if( c < 32 || c > 126 )
                    {
                        stringbuffer.append( '\\' );
                        stringbuffer.append( 'u' );
                        stringbuffer.append( toHex( c >> 12 & 0xf ) );
                        stringbuffer.append( toHex( c >> 8 & 0xf ) );
                        stringbuffer.append( toHex( c >> 4 & 0xf ) );
                        stringbuffer.append( toHex( c & 0xf ) );
                    }
                    else
                    {
                        if( OTHER_WHITESPACE.indexOf( c ) != -1 )
                        {
                            stringbuffer.append( '\\' );
                        }
                        stringbuffer.append( c );
                    }
            }
        }
        return stringbuffer.toString();
    }

    private static char toHex( int i )
    {
        return HEXDIGIT[i & 0xf];
    }
}
