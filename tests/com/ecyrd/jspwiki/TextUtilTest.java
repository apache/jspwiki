
package com.ecyrd.jspwiki;

import junit.framework.*;
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

    public void testEncodeName_1()
    {
        String name = "Hello/World";

        assertEquals( "Hello/World",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testEncodeName_2()
    {
        String name = "Hello~World";

        assertEquals( "Hello%7EWorld",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testEncodeName_3()
    {
        String name = "Hello/World ~";

        assertEquals( "Hello/World+%7E",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testDecodeName_1()
         throws Exception
    {
        String name = "Hello/World+%7E+%2F";

        assertEquals( "Hello/World ~ /",
                      TextUtil.urlDecode(name,"ISO-8859-1") );
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

    public void testEncodeNameUTF8_4()
    {
        String name = "Hello World";

        assertEquals( "Hello+World",
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

    public void testGetBooleanProperty()
    {
        Properties props = new Properties();

        props.setProperty("foobar.0", "YES");
        props.setProperty("foobar.1", "true");
        props.setProperty("foobar.2", "false");
        props.setProperty("foobar.3", "no");
        props.setProperty("foobar.4", "on");
        props.setProperty("foobar.5", "OFF");
        props.setProperty("foobar.6", "gewkjoigew");

        assertTrue( "foobar.0", 
                    TextUtil.getBooleanProperty( props, "foobar.0", false ) );
        assertTrue( "foobar.1", 
                    TextUtil.getBooleanProperty( props, "foobar.1", false ) );

        assertFalse( "foobar.2", 
                     TextUtil.getBooleanProperty( props, "foobar.2", true ) );
        assertFalse( "foobar.3", 
                    TextUtil.getBooleanProperty( props, "foobar.3", true ) );
        assertTrue( "foobar.4", 
                    TextUtil.getBooleanProperty( props, "foobar.4", false ) );

        assertFalse( "foobar.5", 
                     TextUtil.getBooleanProperty( props, "foobar.5", true ) );

        assertFalse( "foobar.6", 
                     TextUtil.getBooleanProperty( props, "foobar.6", true ) );


    }

    public void testGetSection1()
        throws Exception
    {
        String src = "Single page.";

        assertEquals( "section 1", src, TextUtil.getSection(src,1) );

        try
        {
            TextUtil.getSection( src, 5 );
            fail("Did not get exception for 2");
        }
        catch( IllegalArgumentException e ) {}

        try
        {
            TextUtil.getSection( src, -1 );
            fail("Did not get exception for -1");
        }
        catch( IllegalArgumentException e ) {}
    }

    public void testGetSection2()
        throws Exception
    {
        String src = "First section\n----\nSecond section\n\n----\n\nThird section";

        assertEquals( "section 1", "First section\n", TextUtil.getSection(src,1) );
        assertEquals( "section 2", "\nSecond section\n\n", TextUtil.getSection(src,2) );
        assertEquals( "section 3", "\n\nThird section", TextUtil.getSection(src,3) );

        try
        {
            TextUtil.getSection( src, 4 );
            fail("Did not get exception for section 4");
        }
        catch( IllegalArgumentException e ) {}
    }

    public void testGetSection3()
        throws Exception
    {
        String src = "----\nSecond section\n----";

        
        assertEquals( "section 1", "", TextUtil.getSection(src,1) );
        assertEquals( "section 2", "\nSecond section\n", TextUtil.getSection(src,2) );
        assertEquals( "section 3", "", TextUtil.getSection(src,3) );

        try
        {
            TextUtil.getSection( src, 4 );
            fail("Did not get exception for section 4");
        }
        catch( IllegalArgumentException e ) {}
    }

    public static Test suite()
    {
        return new TestSuite( TextUtilTest.class );
    }
}
