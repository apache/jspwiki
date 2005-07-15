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
public class PropertiesUtils
{
    private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * <p>
     * like Properties.store, but stores the properties in sorted order
     * </p>
     * 
     * @param properties
     * @return String
     */
    public static String toSortedString( Properties properties )
    {
        TreeMap treemap = new TreeMap( properties );
        String string = "";
        Iterator iterator = treemap.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            String string_0_ = (String)entry.getKey();
            String string_1_ = entry.getValue() == null ? null : entry.getValue().toString();
            string += toLine( string_0_, string_1_ ) + "\n";
        }
        return string;
    }

    /**
     * @param key
     * @param value
     * @return
     */
    public static String toLine( String key, String value )
    {
        return saveConvert( key, true ) + "=" + saveConvert( value, false );
    }

    /**
     * @param string
     * @param encodeWhiteSpace
     * @return
     */
    public static String saveConvert( String string, boolean encodeWhiteSpace )
    {
        int i = string.length();
        StringBuffer stringbuffer = new StringBuffer( i * 2 );
        for( int i_3_ = 0; i_3_ < i; i_3_++ )
        {
            char c = string.charAt( i_3_ );
            switch( c )
            {
                case ' ':
                    if( i_3_ == 0 || encodeWhiteSpace )
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
                        if( "\t\r\n\014".indexOf( c ) != -1 )
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
        return hexDigit[i & 0xf];
    }
}
