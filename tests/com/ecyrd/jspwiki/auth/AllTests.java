
package com.ecyrd.jspwiki.auth;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("auth package tests");

        suite.addTest( UserProfileTest.suite() );
        suite.addTest( WikiGroupTest.suite() );
        suite.addTest( AuthorizationManagerTest.suite() );

        return suite;
    }
}
