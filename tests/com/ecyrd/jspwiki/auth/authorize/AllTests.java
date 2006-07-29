package com.ecyrd.jspwiki.auth.authorize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.4 $ $Date: 2006-07-29 19:23:14 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Role, group and group manager tests" );
        suite.addTestSuite( GroupTest.class );
        suite.addTestSuite( WebContainerAuthorizerTest.class );
        suite.addTestSuite( XMLGroupDatabaseTest.class );
        return suite;
    }
}