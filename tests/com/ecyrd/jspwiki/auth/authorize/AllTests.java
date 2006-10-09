package com.ecyrd.jspwiki.auth.authorize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.5 $ $Date: 2006-10-09 02:37:37 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Authorizer, group and group database tests" );
        suite.addTestSuite( GroupTest.class );
        suite.addTestSuite( JDBCGroupDatabaseTest.class );
        suite.addTestSuite( WebContainerAuthorizerTest.class );
        suite.addTestSuite( XMLGroupDatabaseTest.class );
        return suite;
    }
}