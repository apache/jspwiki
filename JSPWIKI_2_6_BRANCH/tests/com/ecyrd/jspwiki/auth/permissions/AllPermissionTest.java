package com.ecyrd.jspwiki.auth.permissions;

import junit.framework.TestCase;

/**
 * @author Andrew Jaquith
 */
public class AllPermissionTest extends TestCase
{

    /*
     * Class under test for boolean equals(Object)
     */
    public void testEqualsObject()
    {
        AllPermission p1 = new AllPermission( "*" );
        AllPermission p2 = new AllPermission( "*" );
        AllPermission p3 = new AllPermission( "myWiki" );
        assertTrue( p1.equals( p2 ) );
        assertTrue( p2.equals( p1 ) );
        assertFalse( p1.equals( p3 ) );
        assertFalse( p3.equals( p1 ) );
    }

    public void testImpliesAllPermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        AllPermission p2 = new AllPermission( "*" );
        assertTrue( p1.equals( p2 ) );
        assertTrue( p2.equals( p1 ) );

        p2 = new AllPermission( "myWiki" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
    }

    public void testImpliesPagePermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        PagePermission p2 = new PagePermission( "*:TestPage", "delete" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );

        p2 = new PagePermission( "myWiki:TestPage", "delete" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
        
        p2 = new PagePermission( "*:GroupTest", "delete" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
        
        p2 = new PagePermission( "myWiki:GroupTest", "delete" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
    }

    public void testImpliesWikiPermission()
    {
        AllPermission p1 = new AllPermission( "*" );
        WikiPermission p2 = new WikiPermission( "*", "createPages" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );

        p2 = new WikiPermission( "myWiki", "createPages" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
    }

    /*
     * Class under test for String toString()
     */
    public void testToString()
    {
        AllPermission p = new AllPermission( "myWiki" );
        String result = "(\"com.ecyrd.jspwiki.auth.permissions.AllPermission\",\"myWiki\")";
        assertEquals( result, p.toString() );

        p = new AllPermission( "*" );
        result = "(\"com.ecyrd.jspwiki.auth.permissions.AllPermission\",\"*\")";
        assertEquals( result, p.toString() );
    }

}
