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
package org.apache.wiki.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.wiki.LinkCollector;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.render.XHTMLRenderer;
import org.apache.wiki.stress.Benchmark;
import org.apache.wiki.util.TextUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;

public class JSPWikiMarkupParserTest extends TestCase
{
    Properties props = TestEngine.getTestProperties();
    Vector<String>     created = new Vector<String>();

    static final String PAGE_NAME = "testpage";

    TestEngine testEngine;


    public JSPWikiMarkupParserTest( String s )
    {
        super( s );
    }

    public void setUp()
    throws Exception
    {
        CacheManager.getInstance().removeAllCaches();

        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true" );
        testEngine = new TestEngine( props );
    }

    public void tearDown()
    {
        deleteCreatedPages();
    }

    private void newPage( String name )
        throws WikiException
    {
        testEngine.saveText( name, "<test>" );

        created.addElement( name );
    }

    private void deleteCreatedPages()
    {
        for( Iterator< String > i = created.iterator(); i.hasNext(); )
        {
            String name = i.next();

            testEngine.deleteTestPage(name);
            TestEngine.deleteAttachments(name);
        }

        created.clear();
    }

    private String translate( String src )
    throws IOException,
            NoRequiredPropertyException,
            ServletException
    {
        return translate( new WikiPage(testEngine, PAGE_NAME), src );
    }

    private String translate( WikiEngine e, String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        return translate( e, new WikiPage(testEngine, PAGE_NAME), src );
    }


    private String translate( WikiPage p, String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        return translate( testEngine, p, src );
    }

    private String translate( WikiEngine e, WikiPage p, String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = new WikiContext( e, testEngine.newHttpRequest(), p );
        JSPWikiMarkupParser tr = new JSPWikiMarkupParser( context,
                                                          new BufferedReader( new StringReader(src)) );

        XHTMLRenderer conv = new XHTMLRenderer( context, tr.parse() );

        return conv.getString();
    }

    private String translate_nofollow( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException,
               WikiException
    {
        props = TestEngine.getTestProperties();

        props.setProperty( "jspwiki.translatorReader.useRelNofollow", "true" );
        TestEngine testEngine2 = new TestEngine( props );

        WikiContext context = new WikiContext( testEngine2,
                                               new WikiPage(testEngine2, PAGE_NAME) );
        JSPWikiMarkupParser r = new JSPWikiMarkupParser( context,
                                                         new BufferedReader( new StringReader(src)) );

        XHTMLRenderer conv = new XHTMLRenderer( context, r.parse() );

        return conv.getString();
    }

    public void testHyperlinks2()
    throws Exception
    {
        newPage("Hyperlink");

        String src = "This should be a [hyperlink]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Hyperlink\">hyperlink</a>",
                      translate(src) );
    }

    public void testHyperlinks3()
    throws Exception
    {
        newPage("HyperlinkToo");

        String src = "This should be a [hyperlink too]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperlinkToo\">hyperlink too</a>",
                      translate(src) );
    }

    public void testHyperlinks4()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [HyperLink]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>",
                      translate(src) );
    }

    public void testHyperlinks5()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [here|HyperLink]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">here</a>",
                      translate(src) );
    }

    public void testHyperlinksNamed1()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [here|HyperLink#heading]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink#section-HyperLink-Heading\">here</a>",
                      translate(src) );
    }

    public void testHyperlinksNamed2()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [HyperLink#heading]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink#section-HyperLink-Heading\">HyperLink#heading</a>",
                      translate(src) );
    }

    public void testHyperlinksNamed3()
    throws Exception
    {
        newPage("HyperLink");

        String src = "!Heading Too\r\nThis should be a [HyperLink#heading too]";

        assertEquals( "<h4 id=\"section-testpage-HeadingToo\">Heading Too<a class=\"hashlink\" href=\"#section-testpage-HeadingToo\">#</a></h4>\nThis should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink#section-HyperLink-HeadingToo\">HyperLink#heading too</a>",
                      translate(src) );
    }

    // test hyperlink to a section with non-ASCII character in it
    public void testHyperlinksNamed4()
            throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [HyperLink#headingwithnonASCIIZoltán]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink#section-HyperLink-HeadingwithnonASCIIZolt_E1n\">HyperLink#headingwithnonASCIIZoltán</a>",
                translate(src) );
    }

    //
    //  Testing CamelCase hyperlinks
    //

    public void testHyperLinks6()
    throws Exception
    {
        newPage("DiscussionAboutWiki");
        newPage("WikiMarkupDevelopment");

        String src = "[DiscussionAboutWiki] [WikiMarkupDevelopment].";

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=DiscussionAboutWiki\">DiscussionAboutWiki</a> <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=WikiMarkupDevelopment\">WikiMarkupDevelopment</a>.",
                      translate(src) );
    }

    public void testHyperlinksCC()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a HyperLink.";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>.",
                      translate(src) );
    }

    public void testHyperlinksCCNonExistant()
    throws Exception
    {
        String src = "This should be a HyperLink.";

        assertEquals( "This should be a <a class=\"createpage\" href=\"/test/Edit.jsp?page=HyperLink\" title=\"Create &quot;HyperLink&quot;\">HyperLink</a>.",
                      translate(src) );
    }

    /**
     *  Check if the CC hyperlink translator gets confused with
     *  unorthodox bracketed links.
     */

    public void testHyperlinksCC2()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [  HyperLink  ].";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">  HyperLink  </a>.",
                      translate(src) );
    }

    public void testHyperlinksCC3()
    throws Exception
    {
        String src = "This should be a nonHyperLink.";

        assertEquals( "This should be a nonHyperLink.",
                      translate(src) );
    }

    /** Two links on same line. */


    public void testHyperlinksCC4()
    throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "This should be a HyperLink, and ThisToo.";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>, and <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ThisToo\">ThisToo</a>.",
                      translate(src) );
    }


    /** Two mixed links on same line. */

    public void testHyperlinksCC5()
    throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "This should be a [HyperLink], and ThisToo.";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>, and <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ThisToo\">ThisToo</a>.",
                      translate(src) );
    }

    /** Closing tags only. */

    public void testHyperlinksCC6()
    throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "] This ] should be a HyperLink], and ThisToo.";

        assertEquals( "] This ] should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>], and <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ThisToo\">ThisToo</a>.",
                      translate(src) );
    }

    /** First and last words on line. */
    public void testHyperlinksCCFirstAndLast()
    throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "HyperLink, and ThisToo";

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>, and <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ThisToo\">ThisToo</a>",
                      translate(src) );
    }

    /** Hyperlinks inside URIs. */

    public void testHyperlinksCCURLs()
    throws Exception
    {
        String src = "http://www.foo.bar/ANewHope/";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "<a class=\"external\" href=\"http://www.foo.bar/ANewHope/\">http://www.foo.bar/ANewHope/</a>",
                      translate(src) );
    }

    /** Hyperlinks inside URIs. */

    public void testHyperlinksCCURLs2()
    throws Exception
    {
        String src = "mailto:foo@bar.com";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "<a class=\"external\" href=\"mailto:foo@bar.com\">mailto:foo@bar.com</a>",
                      translate(src) );
    }

    /** Hyperlinks inside URIs. */

    public void testHyperlinksCCURLs3()
    throws Exception
    {
        String src = "This should be a link: http://www.foo.bar/ANewHope/.  Is it?";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "This should be a link: <a class=\"external\" href=\"http://www.foo.bar/ANewHope/\">http://www.foo.bar/ANewHope/</a>.  Is it?",
                      translate(src) );
    }

    /** Hyperlinks in brackets. */

    public void testHyperlinksCCURLs4()
    throws Exception
    {
        String src = "This should be a link: (http://www.foo.bar/ANewHope/)  Is it?";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "This should be a link: (<a class=\"external\" href=\"http://www.foo.bar/ANewHope/\">http://www.foo.bar/ANewHope/</a>)  Is it?",
                      translate(src) );
    }

    /** Hyperlinks end line. */

    public void testHyperlinksCCURLs5()
    throws Exception
    {
        String src = "This should be a link: http://www.foo.bar/ANewHope/\nIs it?";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "This should be a link: <a class=\"external\" href=\"http://www.foo.bar/ANewHope/\">http://www.foo.bar/ANewHope/</a>\nIs it?",
                      translate(src) );
    }

    /** Hyperlinks with odd chars. */

    public void testHyperlinksCCURLs6()
    throws Exception
    {
        String src = "This should not be a link: http://''some.server''/wiki//test/Wiki.jsp\nIs it?";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "This should not be a link: http://<i>some.server</i>/wiki//test/Wiki.jsp\nIs it?",
                      translate(src) );
    }


    public void testHyperlinksCCURLs7()
    throws Exception
    {
        String src = "http://www.foo.bar/ANewHope?q=foobar&gobble=bobble+gnoo";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "<a class=\"external\" href=\"http://www.foo.bar/ANewHope?q=foobar&amp;gobble=bobble+gnoo\">http://www.foo.bar/ANewHope?q=foobar&amp;gobble=bobble+gnoo</a>",
                      translate(src) );
    }

    public void testHyperlinksCCURLs8()
    throws Exception
    {
        String src = "http://www.foo.bar/~ANewHope/";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "<a class=\"external\" href=\"http://www.foo.bar/~ANewHope/\">http://www.foo.bar/~ANewHope/</a>",
                      translate(src) );
    }
    
    public void testHyperlinksCCURLs9()
    throws Exception
    {
        String src = "http://www.foo.bar/%7EANewHope/";

        // System.out.println( "EX:"+translate(src) );
        assertEquals( "<a class=\"external\" href=\"http://www.foo.bar/%7EANewHope/\">http://www.foo.bar/%7EANewHope/</a>",
                      translate(src) );
    }

    public void testHyperlinksCCNegated()
    throws Exception
    {
        String src = "This should not be a ~HyperLink.";

        assertEquals( "This should not be a HyperLink.",
                      translate(src) );
    }

    public void testHyperlinksCCNegated2()
    throws Exception
    {
        String src = "~HyperLinks should not be matched.";

        assertEquals( "HyperLinks should not be matched.",
                      translate(src) );
    }

    public void testHyperlinksCCNegated3()
    throws Exception
    {
        String src = "The page ~ASamplePage is not a hyperlink.";

        assertEquals( "The page ASamplePage is not a hyperlink.",
                      translate(src) );
    }

    public void testHyperlinksCCNegated4()
    throws Exception
    {
        String src = "The page \"~ASamplePage\" is not a hyperlink.";

        assertEquals( "The page &quot;ASamplePage&quot; is not a hyperlink.",
                      translate(src) );
    }

    public void testCCLinkInList()
    throws Exception
    {
        newPage("HyperLink");

        String src = "*HyperLink";

        assertEquals( "<ul><li><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a></li></ul>",
                      translate(src) );
    }

    public void testCCLinkBold()
    throws Exception
    {
        newPage("BoldHyperLink");

        String src = "__BoldHyperLink__";

        assertEquals( "<b><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=BoldHyperLink\">BoldHyperLink</a></b>",
                      translate(src) );
    }

    public void testCCLinkBold2()
    throws Exception
    {
        newPage("HyperLink");

        String src = "Let's see, if a bold __HyperLink__ is correct?";

        assertEquals( "Let's see, if a bold <b><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a></b> is correct?",
                      translate(src) );
    }

    public void testCCLinkItalic()
    throws Exception
    {
        newPage("ItalicHyperLink");

        String src = "''ItalicHyperLink''";

        assertEquals( "<i><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ItalicHyperLink\">ItalicHyperLink</a></i>",
                      translate(src) );
    }

    public void testCCLinkWithPunctuation()
    throws Exception
    {
        newPage("HyperLink");

        String src = "Test. Punctuation. HyperLink.";

        assertEquals( "Test. Punctuation. <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>.",
                      translate(src) );
    }

    public void testCCLinkWithPunctuation2()
    throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "Punctuations: HyperLink,ThisToo.";

        assertEquals( "Punctuations: <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLink</a>,<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ThisToo\">ThisToo</a>.",
                      translate(src) );
    }

    public void testCCLinkWithScandics()
    throws Exception
    {
        newPage("\u00c4itiSy\u00f6\u00d6ljy\u00e4");

        String src = "Onko t\u00e4m\u00e4 hyperlinkki: \u00c4itiSy\u00f6\u00d6ljy\u00e4?";

        assertEquals( "Onko t\u00e4m\u00e4 hyperlinkki: <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=%C4itiSy%F6%D6ljy%E4\">\u00c4itiSy\u00f6\u00d6ljy\u00e4</a>?",
                      translate(src) );
    }


    public void testHyperlinksExt()
    throws Exception
    {
        String src = "This should be a [http://www.regex.fi/]";

        assertEquals( "This should be a <a class=\"external\" href=\"http://www.regex.fi/\">http://www.regex.fi/</a>",
                      translate(src) );
    }

    public void testHyperlinksExt2()
    throws Exception
    {
        String src = "This should be a [link|http://www.regex.fi/]";

        assertEquals( "This should be a <a class=\"external\" href=\"http://www.regex.fi/\">link</a>",
                      translate(src) );
    }

    public void testHyperlinksExtNofollow()
    throws Exception
    {
        String src = "This should be a [link|http://www.regex.fi/]";

        assertEquals( "This should be a <a class=\"external\" href=\"http://www.regex.fi/\" rel=\"nofollow\">link</a>",
                      translate_nofollow(src) );
    }

    //
    //  Testing various odds and ends about hyperlink matching.
    //

    public void testHyperlinksPluralMatch()
    throws Exception
    {
        String src = "This should be a [HyperLinks]";

        newPage("HyperLink");

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">HyperLinks</a>",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch2()
    throws Exception
    {
        String src = "This should be a [HyperLinks]";

        assertEquals( "This should be a <a class=\"createpage\" href=\"/test/Edit.jsp?page=HyperLinks\" title=\"Create &quot;HyperLinks&quot;\">HyperLinks</a>",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch3()
    throws Exception
    {
        String src = "This should be a [HyperLink]";

        newPage("HyperLinks");

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLinks\">HyperLink</a>",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch4()
    throws Exception
    {
        String src = "This should be a [Hyper links]";

        newPage("HyperLink");

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=HyperLink\">Hyper links</a>",
                      translate(src) );
    }


    public void testHyperlinkJS1()
    throws Exception
    {
        String src = "This should be a [link|http://www.haxored.com/\" onMouseOver=\"alert('Hahhaa');\"]";

        assertEquals( "This should be a <a class=\"external\" href=\"http://www.haxored.com/&quot; onMouseOver=&quot;alert('Hahhaa');&quot;\">link</a>",
                      translate(src) );
    }

    public void testHyperlinksInterWiki1()
    throws Exception
    {
        String src = "This should be a [link|JSPWiki:HyperLink]";

        assertEquals( "This should be a <a class=\"interwiki\" href=\"http://jspwiki-wiki.apache.org/Wiki.jsp?page=HyperLink\">link</a>",
                      translate(src) );
    }

    public void testHyperlinksInterWiki2()
    throws Exception
    {
        String src = "This should be a [JSPWiki:HyperLink]";

        assertEquals( "This should be a <a class=\"interwiki\" href=\"http://jspwiki-wiki.apache.org/Wiki.jsp?page=HyperLink\">JSPWiki:HyperLink</a>",
                      translate(src) );
    }

    public void testAttachmentLink()
    throws Exception
    {
        newPage("Test");

        Attachment att = new Attachment( testEngine, "Test", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        testEngine.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        String src = "This should be an [attachment link|Test/TestAtt.txt]";

        assertEquals( "This should be an <a class=\"attachment\" href=\"/test/attach/Test/TestAtt.txt\">attachment link</a>"+
                      "<a href=\"/test/PageInfo.jsp?page=Test/TestAtt.txt\" class=\"infolink\"><img src=\"/test/images/attachment_small.png\" border=\"0\" alt=\"(info)\" /></a>",
                      translate(src));
    }

    public void testAttachmentLink2()
    throws Exception
    {
        props.setProperty( "jspwiki.encoding", "ISO-8859-1" );

        //TODO
        TestEngine testEngine2 = new TestEngine( props );

        testEngine2.saveText( "Test", "foo ");
        created.addElement( "Test" );

        Attachment att = new Attachment( testEngine2, "Test", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );

        testEngine2.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        String src = "This should be an [attachment link|Test/TestAtt.txt]";

        assertEquals( "This should be an <a class=\"attachment\" href=\"/test/attach/Test/TestAtt.txt\">attachment link</a>"+
                      "<a href=\"/test/PageInfo.jsp?page=Test/TestAtt.txt\" class=\"infolink\"><img src=\"/test/images/attachment_small.png\" border=\"0\" alt=\"(info)\" /></a>",
                      translate(testEngine2,src));
    }

    /**
     * Are attachments parsed correctly also when using gappy text?
     */
    public void testAttachmentLink3()
    throws Exception
    {
        TestEngine testEngine2 = new TestEngine( props );

        testEngine2.saveText( "TestPage", "foo ");
        created.addElement( "TestPage" );

        Attachment att = new Attachment( testEngine2, "TestPage", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );

        testEngine2.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        String src = "[Test page/TestAtt.txt]";

        assertEquals( "<a class=\"attachment\" href=\"/test/attach/TestPage/TestAtt.txt\">Test page/TestAtt.txt</a>"+
                      "<a href=\"/test/PageInfo.jsp?page=TestPage/TestAtt.txt\" class=\"infolink\"><img src=\"/test/images/attachment_small.png\" border=\"0\" alt=\"(info)\" /></a>",
                      translate(testEngine2,src));
    }

    public void testAttachmentLink4()
    throws Exception
    {
        TestEngine testEngine2 = new TestEngine( props );

        testEngine2.saveText( "TestPage", "foo ");
        created.addElement( "TestPage" );

        Attachment att = new Attachment( testEngine2, "TestPage", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );

        testEngine2.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        String src = "["+testEngine2.beautifyTitle("TestPage/TestAtt.txt")+"]";

        assertEquals( "<a class=\"attachment\" href=\"/test/attach/TestPage/TestAtt.txt\">Test Page/TestAtt.txt</a>"+
                      "<a href=\"/test/PageInfo.jsp?page=TestPage/TestAtt.txt\" class=\"infolink\"><img src=\"/test/images/attachment_small.png\" border=\"0\" alt=\"(info)\" /></a>",
                      translate(testEngine2,src));
    }

    public void testNoHyperlink()
    throws Exception
    {
        newPage("HyperLink");

        String src = "This should not be a [[HyperLink]";

        assertEquals( "This should not be a [HyperLink]",
                      translate(src) );
    }

    public void testNoHyperlink2()
    throws Exception
    {
        String src = "This should not be a [[[[HyperLink]";

        assertEquals( "This should not be a [[[HyperLink]",
                      translate(src) );
    }

    public void testNoHyperlink3()
    throws Exception
    {
        String src = "[[HyperLink], and this [[Neither].";

        assertEquals( "[HyperLink], and this [Neither].",
                      translate(src) );
    }

    public void testNoPlugin()
    throws Exception
    {
        String src = "There is [[{NoPlugin}] here.";

        assertEquals( "There is [{NoPlugin}] here.",
                      translate(src) );
    }

    public void testErroneousHyperlink()
    throws Exception
    {
        String src = "What if this is the last char [";

        assertEquals( "What if this is the last char ",
                      translate(src) );
    }

    public void testErroneousHyperlink2()
    throws Exception
    {
        String src = "What if this is the last char [[";

        assertEquals( "What if this is the last char [",
                      translate(src) );
    }

    public void testExtraPagename1()
    throws Exception
    {
        String src = "Link [test_page]";

        newPage("Test_page");

        assertEquals("Link <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Test_page\">test_page</a>",
                     translate(src) );
    }

    public void testExtraPagename2()
    throws Exception
    {
        String src = "Link [test.page]";

        newPage("Test.page");

        assertEquals("Link <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Test.page\">test.page</a>",
                     translate(src) );
    }

    public void testExtraPagename3()
    throws Exception
    {
        String src = "Link [.testpage_]";

        newPage(".testpage_");

        assertEquals("Link <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=.testpage_\">.testpage_</a>",
                     translate(src) );
    }

    public void testInlineImages()
    throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.png]";

        assertEquals("Link <img class=\"inline\" src=\"http://www.ecyrd.com/test.png\" alt=\"test\" />",
                     translate(src) );
    }

    public void testInlineImages2()
    throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.ppm]";

        assertEquals("Link <a class=\"external\" href=\"http://www.ecyrd.com/test.ppm\">test</a>",
                     translate(src) );
    }

    public void testInlineImages3()
    throws Exception
    {
        String src = "Link [test|http://images.com/testi]";

        assertEquals("Link <img class=\"inline\" src=\"http://images.com/testi\" alt=\"test\" />",
                     translate(src) );
    }

    public void testInlineImages4()
    throws Exception
    {
        String src = "Link [test|http://foobar.jpg]";

        assertEquals("Link <img class=\"inline\" src=\"http://foobar.jpg\" alt=\"test\" />",
                     translate(src) );
    }

    // No link text should be just embedded link.
    public void testInlineImagesLink2()
    throws Exception
    {
        String src = "Link [http://foobar.jpg]";

        assertEquals("Link <img class=\"inline\" src=\"http://foobar.jpg\" alt=\"http://foobar.jpg\" />",
                     translate(src) );
    }

    public void testInlineImagesLink()
    throws Exception
    {
        String src = "Link [http://link.to/|http://foobar.jpg]";

        assertEquals("Link <a class=\"external\" href=\"http://link.to/\"><img class=\"inline\" src=\"http://foobar.jpg\" alt=\"http://link.to/\" /></a>",
                     translate(src) );
    }

    public void testInlineImagesLink3()
    throws Exception
    {
        String src = "Link [SandBox|http://foobar.jpg]";

        newPage("SandBox");

        assertEquals("Link <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=SandBox\"><img class=\"inline\" src=\"http://foobar.jpg\" alt=\"SandBox\" /></a>",
                     translate(src) );
    }

    public void testScandicPagename1()
    throws Exception
    {
        String src = "Link [\u00C5\u00E4Test]";

        newPage("\u00C5\u00E4Test"); // FIXME: Should be capital

        assertEquals("Link <a class=\"wikipage\" href=\"/test/Wiki.jsp?page=%C5%E4Test\">\u00c5\u00e4Test</a>",
                     translate(src));
    }

    public void testParagraph()
    throws Exception
    {
        String src = "1\n\n2\n\n3";

        assertEquals( "<p>1\n</p><p>2\n</p>\n<p>3</p>", translate(src) );
    }

    public void testParagraph2()
    throws Exception
    {
        String src = "[WikiEtiquette]\r\n\r\n[Search]";

        newPage( "WikiEtiquette" );
        
        assertEquals( "<p><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=WikiEtiquette\">WikiEtiquette</a>\n</p>"+
                      "<p><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Search\">Search</a></p>", translate(src) );
    }

    public void testParagraph3()
    throws Exception
    {
        String src = "\r\n\r\n!Testi\r\n\r\nFoo.";

        assertEquals( "<p />\n<h4 id=\"section-testpage-Testi\">Testi<a class=\"hashlink\" href=\"#section-testpage-Testi\">#</a></h4>\n<p>Foo.</p>",
                      translate(src) );
    }

    public void testParagraph4()
    throws Exception
    {
        String src = "\r\n[Recent Changes]\\\\\r\n[WikiEtiquette]\r\n\r\n[Find pages|Search]\\\\\r\n[Unused pages|UnusedPages]";

        newPage("WikiEtiquette");
        newPage("RecentChanges");
        newPage("Search");
        newPage("UnusedPages");

        assertEquals( "<p><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=RecentChanges\">Recent Changes</a><br />\n"+
                      "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=WikiEtiquette\">WikiEtiquette</a>\n</p>\n"+
                      "<p><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Search\">Find pages</a><br />\n"+
                      "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=UnusedPages\">Unused pages</a></p>",
                      translate(src) );
    }

    public void testParagraph5() throws Exception
    {
        String src = "__File type sniffing__ is a way of identifying the content type of a document.\n\n"+
                     "In UNIX, the file(1) command can be used.";

        assertEquals( "<p><b>File type sniffing</b> is a way of identifying the content type of a document.\n</p>"+
                      "<p>In UNIX, the file(1) command can be used.</p>",
                      translate(src) );
    }

    public void testParagraph6() throws Exception
    {
        String src = "[{Counter}]\n\n__File type sniffing__ is a way of identifying the content type of a document.\n\n"+
                     "In UNIX, the file(1) command can be used.";

        assertEquals( "<p>1\n</p><p><b>File type sniffing</b> is a way of identifying the content type of a document.\n</p>\n"+
                      "<p>In UNIX, the file(1) command can be used.</p>",
                      translate(src) );
    }

    public void testParagraph7() throws Exception
    {
        String src = "[{$encoding}]\n\n__File type sniffing__ is a way of identifying the content type of a document.\n\n"+
                     "In UNIX, the file(1) command can be used.";

        assertEquals( "<p>ISO-8859-1\n</p><p><b>File type sniffing</b> is a way of identifying the content type of a document.\n</p>\n"+
                      "<p>In UNIX, the file(1) command can be used.</p>",
                      translate(src) );
    }

    public void testParagraph8() throws Exception
    {
        String src = "[{SET foo=bar}]\n\n__File type sniffing__ is a way of identifying the content type of a document.\n\n"+
                     "In UNIX, the file(1) command can be used.";

        assertEquals( "<p><b>File type sniffing</b> is a way of identifying the content type of a document.\n</p>\n"+
                      "<p>In UNIX, the file(1) command can be used.</p>",
                      translate(src) );
    }

    public void testLinebreak()
    throws Exception
    {
        String src = "1\\\\2";

        assertEquals( "1<br />2", translate(src) );
    }

    public void testLinebreakEscape()
    throws Exception
    {
        String src = "1~\\\\2";

        assertEquals( "1\\\\2", translate(src) );
    }

    public void testLinebreakClear()
    throws Exception
    {
        String src = "1\\\\\\2";

        assertEquals( "1<br clear=\"all\" />2", translate(src) );
    }

    public void testTT()
    throws Exception
    {
        String src = "1{{2345}}6";

        assertEquals( "1<tt>2345</tt>6", translate(src) );
    }

    public void testTTAcrossLines()
    throws Exception
    {
        String src = "1{{\n2345\n}}6";

        assertEquals( "1<tt>\n2345\n</tt>6", translate(src) );
    }

    public void testTTLinks()
    throws Exception
    {
        String src = "1{{\n2345\n[a link]\n}}6";

        newPage("ALink");

        assertEquals( "1<tt>\n2345\n<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ALink\">a link</a>\n</tt>6", translate(src) );
    }

    public void testPre()
    throws Exception
    {
        String src = "1{{{2345}}}6";

        assertEquals( "1<span style=\"font-family:monospace; white-space:pre;\">2345</span>6", translate(src) );
    }

    public void testPre2()
    throws Exception
    {
        String src = "1 {{{ {{{ 2345 }}} }}} 6";

        assertEquals( "1 <span style=\"font-family:monospace; white-space:pre;\"> {{{ 2345 </span> }}} 6", translate(src) );
    }

    public void testPre3()
    throws Exception
    {
        String src = "foo\n\nbar{{{2345}}}6";

        assertEquals( "<p>foo\n</p><p>bar<span style=\"font-family:monospace; white-space:pre;\">2345</span>6</p>", translate(src) );
    }

    public void testPreEscape()
    throws Exception
    {
        String src = "1~{{{2345}}}6";

        assertEquals( "1{{{2345}}}6", translate(src) );
    }

    public void testPreEscape2()
    throws Exception
    {
        String src = "1{{{{{{2345~}}}}}}6";

        assertEquals( "1<span style=\"font-family:monospace; white-space:pre;\">{{{2345}}}</span>6", translate(src) );
    }

    public void testPreEscape3()
    throws Exception
    {
        String src = "1 {{{ {{{ 2345 ~}}} }}} 6";

        assertEquals( "1 <span style=\"font-family:monospace; white-space:pre;\"> {{{ 2345 }}} </span> 6", translate(src) );
    }

    public void testPreEscape4()
    throws Exception
    {
        String src = "1{{{ {{{2345~}} }}}6";

        assertEquals( "1<span style=\"font-family:monospace; white-space:pre;\"> {{{2345~}} </span>6", translate(src) );
    }

    public void testPreEscape5()
    throws Exception
    {
        String src = "1{{{ ~ }}}6";

        assertEquals( "1<span style=\"font-family:monospace; white-space:pre;\"> ~ </span>6", translate(src) );
    }


    public void testHTMLInPre()
    throws Exception
    {
        String src = "1\n{{{ <b> }}}";

        assertEquals( "1\n<pre> &lt;b&gt; </pre>", translate(src) );
    }

    public void testCamelCaseInPre()
    throws Exception
    {
        String src = "1\n{{{ CamelCase }}}";

        assertEquals( "1\n<pre> CamelCase </pre>", translate(src) );
    }

    public void testPreWithLines()
    throws Exception
    {
        String src = "1\r\n{{{\r\nZippadii\r\n}}}";

        assertEquals( "1\n<pre>\nZippadii\n</pre>", translate(src) );
    }

    public void testList1()
    throws Exception
    {
        String src = "A list:\n* One\n* Two\n* Three\n";

        assertEquals( "A list:\n<ul><li>One\n</li><li>Two\n</li><li>Three\n</li></ul>",
                      translate(src) );
    }

    /** Plain multi line testing:
     <pre>
     * One
     continuing
     * Two
     * Three
     </pre>
     */
    public void testMultilineList1()
    throws Exception
    {
        String src = "A list:\n* One\n continuing.\n* Two\n* Three\n";

        assertEquals( "A list:\n<ul><li>One\n continuing.\n</li><li>Two\n</li><li>Three\n</li></ul>",
                      translate(src) );
    }

    public void testMultilineList2()
    throws Exception
    {
        String src = "A list:\n* One\n continuing.\n* Two\n* Three\nShould be normal.";

        assertEquals( "A list:\n<ul><li>One\n continuing.\n</li><li>Two\n</li><li>Three\n</li></ul>Should be normal.",
                      translate(src) );
    }

    public void testHTML()
    throws Exception
    {
        String src = "<b>Test</b>";

        assertEquals( "&lt;b&gt;Test&lt;/b&gt;", translate(src) );
    }

    public void testHTML2()
    throws Exception
    {
        String src = "<p>";

        assertEquals( "&lt;p&gt;", translate(src) );
    }

    public void testHTMLWhenAllowed()
    throws Exception
    {
        String src = "<p>";

        props.setProperty( "jspwiki.translatorReader.allowHTML", "true" );
        testEngine = new TestEngine( props );

        WikiPage page = new WikiPage(testEngine,PAGE_NAME);

        String out = translate( testEngine, page, src );

        assertEquals( "<p>", out );
    }

    public void testHTMLWhenAllowedPre()
    throws Exception
    {
        String src = "{{{ <br /> }}}";

        props.setProperty( "jspwiki.translatorReader.allowHTML", "true" );
        testEngine = new TestEngine( props );

        WikiPage page = new WikiPage(testEngine,PAGE_NAME);

        String out = translate( testEngine, page, src );

        assertEquals( "<pre> &lt;br /&gt; </pre>", out );
    }

    /*
    // This test is not really needed anymore: the JDOM output mechanism
    // handles attribute and element content escaping properly.
    public void testQuotes()
    throws Exception
    {
        String src = "\"Test\"\"";

        assertEquals( "&quot;Test&quot;&quot;", translate(src) );
    }
    */
    public void testHTMLEntities()
    throws Exception
    {
        String src = "& &darr; foo&nbsp;bar &nbsp;&quot; &#2020;&";

        assertEquals( "&amp; &darr; foo&nbsp;bar &nbsp;&quot; &#2020;&amp;", translate(src) );
    }

    public void testItalicAcrossLinebreak()
    throws Exception
    {
        String src="''This is a\ntest.''";

        assertEquals( "<i>This is a\ntest.</i>", translate(src) );
    }

    public void testBoldAcrossLinebreak()
    throws Exception
    {
        String src="__This is a\ntest.__";

        assertEquals( "<b>This is a\ntest.</b>", translate(src) );
    }

    public void testBoldAcrossParagraph()
    throws Exception
    {
        String src="__This is a\n\ntest.__";

        assertEquals( "<p><b>This is a\n</b></p><p><b>test.</b></p>", translate(src) );
    }

    public void testBoldItalic()
    throws Exception
    {
        String src="__This ''is'' a test.__";

        assertEquals( "<b>This <i>is</i> a test.</b>", translate(src) );
    }

    public void testFootnote1()
    throws Exception
    {
        String src="Footnote[1]";

        assertEquals( "Footnote<a class=\"footnoteref\" href=\"#ref-testpage-1\">[1]</a>",
                      translate(src) );
    }

    public void testFootnote2()
    throws Exception
    {
        String src="[#2356] Footnote.";

        assertEquals( "<a class=\"footnote\" name=\"ref-testpage-2356\">[#2356]</a> Footnote.",
                      translate(src) );
    }

    /** Check an reported error condition where empty list items could cause crashes */

    public void testEmptySecondLevelList()
    throws Exception
    {
        String src="A\n\n**\n\nB";

        // System.out.println(translate(src));
        assertEquals( "<p>A\n</p><ul><li><ul><li>\n</li></ul></li></ul><p>B</p>",
                      translate(src) );
    }

    public void testEmptySecondLevelList2()
    throws Exception
    {
        String src="A\n\n##\n\nB";

        // System.out.println(translate(src));
        assertEquals( "<p>A\n</p><ol><li><ol><li>\n</li></ol></li></ol><p>B</p>",
                      translate(src) );
    }

    /**
     * <pre>
     *   *Item A
     *   ##Numbered 1
     *   ##Numbered 2
     *   *Item B
     * </pre>
     *
     * would come out as:
     *<ul>
     * <li>Item A
     * </ul>
     * <ol>
     * <ol>
     * <li>Numbered 1
     * <li>Numbered 2
     * <ul>
     * <li></ol>
     * </ol>
     * Item B
     * </ul>
     *
     *  (by Mahlen Morris).
     */

    public void testMixedList()
    throws Exception
    {
        String src="*Item A\n##Numbered 1\n##Numbered 2\n*Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul><li>Item A"+
                      "<ol><li>Numbered 1</li>"+
                      "<li>Numbered 2</li>"+
                      "</ol></li>"+
                      "<li>Item B</li>"+
                      "</ul>",
                      result );
    }
    /**
     *  Like testMixedList() but the list types have been reversed.
     */

    public void testMixedList2()
    throws Exception
    {
        String src="#Item A\n**Numbered 1\n**Numbered 2\n#Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ol><li>Item A"+
                      "<ul><li>Numbered 1</li>"+
                      "<li>Numbered 2</li>"+
                      "</ul></li>"+
                      "<li>Item B</li>"+
                      "</ol>",
                      result );
    }

    /**
     * <pre>
     *   * bullet A
     *   ** bullet A_1
     *   *# number A_1
     *   * bullet B
     * </pre>
     * 
     * would come out as:
     * 
     * <ul>
     *   <li>bullet A
     *     <ul>
     *       <li>bullet A_1</li>
     *     </ul>
     *     <ol>
     *       <li>number A_1</li>
     *     </ol>
     *   </li>
     *   <li>bullet B</li>
     * </ul>
     *  
     */

    public void testMixedListOnSameLevel()
    throws Exception
    {
        String src="* bullet A\n** bullet A_1\n*# number A_1\n* bullet B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul>"+
                      "<li>bullet A"+
                      "<ul>"+
                      "<li>bullet A_1</li>"+
                      "</ul>"+
                      "<ol>"+
                      "<li>number A_1</li>"+
                      "</ol>"+
                      "</li>"+
                      "<li>bullet B</li>"+
                      "</ul>"
                      ,
                      result );
    }
    /**
     * <pre>
     *   * bullet A
     *   ** bullet A_1
     *   ## number A_1
     *   * bullet B
     * </pre>
     * 
     * would come out as:
     * 
     * <ul>
     *   <li>bullet A
     *     <ul>
     *       <li>bullet A_1</li>
     *     </ul>
     *     <ol>
     *       <li>number A_1</li>
     *     </ol>
     *   </li>
     *   <li>bullet B</li>
     * </ul>
     *  
     */

    public void testMixedListOnSameLevel2()
    throws Exception
    {
        String src="* bullet A\n** bullet A_1\n## number A_1\n* bullet B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul>"+
                      "<li>bullet A"+
                      "<ul>"+
                      "<li>bullet A_1</li>"+
                      "</ul>"+
                      "<ol>"+
                      "<li>number A_1</li>"+
                      "</ol>"+
                      "</li>"+
                      "<li>bullet B</li>"+
                      "</ul>"
                      ,
                      result );
    }

    /**
     * <pre>
     *   * bullet 1
     *   ## number 2
     *   ** bullet 3
     *   ## number 4
     *   * bullet 5 
     * </pre>
     * 
     * would come out as:
     * 
     *   <ul>
     *       <li>bullet 1
     *           <ol><li>number 2</li></ol>
     *           <ul><li>bullet 3</li></ul>
     *           <ol><li>number 4</li></ol>
     *       </li>
     *       <li>bullet 5</li>
     *   </ul>
     *  
     */

    public void testMixedListOnSameLevel3()
    throws Exception
    {
        String src="* bullet 1\n## number 2\n** bullet 3\n## number 4\n* bullet 5\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul>"+
                      "<li>bullet 1"+
                      "<ol><li>number 2</li></ol>"+
                      "<ul><li>bullet 3</li></ul>"+
                      "<ol><li>number 4</li></ol>"+
                      "</li>"+
                      "<li>bullet 5</li>"+
                      "</ul>"
                      ,
                      result );
    }
    /**
     * <pre>
     *   # number 1
     *   ** bullet 2
     *   ## number 3
     *   ** bullet 4
     *   # number 5 
     * </pre>
     * 
     * would come out as:
     * 
     *   <ol>
     *       <li>number 1
     *           <ul><li>bullet 2</li></ul>
     *           <ol><li>number 3</li></ol>
     *           <ul><li>bullet 4</li></ul>
     *       </li>
     *       <li>number 5</li>
     *   </ol>
     *  
     */

    public void testMixedListOnSameLevel4()
    throws Exception
    {
        String src="# number 1\n** bullet 2\n## number 3\n** bullet 4\n# number 5\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ol>"+
                      "<li>number 1"+
                      "<ul><li>bullet 2</li></ul>"+
                      "<ol><li>number 3</li></ol>"+
                      "<ul><li>bullet 4</li></ul>"+
                      "</li>"+
                      "<li>number 5</li>"+
                      "</ol>"
                      ,
                      result );
    }

    public void testNestedList()
    throws Exception
    {
        String src="*Item A\n**Numbered 1\n**Numbered 2\n*Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul><li>Item A"+
                      "<ul><li>Numbered 1</li>"+
                      "<li>Numbered 2</li>"+
                      "</ul></li>"+
                      "<li>Item B</li>"+
                      "</ul>",
                      result );
    }

    public void testNestedList2()
    throws Exception
    {
        String src="*Item A\n**Numbered 1\n**Numbered 2\n***Numbered3\n*Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<ul><li>Item A"+
                      "<ul><li>Numbered 1</li>"+
                      "<li>Numbered 2"+
                      "<ul><li>Numbered3</li>"+
                      "</ul></li>"+
                      "</ul></li>"+
                      "<li>Item B</li>"+
                      "</ul>",
                      result );
    }


    public void testPluginInsert()
    throws Exception
    {
        String src="[{INSERT org.apache.wiki.plugin.SamplePlugin WHERE text=test}]";

        assertEquals( "test", translate(src) );
    }

    public void testPluginHTMLInsert()
    throws Exception
    {
        String src="[{INSERT org.apache.wiki.plugin.SamplePlugin WHERE text='<b>Foo</b>'}]";

        assertEquals( "<b>Foo</b>", translate(src) );
    }

    public void testPluginNoInsert()
    throws Exception
    {
        String src="[{SamplePlugin text=test}]";

        assertEquals( "test", translate(src) );
    }

    public void testPluginInsertJS()
    throws Exception
    {
        String src="Today: [{INSERT JavaScriptPlugin}] ''day''.";

        assertEquals( "Today: <script language=\"JavaScript\"><!--\nfoo='';\n--></script>\n <i>day</i>.", translate(src) );
    }

    public void testShortPluginInsert()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text=test}]";

        assertEquals( "test", translate(src) );
    }

    /**
     *  Test two plugins on same row.
     */
    public void testShortPluginInsert2()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text=test}] [{INSERT SamplePlugin WHERE text=test2}]";

        assertEquals( "test test2", translate(src) );
    }

    public void testPluginQuotedArgs()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text='test me now'}]";

        assertEquals( "test me now", translate(src) );
    }

    public void testPluginDoublyQuotedArgs()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text='test \\'me too\\' now'}]";

        assertEquals( "test 'me too' now", translate(src) );
    }

    public void testPluginQuotedArgs2()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text=foo}] [{INSERT SamplePlugin WHERE text='test \\'me too\\' now'}]";

        assertEquals( "foo test 'me too' now", translate(src) );
    }

    /**
     *  Plugin output must not be parsed as Wiki text.
     */
    public void testPluginWikiText()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text=PageContent}]";

        assertEquals( "PageContent", translate(src) );
    }

    /**
     *  Nor should plugin input be interpreted as wiki text.
     */
    public void testPluginWikiText2()
    throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text='----'}]";

        assertEquals( "----", translate(src) );
    }

    public void testMultilinePlugin1()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin WHERE\n text=PageContent}]";

        assertEquals( "Test PageContent", translate(src) );
    }

    public void testMultilinePluginBodyContent()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\ntext=PageContent\n\n123\n456\n}]";

        assertEquals( "Test PageContent (123+456+)", translate(src) );
    }

    public void testMultilinePluginBodyContent2()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\ntext=PageContent\n\n\n123\n456\n}]";

        assertEquals( "Test PageContent (+123+456+)", translate(src) );
    }

    public void testMultilinePluginBodyContent3()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\n123\n456\n}]";

        assertEquals( "Test  (123+456+)", translate(src) );
    }

    /**
     *  Has an extra space after plugin name.
     */
    public void testMultilinePluginBodyContent4()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin \n\n123\n456\n}]";

        assertEquals( "Test  (123+456+)", translate(src) );
    }

    /**
     *  Check that plugin end is correctly recognized.
     */
    public void testPluginEnd()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin text=']'}]";

        assertEquals( "Test ]", translate(src) );
    }

    public void testPluginEnd2()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin text='a[]+b'}]";

        assertEquals( "Test a[]+b", translate(src) );
    }

    public void testPluginEnd3()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\na[]+b\n}]";

        assertEquals( "Test  (a[]+b+)", translate(src) );
    }

    public void testPluginEnd4()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin text='}'}]";

        assertEquals( "Test }", translate(src) );
    }

    public void testPluginEnd5()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\na[]+b{}\nGlob.\n}]";

        assertEquals( "Test  (a[]+b{}+Glob.+)", translate(src) );
    }

    public void testPluginEnd6()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\na[]+b{}\nGlob.\n}}]";

        assertEquals( "Test  (a[]+b{}+Glob.+})", translate(src) );
    }

    public void testNestedPlugin1()
        throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\n[{SamplePlugin}]\nGlob.\n}}]";

        assertEquals( "Test  ([{SamplePlugin}]+Glob.+})", translate(src) );
    }


    public void testNestedPlugin2()
        throws Exception
    {
        String src="[{SET foo='bar'}]Test [{INSERT SamplePlugin\n\n[{SamplePlugin text='[{$foo}]'}]\nGlob.\n}}]";

        assertEquals( "Test  ([{SamplePlugin text='[bar]'}]+Glob.+})", translate(src) );
    }


    //  FIXME: I am not entirely certain if this is the right result
    //  Perhaps some sort of an error should be checked?
    public void testPluginNoEnd()
    throws Exception
    {
        String src="Test [{INSERT SamplePlugin\n\na+b{}\nGlob.\n}";

        assertEquals( "Test {INSERT SamplePlugin\n\na+b{}\nGlob.\n}", translate(src) );
    }
    
    public void testMissingPlugin() throws Exception {
    	String src="Test [{SamplePlugino foo='bar'}]";
    	
    	assertEquals( "Test JSPWiki : testpage - Plugin insertion failed: Could not find plugin SamplePlugino" +
    			      "<span class=\"error\">JSPWiki : testpage - Plugin insertion failed: Could not find plugin SamplePlugino</span>", 
    			      translate( src ) );
    }

    public void testVariableInsert()
    throws Exception
    {
        String src="[{$pagename}]";

        assertEquals( PAGE_NAME+"", translate(src) );
    }

    public void testTable1()
    throws Exception
    {
        String src="|| heading || heading2 \n| Cell 1 | Cell 2 \n| Cell 3 | Cell 4\n\n";

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><th> heading </th><th> heading2 </th></tr>\n"+
                      "<tr><td> Cell 1 </td><td> Cell 2 </td></tr>\n"+
                      "<tr class=\"odd\"><td> Cell 3 </td><td> Cell 4</td></tr>\n"+
                      "</table><p />",
                      translate(src) );
    }

    public void testTable2()
    throws Exception
    {
        String src="||heading||heading2\n|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><th>heading</th><th>heading2</th></tr>\n"+
                      "<tr><td>Cell 1</td><td> Cell 2</td></tr>\n"+
                      "<tr class=\"odd\"><td> Cell 3 </td><td>Cell 4</td></tr>\n"+
                      "</table><p />",
                      translate(src) );
    }

    public void testTable3()
    throws Exception
    {
        String src="|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><td>Cell 1</td><td> Cell 2</td></tr>\n"+
                      "<tr><td> Cell 3 </td><td>Cell 4</td></tr>\n"+
                      "</table><p />",
                      translate(src) );
    }

    public void testTable4()
    throws Exception
    {
        String src="|a\nbc";

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><td>a</td></tr>\n"+
                      "</table>"+
                      "bc",
                      translate(src) );
    }

    /**
     * Tests BugTableHeaderNotXHMTLCompliant
     * @throws Exception
     */
    public void testTable5()
    throws Exception
    {
        String src="Testtable\n||header|cell\n\n|cell||header";

        assertEquals( "<p>Testtable\n</p>"+
                      "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><th>header</th><td>cell</td></tr>\n</table><p />\n"+
                      "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><td>cell</td><th>header</th></tr>"+
                      "</table>",
                      translate(src) );
    }

    public void testTableLink()
    throws Exception
    {
        String src="|Cell 1| Cell 2\n|[Cell 3|ReallyALink]|Cell 4\n\n";

        newPage("ReallyALink");

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><td>Cell 1</td><td> Cell 2</td></tr>\n"+
                      "<tr><td><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ReallyALink\">Cell 3</a></td><td>Cell 4</td></tr>\n"+
                      "</table><p />",
                      translate(src) );
    }

    public void testTableLinkEscapedBar()
    throws Exception
    {
        String src="|Cell 1| Cell~| 2\n|[Cell 3|ReallyALink]|Cell 4\n\n";

        newPage("ReallyALink");

        assertEquals( "<table class=\"wikitable\" border=\"1\">"+
                      "<tr class=\"odd\"><td>Cell 1</td><td> Cell| 2</td></tr>\n"+
                      "<tr><td><a class=\"wikipage\" href=\"/test/Wiki.jsp?page=ReallyALink\">Cell 3</a></td><td>Cell 4</td></tr>\n"+
                      "</table><p />",
                      translate(src) );
    }

    public void testDescription()
    throws Exception
    {
        String src=";:Foo";

        assertEquals( "<dl><dt></dt><dd>Foo</dd></dl>",
                      translate(src) );
    }

    public void testDescription2()
    throws Exception
    {
        String src=";Bar:Foo";

        assertEquals( "<dl><dt>Bar</dt><dd>Foo</dd></dl>",
                      translate(src) );
    }

    public void testDescription3()
    throws Exception
    {
        String src=";:";

        assertEquals( "<dl><dt></dt><dd /></dl>",
                      translate(src) );
    }

    public void testDescription4()
    throws Exception
    {
        String src=";Bar:Foo :-)";

        assertEquals( "<dl><dt>Bar</dt><dd>Foo :-)</dd></dl>",
                      translate(src) );
    }

    public void testDescription5()
    throws Exception
    {
        String src=";Bar:Foo :-) ;-) :*]";

        assertEquals( "<dl><dt>Bar</dt><dd>Foo :-) ;-) :*]</dd></dl>",
                      translate(src) );
    }


    public void testRuler()
    throws Exception
    {
        String src="----";

        assertEquals( "<hr />",
                      translate(src) );
    }

    public void testRulerCombo()
    throws Exception
    {
        String src="----Foo";

        assertEquals( "<hr />Foo",
                      translate(src) );
    }

    public void testRulerCombo2()
    throws Exception
    {
        String src="Bar----Foo";

        assertEquals( "Bar----Foo",
                      translate(src) );
    }

    public void testShortRuler1()
    throws Exception
    {
        String src="-";

        assertEquals( "-",
                      translate(src) );
    }

    public void testShortRuler2()
    throws Exception
    {
        String src="--";

        assertEquals( "--",
                      translate(src) );
    }

    public void testShortRuler3()
    throws Exception
    {
        String src="---";

        assertEquals( "---",
                      translate(src) );
    }

    public void testLongRuler()
    throws Exception
    {
        String src="------";

        assertEquals( "<hr />",
                      translate(src) );
    }

    public void testHeading1()
    throws Exception
    {
        String src="!Hello\nThis is a test";

        assertEquals( "<h4 id=\"section-testpage-Hello\">Hello<a class=\"hashlink\" href=\"#section-testpage-Hello\">#</a></h4>\nThis is a test",
                      translate(src) );
    }

    public void testHeading2()
    throws Exception
    {
        String src="!!Hello, testing 1, 2, 3";

        assertEquals( "<h3 id=\"section-testpage-HelloTesting123\">Hello, testing 1, 2, 3<a class=\"hashlink\" href=\"#section-testpage-HelloTesting123\">#</a></h3>",
                      translate(src) );
    }

    public void testHeading3()
    throws Exception
    {
        String src="!!!Hello there, how are you doing?";

        assertEquals( "<h2 id=\"section-testpage-HelloThereHowAreYouDoing\">Hello there, how are you doing?<a class=\"hashlink\" href=\"#section-testpage-HelloThereHowAreYouDoing\">#</a></h2>",
                      translate(src) );
    }

    public void testHeadingHyperlinks()
    throws Exception
    {
        String src="!!![Hello]";

        assertEquals( "<h2 id=\"section-testpage-Hello\"><a class=\"createpage\" href=\"/test/Edit.jsp?page=Hello\" title=\"Create &quot;Hello&quot;\">Hello</a><a class=\"hashlink\" href=\"#section-testpage-Hello\">#</a></h2>",
                      translate(src) );
    }

    public void testHeadingHyperlinks2()
    throws Exception
    {
        String src="!!![Hello|http://www.google.com/]";

        assertEquals( "<h2 id=\"section-testpage-Hello\"><a class=\"external\" href=\"http://www.google.com/\">Hello</a><a class=\"hashlink\" href=\"#section-testpage-Hello\">#</a></h2>",
                      translate(src) );
    }

    public void testHeadingHyperlinks3()
    throws Exception
    {
        String src="![Hello|http://www.google.com/?p=a&c=d]";

        assertEquals( "<h4 id=\"section-testpage-Hello\"><a class=\"external\" href=\"http://www.google.com/?p=a&amp;c=d\">Hello</a><a class=\"hashlink\" href=\"#section-testpage-Hello\">#</a></h4>",
                      translate(src) );
    }

    /**
     *  in 2.0.0, this one throws OutofMemoryError.
     */
    public void testBrokenPageText()
    throws Exception
    {
        String translation = translate( brokenPageText );

        assertNotNull( translation );
    }

    /**
     *  Shortened version of the previous one.
     */
    public void testBrokenPageTextShort()
    throws Exception
    {
        String src = "{{{\ncode.}}\n";

        assertEquals( "<pre>\ncode.}}\n</pre>", translate(src) );
    }

    /**
     *  Shortened version of the previous one.
     */
    public void testBrokenPageTextShort2()
    throws Exception
    {
        String src = "{{{\ncode.}\n";

        assertEquals( "<pre>\ncode.}\n</pre>", translate(src) );
    }

    public void testExtraExclamation()
        throws Exception
    {
        String src = "Hello!";

        assertEquals( "Hello!", translate(src) );
    }

    /**
     * Used by the ACL tests.
     * @param array
     * @param key
     * @return
     */
    /*
    private boolean inArray( Object[] array, Object key )
    {
        for( int i = 0; i < array.length; i++ )
        {
            if ( array[i].equals( key ) )
            {
                return true;
            }
        }
        return false;
    }
    */
    /**
     * Used by the ACL tests.
     * @param array
     * @param key
     * @return
     */
    /*
    private boolean inGroup( Object[] array, Principal key )
    {
        for( int i = 0; i < array.length; i++ )
        {
            if (array[i] instanceof Group)
            {
                if (((Group)array[i]).isMember(key))
                {
                    return true;
                }
            }
        }
        return false;
    }
    */
    /**
     *  ACL tests.
     */
    /*
     public void testSimpleACL1()
     throws Exception
     {
     String src = "Foobar.[{ALLOW view JanneJalkanen}]";

     WikiPage p = new WikiPage( PAGE_NAME );

     String res = translate( p, src );

     assertEquals("Page text", "Foobar.", res);

     Acl acl = p.getAcl();

     Principal prof = new WikiPrincipal("JanneJalkanen");

     assertTrue( "has read", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "view") ), prof ) );
     assertFalse( "no edit", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "edit") ), prof ) );
     }

     public void testSimpleACL2()
     throws Exception
     {
     String src = "Foobar.[{ALLOW view JanneJalkanen}]\n"+
     "[{ALLOW edit JanneJalkanen, SuloVilen}]";

     WikiPage p = new WikiPage( PAGE_NAME );

     String res = translate( p, src );

     assertEquals("Page text", "Foobar.\n", res);

     Acl acl = p.getAcl();

     // ACL says Janne can read and edit
      Principal prof = new WikiPrincipal("JanneJalkanen");
      assertTrue( "read for JJ", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "view") ), prof ) );
      assertTrue( "edit for JJ", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "edit") ), prof ) );

      // ACL doesn't say Erik can read or edit
       prof = new WikiPrincipal("ErikBunn");
       assertFalse( "no read for BB", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "view") ), prof ) );
       assertFalse( "no edit for EB", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "edit") ), prof ) );

       // ACL says Sulo can edit, but doens't say he can read (though the AuthMgr will tell us it's implied)
        prof = new WikiPrincipal("SuloVilen");
        assertFalse( "read for SV", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "view") ), prof ) );
        assertTrue( "edit for SV", inArray( acl.findPrincipals( new PagePermission( PAGE_NAME, "edit") ), prof ) );
        }
        */
    /*
    private boolean containsGroup( List l, String name )
    {
        for( Iterator i = l.iterator(); i.hasNext(); )
        {
            String group = (String) i.next();

            if( group.equals( name ) )
                return true;
        }

        return false;
    }
    */
    /**
     *   Metadata tests
     */
    public void testSet1()
    throws Exception
    {
        String src = "Foobar.[{SET name=foo}]";

        WikiPage p = new WikiPage( testEngine, PAGE_NAME );

        String res = translate( p, src );

        assertEquals("Page text", "Foobar.", res);

        assertEquals( "foo", p.getAttribute("name") );
    }

    public void testSet2()
    throws Exception
    {
        String src = "Foobar.[{SET name = foo}]";

        WikiPage p = new WikiPage( testEngine, PAGE_NAME );

        String res = translate( p, src );

        assertEquals("Page text", "Foobar.", res);

        assertEquals( "foo", p.getAttribute("name") );
    }

    public void testSet3()
    throws Exception
    {
        String src = "Foobar.[{SET name= Janne Jalkanen}]";

        WikiPage p = new WikiPage( testEngine, PAGE_NAME );

        String res = translate( p, src );

        assertEquals("Page text", "Foobar.", res);

        assertEquals( "Janne Jalkanen", p.getAttribute("name") );
    }

    public void testSet4()
    throws Exception
    {
        String src = "Foobar.[{SET name='Janne Jalkanen'}][{SET too='{$name}'}]";

        WikiPage p = new WikiPage( testEngine, PAGE_NAME );

        String res = translate( p, src );

        assertEquals("Page text", "Foobar.", res);

        assertEquals( "Janne Jalkanen", p.getAttribute("name") );
        assertEquals( "Janne Jalkanen", p.getAttribute("too") );
    }

    public void testSetHTML()
    throws Exception
    {
        String src = "Foobar.[{SET name='<b>danger</b>'}] [{$name}]";

        WikiPage p = new WikiPage( testEngine, PAGE_NAME );

        String res = translate( p, src );

        assertEquals("Page text", "Foobar. &lt;b&gt;danger&lt;/b&gt;", res);

        assertEquals( "<b>danger</b>", p.getAttribute("name") );
    }


    /**
     *  Test collection of links.
     */

    public void testCollectingLinks()
    throws Exception
    {
        LinkCollector coll = new LinkCollector();
        String src = "[Test]";
        WikiContext context = new WikiContext( testEngine,
                                               new WikiPage(testEngine,PAGE_NAME) );

        MarkupParser p = new JSPWikiMarkupParser( context,
                                                  new BufferedReader( new StringReader(src)) );
        p.addLocalLinkHook( coll );
        p.addExternalLinkHook( coll );
        p.addAttachmentLinkHook( coll );

        p.parse();

        Collection< String > links = coll.getLinks();

        assertEquals( "no links found", 1, links.size() );
        assertEquals( "wrong link", "Test", links.iterator().next() );
    }

    public void testCollectingLinks2()
    throws Exception
    {
        LinkCollector coll = new LinkCollector();
        String src = "["+PAGE_NAME+"/Test.txt]";

        WikiContext context = new WikiContext( testEngine,
                                               new WikiPage(testEngine,PAGE_NAME) );

        MarkupParser p = new JSPWikiMarkupParser( context,
                                                  new BufferedReader( new StringReader(src)) );
        p.addLocalLinkHook( coll );
        p.addExternalLinkHook( coll );
        p.addAttachmentLinkHook( coll );

        p.parse();

        Collection< String > links = coll.getLinks();

        assertEquals( "no links found", 1, links.size() );
        assertEquals( "wrong link", PAGE_NAME+"/Test.txt",
                      links.iterator().next() );
    }

    public void testCollectingLinksAttachment()
    throws Exception
    {
        // First, make an attachment.

        try
        {
            testEngine.saveText( PAGE_NAME, "content" );
            Attachment att = new Attachment( testEngine, PAGE_NAME, "TestAtt.txt" );
            att.setAuthor( "FirstPost" );
            testEngine.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

            LinkCollector coll        = new LinkCollector();
            LinkCollector coll_others = new LinkCollector();

            String src = "[TestAtt.txt]";
            WikiContext context = new WikiContext( testEngine,
                                                   new WikiPage(testEngine,PAGE_NAME) );

            MarkupParser p = new JSPWikiMarkupParser( context,
                                                      new BufferedReader( new StringReader(src)) );
            p.addLocalLinkHook( coll_others );
            p.addExternalLinkHook( coll_others );
            p.addAttachmentLinkHook( coll );

            p.parse();

            Collection< String > links = coll.getLinks();

            assertEquals( "no links found", 1, links.size() );
            assertEquals( "wrong link", PAGE_NAME+"/TestAtt.txt",
                          links.iterator().next() );

            assertEquals( "wrong links found", 0, coll_others.getLinks().size() );
        }
        finally
        {
            String files = testEngine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
            File storagedir = new File( files, PAGE_NAME+BasicAttachmentProvider.DIR_EXTENSION );

            if( storagedir.exists() && storagedir.isDirectory() )
                TestEngine.deleteAll( storagedir );
        }
    }

    public void testDivStyle1()
    throws Exception
    {
        String src = "%%foo\ntest\n%%\n";

        assertEquals( "<div class=\"foo\">\ntest\n</div>\n", translate(src) );
    }

    public void testDivStyle2()
    throws Exception
    {
        String src = "%%(foo:bar;)\ntest\n%%\n";

        assertEquals( "<div style=\"foo:bar;\">\ntest\n</div>\n", translate(src) );
    }

    public void testSpanStyle1()
    throws Exception
    {
        String src = "%%foo test%%\n";

        assertEquals( "<span class=\"foo\">test</span>\n", translate(src) );
    }

    public void testSpanStyle2()
    throws Exception
    {
        String src = "%%(foo:bar;)test%%\n";

        assertEquals( "<span style=\"foo:bar;\">test</span>\n", translate(src) );
    }

    public void testSpanStyle3()
    throws Exception
    {
        String src = "Johan %%(foo:bar;)test%%\n";

        assertEquals( "Johan <span style=\"foo:bar;\">test</span>\n", translate(src) );
    }

    public void testSpanStyle4()
    throws Exception
    {
        String src = "Johan %%(foo:bar;)test/%\n";

        assertEquals( "Johan <span style=\"foo:bar;\">test</span>\n", translate(src) );
    }

    public void testSpanEscape()
    throws Exception
    {
        String src = "~%%foo test~%%\n";

        assertEquals( "%%foo test%%\n", translate(src) );
    }

    public void testSpanNested()
    throws Exception
    {
        String src = "Johan %%(color: rgb(1,2,3);)test%%\n";

        assertEquals( "Johan <span style=\"color: rgb(1,2,3);\">test</span>\n", translate(src) );
    }

    public void testSpanStyleTable()
    throws Exception
    {
        String src = "|%%(foo:bar;)test%%|no test\n";

        assertEquals( "<table class=\"wikitable\" border=\"1\"><tr class=\"odd\"><td><span style=\"foo:bar;\">test</span></td><td>no test</td></tr>\n</table>",
                      translate(src) );
    }

    public void testSpanJavascript()
    throws Exception
    {
        String src = "%%(visibility: hidden; background-image:url(javascript:alert('X')))%%\nTEST";

        assertEquals( "<span class=\"error\">Attempt to output javascript!</span>\nTEST", translate(src) );
    }

    // FIXME: This test must be enabled later on!
    /*
    public void testSpanJavascript2()
    throws Exception
    {
        String src = "%%(visibility: hidden; background&#09;-image:url(j&#000013;avas&#99;ript:'url()';alert('X');)%%\nTEST";

        assertEquals( "<span class=\"error\">Attempt to output javascript!</span>\nTEST", translate(src) );
    }
    */
    public void testHTMLEntities1()
    throws Exception
    {
        String src = "Janne&apos;s test";

        assertEquals( "Janne&apos;s test", translate(src) );
    }

    public void testHTMLEntities2()
    throws Exception
    {
        String src = "&Auml;";

        assertEquals( "&Auml;", translate(src) );
    }

    public void testBlankEscape()
    throws Exception
    {
        String src = "H2%%sub 2%%~ O";

        assertEquals( "H2<span class=\"sub\">2</span>O", translate(src) );
    }


    public void testEmptyBold()
    throws Exception
    {
        String src = "____";

        assertEquals( "<b></b>", translate(src) );
    }

    public void testEmptyItalic()
    throws Exception
    {
        String src = "''''";

        assertEquals( "<i></i>", translate(src) );
    }

    public void testRenderingSpeed1()
       throws Exception
    {
        Benchmark sw = new Benchmark();
        sw.start();

        for( int i = 0; i < 100; i++ )
        {
            translate( brokenPageText );
        }

        sw.stop();
        System.out.println("100 page renderings: "+sw+" ("+sw.toString(100)+" renderings/second)");
    }

    public void testPunctuatedWikiNames()
        throws Exception
    {
        String src = "[-phobous]";

        assertEquals( "<a class=\"createpage\" href=\"/test/Edit.jsp?page=-phobous\" title=\"Create &quot;-phobous&quot;\">-phobous</a>", translate(src) );
    }

    public void testPunctuatedWikiNames2()
        throws Exception
    {
        String src = "[?phobous]";

        assertEquals( "<a class=\"createpage\" href=\"/test/Edit.jsp?page=Phobous\" title=\"Create &quot;Phobous&quot;\">?phobous</a>", translate(src) );
    }

    public void testPunctuatedWikiNames3()
        throws Exception
    {
        String src = "[Brightness (apical)]";

        assertEquals( "<a class=\"createpage\" href=\"/test/Edit.jsp?page=Brightness%20%28apical%29\" title=\"Create &quot;Brightness (apical)&quot;\">Brightness (apical)</a>", translate(src) );
    }

    public void testDeadlySpammer()
        throws Exception
    {
        String deadlySpammerText = "zzz <a href=\"http://ring1.gmum.net/frog-ringtone.html\">frogringtone</a> zzz http://ring1.gmum.net/frog-ringtone.html[URL=http://ring1.gmum.net/frog-ringtone.html]frog ringtone[/URL] frogringtone<br>";

        StringBuilder death = new StringBuilder( 20000 );

        for( int i = 0; i < 1000; i++ )
        {
            death.append( deadlySpammerText );
        }

        death.append("\n\n");

        System.out.println("Trying to crash parser with a line which is "+death.length()+" chars in size");
        //  This should not fail
        String res = translate( death.toString() );

        assertTrue( res.length() > 0 );
    }

    public void testSpacesInLinks1() throws Exception
    {
        newPage("Foo bar");
        String src = "[Foo bar]";

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Foo%20bar\">Foo bar</a>", translate(src) );
    }

    /** Too many spaces */
    public void testSpacesInLinks2() throws Exception
    {
        newPage("Foo bar");
        String src = "[Foo        bar]";

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Foo%20bar\">Foo        bar</a>", translate(src) );
    }
    
    public void testIllegalXML() throws Exception
    {
        String src = "Test \u001d foo";
        
        String dst = translate(src);
        
        assertTrue( "No error", dst.indexOf("JDOM") != -1 );
    }

    public void testXSS1() throws Exception
    {
        String src = "[http://www.host.com/du=\"> <img src=\"foobar\" onerror=\"alert(document.cookie)\"/>]";

        String dst = translate(src);

        assertEquals( "<a class=\"external\" href=\"http://www.host.com/du=&quot;&gt; &lt;img src=&quot;foobar&quot; onerror=&quot;alert(document.cookie)&quot;/&gt;\">http://www.host.com/du=&quot;&gt; &lt;img src=&quot;foobar&quot; onerror=&quot;alert(document.cookie)&quot;/&gt;</a>", dst );
    }
    
    public void testAmpersand1() throws Exception
    {
        newPage( "Foo&Bar" );
        String src = "[Foo&Bar]";

        String dst = translate(src);

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Foo%26Bar\">Foo&amp;Bar</a>", dst );
    }

    public void testAmpersand2() throws Exception
    {
        newPage( "Foo & Bar" );
        String src = "[Foo & Bar]";

        String dst = translate(src);

        assertEquals( "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page=Foo%20%26%20Bar\">Foo &amp; Bar</a>", dst );
    }
    
    // This is a random find: the following page text caused an eternal loop in V2.0.x.
    private static final String brokenPageText =
        "Please ''check [RecentChanges].\n" +
        "\n" +
        "Testing. fewfwefe\n" +
        "\n" +
        "CHeck [testpage]\n" +
        "\n" +
        "More testing.\n" +
        "dsadsadsa''\n" +
        "Is this {{truetype}} or not?\n" +
        "What about {{{This}}}?\n" +
        "How about {{this?\n" +
        "\n" +
        "{{{\n" +
        "{{text}}\n" +
        "}}}\n" +
        "goo\n" +
        "\n" +
        "<b>Not bold</b>\n" +
        "\n" +
        "motto\n" +
        "\n" +
        "* This is a list which we\n" +
        "shall continue on a other line.\n" +
        "* There is a list item here.\n" +
        "*  Another item.\n" +
        "* More stuff, which continues\n" +
        "on a second line.  And on\n" +
        "a third line as well.\n" +
        "And a fourth line.\n" +
        "* Third item.\n" +
        "\n" +
        "Foobar.\n" +
        "\n" +
        "----\n" +
        "\n" +
        "!!!Really big heading\n" +
        "Text.\n" +
        "!! Just a normal heading [with a hyperlink|Main]\n" +
        "More text.\n" +
        "!Just a small heading.\n" +
        "\n" +
        "This should be __bold__ text.\n" +
        "\n" +
        "__more bold text continuing\n" +
        "on the next line.__\n" +
        "\n" +
        "__more bold text continuing\n" +
        "\n" +
        "on the next paragraph.__\n" +
        "\n" +
        "\n" +
        "This should be normal.\n" +
        "\n" +
        "Now, let's try ''italic text''.\n" +
        "\n" +
        "Bulleted lists:\n" +
        "* One\n" +
        "Or more.\n" +
        "* Two\n" +
        "\n" +
        "** Two.One\n" +
        "\n" +
        "*** Two.One.One\n" +
        "\n" +
        "* Three\n" +
        "\n" +
        "Numbered lists.\n" +
        "# One\n" +
        "# Two\n" +
        "# Three\n" +
        "## Three.One\n" +
        "## Three.Two\n" +
        "## Three.Three\n" +
        "### Three.Three.One\n" +
        "# Four\n" +
        "\n" +
        "End?\n" +
        "\n" +
        "No, let's {{break}} things.\\ {{{ {{{ {{text}} }}} }}}\n" +
        "\n" +
        "More breaking.\n" +
        "\n" +
        "{{{\n" +
        "code.}}\n" +
        "----\n" +
        "author: [Asser], [Ebu], [JanneJalkanen], [Jarmo|mailto:jarmo@regex.com.au]\n";

    public static Test suite()
    {
        return new TestSuite( JSPWikiMarkupParserTest.class );
    }

    public static Test suiteSingle( String test )
    {
        return new TestSuite( JSPWikiMarkupParserTest.class, test );
    }
    public static void main( String[] argv )
    {
        if( argv.length > 0 )
            junit.textui.TestRunner.run(suiteSingle(argv[0]));
        else
            junit.textui.TestRunner.run(suite());
    }
}

