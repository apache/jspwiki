
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

public class TextUtilTest extends TestCase
{

    public TextUtilTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testEncodeNameUTF8_1()
    {
        String name = "\u0041\u2262\u0391\u002E";

        assertEquals( "A%E2%89%A2%CE%91.",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testEncodeNameUTF8_2()
    {
        String name = "\uD55C\uAD6D\uC5B4";

        assertEquals( "%ED%95%9C%EA%B5%AD%EC%96%B4",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testEncodeNameUTF8_3()
    {
        String name = "\u65E5\u672C\u8A9E";

        assertEquals( "%E6%97%A5%E6%9C%AC%E8%AA%9E",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testDecodeNameUTF8_1()
    {
        String name = "A%E2%89%A2%CE%91.";

        assertEquals( "\u0041\u2262\u0391\u002E",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testDecodeNameUTF8_2()
    {
        String name = "%ED%95%9C%EA%B5%AD%EC%96%B4";

        assertEquals( "\uD55C\uAD6D\uC5B4",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testDecodeNameUTF8_3()
    {
        String name = "%E6%97%A5%E6%9C%AC%E8%AA%9E";

        assertEquals( "\u65E5\u672C\u8A9E",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testReplaceString1()
    {
        String text = "aabacaa";

        assertEquals( "ddbacdd", TextUtil.replaceString( text, "aa", "dd" ) ); 
    }

    public void testReplaceString4()
    {
        String text = "aabacaafaa";

        assertEquals( "ddbacddfdd", TextUtil.replaceString( text, "aa", "dd" ) ); 
    }

    public void testReplaceString5()
    {
        String text = "aaabacaaafaa";

        assertEquals( "dbacdfaa", TextUtil.replaceString( text, "aaa", "d" ) );     
    }

    public void testReplaceString2()
    {
        String text = "abcde";

        assertEquals( "fbcde", TextUtil.replaceString( text, "a", "f" ) ); 
    }

    public void testReplaceString3()
    {
        String text = "ababab";

        assertEquals( "afafaf", TextUtil.replaceString( text, "b", "f" ) ); 
    }

    // Pure UNIX.
    public void testNormalizePostdata1()
    {
        String text = "ab\ncd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure MSDOS.
    public void testNormalizePostdata2()
    {
        String text = "ab\r\ncd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure Mac
    public void testNormalizePostdata3()
    {
        String text = "ab\rcd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Mixed, ending correct.
    public void testNormalizePostdata4()
    {
        String text = "ab\ncd\r\n\r\n\r";

        assertEquals( "ab\r\ncd\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Multiple newlines
    public void testNormalizePostdata5()
    {
        String text = "ab\ncd\n\n\n\n";

        assertEquals( "ab\r\ncd\r\n\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Empty.
    public void testNormalizePostdata6()
    {
        String text = "";

        assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    // Just a newline.
    public void testNormalizePostdata7()
    {
        String text = "\n";

        assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    public static Test suite()
    {
        return new TestSuite( TextUtilTest.class );
    }
}
