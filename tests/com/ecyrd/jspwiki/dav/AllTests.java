/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("DAV tests");

        suite.addTest( DavPathTest.suite() );

        return suite;
    }
}
