package com.ecyrd.jspwiki.auth.authorize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-09-02 23:35:08 $
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
        suite.addTestSuite( DefaultGroupManagerTest.class );
        suite.addTestSuite( DefaultGroupTest.class );
        suite.addTestSuite( WebContainerAuthorizerTest.class );
        return suite;
    }
}