/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jspwiki.api.WikiException;
import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;

public class WysiwygEditingRendererTest extends TestCase
{
    protected TestEngine m_testEngine;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());
        m_testEngine = new TestEngine(props);
        super.setUp();

        m_testEngine.saveText( "WysiwygEditingRendererTest", "test page" );
        m_testEngine.saveText( "This Pagename Has Spaces", "This Pagename Has Spaces" );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "WysiwygEditingRendererTest" );
        TestEngine.deleteTestPage( "This Pagename Has Spaces" );
    }

    private String render(String s) throws IOException, WikiException
    {
        WikiPage dummyPage = m_testEngine.createPage("TestPage");
        WikiContext ctx = m_testEngine.getWikiContextFactory().newViewContext( dummyPage );

        StringReader in = new StringReader(s);

        JSPWikiMarkupParser p = new JSPWikiMarkupParser( ctx, in );
        WikiDocument d = p.parse();

        WysiwygEditingRenderer wer = new WysiwygEditingRenderer( ctx, d );

        return wer.getString();
    }

    public void testDefinedPageLink() throws Exception
    {
        String src = "[WysiwygEditingRendererTest]";
        assertEquals( "<a class=\"wikipage\" href=\"WysiwygEditingRendererTest\">WysiwygEditingRendererTest</a>", render(src) );

        src = "[WysiwygEditingRendererTest#Footnotes]";
        assertEquals( "<a class=\"wikipage\" href=\"WysiwygEditingRendererTest#Footnotes\">WysiwygEditingRendererTest#Footnotes</a>", render(src) );

        src = "[test page|WysiwygEditingRendererTest|class='notWikipageClass']";
        assertEquals( "<a class=\"notWikipageClass\" href=\"WysiwygEditingRendererTest\">test page</a>", render(src) );

        src = "[This Pagename Has Spaces]";
        assertEquals( "<a class=\"wikipage\" href=\"This Pagename Has Spaces\">This Pagename Has Spaces</a>", render(src) );
    }

    public void testUndefinedPageLink() throws Exception
    {
        String src = "[UndefinedPageLinkHere]";
        assertEquals( "<a class=\"createpage\" href=\"UndefinedPageLinkHere\">UndefinedPageLinkHere</a>", render(src) );

        src = "[UndefinedPageLinkHere#SomeSection]";
        assertEquals( "<a class=\"createpage\" href=\"UndefinedPageLinkHere\">UndefinedPageLinkHere#SomeSection</a>", render(src) );

        src = "[test page|UndefinedPageLinkHere|class='notEditpageClass']";
        assertEquals( "<a class=\"notEditpageClass\" href=\"UndefinedPageLinkHere\">test page</a>", render(src) );

        src = "[Non-existent Pagename with Spaces]";
        assertEquals( "<a class=\"createpage\" href=\"Non-existent Pagename with Spaces\">Non-existent Pagename with Spaces</a>", render(src) );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( WysiwygEditingRendererTest.class );

        return suite;
    }

}
