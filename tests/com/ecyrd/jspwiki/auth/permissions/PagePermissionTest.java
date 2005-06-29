package com.ecyrd.jspwiki.auth.permissions;

import junit.framework.TestCase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 */
public class PagePermissionTest extends TestCase
{

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( PagePermissionTest.class );
    }

    /*
     * Class under test for boolean equals(java.lang.Object)
     */
    public final void testEqualsObject()
    {
        PagePermission p1 = new PagePermission("Main", "view,edit,delete");
        PagePermission p2 = new PagePermission("Main", "view,edit,delete");
        PagePermission p3 = new PagePermission("Main", "delete,view,edit");
        PagePermission p4 = new PagePermission("Main*", "delete,view,edit");
        assertEquals(p1, p2);
        assertEquals(p1, p3);
        assertFalse(p3.equals(p4));
    }

    public final void testCreateMask() {
        assertEquals(1, PagePermission.createMask("view"));
        assertEquals(19, PagePermission.createMask("view,edit,delete"));
        assertEquals(19, PagePermission.createMask("edit,delete,view"));
        assertEquals(14, PagePermission.createMask("edit,comment,upload"));
    }
    
    /*
     * Class under test for java.lang.String toString()
     */
    public final void testToString()
    {
        PagePermission p = new PagePermission("Main", "view,edit,delete");
        assertEquals("(\"com.ecyrd.jspwiki.auth.permissions.PagePermission\",\"Main\",\"delete,edit,view\")"
                , p.toString());
    }

    /*
     * Class under test for boolean implies(java.security.Permission)
     */
    public final void testImpliesPermission()
    {
        PagePermission p1;
        PagePermission p2;
        
        // The same permission should imply itself
        p1 = new PagePermission("Main", "view,edit,delete");
        p2 = new PagePermission("Main", "view,edit,delete");
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));

        // Actions on collection should imply permission for page with same actions
        p1 = new PagePermission("*", "view,edit,delete");
        p2 = new PagePermission("Main", "view,edit,delete");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        // Actions on single page should imply subset of those actions
        p1 = new PagePermission("Main", "view,edit,delete");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        // Actions on collectioon should imply subset of actions on single page
        p1 = new PagePermission("*", "view,edit,delete");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        p1 = new PagePermission("Mai*", "view,edit,delete");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        p1 = new PagePermission("*in", "view,edit,delete");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        // Delete action on collection should imply edit/upload/comment/view on single page
        p1 = new PagePermission("*in", "delete");
        p2 = new PagePermission("Main", "edit");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p2 = new PagePermission("Main", "upload");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p2 = new PagePermission("Main", "comment");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        // Rename action on collection should imply edit on single page
        p1 = new PagePermission("*in", "rename");
        p2 = new PagePermission("Main", "edit");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        // Edit action on collection should imply upload/comment/view on single page
        p1 = new PagePermission("*in", "edit");
        p2 = new PagePermission("Main", "upload");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        p2 = new PagePermission("Main", "comment");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        // Upload action on collection should imply view on single page
        p1 = new PagePermission("*in", "upload");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        // Comment action on collection should imply view on single page
        p1 = new PagePermission("*in", "comment");
        p2 = new PagePermission("Main", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        // View action on wildcard collection shouldn't imply view on GroupConfiguration page
        p1 = new PagePermission("*", "view");
        p2 = new PagePermission("GroupConfiguration", "view");
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));

        // However, pre- and post- wildcards should be fine
        p1 = new PagePermission("Group*", "view");
        p2 = new PagePermission("GroupConfiguration", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        
        p1 = new PagePermission("*Configuration", "view");
        p2 = new PagePermission("GroupConfiguration", "view");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

    public final void testImpliedMask()
    {
        int result = ( PagePermission.DELETE_MASK | PagePermission.EDIT_MASK | PagePermission.COMMENT_MASK | PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        assertEquals( result, PagePermission.impliedMask( PagePermission.DELETE_MASK ) );

        result = ( PagePermission.EDIT_MASK | PagePermission.COMMENT_MASK | PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        assertEquals( result, PagePermission.impliedMask( PagePermission.EDIT_MASK ) );

        result = ( PagePermission.COMMENT_MASK | PagePermission.VIEW_MASK );
        assertEquals( result, PagePermission.impliedMask( PagePermission.COMMENT_MASK ) );

        result = ( PagePermission.UPLOAD_MASK | PagePermission.VIEW_MASK );
        assertEquals( result, PagePermission.impliedMask( PagePermission.UPLOAD_MASK ) );
    }
    
    public final void testGetName()
    {
        PagePermission p = new PagePermission("Main", "view,edit,delete");
        assertEquals("Main", p.getName());
    }

    /*
     * Class under test for java.lang.String getActions()
     */
    public final void testGetActions()
    {
        PagePermission p = new PagePermission("Main", "VIEW,edit,delete");
        assertEquals("delete,edit,view", p.getActions());
    }

}
