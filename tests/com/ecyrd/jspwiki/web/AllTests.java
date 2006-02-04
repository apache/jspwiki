
package com.ecyrd.jspwiki.web;

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
        TestSuite suite = new TestSuite("JSPWiki web unit tests");
        suite.addTestSuite( com.ecyrd.jspwiki.web.CustomTest.class );
        suite.addTestSuite( com.ecyrd.jspwiki.web.CustomAbsoluteTest.class );
        suite.addTestSuite( com.ecyrd.jspwiki.web.CustomJDBCTest.class );
        suite.addTestSuite( com.ecyrd.jspwiki.web.ContainerTest.class );
        suite.addTestSuite( com.ecyrd.jspwiki.web.ContainerJDBCTest.class );
        return suite;
    }
}
