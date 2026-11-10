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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class LanguagePluginTest {

    static TestEngine testEngine = TestEngine.build();

    @AfterEach
    void tearDown() throws Exception {
        testEngine.getManager( PageManager.class ).deletePage( "Test" );
    }

    /** Renders the text as a page that is requested with the given {@code Accept-Language} header, which may be {@code null}. */
    String render( final String text, final String acceptLanguage ) throws WikiException {
        testEngine.saveText( "Test", text );
        final Page page = testEngine.getManager( PageManager.class ).getPage( "Test", PageProvider.LATEST_VERSION );
        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( acceptLanguage ).when( request ).getHeader( "Accept-Language" );
        final Context context = Wiki.context().create( testEngine, request, page );
        return testEngine.getManager( RenderingManager.class ).getHTML( context, page );
    }

    static final String BLOCKS = "[{LanguagePlugin\n\n:de:Willkommen::  :en:Welcome::  :else:Fallback::}]";

    @Test
    void testChoosesTheLanguageOfTheVisitor() throws WikiException {
        Assertions.assertEquals( "Willkommen\n", render( BLOCKS, "de" ) );
        Assertions.assertEquals( "Welcome\n", render( BLOCKS, "en" ) );
    }

    @Test
    void testTheRegionIsNotCompared() throws WikiException {
        Assertions.assertEquals( "Willkommen\n", render( BLOCKS, "de-AT" ) );
    }

    @Test
    void testFollowsTheQualityValuesNotTheOrder() throws WikiException {
        Assertions.assertEquals( "Willkommen\n", render( BLOCKS, "en;q=0.3, de;q=0.9" ) );
        Assertions.assertEquals( "Welcome\n", render( BLOCKS, "de;q=0, en;q=0.5" ) );
    }

    @Test
    void testFallsBackToTheNextLanguageThatHasABlock() throws WikiException {
        Assertions.assertEquals( "Welcome\n", render( BLOCKS, "fr, en;q=0.8" ) );
    }

    @Test
    void testElseBlockWhenNoLanguageMatchesOrNoHeader() throws WikiException {
        Assertions.assertEquals( "Fallback\n", render( BLOCKS, "fr, es" ) );
        Assertions.assertEquals( "Fallback\n", render( BLOCKS, "*" ) );
        Assertions.assertEquals( "Fallback\n", render( BLOCKS, null ) );
    }

    @Test
    void testNothingWhenNoBlockMatchesAndThereIsNoElseBlock() throws WikiException {
        final String res = render( "[{LanguagePlugin\n\n:de:Willkommen::}]", "fr" );
        Assertions.assertFalse( res.contains( "Willkommen" ) );
    }

    @Test
    void testNoBodyShowsNothing() throws WikiException {
        final String res = render( "[{LanguagePlugin}]", "de" );
        Assertions.assertFalse( res.toLowerCase().contains( "error" ), res );
    }

    @Test
    void testTheChosenBlockIsRenderedAsWikiText() throws WikiException {
        final String res = render( "[{LanguagePlugin\n\n:en:__bold__::}]", "en" );
        Assertions.assertTrue( res.contains( "<b>bold</b>" ), res );
    }

    @Test
    void testHtmlInABlockIsEscaped() throws WikiException {
        final String res = render( "[{LanguagePlugin\n\n:en:<script>alert(1)</script>::}]", "en" );
        Assertions.assertFalse( res.contains( "<script>" ), res );
    }

    @Test
    void testParse() {
        Assertions.assertEquals( List.of( "de", "en" ), LanguagePlugin.parse( "en;q=0.4, de-AT, *;q=0.1, fr;q=0, ;q=1" ) );
        Assertions.assertEquals( List.of(), LanguagePlugin.parse( null ) );
        Assertions.assertEquals( List.of( "en" ), LanguagePlugin.parse( "en;q=abc, en" ) );
    }

}
