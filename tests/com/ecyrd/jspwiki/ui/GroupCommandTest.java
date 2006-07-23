/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;

public class GroupCommandTest extends TestCase
{
    TestEngine     testEngine;
    
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
    }
    
    protected void tearDown() throws Exception
    {
    }

    public void testStaticCommand()
    {
        Command a;
        
        a = GroupCommand.VIEW_GROUP;
        assertEquals( "viewGroup", a.getRequestContext() );
        assertEquals( "Group.jsp", a.getJSP() );
        assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.VIEW_GROUP );
        
        a = GroupCommand.EDIT_GROUP;
        assertEquals( "editGroup", a.getRequestContext() );
        assertEquals( "EditGroup.jsp", a.getJSP() );
        assertEquals( "%uEditGroup.jsp?group=%n", a.getURLPattern() );
        assertEquals( "EditGroupContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.EDIT_GROUP );
        
        a = GroupCommand.DELETE_GROUP;
        assertEquals( "deleteGroup", a.getRequestContext() );
        assertEquals( "DeleteGroup.jsp", a.getJSP() );
        assertEquals( "%uDeleteGroup.jsp?group=%n", a.getURLPattern() );
        assertNull( null );
        assertNull( a.getTarget());
        assertNull( a.requiredPermission() );
        assertEquals( a, GroupCommand.DELETE_GROUP );
    }
    
    public void testTargetedCommand()
    {
        // Get view command
        Command a = GroupCommand.VIEW_GROUP;
        GroupPrincipal group = new GroupPrincipal( "MyWiki", "Test" );
        
        // Combine with wiki group; make sure it's not equal to old command
        Command b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertEquals( a.getRequestContext(), b.getRequestContext() );
        assertEquals( a.getJSP(), b.getJSP() );
        assertEquals( a.getURLPattern(), b.getURLPattern() );
        assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "MyWiki:Test", "view" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
        
        // Do the same with edit command
        a = GroupCommand.EDIT_GROUP;
        b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "MyWiki:Test", "edit" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
        
        // Do the same with delete command
        a = GroupCommand.DELETE_GROUP;
        b = a.targetedCommand( group );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new GroupPermission( "MyWiki:Test", "delete" ), b.requiredPermission() );
        assertEquals( group, b.getTarget() );
    }
    
    public static Test suite()
    {
        return new TestSuite( GroupCommandTest.class );
    }
}
