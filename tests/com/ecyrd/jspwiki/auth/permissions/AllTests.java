package com.ecyrd.jspwiki.auth.permissions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.5 $ $Date: 2006-07-23 20:14:39 $
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
        suite.addTestSuite( AllPermissionCollectionTest.class );
        suite.addTestSuite( GroupPermissionTest.class );
        suite.addTestSuite( PagePermissionTest.class );
        suite.addTestSuite( WikiPermissionTest.class );
        return suite;
    }
}