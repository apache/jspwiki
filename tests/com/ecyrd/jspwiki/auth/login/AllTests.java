package com.ecyrd.jspwiki.auth.login;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Login module tests" );
        suite.addTestSuite( AnonymousLoginModuleTest.class );
        suite.addTestSuite( CookieAssertionLoginModuleTest.class );
        suite.addTestSuite( UserDatabaseLoginModuleTest.class );
        suite.addTestSuite( WebContainerLoginModuleTest.class );
        return suite;
    }
}