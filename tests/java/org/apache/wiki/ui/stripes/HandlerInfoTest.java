/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.ui.stripes;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Map;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.EditActionBean;
import org.apache.wiki.action.GroupActionBean;
import org.apache.wiki.auth.permissions.GroupPermission;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.ui.stripes.HandlerInfo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;


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

    public void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
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
        assertEquals( 5, map.size() );

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

        method = GroupActionBean.class.getMethod( "saveNew", new Class[0] );
        assertTrue( map.containsKey( method ) );
        handlerInfo = map.get( method );
        assertEquals( WikiPermission.class, handlerInfo.getPermissionClass() );
        assertEquals( "*", handlerInfo.getPermissionTarget() );
        assertNull( handlerInfo.getPermissionTargetExpression() );
        assertEquals( WikiPermission.CREATE_GROUPS_ACTION, handlerInfo.getPermissionActions() );
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
        assertEquals( "*", handlerInfo.getPermissionTarget() );
        assertNull( handlerInfo.getPermissionTargetExpression() );
        assertEquals( WikiPermission.CREATE_GROUPS_ACTION, handlerInfo.getPermissionActions() );
        assertNull( handlerInfo.getActionsExpression() );
    }

    public void testEvaluatedPermissionAnnotation() throws Exception
    {
        MockRoundtrip trip;
        GroupActionBean bean;
        Method method;
        HandlerInfo handlerInfo;
        Permission perm;

        // Set up a new GroupActionBean with the real group Admin and event
        // "view"
        trip = m_engine.guestTrip( "/Group.action" );
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
        trip = m_engine.guestTrip( "/Group.action" );
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
        trip = m_engine.guestTrip( "/Group.action" );
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
        // Set up a new EditActionBean without a page parameter
        EditActionBean bean = new EditActionBean();
        
        // The view handler should NOT return an "edit" PagePermission (because
        // EL can't evaluate)
        Method method = EditActionBean.class.getMethod( "edit", new Class[0] );
        Map<Method,HandlerInfo> handlerInfos = HandlerInfo.getHandlerInfoCollection( EditActionBean.class );
        HandlerInfo handlerInfo = handlerInfos.get( method );
        assertNotNull( handlerInfo );
        Permission perm = handlerInfo.getPermission( bean );
        assertNull( perm );
    }

    public static Test suite()
    {
        return new TestSuite( HandlerInfoTest.class );
    }
}
