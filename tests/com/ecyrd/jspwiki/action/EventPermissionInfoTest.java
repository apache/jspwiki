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

public class EventPermissionInfoTest extends TestCase
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
        catch (Exception e)
        {
            throw new RuntimeException("Could not set up TestEngine: " + e.getMessage());
        }
    }
    
    public void testActionBeanAnnotation() throws Exception {
        Map<Method,EventPermissionInfo> map = EventPermissionInfo.getEventPermissionInfo(GroupActionBean.class);
        assertEquals(3, map.size());
        
        Method method = GroupActionBean.class.getMethod("view", new Class[0]);
        assertTrue(map.containsKey(method));
        EventPermissionInfo info = map.get(method);
        assertEquals(GroupPermission.class, info.getPermissionClass());
        assertEquals("${group.qualifiedName}", info.getTarget());
        assertNotNull(info.getTargetExpression());
        assertEquals("view", info.getActions());
        assertNull(info.getActionsExpression());
        
        method = GroupActionBean.class.getMethod("save", new Class[0]);
        assertTrue(map.containsKey(method));
        info = map.get(method);
        assertEquals(GroupPermission.class, info.getPermissionClass());
        assertEquals("${group.qualifiedName}", info.getTarget());
        assertNotNull(info.getTargetExpression());
        assertEquals("edit", info.getActions());
        assertNull(info.getActionsExpression());
        
        method = GroupActionBean.class.getMethod("delete", new Class[0]);
        assertTrue(map.containsKey(method));
        info = map.get(method);
        assertEquals(GroupPermission.class, info.getPermissionClass());
        assertEquals("${group.qualifiedName}", info.getTarget());
        assertNotNull(info.getTargetExpression());
        assertEquals("delete", info.getActions());
        assertNull(info.getActionsExpression());
    }
    
    public void testEvaluatedAnnotation() throws Exception {
        MockServletContext ctx = m_engine.getServletContext();
        MockRoundtrip trip;
        GroupActionBean bean;
        Method method;
        EventPermissionInfo permInfo;
        Permission perm;

        // Set up a new GroupActionBean with the real group Admin and event "view"
        trip = new MockRoundtrip(ctx, "/Group.action");
        trip.addParameter("group","Admin");
        trip.execute("view");
        bean = trip.getActionBean(GroupActionBean.class);

        // The view handler should return a "view" GroupPermission
        method = GroupActionBean.class.getMethod("view",new Class[0]);
        permInfo = bean.getContext().getPermissionInfo(method);
        assertNotNull(permInfo);
        perm = permInfo.getPermission(bean);
        assertNotNull(perm);
        assertEquals(GroupPermission.class,perm.getClass());
        assertEquals("JSPWiki:Admin",perm.getName());
        assertEquals("view",perm.getActions());
        
        // Set up a new GroupActionBean with the real group Admin and event "save"
        trip = new MockRoundtrip(ctx, "/Group.action");
        trip.addParameter("group","Admin");
        trip.execute("save");
        bean = trip.getActionBean(GroupActionBean.class);

        // The view handler should return a "edit" GroupPermission
        method = GroupActionBean.class.getMethod("save",new Class[0]);
        permInfo = bean.getContext().getPermissionInfo(method);
        assertNotNull(permInfo);
        perm = permInfo.getPermission(bean);
        assertNotNull(perm);
        assertEquals(GroupPermission.class,perm.getClass());
        assertEquals("JSPWiki:Admin",perm.getName());
        assertEquals("edit",perm.getActions());
        
        // Set up a new GroupActionBean with the real group Admin and event "delete"
        trip = new MockRoundtrip(ctx, "/Group.action");
        trip.addParameter("group","Admin");
        trip.execute("delete");
        bean = trip.getActionBean(GroupActionBean.class);

        // The view handler should return a "view" GroupPermission
        method = GroupActionBean.class.getMethod("delete",new Class[0]);
        permInfo = bean.getContext().getPermissionInfo(method);
        assertNotNull(permInfo);
        perm = permInfo.getPermission(bean);
        assertNotNull(perm);
        assertEquals(GroupPermission.class,perm.getClass());
        assertEquals("JSPWiki:Admin",perm.getName());
        assertEquals("delete",perm.getActions());
    }
    
    public void testNotEvaluatedAnnotation() throws Exception
    {
        MockServletContext ctx = m_engine.getServletContext();
        MockRoundtrip trip;
        GroupActionBean bean;
        Method method;
        EventPermissionInfo permInfo;
        Permission perm;

        // Set up a new GroupActionBean with the non-existent group Foo
        trip = new MockRoundtrip(ctx, "/Group.action");
        trip.addParameter("group","Foo");
        trip.execute("view");
        bean = trip.getActionBean(GroupActionBean.class);
        
        // The view handler should NOT return a "view" GroupPermission (because EL can't evaluate)
        method = GroupActionBean.class.getMethod("view",new Class[0]);
        permInfo = bean.getContext().getPermissionInfo(method);
        assertNotNull(permInfo);
        perm = permInfo.getPermission(bean);
        assertNull(perm);
    }

    public static Test suite()
    {
        return new TestSuite( EventPermissionInfoTest.class );
    }
}
