package com.ecyrd.jspwiki.auth.acl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
        suite.addTest( DefaultAclManagerTest.suite() );

        return suite;
    }
}
