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
    static char[] hex = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

    /**
     *  As java.net.URLEncoder class, but this does it in UTF8 character set.
     */
    public static String urlEncodeUTF8( String text )
    {
        byte[] rs = {};

        try
        {
            rs = text.getBytes("UTF-8");
            return java.net.URLEncoder.encode( new String( rs, "ISO-8859-1" ) );
        }
        catch( UnsupportedEncodingException e )
        {
            return java.net.URLEncoder.encode( text );
        }

        // SLOW version commented out.
        /*
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
                    result.append( hex[(c & 0xF0) >> 4] );
                    result.append( hex[c & 0x0F] );
                }
            }

        } // for

        return result.toString();
        */

        
    }

    public static String urlDecodeUTF8( String utf8 )
    {
        String rs = java.net.URLDecoder.decode( utf8 );

        try
        {
            rs = new String( rs.getBytes("ISO-8859-1"), "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
        }

        return rs;
    }

}
