/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.parser;

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
        TestSuite suite = new TestSuite("Parser tests");

        suite.addTest( JSPWikiMarkupParserTest.suite() );

        return suite;
    }
}
