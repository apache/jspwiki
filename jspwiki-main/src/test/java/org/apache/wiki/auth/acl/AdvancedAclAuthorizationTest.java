/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wiki.auth.acl;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Checks who an advanced access rule lets in, with sessions of anonymous, asserted and authenticated users. The wiki groups are
 * Editors (Alice, Fred, Biff), Reviewers (Bob, Fred) and Banned (Charlie, Biff).
 */
public class AdvancedAclAuthorizationTest {

    private static final String PAGE = "AdvancedAclPage";
    private static final String[] GROUPS = { "Editors", "Reviewers", "Banned" };

    private TestEngine engine;
    private AuthorizationManager auth;
    private GroupManager groups;

    @BeforeEach
    public void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.put( AuthorizationManager.POLICY, AuthorizationManager.DEFAULT_POLICY );
        engine = new TestEngine( props );
        auth = engine.getManager( AuthorizationManager.class );
        groups = engine.getManager( GroupManager.class );
        group( "Editors", Users.ALICE, Users.FRED, Users.BIFF );
        group( "Reviewers", Users.BOB, Users.FRED );
        group( "Banned", Users.CHARLIE, Users.BIFF );
    }

    @AfterEach
    public void tearDown() throws Exception {
        engine.getManager( PageManager.class ).deletePage( PAGE );
        for( final String name : GROUPS ) {
            groups.removeGroup( name );
        }
    }

    private void group( final String name, final String... members ) throws Exception {
        final Group group = groups.parseGroup( name, "", true );
        for( final String member : members ) {
            group.add( new WikiPrincipal( member ) );
        }
        groups.setGroup( WikiSessionTest.adminSession( engine ), group );
    }

    private boolean can( final Session session, final String action ) {
        return auth.checkPermission( session, PermissionFactory.getPagePermission( engine.getManager( PageManager.class ).getPage( PAGE ), action ) );
    }

    private Session anonymous() throws Exception {
        return WikiSessionTest.anonymousSession( engine );
    }

    private Session user( final String name ) throws Exception {
        return WikiSessionTest.authenticatedSession( engine, name, "password" );
    }

    /** Restricting the editors with an advanced rule restricts the readers too, as a simple rule does. */
    @Test
    public void testOnlyAnAdvancedRuleForEditRestrictsViewToo() throws Exception {
        engine.saveText( PAGE, "Secret. [{ALLOW edit Editors OR Reviewers}]" );

        Assertions.assertFalse( can( anonymous(), "view" ) );
        Assertions.assertFalse( can( anonymous(), "edit" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "view" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );

        for( final String name : new String[] { Users.ALICE, Users.BOB } ) {
            Assertions.assertTrue( can( user( name ), "view" ), name );
            Assertions.assertTrue( can( user( name ), "edit" ), name );
            Assertions.assertFalse( can( user( name ), "delete" ), name );
        }
    }

    @Test
    public void testSimpleRuleBeforeAnAdvancedOneIsKept() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW view Authenticated}] [{ALLOW edit Editors OR Reviewers}]" );

        Assertions.assertFalse( can( anonymous(), "view" ) );
        Assertions.assertTrue( can( user( Users.CHARLIE ), "view" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.ALICE ), "edit" ) );
    }

    @Test
    public void testAdvancedRuleBeforeASimpleOneCanBeSaved() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Editors OR Reviewers}] [{ALLOW view Authenticated}]" );

        Assertions.assertTrue( engine.getManager( PageManager.class ).getPureText( PAGE, -1 ).contains( "Text." ) );
        Assertions.assertFalse( can( anonymous(), "view" ) );
        Assertions.assertTrue( can( user( Users.CHARLIE ), "view" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.ALICE ), "edit" ) );
    }

    @Test
    public void testAdvancedAndSimpleRuleForTheSameActionAddUp() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Charlie}] [{ALLOW edit Editors AND Reviewers}]" );

        Assertions.assertTrue( can( user( Users.CHARLIE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.FRED ), "edit" ) );
        Assertions.assertFalse( can( user( Users.ALICE ), "edit" ) );
        Assertions.assertFalse( can( user( Users.BOB ), "edit" ) );
    }

    @Test
    public void testNotAndAnd() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Authenticated AND NOT Banned}]" );

        Assertions.assertTrue( can( user( Users.ALICE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.BOB ), "edit" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );
        Assertions.assertFalse( can( user( Users.BIFF ), "edit" ) );
        Assertions.assertFalse( can( anonymous(), "edit" ) );
    }

    /** A name that is only asserted, as in a cookie, must not satisfy a rule, as it does not satisfy a simple rule. */
    @Test
    public void testAssertedNameDoesNotSatisfyARule() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Alice OR Bob}]" );

        Assertions.assertFalse( can( WikiSessionTest.assertedSession( engine, Users.ALICE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.ALICE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.BOB ), "edit" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );
    }

    @Test
    public void testParenthesesChangeTheMeaning() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Editors AND (Reviewers OR Banned)}]" );

        Assertions.assertFalse( can( user( Users.ALICE ), "edit" ) );
        Assertions.assertTrue( can( user( Users.FRED ), "edit" ) );
        Assertions.assertTrue( can( user( Users.BIFF ), "edit" ) );
        Assertions.assertFalse( can( user( Users.BOB ), "edit" ) );
        Assertions.assertFalse( can( user( Users.CHARLIE ), "edit" ) );
    }

    @Test
    public void testPageWithoutAdvancedRulesIsAsBefore() throws Exception {
        engine.saveText( PAGE, "Text. [{ALLOW edit Alice, Bob}]" );

        Assertions.assertTrue( can( user( Users.ALICE ), "edit" ) );
        Assertions.assertFalse( can( anonymous(), "view" ) );
        Assertions.assertFalse( can( user( Users.FRED ), "edit" ) );
    }

}
