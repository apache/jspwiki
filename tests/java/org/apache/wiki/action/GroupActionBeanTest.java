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
package org.apache.wiki.action;

import java.security.Principal;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupDatabase;
import org.apache.wiki.auth.authorize.GroupManager;


public class GroupActionBeanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( GroupActionBeanTest.class );
    }

    TestEngine m_engine;
    
    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
            deleteGroup( "TestGroup" );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown()
    {
        deleteGroup( "TestGroup" );
        
        m_engine.shutdown();
    }

    public void testView() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, GroupActionBean.class );

        // View the group
        trip.setParameter( "group", "TV" );
        trip.execute( "view" );

        // Verify we are directed to the view page
        GroupActionBean bean = trip.getActionBean( GroupActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/templates/default/Group.jsp", trip.getDestination() );

        // Verify we got the right group
        Group group = bean.getGroup();
        assertNotNull( group );
        assertEquals( "TV", group.getName() );
        assertEquals( 3, group.members().length );
        assertTrue( group.isMember( new WikiPrincipal( "Archie Bunker" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "BullwinkleMoose" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Fred Friendly" ) ) );
    }
      
    public void testViewNonExistent() throws Exception
    {
        MockRoundtrip trip;
        GroupActionBean bean;
        ValidationErrors errors;

        // Start with an authenticated user
        trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, GroupActionBean.class );

        // View a non-existent group
        trip.setParameter( "group", "NonExistentGroup" );
        trip.execute( "view" );

        // Verify we are directed to the "create" event
        bean = trip.getActionBean( GroupActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Group.jsp?create=&group=NonExistentGroup", trip.getDestination() );
    }
        
    public void testViewNullGroup() throws Exception
    {
        MockRoundtrip trip;
        GroupActionBean bean;
        ValidationErrors errors;

        // Start with an authenticated user
        trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, GroupActionBean.class );

        // View a non-existent group
        trip.execute( "view" );

        // Verify we are directed to the "create" event with suggested group name
        bean = trip.getActionBean( GroupActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertNotNull(  bean.getGroup() );
        assertEquals( "Group1", bean.getGroup().getName() );
        assertEquals( "/Group.jsp?create=&group=Group1", trip.getDestination() );
    }
   
    public void testDeleteGroup() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, GroupActionBean.class );
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( trip.getRequest().getSession() );

        // Create a new group
        GroupManager mgr = m_engine.getGroupManager();
        Group group = mgr.getGroup( "TestGroup", true );
        group.add( new WikiPrincipal( "Janne Jalkanen" ) );
        group.add( new WikiPrincipal( "Princess Buttercup" ) );
        mgr.setGroup( wikiSession, group );
        
        // Make sure the group saved correctly
        group = mgr.getGroup( "TestGroup" );
        assertNotNull( group );
        
        // Now, try to delete the group
        trip.setParameter( "group", "TestGroup" );
        trip.execute( "delete" );
        
        // Make sure the group was deleted
        try
        {
            group = mgr.getGroup( "TestGroup" );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Excellent! This is what we expected
        }
        assertNull( null );
        
        // Verify we are directed to the wiki front page
        GroupActionBean bean = trip.getActionBean( GroupActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.jsp", trip.getDestination() );
    }
    
    public void testSaveExistingGroup() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, GroupActionBean.class );
        WikiSession wikiSession = SessionMonitor.getInstance( m_engine ).find( trip.getRequest().getSession() );

        // Create a new group using tha back-end APIs so that it exists already
        GroupManager mgr = m_engine.getGroupManager();
        Group group = mgr.getGroup( "TestSaveExistingGroup", true );
        group.add( new WikiPrincipal( "Janne Jalkanen" ) );
        group.add( new WikiPrincipal( "Princess Buttercup" ) );
        mgr.setGroup( wikiSession, group );
        
        // Make sure the group saved correctly
        group = mgr.getGroup( "TestSaveExistingGroup" );
        assertNotNull( group );

        // Now, via the UI, try to save the group with some new members
        trip.setParameter( "group", "TestSaveExistingGroup" );
        trip.setParameter( "members","Princess Buttercup\nInigo Montoya\nMiracle Max" );
        trip.setParameter( "new", "false" );
        trip.execute( "save" );
        
        // Verify we are directed to the group-view page
        GroupActionBean bean = trip.getActionBean( GroupActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Group.jsp?view=&group=TestSaveExistingGroup", trip.getDestination() );
        
        // Verify the Group members were set on the Actionbean correctly (sorted!)
        List<Principal> members = bean.getMembers();
        assertEquals( 3, members.size() );
        assertEquals( new WikiPrincipal( "Inigo Montoya" ), members.get( 0 ) );
        assertEquals( new WikiPrincipal( "Miracle Max" ), members.get( 1 ) );
        assertEquals( new WikiPrincipal( "Princess Buttercup" ), members.get( 2 ) );

        // Verify the Group saved correctly
        group = bean.getGroup();
        assertNotNull( group );
        assertEquals( "TestSaveExistingGroup", group.getName() );
        assertEquals( 3, group.members().length );
        assertTrue( group.isMember( new WikiPrincipal( "Princess Buttercup" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Inigo Montoya" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Miracle Max" ) ) );

        // Remove the test group
        mgr.removeGroup( "TestSaveExistingGroup" );
    }
    
    public void testSaveNewGroup() throws Exception
    {
        // Start with a guest session
        MockRoundtrip trip = m_engine.guestTrip( GroupActionBean.class );

        // Try to save the group (make sure Janne is in the list!)
        trip.setParameter( "group", "TestSaveNewGroup" );
        trip.setParameter( "members","Janne Jalkanen\nPrincess Buttercup\nInigo Montoya\nMiracle Max" );
        trip.setParameter( "new", "true" );
        trip.execute( "save" );

        // Should NOT succeed because the save event requires Edit permissions, which we don't have
        GroupActionBean bean = trip.getActionBean( GroupActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Login.jsp", trip.getDestination().substring( 0, 10 ) );
        
        // Try saving again, this time authenticated
        trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, GroupActionBean.class );
        trip.setParameter( "group", "TestSaveNewGroup" );
        trip.setParameter( "members","Janne Jalkanen\nPrincess Buttercup\nInigo Montoya\nMiracle Max" );
        trip.setParameter( "new","true" );
        trip.execute( "save" );
        
        // Verify we are directed to the view page
        bean = trip.getActionBean( GroupActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Group.jsp?view=&group=TestSaveNewGroup", trip.getDestination() );

        // Verify the Group members were set on the Actionbean correctly (sorted!)
        List<Principal> members = bean.getMembers();
        assertEquals( 4, members.size() );
        assertEquals( new WikiPrincipal( "Inigo Montoya" ), members.get( 0 ) );
        assertEquals( new WikiPrincipal( "Janne Jalkanen" ), members.get( 1 ) );
        assertEquals( new WikiPrincipal( "Miracle Max" ), members.get( 2 ) );
        assertEquals( new WikiPrincipal( "Princess Buttercup" ), members.get( 3 ) );

        // Verify the Group saved correctly
        Group group = bean.getGroup();
        assertNotNull( group );
        assertEquals( "TestSaveNewGroup", group.getName() );
        assertEquals( 4, group.members().length );
        assertTrue( group.isMember( new WikiPrincipal( "Janne Jalkanen" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Princess Buttercup" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Inigo Montoya" ) ) );
        assertTrue( group.isMember( new WikiPrincipal( "Miracle Max" ) ) );

        // Remove the test group
        m_engine.getGroupManager().removeGroup( "TestSaveNewGroup" );
    }

    private void deleteGroup( String groupName )
    {
        GroupManager mgr = m_engine.getGroupManager();
        try
        {
            Group group = mgr.getGroup( groupName );
            GroupDatabase db = m_engine.getGroupManager().getGroupDatabase();
            db.delete( group );
        }
        catch ( Exception e )
        {
            // Ok; no group there, or GroupDatabase not initialized
        }
    }
}
