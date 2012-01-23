package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
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

    private String render(String s) throws IOException
    {
        WikiPage dummyPage = new WikiPage(m_testEngine,"TestPage");
        WikiContext ctx = new WikiContext(m_testEngine,dummyPage);

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
