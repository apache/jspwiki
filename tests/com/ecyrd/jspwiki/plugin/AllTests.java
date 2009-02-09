
package com.ecyrd.jspwiki.plugin;

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
        TestSuite suite = new TestSuite("JSPWiki plugins");

        suite.addTest( CounterPluginTest.suite() );
        suite.addTest( GroupsTest.suite() );
        suite.addTest( InsertPageTest.suite() );
        suite.addTest( PluginManagerTest.suite() );
        suite.addTest( ReferringPagesPluginTest.suite() );
        suite.addTest( TableOfContentsTest.suite() );
        suite.addTest( UndefinedPagesPluginTest.suite() );
        suite.addTest( RecentChangesPluginTest.suite() );

        return suite;
    }
}
