package com.ecyrd.jspwiki.auth.permissions;

import java.security.AccessControlException;
import java.security.Permission;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

import junit.framework.TestCase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-07-23 20:14:39 $
 */
public class GroupPermissionTest extends TestCase
{

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( GroupPermissionTest.class );
    }

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    public final void testEqualsObject()
    {
        GroupPermission p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        GroupPermission p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        GroupPermission p3 = new GroupPermission( "mywiki:Test", "delete,view,edit" );
        GroupPermission p4 = new GroupPermission( "mywiki:Test*", "delete,view,edit" );
        assertEquals( p1, p2 );
        assertEquals( p1, p3 );
        assertFalse( p3.equals( p4 ) );
    }

    public final void testCreateMask()
    {
        assertEquals( 1, GroupPermission.createMask( "view" ) );
        assertEquals( 7, GroupPermission.createMask( "view,edit,delete" ) );
        assertEquals( 7, GroupPermission.createMask( "edit,delete,view" ) );
        assertEquals( 2, GroupPermission.createMask( "edit" ) );
        assertEquals( 6, GroupPermission.createMask( "edit,delete" ) );
    }

    /*
     * Class under test for java.lang.String toString()
     */
    public final void testToString()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        assertEquals( "(\"com.ecyrd.jspwiki.auth.permissions.GroupPermission\",\":Test\",\"delete,edit,view\")", p
                .toString() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        assertEquals( "(\"com.ecyrd.jspwiki.auth.permissions.GroupPermission\",\"mywiki:Test\",\"delete,edit,view\")", p
                .toString() );
    }

    /**
     * Tests wiki name support.
     */
    public final void testWikiNames()
    {
        GroupPermission p1;
        GroupPermission p2;

        // Permissions without prepended wiki name should never imply themselves
        // or others
        p1 = new GroupPermission( "Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        assertFalse( p1.implies( p1 ) );
        assertFalse( p1.implies( p2 ) );

        // Permissions with a wildcard wiki should imply other wikis
        p1 = new GroupPermission( "*:Test", "edit" );
        p2 = new GroupPermission( "mywiki:Test", "edit" );
        assertTrue( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );

        // Permissions that start with ":" are just like null
        p1 = new GroupPermission( ":Test", "edit" );
        p2 = new GroupPermission( "Test", "edit" );
        assertFalse( p1.implies( p1 ) );
        assertFalse( p1.implies( p2 ) );
    }

    public final void testImpliesMember()
    {
        GroupPermission p1;
        Permission p2;
        Subject s;
        
        // <groupmember> implies TestGroup if Subject has GroupPermission("TestGroup")
        p1 = new GroupPermission( "*:<groupmember>", "view" );
        p2 = new GroupPermission ("*:TestGroup", "view" );
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "MyWiki", "TestGroup" ) );
        assertTrue( subjectImplies( s, p1, p2 ) );
        
        // <groupmember> doesn't imply it if Subject has no GroupPermission("TestGroup")
        s = new Subject();
        s.getPrincipals().add( new WikiPrincipal( "TestGroup" ) );
        assertFalse( subjectImplies( s, p1, p2 ) );
        
        // <groupmember> doesn't imply it if Subject's GP doesn't match
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "MyWiki", "FooGroup" ) );
        assertFalse( subjectImplies( s, p1, p2 ) );
        
        // <groupmember> doesn't imply it if p2 isn't GroupPermission type
        p2 = new PagePermission ("*:TestGroup", "view" );
        s = new Subject();
        s.getPrincipals().add( new GroupPrincipal( "MyWiki", "TestGroup" ) );
        assertFalse( subjectImplies( s, p1, p2 ) );
        
        // <groupmember> implies TestGroup if not called with Subject combiner
        p1 = new GroupPermission( "*:<groupmember>", "view" );
        p2 = new GroupPermission ("*:TestGroup", "view" );
        assertFalse( p1.impliesMember( p2 ) );
    }
    
    
    /*
     * Class under test for boolean implies(java.security.Permission)
     */
    public final void testImpliesPermission()
    {
        GroupPermission p1;
        GroupPermission p2;
        GroupPermission p3;

        // The same permission should imply itself
        p1 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        p2 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p2.implies( p1 ) );

        // The same permission should imply itself for wildcard wikis
        p1 = new GroupPermission( "Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        assertFalse( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p1.implies( p3 ) );
        assertTrue( p2.implies( p3 ) );
        assertFalse( p3.implies( p1 ) );
        assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply permission for group with same
        // actions
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view,edit,delete" );
        p3 = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertTrue( p2.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        // Actions on single group should imply subset of those actions
        p1 = new GroupPermission( "*:Test", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );
        assertFalse( p3.implies( p2 ) );

        // Actions on collection should imply subset of actions on single group
        p1 = new GroupPermission( "*:*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:Tes*", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*st", "view,edit,delete" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        // Delete action on collection should imply edit/view on
        // single group
        p1 = new GroupPermission( "*:*st", "delete" );
        p2 = new GroupPermission( "*:Test", "edit" );
        p3 = new GroupPermission( "mywiki:Test", "edit" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        // Edit action on collection should imply view on single group
        p1 = new GroupPermission( "*:*st", "edit" );
        p2 = new GroupPermission( "*:Test", "view" );
        p3 = new GroupPermission( "mywiki:Test", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );


        // Pre- and post- wildcards should also be fine
        p1 = new GroupPermission( "*:Test*", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );

        p1 = new GroupPermission( "*:*Group", "view" );
        p2 = new GroupPermission( "*:TestGroup", "view" );
        p3 = new GroupPermission( "mywiki:TestGroup", "view" );
        assertTrue( p1.implies( p2 ) );
        assertTrue( p1.implies( p3 ) );
        assertFalse( p2.implies( p1 ) );
        assertFalse( p3.implies( p1 ) );
        
        // Wildcards don't imply the <groupmember> target
        p1 = new GroupPermission( "*:*", "view" );
        p2 = new GroupPermission( "*:<groupmember>", "view" );
        assertFalse( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
        
        p1 = new GroupPermission( "*:*ber>", "view" );
        assertFalse( p1.implies( p2 ) );
        assertFalse( p2.implies( p1 ) );
    }
    
    public final void testImplies()
    {
        assertTrue( GroupPermission.DELETE.implies( GroupPermission.EDIT ) );
        assertTrue( GroupPermission.DELETE.implies( GroupPermission.VIEW ) );
        assertTrue( GroupPermission.EDIT.implies( GroupPermission.VIEW ) );
    }

    public final void testImpliedMask()
    {
        int result = ( GroupPermission.DELETE_MASK | GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        assertEquals( result, GroupPermission.impliedMask( GroupPermission.DELETE_MASK ) );

        result = ( GroupPermission.EDIT_MASK | GroupPermission.VIEW_MASK );
        assertEquals( result, GroupPermission.impliedMask( GroupPermission.EDIT_MASK ) );
    }

    public final void testGetName()
    {
        GroupPermission p;
        p = new GroupPermission( "Test", "view,edit,delete" );
        assertEquals( "Test", p.getName() );
        p = new GroupPermission( "mywiki:Test", "view,edit,delete" );
        assertEquals( "mywiki:Test", p.getName() );
        assertNotSame( "*:Test", p.getName() );
    }

    /*
     * Class under test for java.lang.String getActions()
     */
    public final void testGetActions()
    {
        GroupPermission p = new GroupPermission( "Test", "VIEW,edit,delete" );
        assertEquals( "delete,edit,view", p.getActions() );
    }

    /**
     * Binds a Subject to the current AccessControlContext and calls 
     * p1.implies(p2).
     * @param subject
     * @param p1
     * @param p2
     * @return
     */
    protected final boolean subjectImplies( final Subject subject, final GroupPermission p1, final Permission p2 )
    {
        try
        {
            Boolean result = (Boolean)Subject.doAsPrivileged( subject, new PrivilegedAction()
            {
                public Object run()
                {
                    return Boolean.valueOf( p1.impliesMember( p2 ) );
                }
            }, null );
            return result.booleanValue();
        }
        catch( AccessControlException e )
        {
            return false;
        }
    }

}
