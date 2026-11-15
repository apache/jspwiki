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
package org.apache.wiki.search;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.wiki.HttpMockFactory;
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

import java.util.List;

import static org.apache.wiki.TestEngine.with;

/** The suggestions of page names are offered to everybody, so they must not tell about a page that the viewer may not view. */
public class PageNameSuggestionsViewPermissionTest {

    private TestEngine engine;
    private DefaultSearchManager.JSONSearch suggestions;

    @BeforeEach
    public void setUp() throws Exception {
        engine = TestEngine.build( with( "jspwiki.cache.enable", "false" ) );
        engine.saveText( "PnsPublic", "Everybody may read this." );
        engine.saveText( "PnsSecret", "[{ALLOW view Admin}]\nOnly the administrators may read this." );
        engine.saveText( "PnsJanne", "[{ALLOW view janne}]\nOnly janne may read this." );
        suggestions = ( ( DefaultSearchManager ) engine.getManager( SearchManager.class ) ).new JSONSearch();
    }

    @AfterEach
    public void tearDown() {
        engine.deleteTestPage( "PnsPublic" );
        engine.deleteTestPage( "PnsSecret" );
        engine.deleteTestPage( "PnsJanne" );
        TestEngine.emptyWorkDir();
    }

    @Test
    public void testAnonymousVisitorIsNotOfferedAPageThatItMayNotView() {
        final Context context = Wiki.context().create( engine, Wiki.contents().page( engine, "PnsPublic" ) );

        final List< String > names = suggestions.getSuggestions( "Pns", 20, context );

        Assertions.assertTrue( names.contains( "PnsPublic" ), names.toString() );
        Assertions.assertFalse( names.contains( "PnsSecret" ), names.toString() );
        Assertions.assertFalse( names.contains( "PnsJanne" ), names.toString() );
    }

    @Test
    public void testAuthorizedViewerIsStillOfferedTheirPage() throws Exception {
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.isAuthenticated() );
        final Context context = Wiki.context().create( engine, request, Wiki.contents().page( engine, "PnsPublic" ) );

        final List< String > names = suggestions.getSuggestions( "Pns", 20, context );

        Assertions.assertTrue( names.contains( "PnsJanne" ), names.toString() );
        Assertions.assertTrue( names.contains( "PnsPublic" ), names.toString() );
        Assertions.assertFalse( names.contains( "PnsSecret" ), names.toString() );
    }

}
