
package com.ecyrd.jspwiki.util;

import junit.framework.*;
import java.util.*;

import org.apache.log4j.*;

public class ClassUtilTest extends TestCase
{
    public ClassUtilTest( String s )
    {
        super( s );
    }

    /**
     *  Tries to find an existing class.
     */
    public void testFindClass()
        throws Exception
    {
        Class foo = ClassUtil.findClass( "com.ecyrd.jspwiki", "WikiPage" );

        assertEquals( foo.getName(), "com.ecyrd.jspwiki.WikiPage" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    public void testFindClassNoClass()
        throws Exception
    {
        try
        {
            Class foo = ClassUtil.findClass( "com.ecyrd.jspwiki", "MubbleBubble" );
            fail("Found class");
        }
        catch( ClassNotFoundException e )
        {
            // Expected
        }
    }

    public static Test suite()
    {
        return new TestSuite( ClassUtilTest.class );
    }
}


