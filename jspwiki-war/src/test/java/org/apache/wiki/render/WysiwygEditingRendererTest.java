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

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WysiwygEditingRendererTest
{
    protected TestEngine testEngine;

    @Before
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        testEngine = new TestEngine(props);

        testEngine.saveText( "WysiwygEditingRendererTest", "test page" );
        testEngine.saveText( "This Pagename Has Spaces", "This Pagename Has Spaces" );
    }

    @After
    public void tearDown()
    {
        testEngine.deleteTestPage( "WysiwygEditingRendererTest" );
        testEngine.deleteTestPage( "This Pagename Has Spaces" );
    }

    private String render(String s) throws IOException
    {
        WikiPage dummyPage = new WikiPage(testEngine,"TestPage");
        WikiContext ctx = new WikiContext(testEngine,dummyPage);

        StringReader in = new StringReader(s);

        JSPWikiMarkupParser p = new JSPWikiMarkupParser( ctx, in );
        WikiDocument d = p.parse();

        WysiwygEditingRenderer wer = new WysiwygEditingRenderer( ctx, d );

        return wer.getString();
    }

    @Test
    public void testDefinedPageLink() throws Exception
    {
        String src = "[WysiwygEditingRendererTest]";
        Assert.assertEquals( "<a class=\"wikipage\" href=\"WysiwygEditingRendererTest\">WysiwygEditingRendererTest</a>", render(src) );

        src = "[WysiwygEditingRendererTest#Footnotes]";
        Assert.assertEquals( "<a class=\"wikipage\" href=\"WysiwygEditingRendererTest#Footnotes\">WysiwygEditingRendererTest#Footnotes</a>", render(src) );

        src = "[test page|WysiwygEditingRendererTest|class='notWikipageClass']";
        Assert.assertEquals( "<a class=\"notWikipageClass\" href=\"WysiwygEditingRendererTest\">test page</a>", render(src) );

        src = "[This Pagename Has Spaces]";
        Assert.assertEquals( "<a class=\"wikipage\" href=\"This Pagename Has Spaces\">This Pagename Has Spaces</a>", render(src) );
    }

    @Test
    public void testUndefinedPageLink() throws Exception
    {
        String src = "[UndefinedPageLinkHere]";
        Assert.assertEquals( "<a class=\"createpage\" href=\"UndefinedPageLinkHere\">UndefinedPageLinkHere</a>", render(src) );

        src = "[UndefinedPageLinkHere#SomeSection]";
        Assert.assertEquals( "<a class=\"createpage\" href=\"UndefinedPageLinkHere\">UndefinedPageLinkHere#SomeSection</a>", render(src) );

        src = "[test page|UndefinedPageLinkHere|class='notEditpageClass']";
        Assert.assertEquals( "<a class=\"notEditpageClass\" href=\"UndefinedPageLinkHere\">test page</a>", render(src) );

        src = "[Non-existent Pagename with Spaces]";
        Assert.assertEquals( "<a class=\"createpage\" href=\"Non-existent Pagename with Spaces\">Non-existent Pagename with Spaces</a>", render(src) );
    }

}
