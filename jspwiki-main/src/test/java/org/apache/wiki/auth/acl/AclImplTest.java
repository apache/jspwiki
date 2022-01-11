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
package org.apache.wiki.auth.acl;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class AclImplTest {

    final TestEngine engine = TestEngine.build();
    final GroupManager m_groupMgr = engine.getManager( GroupManager.class );

    private Acl m_acl;
    private Acl m_aclGroup;
    private Map< String, Group > m_groups;

    /**
     * We setup the following rules: Alice = may view Bob = may view, may edit
     * Charlie = may view Dave = may view, may comment groupAcl: FooGroup =
     * Alice, Bob - may edit BarGroup = Bob, Charlie - may view
     */
    @BeforeEach
    public void setUp() throws Exception {
        final Session m_session = WikiSessionTest.adminSession( engine );
        m_acl = Wiki.acls().acl();
        m_aclGroup = Wiki.acls().acl();
        m_groups = new HashMap<>();
        final Principal uAlice = new WikiPrincipal( "Alice" );
        final Principal uBob = new WikiPrincipal( "Bob" );
        final Principal uCharlie = new WikiPrincipal( "Charlie" );
        final Principal uDave = new WikiPrincipal( "Dave" );

        //  Alice can view
        final AclEntry ae = Wiki.acls().entry();
        ae.addPermission( PagePermission.VIEW );
        ae.setPrincipal( uAlice );

        //  Charlie can view
        final AclEntry ae2 = Wiki.acls().entry();
        ae2.addPermission( PagePermission.VIEW );
        ae2.setPrincipal( uCharlie );

        //  Bob can view and edit (and by implication, comment)
        final AclEntry ae3 = Wiki.acls().entry();
        ae3.addPermission( PagePermission.VIEW );
        ae3.addPermission( PagePermission.EDIT );
        ae3.setPrincipal( uBob );

        // Dave can view and comment
        final AclEntry ae4 = Wiki.acls().entry();
        ae4.addPermission( PagePermission.VIEW );
        ae4.addPermission( PagePermission.COMMENT );
        ae4.setPrincipal( uDave );

        // Create ACL with Alice, Bob, Charlie, Dave
        m_acl.addEntry( ae );
        m_acl.addEntry( ae2 );
        m_acl.addEntry( ae3 );
        m_acl.addEntry( ae4 );

        // Foo group includes Alice and Bob
        final Group foo = m_groupMgr.parseGroup( "FooGroup", "", true );
        m_groupMgr.setGroup( m_session, foo );
        foo.add( uAlice );
        foo.add( uBob );
        final AclEntry ag1 = Wiki.acls().entry();
        ag1.setPrincipal( foo.getPrincipal() );
        ag1.addPermission( PagePermission.EDIT );
        m_aclGroup.addEntry( ag1 );
        m_groups.put( "FooGroup", foo );

        // Bar group includes Bob and Charlie
        final Group bar = m_groupMgr.parseGroup( "BarGroup", "", true );
        m_groupMgr.setGroup( m_session, bar );
        bar.add( uBob );
        bar.add( uCharlie );
        final AclEntry ag2 = Wiki.acls().entry();
        ag2.setPrincipal( bar.getPrincipal() );
        ag2.addPermission( PagePermission.VIEW );
        m_aclGroup.addEntry( ag2 );
        m_groups.put( "BarGroup", bar );
    }

    @AfterEach
    public void tearDown() throws Exception {
        m_groupMgr.removeGroup( "FooGroup" );
        m_groupMgr.removeGroup( "BarGroup" );
    }

    private boolean inArray( final Object[] array, final Object key ) {
        for( final Object o : array ) {
            if( o.equals( key ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean inGroup( final Object[] array, final Principal key ) {
        for( final Object o : array ) {
            if( o instanceof GroupPrincipal ) {
                final String groupName = ( ( GroupPrincipal )o ).getName();
                final Group group = m_groups.get( groupName );
                if( group != null && group.isMember( key ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void testAlice() {
        // Alice should be able to view but not edit or comment
        final Principal wup = new WikiPrincipal( "Alice" );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.VIEW ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.EDIT ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.COMMENT ), wup ) );
    }

    @Test
    public void testBob() {
        // Bob should be able to view, edit, and comment but not delete
        final Principal wup = new WikiPrincipal( "Bob" );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.VIEW ), wup ) );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.EDIT ), wup ) );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.COMMENT ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.DELETE ), wup ) );
    }

    @Test
    public void testCharlie() {
        // Charlie should be able to view, but not edit, comment or delete
        final Principal wup = new WikiPrincipal( "Charlie" );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.VIEW ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.EDIT ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.COMMENT ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.DELETE ), wup ) );
    }

    @Test
    public void testDave() {
        // Dave should be able to view and comment but not edit or delete
        final Principal wup = new WikiPrincipal( "Dave" );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.VIEW ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.EDIT ), wup ) );
        Assertions.assertTrue( inArray( m_acl.findPrincipals( PagePermission.COMMENT ), wup ) );
        Assertions.assertFalse( inArray( m_acl.findPrincipals( PagePermission.DELETE ), wup ) );
    }

    @Test
    public void testGroups() {
        Principal wup = new WikiPrincipal( "Alice" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.VIEW ), wup ), "Alice view" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.EDIT ), wup ), "Alice edit" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.COMMENT ), wup ), "Alice comment" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.DELETE ), wup ),"Alice delete" );

        wup = new WikiPrincipal( "Bob" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.VIEW ), wup ), "Bob view" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.EDIT ), wup ), "Bob edit" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.COMMENT ), wup ), "Bob comment" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.DELETE ), wup ), "Bob delete" );

        wup = new WikiPrincipal( "Charlie" );
        Assertions.assertTrue( inGroup( m_aclGroup.findPrincipals( PagePermission.VIEW ), wup ), "Charlie view" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.EDIT ), wup ), "Charlie edit" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.COMMENT ), wup ), "Charlie comment" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.DELETE ), wup ),"Charlie delete" );

        wup = new WikiPrincipal( "Dave" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.VIEW ), wup ), "Dave view" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.EDIT ), wup ), "Dave edit" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.COMMENT ), wup ), "Dave comment" );
        Assertions.assertFalse( inGroup( m_aclGroup.findPrincipals( PagePermission.DELETE ), wup ), "Dave delete" );
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectOutputStream out2 = new ObjectOutputStream(out);
        out2.writeObject( m_acl );
        out2.close();
        final byte[] stuff = out.toByteArray();
        final ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream(stuff) );
        final AclImpl newacl = (AclImpl) in.readObject();
        Assertions.assertEquals( newacl.toString(), m_acl.toString() );
    }

}
