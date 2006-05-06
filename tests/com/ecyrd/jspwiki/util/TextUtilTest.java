package com.ecyrd.jspwiki.util;

import com.ecyrd.jspwiki.TextUtil;

import junit.framework.*;

public class TextUtilTest extends TestCase
{
    public TextUtilTest( String s )
    {
        super( s );
    }
    
    public void testGenerateRandomPassword()
    {
        for (int i=0; i<1000; i++) {
            assertEquals("pw", TextUtil.PASSWORD_LENGTH, TextUtil.generateRandomPassword().length());
        }
    }

    public static Test suite()
    {
        return new TestSuite( TextUtilTest.class );
    }
}


