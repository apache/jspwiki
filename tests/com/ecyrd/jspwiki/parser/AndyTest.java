package com.ecyrd.jspwiki.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import stress.Benchmark;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.BasicAttachmentProvider;
import com.ecyrd.jspwiki.render.XHTMLRenderer;

public class AndyTest extends TestCase
{
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
    static final String PAGE_NAME = "testpage";

    public static void main( String[] argv )
    {
        if( argv.length > 0 )
            junit.textui.TestRunner.run(suiteSingle(argv[0]));
        else
            junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
    {
        return new TestSuite( AndyTest.class );
    }


    public static Test suiteSingle( String test )
    {
        return new TestSuite( AndyTest.class, test );
    }

    Properties props = new Properties();

    Vector     created = new Vector();

    TestEngine testEngine;

    public AndyTest( String s )
    {
        super( s );
    }

    public void setUp()
    throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true" );
        testEngine = new TestEngine( props );
    }

    public void tearDown()
    {
        deleteCreatedPages();
    }


    public void testAttachmentLink()
    throws Exception
    {
        newPage("Test");

        Attachment att = new Attachment( testEngine, "Test", "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        testEngine.getAttachmentManager().storeAttachment( att, testEngine.makeAttachmentFile() );

        String src = "This should be an [attachment link|Test/TestAtt.txt]";

        String dest =  translate(src);
        
        assertEquals( "This should be an <a class=\"attachment\" href=\"/attach/Test/TestAtt.txt\">attachment link</a>"+
                      "<a href=\"/PageInfo.jsp?page=Test/TestAtt.txt\"><img src=\"/images/attachment_small.png\" border=\"0\" alt=\"(info)\" /></a>",
                     dest );
    }

    public void testHyperlinks2()
    throws Exception
    {
        newPage("Hyperlink");

        String src = "This should be a [hyperlink]";

        assertEquals( "This should be a <a class=\"wikipage\" href=\"/Wiki.jsp?page=Hyperlink\">hyperlink</a>",
                      translate(src) );
    }

    private void deleteCreatedPages()
    {
        for( Iterator i = created.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            TestEngine.deleteTestPage(name);
            testEngine.deleteAttachments(name);
        }

        created.clear();
    }
    
    private void newPage( String name )
        throws WikiException
    {
        testEngine.saveText( name, "<test>" );

        created.addElement( name );
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

    private String translate( WikiEngine e, WikiPage p, String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = e.getWikiActionBeanFactory().newViewActionBean( p );
        JSPWikiMarkupParser tr = new JSPWikiMarkupParser( context, 
                                                          new BufferedReader( new StringReader(src)) );

        XHTMLRenderer conv = new XHTMLRenderer( context, tr.parse() );

        return conv.getString();
    }

    private String translate( WikiPage p, String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        return translate( testEngine, p, src );
    }
    private String translate_nofollow( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException,
               WikiException
    {
        props.load( TestEngine.findTestProperties() );

        props.setProperty( "jspwiki.translatorReader.useRelNofollow", "true" );
        TestEngine testEngine2 = new TestEngine( props );

        WikiContext context = testEngine2.getWikiActionBeanFactory().newViewActionBean(
                                               new WikiPage(testEngine2, PAGE_NAME) );
        JSPWikiMarkupParser r = new JSPWikiMarkupParser( context,
                                                         new BufferedReader( new StringReader(src)) );

        XHTMLRenderer conv = new XHTMLRenderer( context, r.parse() );

        return conv.getString();
    }
}

