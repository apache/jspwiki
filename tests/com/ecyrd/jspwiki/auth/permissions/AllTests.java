package com.ecyrd.jspwiki.auth.permissions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2006-02-21 08:46:45 $
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
        suite.addTestSuite( AllPermissionTest.class );
        suite.addTestSuite( PagePermissionTest.class );
        suite.addTestSuite( WikiPermissionTest.class );
        return suite;
    }
}