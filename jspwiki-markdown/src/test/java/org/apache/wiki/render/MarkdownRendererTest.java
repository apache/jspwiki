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
package org.apache.wiki.render;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.parser.markdown.MarkdownParser;
import org.apache.wiki.render.markdown.MarkdownRenderer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.ehcache.CacheManager;


public class MarkdownRendererTest {

    Properties props = TestEngine.getTestProperties();
    List<String> created = new ArrayList<>();

    static final String PAGE_NAME = "testpage";

    TestEngine testEngine;

    @Test
    public void testMarkupSimpleMarkdown() throws Exception {
        String src = "This should be a **bold**";

        Assert.assertEquals( "<p>This should be a <strong>bold</strong></p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionSelfViewLink() throws Exception {
    	newPage( "MarkupExtensionSelfViewLink" );
        String src = "This should be a [MarkupExtensionSelfViewLink]()";

        Assert.assertEquals( "<p>This should be a <a href=\"/test/Wiki.jsp?page=MarkupExtensionSelfViewLink\" class=\"wikipage\">MarkupExtensionSelfViewLink</a></p>\n",
                             translate( src ) );
    }

    @Test
    public void testMarkupExtensionSelfEditLink() throws Exception {
        String src = "This should be a [self<->link]()";

        Assert.assertEquals( "<p>This should be a <a href=\"/test/Edit.jsp?page=self%3C-%3Elink\" title=\"Create &quot;self&lt;-&gt;link&quot;\" class=\"createpage\">self&lt;-&gt;link</a></p>\n",
                             translate( src ) );
    }

    @Test
    public void testMarkupExtensionExternalLink() throws Exception {
        testEngine.getWikiProperties().setProperty( "jspwiki.translatorReader.useOutlinkImage", "true" );
        String src = "This should be an [external link](https://jspwiki.apache.org)";

        Assert.assertEquals( "<p>This should be an <a href=\"https://jspwiki.apache.org\" class=\"external\">external link</a><img class=\"outlink\" alt=\"\" src=\"/test/images/out.png\" /></p>\n",
                             translate( src ) );
        testEngine.getWikiProperties().remove( "jspwiki.translatorReader.useOutlinkImage" );
    }

    @Test
    public void testMarkupExtensionInterWikiLink() throws Exception {
        String src = "This should be an [interwiki link](JSPWiki:About)";

        Assert.assertEquals( "<p>This should be an <a href=\"http://jspwiki-wiki.apache.org/Wiki.jsp?page=About\" class=\"interwiki\">interwiki link</a></p>\n",
                             translate( src ) );
    }

    @Test
    public void testMarkupExtensionWrongInterWikiLink() throws Exception {
        String src = "This should be an [interwiki link](JSPWiko:About)";

        Assert.assertEquals( "<p>This should be an <span class=\"error\">No InterWiki reference defined in properties for Wiki called \"JSPWiko\"!</span></p>\n",
                             translate( src ) );
    }

    @Test
    public void testMarkupExtensionACL() throws Exception {
        String src = "[{ALLOW view PerryMason}]() This should be visible if the ACL allows you to see it";
        // text is seen because although ACL is added to the page, it is not applied while parsing / rendering
        Assert.assertEquals( "<p> This should be visible if the ACL allows you to see it</p>\n", translate( src ) );
        // in any case, we also check that the created wikipage has the ACL added
        Assert.assertEquals( "  user = PerryMason: ((\"org.apache.wiki.auth.permissions.PagePermission\",\"JSPWiki:testpage\",\"view\"))\n",
        		             testEngine.getPage( PAGE_NAME ).getAcl().toString() );
    }

    @Test
    public void testMarkupExtensionMetadata() throws Exception {
        String src = "[{SET Perry='Mason'}]() Some text after setting metadata";
        Assert.assertEquals( "<p> Some text after setting metadata</p>\n", translate( src ) );
        Assert.assertEquals( "Mason", testEngine.getPage( PAGE_NAME ).getAttribute( "Perry" ) );
    }

    @Test
    public void testMarkupExtensionPlugin() throws Exception {
        String src = "[{SamplePlugin text=test}]()";
        Assert.assertEquals( "<p>test</p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionTOCPluginGetsSubstitutedWithMDTocExtension() throws Exception {
        String src = "[{TableOfContents}]()\n" +
                     "# Header 1\n" +
                     "## Header 2\n" +
                     "## Header 2\n";
        Assert.assertEquals( "<p><div class=\"toc\">\n" +
                             "<div class=\"collapsebox\">\n" +
                             "<h4 id=\"section-TOC\">Table of Contents</h4>\n" +
                             "<ul>\n" +
                             "<li><a href=\"#header-1\">Header 1</a>\n" +
                             "<ul>\n" +
                             "<li><a href=\"#header-2\">Header 2</a></li>\n" +
                             "<li><a href=\"#header-2-1\">Header 2</a></li>\n" +
                             "</ul>\n" +
                             "</li>\n" +
                             "</ul>\n" +
                             "</div>\n" +
                             "</div>\n" +
                             "</p>\n" +
                             "<h1 id=\"header-1\">Header 1</h1>\n" +
                             "<h2 id=\"header-2\">Header 2</h2>\n" +
                             "<h2 id=\"header-2-1\">Header 2</h2>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionNonExistentPlugin() throws Exception {
        String src = "[{PampleSlugin text=test}]()";
        Assert.assertEquals( "<p><span class=\"error\">JSPWiki : testpage - Plugin insertion failed: Could not find plugin PampleSlugin</span></p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionVariable0() throws Exception {
        String src = "Some text with some pre-set variable: [{$applicationname}]()";
        Assert.assertEquals( "<p>Some text with some pre-set variable: JSPWiki</p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionVariable1() throws Exception {
        String src = "[{SET Perry='Mason'}]() Some text after setting some metadata: [{$Perry}]()";
        Assert.assertEquals( "<p> Some text after setting some metadata: Mason</p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionFootnote0() throws Exception {
        String src = "Footnote[1]()";
        Assert.assertEquals( "<p>Footnote<a href=\"#ref-testpage-1\" class=\"footnoteref\">[1]</a></p>\n", translate( src ) );
    }

    @Test
    public void testMarkupExtensionFootnoteMD() throws Exception {
        String src = "text [^footnote] embedded.\n\n" +
        		     "[^footnote]: footnote text\n" +
        		     "with continuation";
        Assert.assertEquals( "<p>text <sup id=\"fnref-1\"><a class=\"footnoteref\" href=\"#fn-1\">1</a></sup> embedded.</p>\n" +
        		             "<div class=\"footnotes\">\n" +
        		             "<hr />\n" +
        		             "<ol>\n" +
        		             "<li id=\"fn-1\">\n" +
        		             "<p>footnote text\n" +
        		             "with continuation</p>\n" +
        		             "<a href=\"#fnref-1\" class=\"footnote-backref\">&#8617;</a>\n" +
        		             "</li>\n" +
        		             "</ol>\n" +
        		             "</div>\n", translate( src ) );
    }

    @Test
    public void testAttachmentLink() throws Exception {
        String src = "This should be an [attachment link](Test/TestAtt.txt)";
        newPage( "Test" );

        Attachment att = new Attachment( testEngine, "Test", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        testEngine.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        Assert.assertEquals( "<p>This should be an <a href=\"/test/attach/Test/TestAtt.txt\" class=\"attachment\">attachment link</a>" +
                             "<a href=\"/test/PageInfo.jsp?page=Test/TestAtt.txt\" class=\"infolink\">" +
                               "<img src=\"/test/images/attachment_small.png\" border=\"0\" alt=\"(info)\" />" +
                             "</a></p>\n",
                translate( src ) );
    }

    @Test
    public void testInlineImages() throws Exception {
        String src = "Link [test](http://www.ecyrd.com/test.png)";

        Assert.assertEquals( "<p>Link <img class=\"inline\" src=\"http://www.ecyrd.com/test.png\" alt=\"test\" /></p>\n", translate( src ) );
    }

    @Test
    public void testInlineImages2() throws Exception {
        String src = "Link [test](http://www.ecyrd.com/test.ppm)";

        Assert.assertEquals( "<p>Link <a href=\"http://www.ecyrd.com/test.ppm\" class=\"external\">test</a></p>\n", translate( src ) );
    }

    @Test
    public void testInlineImages3() throws Exception {
        String src = "Link [test](http://images.com/testi)";

        Assert.assertEquals( "<p>Link <img class=\"inline\" src=\"http://images.com/testi\" alt=\"test\" /></p>\n", translate( src ) );
    }

    @Test
    public void testInlineImages4() throws Exception {
        String src = "Link [test](http://foobar.jpg)";

        Assert.assertEquals( "<p>Link <img class=\"inline\" src=\"http://foobar.jpg\" alt=\"test\" /></p>\n", translate( src ) );
    }

    // No link text should be just embedded link.
    @Test
    public void testInlineImagesLink2() throws Exception {
        String src = "Link [http://foobar.jpg]()";

        Assert.assertEquals( "<p>Link <img class=\"inline\" src=\"http://foobar.jpg\" alt=\"http://foobar.jpg\" /></p>\n", translate( src ) );
    }

    @Test
    public void testInlineImagesLink() throws Exception {
        String src = "Link [http://link.to/](http://foobar.jpg)";

        Assert.assertEquals( "<p>Link <a href=\"http://link.to/\" class=\"external\"><img class=\"inline\" src=\"http://foobar.jpg\" alt=\"http://link.to/\" /></a></p>\n",
                             translate( src ) );
    }

    @Test
    public void testInlineImagesLink3() throws Exception {
        String src = "Link [SandBox](http://foobar.jpg)";

        newPage( "SandBox" );

        Assert.assertEquals( "<p>Link <a href=\"/test/Wiki.jsp?page=SandBox\" class=\"wikipage\"><img class=\"inline\" src=\"http://foobar.jpg\" alt=\"SandBox\" /></a></p>\n",
                             translate( src ) );
    }

    @Test
    public void testHeadersWithSameNameGetIdWithCounter() throws Exception {
        String src = "### Awesome H3\n" +
                     "### Awesome H3";

        Assert.assertEquals( "<h3 id=\"awesome-h3\">Awesome H3</h3>\n" +
                             "<h3 id=\"awesome-h3-1\">Awesome H3</h3>\n",
                             translate( src ) );
    }

    @Before
    public void setUp() throws Exception {
        CacheManager.getInstance().removeAllCaches();
        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true" );
        props.setProperty( "jspwiki.fileSystemProvider.pageDir", "./target/md-pageDir" );
        props.setProperty( "jspwiki.renderingManager.markupParser", MarkdownParser.class.getName() );
        props.setProperty( "jspwiki.renderingManager.renderer", MarkdownRenderer.class.getName() );
        testEngine = new TestEngine( props );
    }

    @After
    public void tearDown() {
        for( String name : created ) {
            testEngine.deleteTestPage(name);
            TestEngine.deleteAttachments(name);
        }

        created.clear();
    }

    String translate( final String src ) throws Exception {
        return translate( new WikiPage( testEngine, PAGE_NAME ), src );
    }

    String translate( final WikiEngine e, final String src) throws Exception {
        return translate( e, new WikiPage( testEngine, PAGE_NAME ), src );
    }

    String translate( final WikiPage p, final String src ) throws Exception {
        return translate( testEngine, p, src );
    }

    String translate( final WikiEngine e, final WikiPage p, final String src ) throws Exception {
        WikiContext context = new WikiContext( e, testEngine.newHttpRequest(), p );
        MarkdownParser tr = new MarkdownParser( context, new BufferedReader( new StringReader( src ) ) );
        MarkdownRenderer conv = new MarkdownRenderer( context, tr.parse() );
        newPage( p.getName(), src );

        return conv.getString();
    }

    void newPage( final String name ) throws WikiException {
        newPage( name, "<test>" );
    }

    void newPage( final String name, final String text ) throws WikiException {
        testEngine.saveText( name, text );
        created.add( name );
    }

}
