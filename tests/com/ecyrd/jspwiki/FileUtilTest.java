
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

public class FileUtilTest extends TestCase
{
    public FileUtilTest( String s )
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

    public void testReadContentsLatin1()
        throws Exception
    {
        String src = "abcåäö";

        String res = FileUtil.readContents( new ByteArrayInputStream( src.getBytes("ISO-8859-1") ),
                                            "ISO-8859-1" );

        assertEquals( src, res );
    }

    /**
     *  Check that fallbacks to ISO-Latin1 still work.
     */
    public void testReadContentsLatin1_2()
        throws Exception
    {
        String src = "abcåäö";

        String res = FileUtil.readContents( new ByteArrayInputStream( src.getBytes("ISO-8859-1") ),
                                            "UTF-8" );

        assertEquals( src, res );
    }

    public static Test suite()
    {
        return new TestSuite( FileUtilTest.class );
    }
}
