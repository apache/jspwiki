/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.render;

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
        TestSuite suite = new TestSuite("rendering tests");

        suite.addTest( RenderingManagerTest.suite() );

        return suite;
    }
}
