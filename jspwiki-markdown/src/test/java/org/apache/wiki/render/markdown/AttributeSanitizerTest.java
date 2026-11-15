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
package org.apache.wiki.render.markdown;

import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.parser.markdown.MarkdownParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;


/**
 * Regression tests for the markdown attribute sanitizer: author-written flexmark attribute lists must not be able
 * to attach event handlers, styles or script-scheme URLs to rendered elements.
 */
public class AttributeSanitizerTest {

    static final String PAGE_NAME = "attributesanitizertestpage";

    TestEngine testEngine = TestEngine.build( TestEngine.with( "jspwiki.fileSystemProvider.pageDir", "./target/md-sanitizer-pageDir" ),
                                              TestEngine.with( "jspwiki.renderingManager.markupParser", MarkdownParser.class.getName() ),
                                              TestEngine.with( "jspwiki.renderingManager.renderer", MarkdownRenderer.class.getName() ) );

    @Test
    public void testEventHandlerAttributeIsStripped() throws Exception {
        final String html = translate( "Click me{onmouseover=alert(document.cookie)}" );
        Assertions.assertFalse( html.contains( "onmouseover" ), html );
        Assertions.assertFalse( html.contains( "document.cookie" ), html );
    }

    @Test
    public void testEventHandlerCaseVariationIsStripped() throws Exception {
        final String html = translate( "Click me{ONCLICK=alert(1)}" );
        Assertions.assertFalse( html.toLowerCase().contains( "onclick" ), html );
    }

    @Test
    public void testStyleAttributeIsStripped() throws Exception {
        final String html = translate( "Overlay{style=position:fixed;top:0;left:0;width:100vw;height:100vh}" );
        Assertions.assertFalse( html.contains( "style=" ), html );
        Assertions.assertFalse( html.contains( "position:fixed" ), html );
    }

    @Test
    public void testJavascriptHrefIsStripped() throws Exception {
        final String html = translate( "[link](x){href=javascript:alert(1)}" );
        Assertions.assertFalse( html.contains( "javascript:" ), html );
    }

    @Test
    public void testBenignAttributesSurvive() throws Exception {
        final String html = translate( "Text{#anchor .highlight}" );
        Assertions.assertTrue( html.contains( "anchor" ), html );
        Assertions.assertTrue( html.contains( "highlight" ), html );
    }

    String translate( final String src ) throws Exception {
        final Page page = Wiki.contents().page( testEngine, PAGE_NAME );
        final Context context = Wiki.context().create( testEngine, HttpMockFactory.createHttpRequest(), page );
        final MarkdownParser tr = new MarkdownParser( context, new BufferedReader( new StringReader( src ) ) );
        final MarkdownRenderer conv = new MarkdownRenderer( context, tr.parse() );
        return conv.getString();
    }

}
