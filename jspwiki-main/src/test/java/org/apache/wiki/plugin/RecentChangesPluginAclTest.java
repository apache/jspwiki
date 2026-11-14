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
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.wiki.TestEngine.with;

/** The list of recent changes is shown to everybody, so it must not tell about a page that the viewer may not view. */
public class RecentChangesPluginAclTest {

    private static final String PLUGIN = "{INSERT org.apache.wiki.plugin.RecentChangesPlugin}";

    private TestEngine engine;

    @BeforeEach
    public void setUp() throws Exception {
        engine = TestEngine.build( with( "jspwiki.cache.enable", "false" ) );
        save( "RcAclPublicPage", "Everybody may read this.", "public note" );
        save( "RcAclSecretPage", "[{ALLOW view Admin}]\nOnly the administrators may read this.", "SECRET-CHANGE-NOTE" );
        save( "RcAclJannePage", "[{ALLOW view janne}]\nOnly janne may read this.", "JANNE-CHANGE-NOTE" );
    }

    @AfterEach
    public void tearDown() {
        engine.deleteTestPage( "RcAclPublicPage" );
        engine.deleteTestPage( "RcAclSecretPage" );
        engine.deleteTestPage( "RcAclJannePage" );
        TestEngine.emptyWorkDir();
    }

    private void save( final String name, final String text, final String changeNote ) throws Exception {
        final Page page = Wiki.contents().page( engine, name );
        page.setAttribute( Page.CHANGENOTE, changeNote );
        page.setAuthor( "SecretAuthor" );
        final Context context = Wiki.context().create( engine, page );
        engine.getManager( PageManager.class ).saveText( context, text );
    }

    @Test
    public void testAnonymousVisitorDoesNotSeeAPageThatItMayNotView() throws Exception {
        final Context context = Wiki.context().create( engine, Wiki.contents().page( engine, "RcAclPublicPage" ) );

        final String html = engine.getManager( PluginManager.class ).execute( context, PLUGIN );

        Assertions.assertTrue( html.contains( "RcAclPublicPage" ), "the page that everybody may view is listed: " + html );
        Assertions.assertFalse( html.contains( "RcAclSecretPage" ), "the name of the protected page is not listed: " + html );
        Assertions.assertFalse( html.contains( "SECRET-CHANGE-NOTE" ), "the change note of the protected page is not listed: " + html );
    }

    @Test
    public void testAuthorizedViewerStillSeesTheirPage() throws Exception {
        final MockHttpServletRequest request = engine.newHttpRequest();
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.isAuthenticated() );
        final Context context = Wiki.context().create( engine, request, Wiki.contents().page( engine, "RcAclPublicPage" ) );

        final String html = engine.getManager( PluginManager.class ).execute( context, PLUGIN );

        Assertions.assertTrue( html.contains( "RcAclJannePage" ), "the page that the viewer may view is listed: " + html );
        Assertions.assertTrue( html.contains( "JANNE-CHANGE-NOTE" ), "its change note is listed: " + html );
        Assertions.assertFalse( html.contains( "RcAclSecretPage" ), "the page of the administrators is not listed: " + html );
    }

}
