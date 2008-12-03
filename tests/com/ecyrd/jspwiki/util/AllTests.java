
package com.ecyrd.jspwiki.util;


import com.ecyrd.jspwiki.ui.stripes.JspParserTest;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Utility suite tests");

        suite.addTest( ClassUtilTest.suite() );
        suite.addTest( CommentedPropertiesTest.suite() );
        suite.addTest( CryptoUtilTest.suite() );
        suite.addTest( JspParserTest.suite() );
        suite.addTest( MailUtilTest.suite() );
        suite.addTest( PriorityListTest.suite() );
        suite.addTest( SerializerTest.suite() );
        suite.addTest( TextUtilTest.suite() );
        suite.addTest( TimedCounterListTest.suite() );
        suite.addTest( UtilJ2eeCompatTest.suite() );
        
        return suite;
    }
}
