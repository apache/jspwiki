
package com.ecyrd.jspwiki.filters;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("PageFilter tests");

        suite.addTest( FilterManagerTest.suite() );

        return suite;
    }
}
