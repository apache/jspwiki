
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.*;

public class BasicAttachmentProviderTest extends TestCase
{
    Properties props = new Properties();

    BasicAttachmentProvider m_provider;

    public BasicAttachmentProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        m_provider = new BasicAttachmentProvider();
        m_provider.initialize( props );
    }

    public void tearDown()
    {
    }

    public void testExtension()
    {
        String s = "test.png";

        assertEquals( m_provider.getFileExtension(s), "png" );
    }

    public void testExtension2()
    {
        String s = ".foo";

        assertEquals( "foo", m_provider.getFileExtension(s) );
    }

    public void testExtension3()
    {
        String s = "test.png.3";

        assertEquals( "3", m_provider.getFileExtension(s) );
    }

    public void testExtension4()
    {
        String s = "testpng";

        assertEquals( "", m_provider.getFileExtension(s) );
    }


    public void testExtension5()
    {
        String s = "test.";

        assertEquals( "", m_provider.getFileExtension(s) );
    }


    public static Test suite()
    {
        return new TestSuite( BasicAttachmentProviderTest.class );
    }


}
