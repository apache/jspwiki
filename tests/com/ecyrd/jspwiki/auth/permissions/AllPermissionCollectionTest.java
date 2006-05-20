package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.util.Enumeration;

import junit.framework.TestCase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2006-05-20 23:53:11 $
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
    
    public void testAddPagePermission()
    {
        PagePermission p1 = new PagePermission( "JSPWiki:Main", "edit" );
        PagePermission p2 = new PagePermission( "JSPWiki:GroupAdmin", "edit" );
        PagePermission p3 = new PagePermission( "JSPWiki:Foobar", "delete" );
        PagePermission p4 = new PagePermission( "JSPWiki:Main", "edit" );

        c_all.add( p1 );
        assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
        assertEquals( 3, count( c_all ) );
    }
    
    public void testAddWikiPermission()
    {
        WikiPermission p1 = new WikiPermission( "JSPWiki", "login" );
        WikiPermission p2 = new WikiPermission( "JSPWiki", "createGroups" );
        WikiPermission p3 = new WikiPermission( "JSPWiki", "editPreferences" );
        WikiPermission p4 = new WikiPermission( "JSPWiki", "login" );

        c_all.add( p1 );
        assertEquals( 1, count( c_all ) );

        c_all.add( p2 );
        assertEquals( 2, count( c_all ) );

        c_all.add( p3 );
        assertEquals( 3, count( c_all ) );

        // The last one is a duplicate and shouldn't be counted...
        c_all.add( p4 );
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
    
    public void testImpliesMixedPermissions()
    {
        Permission p1 = new AllPermission( "JSPWiki" );
        Permission p2 = new WikiPermission( "myWiki", "editPreferences" );
        Permission p3 = new PagePermission( "bigTimeWiki:FooBar", "modify" );
        Permission p4 = new AllPermission( "*" );

        c_all.add( p1 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );

        c_all.add( p2 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );
        
        c_all.add( p3 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        assertFalse( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        assertFalse( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );
        
        c_all.add( p4 );
        assertTrue( c_all.implies( new WikiPermission( "JSPWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "JSPWiki:FooBar", "edit" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "myWiki", "editPreferences" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "myWiki", "login" ) ) );
        assertTrue( c_all.implies( new WikiPermission( "bigTimeWiki", "login" ) ) );
        assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "edit" ) ) );
        assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:FooBar", "delete" ) ) );
        assertTrue( c_all.implies( new PagePermission( "bigTimeWiki:Bar", "delete" ) ) );
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
