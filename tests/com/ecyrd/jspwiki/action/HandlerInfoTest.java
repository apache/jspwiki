package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

public class HandlerInfoTest extends TestCase
{
    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void testGetRequestContext() throws Exception
    {
        assertEquals( "createGroup", HandlerInfo.getHandlerInfo( GroupActionBean.class, "create" ).getRequestContext() );
        assertEquals( "deleteGroup", HandlerInfo.getHandlerInfo( GroupActionBean.class, "delete" ).getRequestContext() );
        assertEquals( "editGroup", HandlerInfo.getHandlerInfo( GroupActionBean.class, "save" ).getRequestContext() );
        assertEquals( "group", HandlerInfo.getHandlerInfo( GroupActionBean.class, "view" ).getRequestContext() );
    }

    public void testPermissionAnnotations() throws Exception
    {
        Map<Method, HandlerInfo> map = HandlerInfo.getHandlerInfoCollection( GroupActionBean.class );
        assertEquals( 4, map.size() );

        Method method = GroupActionBean.class.getMethod( "view", new Class[0] );
        assertTrue( map.containsKey( method ) );
        HandlerInfo handlerInfo = map.get( method );
        assertEquals( GroupPermission.class, handlerInfo.getPermissionClass() );
        assertEquals( "${group.name}", handlerInfo.getPermissionTarget() );
        assertNotNull( handlerInfo.getPermissionTargetExpression() );
        assertEquals( "view", handlerInfo.getPermissionActions() );
        assertNull( handlerInfo.getActionsExpression() );

        method = GroupActionBean.class.getMethod( "save", new Class[0] );
        assertTrue( map.containsKey( method ) );
        handlerInfo = map.get( method );
        assertEquals( GroupPermission.class, handlerInfo.getPermissionClass() );
        assertEquals( "${group.name}", handlerInfo.getPermissionTarget() );
        assertNotNull( handlerInfo.getPermissionTargetExpression() );
        assertEquals( "edit", handlerInfo.getPermissionActions() );
        assertNull( handlerInfo.getActionsExpression() );

        method = GroupActionBean.class.getMethod( "delete", new Class[0] );
        assertTrue( map.containsKey( method ) );
        handlerInfo = map.get( method );
        assertEquals( GroupPermission.class, handlerInfo.getPermissionClass() );
        assertEquals( "${group.name}", handlerInfo.getPermissionTarget() );
        assertNotNull( handlerInfo.getPermissionTargetExpression() );
        assertEquals( "delete", handlerInfo.getPermissionActions() );
        assertNull( handlerInfo.getActionsExpression() );

        method = GroupActionBean.class.getMethod( "create", new Class[0] );
        assertTrue( map.containsKey( method ) );
        handlerInfo = map.get( method );
        assertEquals( WikiPermission.class, handlerInfo.getPermissionClass() );
        assertEquals( WikiPermission.CREATE_GROUPS_ACTION, handlerInfo.getPermissionTarget() );
        assertNull( handlerInfo.getPermissionTargetExpression() );
        assertNull( handlerInfo.getPermissionActions() );
        assertNull( handlerInfo.getActionsExpression() );
    }

    public void testEvaluatedPermissionAnnotation() throws Exception
    {
        MockServletContext ctx = (MockServletContext) m_engine.getServletContext();
        MockRoundtrip trip;
        GroupActionBean bean;
        Method method;
        HandlerInfo handlerInfo;
        Permission perm;

        // Set up a new GroupActionBean with the real group Admin and event
        // "view"
        trip = new MockRoundtrip( ctx, "/Group.jsp" );
        trip.getRequest().setMethod( "GET" );
        trip.addParameter( "group", "Admin" );
        trip.execute( "view" );
        bean = trip.getActionBean( GroupActionBean.class );
        assertNotNull( bean );

        // The view handler should return a "view" GroupPermission
        method = GroupActionBean.class.getMethod( "view", new Class[0] );
        Map<Method,HandlerInfo> handlerInfos = HandlerInfo.getHandlerInfoCollection( GroupActionBean.class );
        handlerInfo = handlerInfos.get( method );
        assertNotNull( handlerInfo );
        perm = handlerInfo.getPermission( bean );
        assertNotNull( perm );
        assertEquals( GroupPermission.class, perm.getClass() );
        assertEquals( "Admin", perm.getName() );
        assertEquals( "view", perm.getActions() );

        // Set up a new GroupActionBean with the real group Admin and event
        // "save"
        trip = new MockRoundtrip( ctx, "/Group.jsp" );
        trip.addParameter( "group", "Admin" );
        trip.execute( "save" );
        bean = trip.getActionBean( GroupActionBean.class );
        assertNotNull( bean );

        // The view handler should return a "edit" GroupPermission
        method = GroupActionBean.class.getMethod( "save", new Class[0] );
        handlerInfo = handlerInfos.get( method );
        assertNotNull( handlerInfo );
        perm = handlerInfo.getPermission( bean );
        assertNotNull( perm );
        assertEquals( GroupPermission.class, perm.getClass() );
        assertEquals( "Admin", perm.getName() );
        assertEquals( "edit", perm.getActions() );

        // Set up a new GroupActionBean with the real group Admin and event
        // "delete"
        trip = new MockRoundtrip( ctx, "/Group.jsp" );
        trip.addParameter( "group", "Admin" );
        trip.execute( "delete" );
        bean = trip.getActionBean( GroupActionBean.class );
        assertNotNull( bean );

        // The view handler should return a "view" GroupPermission
        method = GroupActionBean.class.getMethod( "delete", new Class[0] );
        handlerInfo = handlerInfos.get( method );
        assertNotNull( handlerInfo );
        perm = handlerInfo.getPermission( bean );
        assertNotNull( perm );
        assertEquals( GroupPermission.class, perm.getClass() );
        assertEquals( "Admin", perm.getName() );
        assertEquals( "delete", perm.getActions() );
    }

    public void testNotEvaluatedPermissionAnnotation() throws Exception
    {
        MockServletContext ctx = (MockServletContext) m_engine.getServletContext();
        MockRoundtrip trip;
        GroupActionBean bean;
        Method method;
        HandlerInfo handlerInfo;
        Permission perm;

        // Set up a new GroupActionBean with the non-existent group Foo
        trip = new MockRoundtrip( ctx, "/Group.jsp" );
        trip.addParameter( "group", "Foo" );
        trip.execute( "view" );
        bean = trip.getActionBean( GroupActionBean.class );
        assertNotNull( bean );

        // The view handler should NOT return a "view" GroupPermission (because
        // EL can't evaluate)
        method = GroupActionBean.class.getMethod( "view", new Class[0] );
        Map<Method,HandlerInfo> handlerInfos = HandlerInfo.getHandlerInfoCollection( GroupActionBean.class );
        handlerInfo = handlerInfos.get( method );
        assertNotNull( handlerInfo );
        perm = handlerInfo.getPermission( bean );
        assertNull( perm );
    }

    public static Test suite()
    {
        return new TestSuite( HandlerInfoTest.class );
    }
}
