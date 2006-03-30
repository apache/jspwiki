package com.ecyrd.jspwiki.auth.permissions;

import java.util.Enumeration;

import junit.framework.TestCase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-03-30 04:52:03 $
 */
public class AllPermissionCollectionTest extends TestCase
{

    AllPermissionCollection c_all;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        c_all = new AllPermissionCollection();
    }

    public void testAddAllPermission()
    {
        AllPermission all1 = new AllPermission( "*" );
        AllPermission all2 = new AllPermission( "JSPWiki" );
        AllPermission all3 = new AllPermission( "myWiki" );
        AllPermission all4 = new AllPermission( "*" );

        c_all.add( all1 );
        assertEquals( 1, count( c_all ) );

        c_all.add( all2 );
        assertEquals( 2, count( c_all ) );

        c_all.add( all3 );
        assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( all4 );
        assertEquals( 3, count( c_all ) );
    }

    public void testReadOnly()
    {
        AllPermission all1 = new AllPermission( "*" );
        AllPermission all2 = new AllPermission( "JSPWiki" );

        assertFalse( c_all.isReadOnly() );
        c_all.add( all1 );
        assertFalse( c_all.isReadOnly() );

        // Mark as read-only; isReadOnly should return "true", as we'll get an
        // error
        c_all.setReadOnly();
        assertTrue( c_all.isReadOnly() );
        boolean exception = false;
        try
        {
            c_all.add( all2 );
        }
        catch( SecurityException e )
        {
            exception = true;
        }

        if ( !exception )
        {
            // We should never get here
            assertTrue( false );
        }
    }

    public void testImpliesAllPermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "myWiki" );
        AllPermission all3 = new AllPermission( "big*" );
        AllPermission all4 = new AllPermission( "*" );

        c_all.add( all1 );
        assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        assertFalse( c_all.implies( new AllPermission( "myWiki" ) ) );

        c_all.add( all2 );
        assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        assertFalse( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );

        c_all.add( all3 );
        assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        assertFalse( c_all.implies( new AllPermission( "bittyWiki" ) ) );

        c_all.add( all4 );
        assertTrue( c_all.implies( new AllPermission( "JSPWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "myWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "bigTimeWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "bigBigBigWiki" ) ) );
        assertTrue( c_all.implies( new AllPermission( "bittyWiki" ) ) );
    }

    public void testImpliesPagePermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        assertFalse( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );

        c_all.add( all2 );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:Main", "edit" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:GroupAdmin", "edit" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:Foobar", "delete" ) ) );
        assertTrue( c_all.implies( new PagePermission( "myWiki:Foobar", "delete" ) ) );
        assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:*", "view" ) ) );
    }

    public void testImpliesWikiPermission()
    {
        AllPermission all1 = new AllPermission( "JSPWiki" );
        AllPermission all2 = new AllPermission( "*" );

        c_all.add( all1 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );

        c_all.add( all2 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "createGroups" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "editPreferences" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
    }
    
    private int count( AllPermissionCollection collection )
    {
        int i = 0;
        Enumeration perms = collection.elements();
        while( perms.hasMoreElements() )
        {
            perms.nextElement();
            i++;
        }
        return i;
    }

}
