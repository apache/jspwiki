/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.UnsupportedEncodingException;

public class TextUtil
{
    static String HEX_DIGITS =  "0123456789ABCDEF";

    /**
     *  java.net.URLEncoder.encode() method in JDK < 1.4 is buggy.  This duplicates
     *  its functionality.
     */
    protected static String urlEncode( byte[] rs )
    {
        StringBuffer result = new StringBuffer();

        // Does the URLEncoding.  We could use the java.net one, but
        // it does not eat byte[]s.

        for( int i = 0; i < rs.length; i++ )
        {
            char c = (char) rs[i];

            switch( c )
            {
              case '_':
              case '.':
              case '*':
              case '-':
                result.append( c );
                break;

              case ' ':
                result.append( '+' );
                break;

              default:
                if( (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') )
                {                    
                    result.append( c );
                }
                else
                {
                    result.append( '%' );
                    result.append( HEX_DIGITS.charAt( (c & 0xF0) >> 4 ) );
                    result.append( HEX_DIGITS.charAt( c & 0x0F ) );
                }
            }

        } // for

        return result.toString();
    }

    /**
     *  URL encoder does not handle all characters correctly.
     *  See <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4257115.html">
     *  Bug parade, bug #4257115</A> for more information.
     *  <P>
     *  Thanks to CJB for this fix.
     */ 
    protected static String urlDecode( byte[] bytes )
        throws UnsupportedEncodingException,
               IllegalArgumentException
    {
        if(bytes == null) 
        {
            return null;
        }

        byte[] decodeBytes   = new byte[bytes.length];
        int decodedByteCount = 0;

        try 
        {
            for( int count = 0; count < bytes.length; count++ ) 
            {
                switch( bytes[count] ) 
                {
                  case '+':
                    decodeBytes[decodedByteCount++] = (byte) ' ';
                    break ;

                  case '%':
                    decodeBytes[decodedByteCount++] = (byte)((HEX_DIGITS.indexOf(bytes[++count]) << 4) +
                                                             (HEX_DIGITS.indexOf(bytes[++count])) );

                    break ;

                  default:
                    decodeBytes[decodedByteCount++] = bytes[count] ;
                }
            }

        }
        catch (IndexOutOfBoundsException ae) 
        {
            throw new IllegalArgumentException( "Malformed UTF-8 string?" );
        }

        String processedPageName = null ;

        try 
        {
            processedPageName = new String(decodeBytes, 0, decodedByteCount, "UTF-8") ;
        } 
        catch (UnsupportedEncodingException e) 
        {
            throw new UnsupportedEncodingException( "UTF-8 encoding not supported on this platform" );
        }

        return(processedPageName.toString());
    }

    /**
     *  As java.net.URLEncoder class, but this does it in UTF8 character set.
     */
    public static String urlEncodeUTF8( String text )
    {
        byte[] rs = {};

        try
        {
            rs = text.getBytes("UTF-8");
            return urlEncode( rs );
        }
        catch( UnsupportedEncodingException e )
        {
            return java.net.URLEncoder.encode( text );
        }

    }

    /**
     *  As java.net.URLDecoder class, but for UTF-8 strings.
     */
    public static String urlDecodeUTF8( String utf8 )
    {
        String rs = null;

        try
        {
            rs = urlDecode( utf8.getBytes("ISO-8859-1") );
        }
        catch( UnsupportedEncodingException e )
        {
            rs = java.net.URLDecoder.decode( utf8 );
        }

        return rs;
    }

    /**
     *  Replaces the relevant entities inside the String.
     *  All &gt;, &lt; and &quot; are replaced by their
     *  respective names.
     *
     *  @since 1.6.1
     */
    public static String replaceEntities( String src )
    {
        src = TranslatorReader.replaceString( src, "<", "&lt;" );
        src = TranslatorReader.replaceString( src, ">", "&gt;" );
        src = TranslatorReader.replaceString( src, "\"", "&quot;" );

        return src;
    }

}
