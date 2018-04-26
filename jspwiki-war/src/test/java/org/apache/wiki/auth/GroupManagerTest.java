/*
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
package org.apache.wiki.auth;
import java.security.Principal;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GroupManagerTest
{
    private TestEngine        m_engine;

    private GroupManager      m_groupMgr;

    private SecurityEventTrap m_trap = new SecurityEventTrap();

    private WikiSession       m_session;

    @Before
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        m_engine = new TestEngine( props );
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );

        // Flush any pre-existing groups (left over from previous Assert.failures, perhaps)
        try
        {
            m_groupMgr.removeGroup( "Test" );
            m_groupMgr.removeGroup( "Test2" );
            m_groupMgr.removeGroup( "Test3" );
        }
        catch ( NoSuchPrincipalException e )
        {
            // It's not a problem if we can't find the principals...
        }

        m_groupMgr.addWikiEventListener( m_trap );
        m_trap.clearEvents();

        // Add 3 test groups
        Group group;
        group = m_groupMgr.parseGroup( "Test", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, group );
        group = m_groupMgr.parseGroup( "Test2", "Bob", true );
        m_groupMgr.setGroup( m_session, group );
        group = m_groupMgr.parseGroup( "Test3", "Fred Flintstone", true );
        m_groupMgr.setGroup( m_session, group );

        // We should see 3 events: 1 for each group add
        Assert.assertEquals( 3, m_trap.events().length );
        m_trap.clearEvents();
    }

    @After
    public void tearDown() throws WikiException
    {
        m_groupMgr.removeGroup( "Test" );
        m_groupMgr.removeGroup( "Test2" );
        m_groupMgr.removeGroup( "Test3" );
   }

    @Test
    public void testParseGroup() throws WikiSecurityException
    {
        String members = "Biff";
        Group group = m_groupMgr.parseGroup( "Group1", members, true );
        Assert.assertEquals( 1, group.members().length );
        Assert.assertTrue ( group.isMember( new WikiPrincipal( "Biff" ) ) );

        members = "Biff \n SteveAustin \n FredFlintstone";
        group = m_groupMgr.parseGroup( "Group2", members, true );
        Assert.assertEquals( 3, group.members().length );
        Assert.assertTrue ( group.isMember( new WikiPrincipal( "Biff" ) ) );
        Assert.assertTrue ( group.isMember( new WikiPrincipal( "SteveAustin" ) ) );
        Assert.assertTrue ( group.isMember( new WikiPrincipal( "FredFlintstone" ) ) );
    }

    @Test
    public void testGetRoles()
    {
        Principal[] roles = m_groupMgr.getRoles();
        Assert.assertTrue( "Found Test", ArrayUtils.contains( roles, new GroupPrincipal( "Test" ) ) );
        Assert.assertTrue( "Found Test2", ArrayUtils.contains( roles, new GroupPrincipal( "Test2" ) ) );
        Assert.assertTrue( "Found Test3", ArrayUtils.contains( roles, new GroupPrincipal( "Test3" ) ) );
    }

    @Test
    public void testGroupMembership() throws Exception
    {
        WikiSession s;

        // Anonymous; should belong to NO groups
        s = WikiSessionTest.anonymousSession( m_engine );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Alice is asserted; should belong to NO groups
        s = WikiSessionTest.assertedSession( m_engine, Users.ALICE );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Alice is authenticated; should belong to Test
        s = WikiSessionTest.authenticatedSession( m_engine, Users.ALICE, Users.ALICE_PASS );
        Assert.assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Bob is authenticated; should belong to Test & Test2
        s = WikiSessionTest.authenticatedSession( m_engine, Users.BOB, Users.BOB_PASS );
        Assert.assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Charlie is authenticated; should belong to Test
        s = WikiSessionTest.authenticatedSession( m_engine, Users.CHARLIE, Users.CHARLIE_PASS );
        Assert.assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Fred is authenticated; should belong to Test3
        s = WikiSessionTest.authenticatedSession( m_engine, Users.FRED, Users.FRED_PASS );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertTrue( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );

        // Nobody loves Biff!
        s = WikiSessionTest.authenticatedSession( m_engine, Users.BIFF, Users.BIFF_PASS );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test2" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "Test3" ) ) );
        Assert.assertFalse( m_groupMgr.isUserInRole( s, new GroupPrincipal( "NonExistant" ) ) );
    }

    @Test
    public void testGroupAddEvents() throws Exception
    {
        // Flush any pre-existing groups (left over from previous Assert.failures, perhaps)
        try
        {
            m_groupMgr.removeGroup( "Events" );
        }
        catch ( NoSuchPrincipalException e )
        {
            // It's not a problem if we get here...
        }
        m_trap.clearEvents();

        Group group = m_groupMgr.parseGroup( "Events", "", true );
        m_groupMgr.setGroup( m_session, group );
        WikiSecurityEvent event;
        group = m_groupMgr.getGroup( "Events" );
        group.add( new WikiPrincipal( "Alice" ) );
        group.add( new WikiPrincipal( "Bob" ) );
        group.add( new WikiPrincipal( "Charlie" ) );

        // We should see a GROUP_ADD event
        WikiSecurityEvent[] events = m_trap.events();
        Assert.assertEquals( 1, events.length );
        event = events[0];
        Assert.assertEquals( m_groupMgr, event.getSrc() );
        Assert.assertEquals( WikiSecurityEvent.GROUP_ADD, event.getType() );
        Assert.assertEquals( group, event.getTarget() );

        // Clean up
        m_groupMgr.removeGroup( "Events" );
    }

}