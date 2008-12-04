package com.ecyrd.jspwiki.ui.stripes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Stripes UI tests");
        suite.addTest( HandlerInfoTest.suite() );
        return suite;
    }

}
