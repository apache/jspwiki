/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.htmltowiki;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * some usefull methods for properties
 *
 * @author Sebastian Baltes (sbaltes@gmx.com)
 * @version 1.0
 */
public final class PropertiesUtils
{
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
        TreeMap treemap = new TreeMap( properties );
        String string = "";
        Iterator iterator = treemap.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            String key = (String)entry.getKey();
            String value = entry.getValue() == null ? "null" : entry.getValue().toString();
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
        StringBuffer stringbuffer = new StringBuffer( i * 2 );
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
