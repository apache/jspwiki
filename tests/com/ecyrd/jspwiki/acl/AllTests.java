
package com.ecyrd.jspwiki.acl;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("ACL tests");

        suite.addTest( AclEntryImplTest.suite() );
        suite.addTest( AclImplTest.suite() );

        return suite;
    }
}
