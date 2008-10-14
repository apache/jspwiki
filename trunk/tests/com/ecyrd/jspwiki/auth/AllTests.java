
package com.ecyrd.jspwiki.auth;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("AAA package tests");

        suite.addTest( AuthenticationManagerTest.suite() );
        suite.addTest( AuthorizationManagerTest.suite() );
        suite.addTest( GroupManagerTest.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.acl.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.authorize.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.login.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.permissions.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.user.AllTests.suite() );
        suite.addTestSuite( com.ecyrd.jspwiki.auth.UserManagerTest.class );
        
        return suite;
    }
}
