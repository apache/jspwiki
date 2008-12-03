package com.ecyrd.jspwiki.ui.stripes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("JSP migration tests");
        suite.addTest( JspParserTest.suite() );
        suite.addTest( JspDocumentTest.suite() );
        suite.addTest( JSPWikiJspTransformerTest.suite() );
        suite.addTest( StripesJspTransformerTest.suite() );
        suite.addTest( TagTest.suite() );
        return suite;
    }

}
