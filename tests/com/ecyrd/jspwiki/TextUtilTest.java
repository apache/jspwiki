
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

    public static Test suite()
    {
        return new TestSuite( TextUtilTest.class );
    }
}
