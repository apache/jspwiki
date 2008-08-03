
package com.ecyrd.jspwiki.content;

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
        TestSuite suite = new TestSuite("JSPWiki Content Unit Tests");

        suite.addTest( PageRenamerTest.suite() );
        
        return suite;
    }
}
