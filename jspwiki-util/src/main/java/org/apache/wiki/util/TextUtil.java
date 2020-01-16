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

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;


/**
 *  Contains a number of static utility methods.
 */
public final class TextUtil {

    static final String HEX_DIGITS = "0123456789ABCDEF";

    /** Pick from some letters that won't be easily mistaken for each other to compose passwords. So, for example, omit o O and 0, 1 l and L.*/
    static final String PWD_BASE = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789+@";

    /** Length of password. {@link #generateRandomPassword() */
    public static final int PASSWORD_LENGTH = 8;

    /** Lists all punctuation characters allowed in WikiMarkup. These will not be cleaned away. This is for compatibility for older versions
     of JSPWiki. */
    public static final String LEGACY_CHARS_ALLOWED = "._";

    /** Lists all punctuation characters allowed in page names. */
    public static final String PUNCTUATION_CHARS_ALLOWED = " ()&+,-=._$";

    /** Private constructor prevents instantiation. */
    private TextUtil() {}

    /**
     *  java.net.URLEncoder.encode() method in JDK < 1.4 is buggy.  This duplicates its functionality.
     *
     *  @param rs the string to encode
     *  @return the URL-encoded string
     */
    protected static String urlEncode( final byte[] rs ) {
    	final StringBuilder result = new StringBuilder( rs.length * 2 );

        // Does the URLEncoding.  We could use the java.net one, but it does not eat byte[]s.
        for( final byte r : rs ) {
            final char c = ( char )r;
            switch( c ) {
            case '_':
            case '.':
            case '*':
            case '-':
            case '/':
                result.append( c );
                break;
            case ' ':
                result.append( '+' );
                break;
            default:
                if( ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || ( c >= '0' && c <= '9' ) ) {
                    result.append( c );
                } else {
                    result.append( '%' );
                    result.append( HEX_DIGITS.charAt( ( c & 0xF0 ) >> 4 ) );
                    result.append( HEX_DIGITS.charAt( c & 0x0F ) );
                }
            }
        }

        return result.toString();
    }

    /**
     *  URL encoder does not handle all characters correctly. See <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4257115.html">
     *  Bug parade, bug #4257115</A> for more information.
     *  <P>
     *  Thanks to CJB for this fix.
     *
     *  @param bytes The byte array containing the bytes of the string
     *  @param encoding The encoding in which the string should be interpreted
     *  @return A decoded String
     *
     *  @throws IllegalArgumentException If the byte array is not a valid string.
     */
    protected static String urlDecode( final byte[] bytes, final String encoding ) throws  IllegalArgumentException {
        if( bytes == null ) {
            return null;
        }

        final byte[] decodeBytes = new byte[ bytes.length ];
        int decodedByteCount = 0;

        try {
            for( int count = 0; count < bytes.length; count++ ) {
                switch( bytes[count] ) {
                  case '+':
                    decodeBytes[decodedByteCount++] = ( byte ) ' ';
                    break ;

                  case '%':
                    decodeBytes[decodedByteCount++] = ( byte )( ( HEX_DIGITS.indexOf( bytes[++count] ) << 4 ) +
                                                                ( HEX_DIGITS.indexOf( bytes[++count] ) ) );
                    break ;

                  default:
                    decodeBytes[decodedByteCount++] = bytes[count] ;
                }
            }

        } catch( final IndexOutOfBoundsException ae ) {
            throw new IllegalArgumentException( "Malformed UTF-8 string?" );
        }

        return new String(decodeBytes, 0, decodedByteCount, Charset.forName( encoding ) );
    }

    /**
     *  As java.net.URLEncoder class, but this does it in UTF8 character set.
     *
     *  @param text The text to decode
     *  @return An URLEncoded string.
     */
    public static String urlEncodeUTF8( final String text ) {
        // If text is null, just return an empty string
        if ( text == null ) {
            return "";
        }

        return urlEncode( text.getBytes( StandardCharsets.UTF_8 ) );
    }

    /**
     *  As java.net.URLDecoder class, but for UTF-8 strings.  null is a safe value and returns null.
     *
     *  @param utf8 The UTF-8 encoded string
     *  @return A plain, normal string.
     */
    public static String urlDecodeUTF8( final String utf8 ) {
        if( utf8 == null ) {
            return null;
        }

        return urlDecode( utf8.getBytes( StandardCharsets.ISO_8859_1 ), StandardCharsets.UTF_8.toString() );
    }

    /**
     * Provides encoded version of string depending on encoding. Encoding may be UTF-8 or ISO-8859-1 (default).
     *
     * <p>This implementation is the same as in FileSystemProvider.mangleName().
     *
     * @param data A string to encode
     * @param encoding The encoding in which to encode
     * @return An URL encoded string.
     */
    public static String urlEncode( final String data, final String encoding ) {
        // Presumably, the same caveats apply as in FileSystemProvider. Don't see why it would be horribly kludgy, though.
        if( StandardCharsets.UTF_8.toString().equals( encoding ) ) {
            return urlEncodeUTF8( data );
        }

        return urlEncode( data.getBytes( Charset.forName( encoding ) ) );
    }

    /**
     * Provides decoded version of string depending on encoding. Encoding may be UTF-8 or ISO-8859-1 (default).
     *
     * <p>This implementation is the same as in FileSystemProvider.unmangleName().
     *
     * @param data The URL-encoded string to decode
     * @param encoding The encoding to use
     * @return A decoded string.
     * @throws IllegalArgumentException If the data cannot be decoded.
     */
    public static String urlDecode( final String data, final String encoding ) throws IllegalArgumentException {
        // Presumably, the same caveats apply as in FileSystemProvider. Don't see why it would be horribly kludgy, though.
        if( "UTF-8".equals( encoding ) ) {
            return urlDecodeUTF8( data );
        }

        return urlDecode( data.getBytes( Charset.forName( encoding ) ), encoding );
    }

    /**
     *  Replaces the relevant entities inside the String. All &amp; &gt;, &lt;, and &quot; are replaced by their respective names.
     *
     *  @since 1.6.1
     *  @param src The source string.
     *  @return The encoded string.
     */
    public static String replaceEntities( String src ) {
        src = replaceString( src, "&", "&amp;" );
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
     *  @return A string with the replacement done.
     */
    public static String replaceString( final String orig, final String src, final String dest ) {
        if ( orig == null ) {
            return null;
        }
        if ( src == null || dest == null ) {
            throw new NullPointerException();
        }
        if ( src.length() == 0 ) {
            return orig;
        }

        final StringBuilder res = new StringBuilder( orig.length() + 20 ); // Pure guesswork
        int start;
        int end = 0;
        int last = 0;

        while ( ( start = orig.indexOf( src,end ) ) != -1 ) {
            res.append( orig.substring( last, start ) );
            res.append( dest );
            end  = start + src.length();
            last = start + src.length();
        }
        res.append( orig.substring( end ) );

        return res.toString();
    }

    /**
     *  Replaces a part of a string with a new String.
     *
     *  @param start Where in the original string the replacing should start.
     *  @param end Where the replacing should end.
     *  @param orig Original string.  Null is safe.
     *  @param text The new text to insert into the string.
     *  @return The string with the orig replaced with text.
     */
    public static String replaceString( final String orig, final int start, final int end, final String text ) {
        if( orig == null ) {
            return null;
        }

        final StringBuilder buf = new StringBuilder( orig );
        buf.replace( start, end, text );
        return buf.toString();
    }

    /**
     *  Replaces a string with an other string. Case insensitive matching is used
     *
     *  @param orig Original string.  Null is safe.
     *  @param src  The string to find.
     *  @param dest The string to replace <I>src</I> with.
     *  @return A string with all instances of src replaced with dest.
     */
    public static String replaceStringCaseUnsensitive( final String orig, final String src, final String dest ) {
        if( orig == null ) {
            return null;
        }

        final StringBuilder res = new StringBuilder();
        int start;
        int end = 0;
        int last = 0;

        final String origCaseUnsn = orig.toLowerCase();
        final String srcCaseUnsn = src.toLowerCase();
        while( ( start = origCaseUnsn.indexOf( srcCaseUnsn, end ) ) != -1 ) {
            res.append( orig.substring( last, start ) );
            res.append( dest );
            end  = start + src.length();
            last = start + src.length();
        }
        res.append( orig.substring( end ) );

        return res.toString();
    }

    /**
     *  Parses an integer parameter, returning a default value if the value is null or a non-number.
     *
     *  @param value The value to parse
     *  @param defvalue A default value in case the value is not a number
     *  @return The parsed value (or defvalue).
     */
    public static int parseIntParameter( final String value, final int defvalue ) {
        try {
            return Integer.parseInt( value.trim() );
        } catch( final Exception e ) {}

        return defvalue;
    }

    /**
     *  Gets an integer-valued property from a standard Properties list.
     *
     *  Before inspecting the props, we first check if there is a Java System Property with the same name, if it exists we use that value,
     *  if not we check an environment variable with that (almost) same name, almost meaning we replace dots with underscores.
     *
     *  If the value does not exist, or is a non-integer, returns defVal.
     *
     *  @since 2.1.48.
     *  @param props The property set to look through
     *  @param key   The key to look for
     *  @param defVal If the property is not found or is a non-integer, returns this value.
     *  @return The property value as an integer (or defVal).
     */
    public static int getIntegerProperty( final Properties props, final String key, final int defVal ) {
        String val = System.getProperties().getProperty( key, System.getenv( StringUtils.replace( key,".","_" ) ) );
        if( val == null ) {
            val = props.getProperty( key );
        }
        return parseIntParameter( val, defVal );
    }

    /**
     *  Gets a boolean property from a standard Properties list. Returns the default value, in case the key has not been set.
     *  Before inspecting the props, we first check if there is a Java System Property with the same name, if it exists
     *  we use that value, if not we check an environment variable with that (almost) same name, almost meaning we replace
     *  dots with underscores.
     *  <P>
     *  The possible values for the property are "true"/"false", "yes"/"no", or "on"/"off".  Any value not recognized is always defined
     *  as "false".
     *
     *  @param props   A list of properties to search.
     *  @param key     The property key.
     *  @param defval  The default value to return.
     *
     *  @return True, if the property "key" was set to "true", "on", or "yes".
     *
     *  @since 2.0.11
     */
    public static boolean getBooleanProperty( final Properties props, final String key, final boolean defval ) {
        String val = System.getProperties().getProperty( key, System.getenv( StringUtils.replace( key,".","_" ) ) );
        if( val == null ) {
            val = props.getProperty( key );
        }
        if( val == null ) {
            return defval;
        }

        return isPositive( val );
    }

    /**
     *  Fetches a String property from the set of Properties.  This differs from Properties.getProperty() in a
     *  couple of key respects: First, property value is trim()med (so no extra whitespace back and front).
     *
     *  Before inspecting the props, we first check if there is a Java System Property with the same name, if it exists
     *  we use that value, if not we check an environment variable with that (almost) same name, almost meaning we replace
     *  dots with underscores.
     *
     *  @param props The Properties to search through
     *  @param key   The property key
     *  @param defval A default value to return, if the property does not exist.
     *  @return The property value.
     *  @since 2.1.151
     */
    public static String getStringProperty( final Properties props, final String key, final String defval ) {
        String val = System.getProperties().getProperty( key, System.getenv( StringUtils.replace( key,".","_" ) ) );
        if( val == null ) {
            val = props.getProperty( key );
        }
        if( val == null ) {
            return defval;
        }
        return val.trim();
    }

    /**
     *  Throws an exception if a property is not found.
     *
     *  @param props A set of properties to search the key in.
     *  @param key The key to look for.
     *  @return The required property
     *
     *  @throws NoSuchElementException If the search key is not in the property set.
     *  @since 2.0.26 (on TextUtils, moved To WikiEngine on 2.11.0-M1 and back to TextUtils on 2.11.0-M6)
     */
    public static String getRequiredProperty( final Properties props, final String key ) throws NoSuchElementException {
        final String value = getStringProperty( props, key, null );
        if( value == null ) {
            throw new NoSuchElementException( "Required property not found: " + key );
        }
        return value;
    }

    /**
     *  Fetches a file path property from the set of Properties.
     *
     *  Before inspecting the props, we first check if there is a Java System Property with the same name, if it exists we use that value,
     *  if not we check an environment variable with that (almost) same name, almost meaning we replace dots with underscores.
     *
     *  If the implementation fails to create the canonical path it just returns the original value of the property which is a bit doggy.
     *
     *  @param props The Properties to search through
     *  @param key   The property key
     *  @param defval A default value to return, if the property does not exist.
     *  @return the canonical path of the file or directory being referenced
     *  @since 2.10.1
     */
    public static String getCanonicalFilePathProperty( final Properties props, final String key, final String defval ) {
        String val = System.getProperties().getProperty( key, System.getenv( StringUtils.replace( key,".","_" ) ) );
        if( val == null ) {
            val = props.getProperty( key );
        }

        if( val == null ) {
            val = defval;
        }

        String result;
        try {
            result = new File( new File( val.trim() ).getCanonicalPath() ).getAbsolutePath();
        } catch( final IOException e ) {
            result = val.trim();
        }
        return result;
    }

    /**
     *  Returns true, if the string "val" denotes a positive string.  Allowed values are "yes", "on", and "true".
     *  Comparison is case-insignificant. Null values are safe.
     *
     *  @param val Value to check.
     *  @return True, if val is "true", "on", or "yes"; otherwise false.
     *
     *  @since 2.0.26
     */
    public static boolean isPositive( String val ) {
        if( val == null ) {
        	return false;
        }
        val = val.trim();
        return val.equalsIgnoreCase( "true" )
               || val.equalsIgnoreCase( "on" )
               || val.equalsIgnoreCase( "yes" );
    }

    /**
     *  Makes sure that the POSTed data is conforms to certain rules.  These rules are:
     *  <UL>
     *  <LI>The data always ends with a newline (some browsers, such as NS4.x series, does not send a newline at
     *      the end, which makes the diffs a bit strange sometimes.
     *  <LI>The CR/LF/CRLF mess is normalized to plain CRLF.
     *  </UL>
     *
     *  The reason why we're using CRLF is that most browser already return CRLF since that is the closest thing to a HTTP standard.
     *
     *  @param postData The data to normalize
     *  @return Normalized data
     */
    public static String normalizePostData( final String postData ) {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < postData.length(); i++ ) {
            switch( postData.charAt(i) ) {
              case 0x0a: // LF, UNIX
                sb.append( "\r\n" );
                break;

              case 0x0d: // CR, either Mac or MSDOS
                sb.append( "\r\n" );
                // If it's MSDOS, skip the LF so that we don't add it again.
                if( i < postData.length() - 1 && postData.charAt( i + 1 ) == 0x0a ) {
                    i++;
                }
                break;

              default:
                sb.append( postData.charAt( i ) );
                break;
            }
        }

        if( sb.length() < 2 || !sb.substring( sb.length()-2 ).equals( "\r\n" ) ) {
            sb.append( "\r\n" );
        }

        return sb.toString();
    }

    private static final int EOI   = 0;
    private static final int LOWER = 1;
    private static final int UPPER = 2;
    private static final int DIGIT = 3;
    private static final int OTHER = 4;
    private static final Random RANDOM = new SecureRandom();

    private static int getCharKind( final int c ) {
        if( c == -1 ) {
            return EOI;
        }

        final char ch = ( char )c;

        if( Character.isLowerCase( ch ) ) {
            return LOWER;
        } else if( Character.isUpperCase( ch ) ) {
            return UPPER;
        } else if( Character.isDigit( ch ) ) {
            return DIGIT;
        } else {
            return OTHER;
        }
    }

    /**
     *  Adds spaces in suitable locations of the input string.  This is used to transform a WikiName into a more readable format.
     *
     *  @param s String to be beautified.
     *  @return A beautified string.
     */
    public static String beautifyString( final String s ) {
        return beautifyString( s, " " );
    }

    /**
     *  Adds spaces in suitable locations of the input string.  This is used to transform a WikiName into a more readable format.
     *
     *  @param s String to be beautified.
     *  @param space Use this string for the space character.
     *  @return A beautified string.
     *  @since 2.1.127
     */
    public static String beautifyString( final String s, final String space ) {
        if( s == null || s.length() == 0 ) {
        	return "";
        }

        final StringBuilder result = new StringBuilder();

        int cur = s.charAt( 0 );
        int curKind = getCharKind( cur );

        int prevKind = LOWER;
        int nextKind;
        int next;
        int nextPos = 1;

        while( curKind != EOI ) {
            next = ( nextPos < s.length() ) ? s.charAt( nextPos++ ) : -1;
            nextKind = getCharKind( next );

            if( ( prevKind == UPPER ) && ( curKind == UPPER ) && ( nextKind == LOWER ) ) {
                result.append( space );
                result.append( ( char ) cur );
            } else {
                result.append((char) cur );
                if( ( ( curKind == UPPER ) && (nextKind == DIGIT) )
                    || ( ( curKind == LOWER ) && ( ( nextKind == DIGIT ) || ( nextKind == UPPER ) ) )
                    || ( ( curKind == DIGIT ) && ( ( nextKind == UPPER ) || ( nextKind == LOWER ) ) ) ) {
                    result.append( space );
                }
            }
            prevKind = curKind;
            cur      = next;
            curKind  = nextKind;
        }

        return result.toString();
    }

    /**
     *  Cleans a Wiki name based on a list of characters.  Also, any multiple whitespace is collapsed into a single space, and any
     *  leading or trailing space is removed.
     *
     *  @param text text to be cleared. Null is safe, and causes this to return null.
     *  @param allowedChars Characters which are allowed in the string.
     *  @return A cleaned text.
     *
     *  @since 2.6
     */
    public static String cleanString( String text, final String allowedChars ) {
        if( text == null ) {
            return null;
        }

        text = text.trim();
        final StringBuilder clean = new StringBuilder( text.length() );

        //  Remove non-alphanumeric characters that should not be put inside WikiNames.  Note that all valid Unicode letters are
        //  considered okay for WikiNames. It is the problem of the WikiPageProvider to take care of actually storing that information.
        //
        //  Also capitalize things, if necessary.

        boolean isWord = true;  // If true, we've just crossed a word boundary
        boolean wasSpace = false;
        for( int i = 0; i < text.length(); i++ ) {
            char ch = text.charAt( i );

            //  Cleans away repetitive whitespace and only uses the first one.
            if( Character.isWhitespace( ch ) ) {
                if( wasSpace ) {
                    continue;
                }

                wasSpace = true;
            } else {
                wasSpace = false;
            }

            //  Check if it is allowed to use this char, and capitalize, if necessary.
            if( Character.isLetterOrDigit( ch ) || allowedChars.indexOf( ch ) != -1 ) {
                // Is a letter
                if( isWord ) {
                    ch = Character.toUpperCase( ch );
                }
                clean.append( ch );
                isWord = false;
            } else {
                isWord = true;
            }
        }

        return clean.toString();
    }

    /**
     *  Creates a Properties object based on an array which contains alternatively a key and a value.  It is useful
     *  for generating default mappings. For example:
     *  <pre>
     *     String[] properties = { "jspwiki.property1", "value1", "jspwiki.property2", "value2 };
     *     Properties props = TextUtil.createPropertes( values );
     *     System.out.println( props.getProperty("jspwiki.property1") );
     *  </pre>
     *  would output "value1".
     *
     *  @param values Alternating key and value pairs.
     *  @return Property object
     *  @see java.util.Properties
     *  @throws IllegalArgumentException if the property array is missing a value for a key.
     *  @since 2.2.
     */
    public static Properties createProperties( final String[] values ) throws IllegalArgumentException {
        if( values.length % 2 != 0 ) {
        	throw new IllegalArgumentException( "One value is missing.");
        }

        final Properties props = new Properties();
        for( int i = 0; i < values.length; i += 2 ) {
            props.setProperty( values[i], values[i + 1] );
        }

        return props;
    }

    /**
     *  Counts the number of sections (separated with "----") from the page.
     *
     *  @param pagedata The WikiText to parse.
     *  @return int Number of counted sections.
     *  @since 2.1.86.
     */
    public static int countSections( final String pagedata ) {
        int tags  = 0;
        int start = 0;

        while( ( start = pagedata.indexOf( "----", start ) ) != -1 ) {
            tags++;
            start += 4; // Skip this "----"
        }

        // The first section does not get the "----"
        return pagedata.length() > 0 ? tags + 1 : 0;
    }

    /**
     *  Gets the given section (separated with "----") from the page text. Note that the first section is always #1.  If a page has no
     *  section markers, then there is only a single section, #1.
     *
     *  @param pagedata WikiText to parse.
     *  @param section  Which section to get.
     *  @return String  The section.
     *  @throws IllegalArgumentException If the page does not contain this many sections.
     *  @since 2.1.86.
     */
    public static String getSection( final String pagedata, final int section ) throws IllegalArgumentException {
        int tags  = 0;
        int start = 0;
        int previous = 0;

        while( ( start = pagedata.indexOf( "----", start ) ) != -1 ) {
            if( ++tags == section ) {
                return pagedata.substring( previous, start );
            }

            start += 4; // Skip this "----"
            // allow additional dashes, treat it as if it was a correct 4-dash
            while (start < pagedata.length() && pagedata.charAt( start ) == '-') {
                start++;
            }

            previous = start;
        }

        if( ++tags == section ) {
            return pagedata.substring( previous );
        }

        throw new IllegalArgumentException( "There is no section no. " + section + " on the page." );
    }

    /**
     *  A simple routine which just repeates the arguments.  This is useful for creating something like a line or something.
     *
     *  @param what String to repeat
     *  @param times How many times to repeat the string.
     *  @return Guess what?
     *  @since 2.1.98.
     */
    public static String repeatString( final String what, final int times ) {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < times; i++ ) {
            sb.append( what );
        }

        return sb.toString();
    }

    /**
     *  Converts a string from the Unicode representation into something that can be embedded in a java
     *  properties file.  All references outside the ASCII range are replaced with \\uXXXX.
     *
     *  @param s The string to convert
     *  @return the ASCII string
     */
    public static String native2Ascii( final String s ) {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < s.length(); i++ ) {
            final char aChar = s.charAt(i);
            if( ( aChar < 0x0020 ) || ( aChar > 0x007e ) ) {
                sb.append( '\\');
                sb.append( 'u');
                sb.append( toHex( ( aChar >> 12 ) & 0xF ) );
                sb.append( toHex( ( aChar >>  8 ) & 0xF ) );
                sb.append( toHex( ( aChar >>  4 ) & 0xF ) );
                sb.append( toHex( aChar        & 0xF ) );
            } else {
                sb.append( aChar );
            }
        }
        return sb.toString();
    }

    private static char toHex( final int nibble ) {
        final char[] hexDigit = {
            '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
        };
        return hexDigit[ nibble & 0xF ];
    }

    /**
     *  Generates a hexadecimal string from an array of bytes.  For example, if the array contains
     *  { 0x01, 0x02, 0x3E }, the resulting string will be "01023E".
     *
     * @param bytes A Byte array
     * @return A String representation
     * @since 2.3.87
     */
    public static String toHexString( final byte[] bytes ) {
        final StringBuilder sb = new StringBuilder( bytes.length * 2 );
        for( final byte aByte : bytes ) {
            sb.append( toHex( aByte >> 4 ) );
            sb.append( toHex( aByte ) );
        }

        return sb.toString();
    }

    /**
     *  Returns true, if the argument contains a number, otherwise false. In a quick test this is roughly the same
     *  speed as Integer.parseInt() if the argument is a number, and roughly ten times the speed, if the argument
     *  is NOT a number.
     *
     *  @since 2.4
     *  @param s String to check
     *  @return True, if s represents a number.  False otherwise.
     */
    public static boolean isNumber( String s ) {
        if( s == null ) {
        	return false;
        }

        if( s.length() > 1 && s.charAt(0) == '-' ) {
        	s = s.substring( 1 );
        }

        for( int i = 0; i < s.length(); i++ ) {
            if( !Character.isDigit( s.charAt( i ) ) ) {
            	return false;
            }
        }

        return true;
    }

    /**
     * Generate a random String suitable for use as a temporary password.
     *
     * @return String suitable for use as a temporary password
     * @since 2.4
     */
    public static String generateRandomPassword() {
        String pw = "";
        for( int i = 0; i < PASSWORD_LENGTH; i++ ) {
            final int index = ( int )( RANDOM.nextDouble() * PWD_BASE.length() );
            pw += PWD_BASE.substring( index, index + 1 );
        }
        return pw;
    }

}
