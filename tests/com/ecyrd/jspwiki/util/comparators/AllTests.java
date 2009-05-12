
package com.ecyrd.jspwiki.util.comparators;


import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Comparator utility suite tests" );

        suite.addTest( HumanComparatorTest.suite() );
        
        return suite;
    }
}
