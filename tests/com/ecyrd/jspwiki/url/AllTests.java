/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.url;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("URL constructor tests");

        suite.addTest( ShortViewURLConstructorTest.suite() );
        suite.addTest( ShortURLConstructorTest.suite() );
        suite.addTest( DefaultURLConstructorTest.suite() );

        return suite;
    }

}
