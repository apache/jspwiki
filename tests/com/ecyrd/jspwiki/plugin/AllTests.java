
package com.ecyrd.jspwiki.plugin;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("JSPWiki plugins");

        suite.addTest( PluginManagerTest.suite() );
        suite.addTest( ReferringPagesPluginTest.suite() );
        suite.addTest( CounterPluginTest.suite() );
        suite.addTest( UndefinedPagesPluginTest.suite() );
        suite.addTest( TableOfContentsTest.suite() );
        suite.addTest( InsertPageTest.suite() );
        
        return suite;
    }
}
