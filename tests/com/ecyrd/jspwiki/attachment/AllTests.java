
package com.ecyrd.jspwiki.attachment;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Attachment package");

        suite.addTest( AttachmentManagerTest.suite() );

        return suite;
    }
}
