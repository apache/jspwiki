
package com.ecyrd.jspwiki.auth;

import com.ecyrd.jspwiki.TextUtil;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("AAA package tests");

        String runTests = System.getProperty( "jspwiki.tests.auth", "false" );
        
        if( TextUtil.isPositive(runTests) )
        {
            suite.addTest( AuthenticationManagerTest.suite() );
            suite.addTest( AuthorizationManagerTest.suite() );
            suite.addTest( GroupManagerTest.suite() );
            suite.addTest( com.ecyrd.jspwiki.auth.acl.AllTests.suite() );
            suite.addTest( com.ecyrd.jspwiki.auth.authorize.AllTests.suite() );
            suite.addTest( com.ecyrd.jspwiki.auth.login.AllTests.suite() );
            suite.addTest( com.ecyrd.jspwiki.auth.permissions.AllTests.suite() );
            suite.addTest( com.ecyrd.jspwiki.auth.user.AllTests.suite() );
        }
        
        return suite;
    }
}
