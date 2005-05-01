package com.ecyrd.jspwiki.htmltowiki;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("TestHtmlStringToWikiTranslator tests");

        suite.addTest( TestHtmlStringToWikiTranslator.suite() );

        return suite;
    }
}
