
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
        TestSuite suite = new TestSuite();

        suite.addTest( PluginManagerTest.suite() );
        suite.addTest( ReferringPagesPluginTest.suite() );
        suite.addTest( CounterPluginTest.suite() );
        suite.addTest( UndefinedPagesPluginTest.suite() );

        return suite;
    }
}
