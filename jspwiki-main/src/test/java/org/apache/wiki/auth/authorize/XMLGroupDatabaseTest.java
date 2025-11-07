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
package org.apache.wiki.auth.authorize;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Properties;


/**
 *
 */
public class XMLGroupDatabaseTest {


    @Test
    public void testDelete() throws WikiException {
        XMLGroupDatabase m_db;

        String m_wiki;
        final Properties props = TestEngine.getTestProperties();
        final WikiEngine engine = new TestEngine(props);
        m_db = new XMLGroupDatabase();
        m_db.initialize(engine, props);
        m_wiki = engine.getApplicationName();
        // First, count the number of groups in the db now.
        final int oldUserCount = m_db.groups().length;

        // Create a new group with random name
        final String name = "TestGroup" + System.currentTimeMillis();
        Group group = new Group( name, m_wiki );
        final Principal al = new WikiPrincipal( "Al" );
        final Principal bob = new WikiPrincipal( "Bob" );
        group.add( al );
        group.add( bob );
        m_db.save( group, new WikiPrincipal( "Tester" ) );

        // Make sure the profile saved successfully
        group = backendGroup( name, m_db );
        Assertions.assertEquals( name, group.getName() );
        Assertions.assertEquals( oldUserCount + 1, m_db.groups().length );

        // Now delete the profile; should be back to old count
        m_db.delete( group );
        Assertions.assertEquals( oldUserCount, m_db.groups().length );
    }

    @Test
    public void testGroups() throws WikiSecurityException, WikiException {
        XMLGroupDatabase m_db;

        String m_wiki;
        final Properties props = TestEngine.getTestProperties();
        final WikiEngine engine = new TestEngine(props);
        m_db = new XMLGroupDatabase();
        m_db.initialize(engine, props);
        m_wiki = engine.getApplicationName();
        // Test file has 4 groups in it: TV, Literature, Art, and Admin
        final Group[] groups = m_db.groups();
        Assertions.assertEquals( 4, groups.length );

        Group group;

        // Group TV has 3 members
        group = backendGroup( "TV", m_db );
        Assertions.assertEquals( "TV", group.getName() );
        Assertions.assertEquals( 3, group.members().length );

        // Group Literature has 2 members
        group = backendGroup( "Literature", m_db );
        Assertions.assertEquals( "Literature", group.getName() );
        Assertions.assertEquals( 2, group.members().length );

        // Group Art has no members
        group = backendGroup( "Art", m_db );
        Assertions.assertEquals( "Art", group.getName() );
        Assertions.assertEquals( 0, group.members().length );

        // Group Admin has 1 member (Administrator)
        group = backendGroup( "Admin", m_db );
        Assertions.assertEquals( "Admin", group.getName() );
        Assertions.assertEquals( 1, group.members().length );
        Assertions.assertEquals( "Administrator", group.members()[ 0 ].getName() );

        // Group Archaeology doesn't exist
        try {
            backendGroup( "Archaeology", m_db );
            // We should never get here
            Assertions.fail();
        } catch( final NoSuchPrincipalException e ) {
            Assertions.assertTrue( true );
        }
    }

    @Test
    public void testSave() throws Exception {
        XMLGroupDatabase m_db;

        String m_wiki;
        final Properties props = TestEngine.getTestProperties();
        final WikiEngine engine = new TestEngine(props);
        m_db = new XMLGroupDatabase();
        m_db.initialize(engine, props);
        m_wiki = engine.getApplicationName();
        // Create a new group with random name
        final String name = "TestGroup" + System.currentTimeMillis();
        Group group = new Group( name, m_wiki );
        final Principal al = new WikiPrincipal( "Al" );
        final Principal bob = new WikiPrincipal( "Bob" );
        final Principal cookie = new WikiPrincipal( "Cookie" );
        group.add( al );
        group.add( bob );
        group.add( cookie );
        m_db.save( group, new WikiPrincipal( "Tester" ) );

        // Make sure the profile saved successfully
        group = backendGroup( name, m_db );
        Assertions.assertEquals( name, group.getName() );
        Assertions.assertEquals( 3, group.members().length );
        Assertions.assertTrue( group.isMember( new WikiPrincipal( "Al" ) ) );
        Assertions.assertTrue( group.isMember( new WikiPrincipal( "Bob" ) ) );
        Assertions.assertTrue( group.isMember( new WikiPrincipal( "Cookie" ) ) );

        // The back-end should have timestamped the create/modify fields
        Assertions.assertNotNull( group.getCreator() );
        Assertions.assertEquals( "Tester", group.getCreator() );
        Assertions.assertNotNull( group.getCreated() );
        Assertions.assertNotNull( group.getModifier() );
        Assertions.assertEquals( "Tester", group.getModifier() );
        Assertions.assertNotNull( group.getLastModified() );
        Assertions.assertNotSame( group.getCreated(), group.getLastModified() );

        // Remove the group
        m_db.delete( group );
    }

    @Test
    public void testResave() throws Exception {
        XMLGroupDatabase m_db;

        String m_wiki;
        final Properties props = TestEngine.getTestProperties();
        final WikiEngine engine = new TestEngine(props);
        m_db = new XMLGroupDatabase();
        m_db.initialize(engine, props);
        m_wiki = engine.getApplicationName();
        // Create a new group with random name & 3 members
        final String name = "TestGroup" + System.currentTimeMillis();
        Group group = new Group( name, m_wiki );
        final Principal al = new WikiPrincipal( "Al" );
        final Principal bob = new WikiPrincipal( "Bob" );
        final Principal cookie = new WikiPrincipal( "Cookie" );
        group.add( al );
        group.add( bob );
        group.add( cookie );
        m_db.save( group, new WikiPrincipal( "Tester" ) );

        // Make sure the profile saved successfully
        group = backendGroup( name, m_db );
        Assertions.assertEquals( name, group.getName() );

        // Modify the members by adding the group; re-add Al while we're at it
        final Principal dave = new WikiPrincipal( "Dave" );
        group.add( al );
        group.add( dave );
        m_db.save( group, new WikiPrincipal( "SecondTester" ) );

        // We should see 4 members and new timestamp info
        Principal[] members = group.members();
        Assertions.assertEquals( 4, members.length );
        Assertions.assertNotNull( group.getCreator() );
        Assertions.assertEquals( "Tester", group.getCreator() );
        Assertions.assertNotNull( group.getCreated() );
        Assertions.assertNotNull( group.getModifier() );
        Assertions.assertEquals( "SecondTester", group.getModifier() );
        Assertions.assertNotNull( group.getLastModified() );

        // Check the back-end; We should see the same thing
        group = backendGroup( name, m_db );
        members = group.members();
        Assertions.assertEquals( 4, members.length );
        Assertions.assertNotNull( group.getCreator() );
        Assertions.assertEquals( "Tester", group.getCreator() );
        Assertions.assertNotNull( group.getCreated() );
        Assertions.assertNotNull( group.getModifier() );
        Assertions.assertEquals( "SecondTester", group.getModifier() );
        Assertions.assertNotNull( group.getLastModified() );

        // Remove the group
        m_db.delete( group );
    }

    private Group backendGroup( final String name, final XMLGroupDatabase m_db ) throws WikiSecurityException {
        final Group[] groups = m_db.groups();
        for( final Group group : groups ) {
            if( group.getName().equals( name ) ) {
                return group;
            }
        }
        throw new NoSuchPrincipalException( "No group named " + name );
    }

}
