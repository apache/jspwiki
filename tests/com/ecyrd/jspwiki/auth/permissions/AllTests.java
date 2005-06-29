package com.ecyrd.jspwiki.auth.permissions;

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
        TestSuite suite = new TestSuite( "Permissions tests" );
        suite.addTestSuite( PagePermissionTest.class );
        suite.addTestSuite( WikiPermissionTest.class );
        return suite;
    }
}