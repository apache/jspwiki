
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
        TestSuite suite = new TestSuite();

        suite.addTest( AccessRuleSetTest.suite() );
        suite.addTest( AuthenticatorTest.suite() );
        suite.addTest( AuthorizerTest.suite() );
        suite.addTest( DummyAuthenticatorTest.suite() );
        suite.addTest( DummyAuthorizerTest.suite() );
        suite.addTest( FileAuthenticatorTest.suite() );
        suite.addTest( FileAuthorizerTest.suite() );
        suite.addTest( RequirePermissionRuleTest.suite() );
        suite.addTest( RoleAllowRuleTest.suite() );
        suite.addTest( RoleDenyRuleTest.suite() );

        return suite;
    }
}
