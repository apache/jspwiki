package com.ecyrd.jspwiki.ui.migrator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("JSP migration tests");
        suite.addTest( JspDocumentTest.suite() );
        suite.addTest( JspMigratorTest.suite() );
        suite.addTest( JspParserTest.suite() );
        suite.addTest( JSPWikiJspTransformerTest.suite() );
        suite.addTest( StripesJspTransformerTest.suite() );
        suite.addTest( TagTest.suite() );
        return suite;
    }

}
