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
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;


public class TextUtil
{
    static String HEX_DIGITS =  "0123456789ABCDEF";
    static final char DIFF_ADDED_SYMBOL    = '+';
    static final char DIFF_REMOVED_SYMBOL  = '-';
    static final String CSS_DIFF_ADDED     = "<TR><TD BGCOLOR=#99FF99 class=\"diffadd\">";
    static final String CSS_DIFF_REMOVED   = "<TR><TD BGCOLOR=#FF9933 class=\"diffrem\">";
    static final String CSS_DIFF_UNCHANGED = "<TR><TD class=\"diff\">";
    static final String CSS_DIFF_CLOSE     = "</TD></TR>";

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
        src = replaceString( src, "<", "&lt;" );
        src = replaceString( src, ">", "&gt;" );
        src = replaceString( src, "\"", "&quot;" );

        return src;
    }

    /**
     *  Replaces a string with an other string.
     *
     *  @param orig Original string.  Null is safe.
     *  @param src  The string to find.
     *  @param dest The string to replace <I>src</I> with.
     */

    public static String replaceString( String orig, String src, String dest )
    {
        if( orig == null ) return null;

        StringBuffer res = new StringBuffer();
        int start, end = 0, last = 0;

        while( (start = orig.indexOf(src,end)) != -1 )
        {
            res.append( orig.substring( last, start ) );
            res.append( dest );
            end  = start+src.length();
            last = start+src.length();
        }

        res.append( orig.substring( end ) );

        return res.toString();
    }

    /**
     *  Replaces a part of a string with a new String.
     *
     *  @param start Where in the original string the replacing should start.
     @  @param end Where the replacing should end.
     *  @param orig Original string.  Null is safe.
     @  @param text The new text to insert into the string.
     */
    public static String replaceString( String orig, int start, int end, String text )
    {
        if( orig == null ) return null;

        StringBuffer buf = new StringBuffer(orig);

        buf.replace( start, end, text );

        return buf.toString();
    }

    /**
     *  Parses an integer parameter, returning a default value
     *  if the value is null or a non-number.
     */

    public static int parseIntParameter( String value, int defvalue )
    {
        int val = defvalue;
        
        try
        {
            val = Integer.parseInt( value );
        }
        catch( Exception e ) {}
            
        return val;
    }


    /**
     * Goes through output provided by a diff command and inserts
     * HTML tags to make the result more legible.
     * Currently colors lines starting with a + green,
     * those starting with - reddish (hm, got to think of
     * color blindness here...).
     */
    public static String colorizeDiff( String diffText )
        throws IOException
    {
        String line = null;
        String start = null;
        String stop = null;

        if( diffText == null )
        {
            return "Invalid diff - probably something wrong with server setup.";
        }

        BufferedReader in = new BufferedReader( new StringReader( diffText ) );
        StringBuffer out = new StringBuffer();

        out.append("<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=0>");
        while( ( line = in.readLine() ) != null )
        {
            stop  = CSS_DIFF_CLOSE;
            switch( line.charAt( 0 ) )
            {
              case DIFF_ADDED_SYMBOL:
                start = CSS_DIFF_ADDED;
                break;
              case DIFF_REMOVED_SYMBOL:
                start = CSS_DIFF_REMOVED;
                break;
              default:
                start = CSS_DIFF_UNCHANGED;
            }
            
            out.append( start );
            out.append( line.trim() );
            out.append( stop + "\n" );

        }
        out.append("</TABLE>");
        return( out.toString() );
    }

}
