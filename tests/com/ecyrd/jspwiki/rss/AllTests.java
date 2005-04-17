/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.rss;

import com.ecyrd.jspwiki.util.ClassUtilTest;
import com.ecyrd.jspwiki.util.PriorityListTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("RSS tests");

        suite.addTest( RSSGeneratorTest.suite() );

        return suite;
    }
}
