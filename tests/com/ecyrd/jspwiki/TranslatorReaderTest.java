
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;

public class TranslatorReaderTest extends TestCase
{
    Properties props = new Properties();

    public TranslatorReaderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
    }

    public void tearDown()
    {
    }

    private String translate( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = new WikiContext( new TestEngine(props),
                                               "testpage" );
        Reader r = new TranslatorReader( context, 
                                         new BufferedReader( new StringReader(src)) );
        StringWriter out = new StringWriter();
        int c;

        while( ( c=r.read()) != -1 )
        {
            out.write( c );
        }

        return out.toString();
    }

    public void testHyperlinks2()
        throws Exception
    {
        String src = "This should be a [hyperlink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Hyperlink\">hyperlink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks3()
        throws Exception
    {
        String src = "This should be a [hyperlink too]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperlinkToo\">hyperlink too</A>\n",
                      translate(src) );
    }

    public void testHyperlinks4()
        throws Exception
    {
        String src = "This should be a [HyperLink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks5()
        throws Exception
    {
        String src = "This should be a [here|HyperLink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">here</A>\n",
                      translate(src) );
    }

    public void testHyperlinksExt()
        throws Exception
    {
        String src = "This should be a [http://www.regex.fi/]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.regex.fi/\">http://www.regex.fi/</A>\n",
                      translate(src) );
    }

    public void testHyperlinksExt2()
        throws Exception
    {
        String src = "This should be a [link|http://www.regex.fi/]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.regex.fi/\">link</A>\n",
                      translate(src) );
    }

    public void testHyperlinkJS1()
        throws Exception
    {
        String src = "This should be a [link|http://www.haxored.com/\" onMouseOver=\"alert('Hahhaa');\"]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.haxored.com/&quot; onMouseOver=&quot;alert('Hahhaa');&quot;\">link</A>\n",
                      translate(src) );
    }

    public void testHyperlinksInterWiki1()
        throws Exception
    {
        String src = "This should be a [link|JSPWiki:HyperLink]";

        assertEquals( "This should be a <A CLASS=\"interwiki\" HREF=\"http://www.ecyrd.com/JSPWiki/Wiki.jsp?page=HyperLink\">link</A>\n",
                      translate(src) );
    }

    public void testExtraPagename1()
        throws Exception
    {
        String src = "Link [test_page]";

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Test_page\">test_page</A>\n",
                     translate(src) );
    }

    public void testExtraPagename2()
        throws Exception
    {
        String src = "Link [test.page]";

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Test.page\">test.page</A>\n",
                     translate(src) );
    }

    public void testExtraPagename3()
        throws Exception
    {
        String src = "Link [.testpage_]";

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=.testpage_\">.testpage_</A>\n",
                     translate(src) );
    }

    public void testInlineImages()
        throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.png]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://www.ecyrd.com/test.png\" ALT=\"test\">\n",
                     translate(src) );
    }

    public void testInlineImages2()
        throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.ppm]";

        assertEquals("Link <A CLASS=\"external\" HREF=\"http://www.ecyrd.com/test.ppm\">test</A>\n",
                     translate(src) );
    }

    public void testInlineImages3()
        throws Exception
    {
        String src = "Link [test|http://images.com/testi]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://images.com/testi\" ALT=\"test\">\n",
                     translate(src) );
    }

    public void testInlineImages4()
        throws Exception
    {
        String src = "Link [test|http://foobar.jpg]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://foobar.jpg\" ALT=\"test\">\n",
                     translate(src) );
    }

    public void testScandicPagename1()
        throws Exception
    {
        String src = "Link [Â‰Test]";

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=%C5%E4Test\">Â‰Test</A>\n",
                     translate(src));
    }

    public void testParagraph()
        throws Exception
    {
        String src = "1\n\n2\n\n3";

        assertEquals( "1\n<P>\n2\n<P>\n3\n", translate(src) );
    }



    public void testLinebreak()
        throws Exception
    {
        String src = "1\\\\2";

        assertEquals( "1<BR>2\n", translate(src) );
    }

    public void testTT()
        throws Exception
    {
        String src = "1{{2345}}6";

        assertEquals( "1<TT>2345</TT>6\n", translate(src) );
    }

    public void testTTAcrossLines()
        throws Exception
    {
        String src = "1{{\n2345\n}}6";

        assertEquals( "1<TT>\n2345\n</TT>6\n", translate(src) );
    }

    public void testTTLinks()
        throws Exception
    {
        String src = "1{{\n2345\n[a link]\n}}6";

        assertEquals( "1<TT>\n2345\n<A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ALink\">a link</A>\n</TT>6\n", translate(src) );
    }

    public void testPre()
        throws Exception
    {
        String src = "1{{{2345}}}6";

        assertEquals( "1<PRE>2345</PRE>6\n", translate(src) );
    }

    public void testPre2()
        throws Exception
    {
        String src = "1 {{{ {{{ 2345 }}} }}} 6";

        assertEquals( "1 <PRE> {{{ 2345 </PRE> }}} 6\n", translate(src) );
    }

    public void testList1()
        throws Exception
    {
        String src = "A list:\n* One\n* Two\n* Three\n";

        assertEquals( "A list:\n<UL>\n<LI> One\n<LI> Two\n<LI> Three\n</UL>\n", 
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

        assertEquals( "A list:\n<UL>\n<LI> One\n continuing.\n<LI> Two\n<LI> Three\n</UL>\n", 
                      translate(src) );
    }

    public void testMultilineList2()
        throws Exception
    {
        String src = "A list:\n* One\n continuing.\n* Two\n* Three\nShould be normal.";

        assertEquals( "A list:\n<UL>\n<LI> One\n continuing.\n<LI> Two\n<LI> Three\n</UL>\nShould be normal.\n", 
                      translate(src) );
    }

    public void testHTML()
        throws Exception
    {
        String src = "<B>Test</B>";

        assertEquals( "&lt;B&gt;Test&lt;/B&gt;\n", translate(src) );
    }

    public void testHTML2()
        throws Exception
    {
        String src = "<P>";

        assertEquals( "&lt;P&gt;\n", translate(src) );
    }

    public void testQuotes()
        throws Exception
    {
        String src = "\"Test\"\"";

        assertEquals( "&quot;Test&quot;&quot;\n", translate(src) );
    }

    public void testItalicAcrossLinebreak()
        throws Exception
    {
        String src="''This is a\ntest.''";

        assertEquals( "<I>This is a\ntest.</I>\n", translate(src) );
    }

    public void testBoldAcrossLinebreak()
        throws Exception
    {
        String src="__This is a\ntest.__";

        assertEquals( "<B>This is a\ntest.</B>\n", translate(src) );
    }

    public void testBoldItalic()
        throws Exception
    {
        String src="__This ''is'' a test.__";

        assertEquals( "<B>This <I>is</I> a test.</B>\n", translate(src) );
    }

    public void testFootnote1()
        throws Exception
    {
        String src="Footnote[1]";

        assertEquals( "Footnote<A CLASS=\"footnoteref\" HREF=\"#ref-testpage-1\">[1]</A>\n", 
                      translate(src) );
    }

    public void testFootnote2()
        throws Exception
    {
        String src="[#2356] Footnote.";

        assertEquals( "<A CLASS=\"footnote\" NAME=\"ref-testpage-2356\">[#2356]</A> Footnote.\n", 
                      translate(src) );
    }

    /** Check an reported error condition where empty list items could cause crashes */

    public void testEmptySecondLevelList()
        throws Exception
    {
        String src="A\n\n**\n\nB";

        assertEquals( "A\n<P>\n<UL>\n<UL>\n<LI>\n</UL>\n</UL>\n<P>\nB\n", 
                      translate(src) );
    }

    public void testEmptySecondLevelList2()
        throws Exception
    {
        String src="A\n\n##\n\nB";

        assertEquals( "A\n<P>\n<OL>\n<OL>\n<LI>\n</OL>\n</OL>\n<P>\nB\n", 
                      translate(src) );
    }

    public void testPluginInsert()
        throws Exception
    {
        String src="[{INSERT com.ecyrd.jspwiki.plugin.SamplePlugin WHERE text=test}]";

        assertEquals( "test\n", translate(src) );
    }

    public void testTable1()
        throws Exception
    {
        String src="|| heading || heading2 \n| Cell 1 | Cell 2 \n| Cell 3 | Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TH> heading <TH> heading2 </TR>\n"+
                      "<TR><TD> Cell 1 <TD> Cell 2 </TR>\n"+
                      "<TR><TD> Cell 3 <TD> Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTable2()
        throws Exception
    {
        String src="||heading||heading2\n|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TH>heading<TH>heading2</TR>\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD> Cell 3 <TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTable3()
        throws Exception
    {
        String src="|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD> Cell 3 <TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTableLink()
        throws Exception
    {
        String src="|Cell 1| Cell 2\n|[Cell 3|ReallyALink]|Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD><A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ReallyALink\">Cell 3</A><TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }
    
    public static Test suite()
    {
        return new TestSuite( TranslatorReaderTest.class );
    }
}
