
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.providers.*;

public class PageManagerTest extends TestCase
{
    Properties props = new Properties();

    public PageManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
    }

    public void tearDown()
    {
    }

    public void testPageCacheExists()
        throws Exception
    {
        props.setProperty( "jspwiki.usePageCache", "true" );
        PageManager m = new PageManager( props );

        assertTrue( m.getProvider() instanceof CachingProvider );
    }

    public void testPageCacheNotInUse()
        throws Exception
    {
        props.setProperty( "jspwiki.usePageCache", "false" );
        PageManager m = new PageManager( props );

        assertTrue( !(m.getProvider() instanceof CachingProvider) );
    }

    public static Test suite()
    {
        return new TestSuite( PageManagerTest.class );
    }
}
