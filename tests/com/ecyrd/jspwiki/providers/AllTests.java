
package com.ecyrd.jspwiki.providers;

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

        suite.addTest( FileSystemProviderTest.suite() );
        suite.addTest( RCSFileProviderTest.suite() );
        suite.addTest( VersioningFileProviderTest.suite() );
        suite.addTest( BasicAttachmentProviderTest.suite() );

        return suite;
    }
}
