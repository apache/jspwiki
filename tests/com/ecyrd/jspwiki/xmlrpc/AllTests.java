
package com.ecyrd.jspwiki.xmlrpc;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("XMLRPC tests");

        suite.addTest( RPCHandlerTest.suite() );

        return suite;
    }
}
