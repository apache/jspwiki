/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("UI tests");
        suite.addTest( InputValidatorTest.suite() );
        suite.addTest( CommandResolverTest.suite() );
        suite.addTest( GroupCommandTest.suite() );
        suite.addTest( PageCommandTest.suite() );
        suite.addTest( RedirectCommandTest.suite() );
        suite.addTest( WikiCommandTest.suite() );
        return suite;
    }

}
