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
package org.apache.wiki.plugin;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.wiki.TestEngine.with;

/** The plugins that list pages are shown to everybody, so they must not tell about a page that the viewer may not view. */
public class ReferralPluginsViewPermissionTest {

    private static final String[] PAGES = { "RpvTarget", "RpvPublic", "RpvSecret", "RpvJanne", "RpvSecretOrphan", "RpvJanneOrphan" };

    private TestEngine engine;

    @BeforeEach
    public void setUp() throws Exception {
        engine = TestEngine.build( with( "jspwiki.cache.enable", "false" ) );
        engine.saveText( "RpvTarget", "The page that the other pages link to." );
        engine.saveText( "RpvSecret", "[{ALLOW view Admin}]\nOnly the administrators. It links to [RpvTarget], to [RpvPublic] and to [RpvUndefinedFromSecret]." );
        engine.saveText( "RpvJanne", "[{ALLOW view janne}]\nOnly janne. It links to [RpvTarget]." );
        engine.saveText( "RpvPublic", "Everybody. It links to [RpvSecret], to [RpvTarget] and to [RpvUndefinedPublic]." );
        engine.saveText( "RpvSecretOrphan", "[{ALLOW view Admin}]\nOnly the administrators. Nobody links to it." );
        engine.saveText( "RpvJanneOrphan", "[{ALLOW view janne}]\nOnly janne. Nobody links to it." );
    }

    @AfterEach
    public void tearDown() {
        for( final String page : PAGES ) {
            engine.deleteTestPage( page );
        }
        TestEngine.emptyWorkDir();
    }

    private Context anonymous() {
        return Wiki.context().create( engine, Wiki.contents().page( engine, "RpvPublic" ) );
    }

    private Context janne() throws Exception {
        final MockHttpServletRequest request = engine.newHttpRequest();
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.isAuthenticated() );
        return Wiki.context().create( engine, request, Wiki.contents().page( engine, "RpvPublic" ) );
    }

    private String run( final Context context, final String plugin ) throws Exception {
        return engine.getManager( PluginManager.class ).execute( context, "{INSERT org.apache.wiki.plugin." + plugin + "}" );
    }

    @Test
    public void testIndexPluginDoesNotListAProtectedPage() throws Exception {
        final String html = run( anonymous(), "IndexPlugin" );

        Assertions.assertTrue( html.contains( "RpvPublic" ), html );
        Assertions.assertFalse( html.contains( "RpvSecret" ), html );
        Assertions.assertFalse( html.contains( "RpvJanne" ), html );
    }

    @Test
    public void testReferringPagesPluginDoesNotListAProtectedPage() throws Exception {
        final String html = run( anonymous(), "ReferringPagesPlugin page=RpvTarget" );

        Assertions.assertTrue( html.contains( "RpvPublic" ), html );
        Assertions.assertFalse( html.contains( "RpvSecret" ), html );
        Assertions.assertFalse( html.contains( "RpvJanne" ), html );
    }

    @Test
    public void testReferredPagesPluginDoesNotShowTheLinksOfAProtectedPage() throws Exception {
        final String ofTheProtectedPage = run( anonymous(), "ReferredPagesPlugin page=RpvSecret" );
        final String ofThePublicPage = run( anonymous(), "ReferredPagesPlugin page=RpvPublic" );

        Assertions.assertFalse( ofTheProtectedPage.contains( "RpvTarget" ), ofTheProtectedPage );
        Assertions.assertFalse( ofTheProtectedPage.contains( "RpvPublic" ), ofTheProtectedPage );
        Assertions.assertTrue( ofThePublicPage.contains( "RpvTarget" ), ofThePublicPage );
        Assertions.assertFalse( ofThePublicPage.contains( "RpvSecret" ), ofThePublicPage );
    }

    @Test
    public void testUnusedPagesPluginDoesNotListAProtectedPage() throws Exception {
        final String html = run( anonymous(), "UnusedPagesPlugin" );

        Assertions.assertFalse( html.contains( "RpvSecretOrphan" ), html );
        Assertions.assertFalse( html.contains( "RpvJanneOrphan" ), html );
    }

    @Test
    public void testUndefinedPagesPluginDoesNotShowALinkWrittenOnlyInAProtectedPage() throws Exception {
        final String html = run( anonymous(), "UndefinedPagesPlugin" );

        Assertions.assertTrue( html.contains( "RpvUndefinedPublic" ), html );
        Assertions.assertFalse( html.contains( "RpvUndefinedFromSecret" ), html );
    }

    @Test
    public void testReferringUndefinedPagesPluginDoesNotListAProtectedPage() throws Exception {
        final String html = run( anonymous(), "ReferringUndefinedPagesPlugin" );

        Assertions.assertTrue( html.contains( "RpvPublic" ), html );
        Assertions.assertFalse( html.contains( "RpvSecret" ), html );
    }

    @Test
    public void testAuthorizedViewerStillSeesTheirPage() throws Exception {
        final Context context = janne();

        Assertions.assertTrue( run( context, "IndexPlugin" ).contains( "RpvJanne" ) );
        final String referring = run( context, "ReferringPagesPlugin page=RpvTarget" );
        Assertions.assertTrue( referring.contains( "RpvJanne" ), referring );
        Assertions.assertFalse( referring.contains( "RpvSecret" ), referring );
        Assertions.assertTrue( run( context, "ReferredPagesPlugin page=RpvJanne" ).contains( "RpvTarget" ) );
        final String unused = run( context, "UnusedPagesPlugin" );
        Assertions.assertTrue( unused.contains( "RpvJanneOrphan" ), unused );
        Assertions.assertFalse( unused.contains( "RpvSecretOrphan" ), unused );
    }

}
