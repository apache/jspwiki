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
import com.ecyrd.jspwiki.parser.MarkupParserTest;
import com.ecyrd.jspwiki.parser.WikiDocument;

public class CreoleRendererTest extends TestCase
{
    protected TestEngine m_testEngine;
    
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());
        m_testEngine = new TestEngine(props);
        super.setUp();
    }

    private String render(String s) throws IOException
    {
        WikiPage dummyPage = new WikiPage(m_testEngine,"TestPage");
        WikiContext ctx = new WikiContext(m_testEngine,dummyPage);
        
        StringReader in = new StringReader(s);
        
        JSPWikiMarkupParser p = new JSPWikiMarkupParser( ctx, in );
        WikiDocument d = p.parse();
        
        CreoleRenderer cr = new CreoleRenderer( ctx, d );
        
        return cr.getString();
    }
    
    public void testItalic() throws Exception
    {
        String src = "123 ''test'' 456";
        
        assertEquals( "123 //test// 456", render(src) );
    }

    public void testBold() throws Exception
    {
        String src = "123 __test__ 456";
        
        assertEquals( "123 **test** 456", render(src) );
    }

    public void testBoldItalic() throws Exception
    {
        String src = "123 __''test''__ 456";
        
        assertEquals( "123 **//test//** 456", render(src) );
    }
    
    public void testList() throws Exception
    {
        String src = "*one\r\n**two\r\n**three\r\n*four";
        
        assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    public void testList2() throws Exception
    {
        String src = "* one\r\n**        two\r\n** three\r\n* four";
        
        assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    public void testList3() throws Exception
    {
        String src = "*one\r\n**two\r\n**three\r\n*four";
        
        assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    public void testList4() throws Exception
    {
        String src = "# one\r\n##        two\r\n## three\r\n#four";
        
        assertEquals( "# one\n## two\n## three\n# four", render(src) );
    }

    /*
    // FIXME: This class does not work.
    public void testPara() throws Exception
    {
        String src = "aaa\n\nbbb\n\nccc";
        
        assertEquals( src, render(src) );
    }
    */
    public void testInlineImages() throws Exception
    {
        String src = "Testing [{Image src='http://test/image.png'}] plugin.";
        
        assertEquals( "Testing {{http://test/image.png}} plugin.", render(src) );
    }

    public void testPlugins() throws Exception
    {
        String src = "[{Counter}] [{Counter}]";
        
        assertEquals( "<<Counter 1>> <<Counter 2>>", render(src) );
    }
    public void testHeading1() throws Exception
    {
        String src = "!!!Hello";
        
        assertEquals( "== Hello ==", render(src) );
    }

    public void testHeading2() throws Exception
    {
        String src = "!!Hello";
        
        assertEquals( "=== Hello ===", render(src) );
    }
    
    public void testHeading3() throws Exception
    {
        String src = "!Hello";
        
        assertEquals( "==== Hello ====", render(src) );
    }

    public void testExternalAnchor() throws Exception
    {
        String src = "[http://www.jspwiki.org]";
        
        assertEquals( "[[http://www.jspwiki.org]]", render(src) );
    }
    
    public void testExternalAnchor2() throws Exception
    {
        String src = "[JSPWiki|http://www.jspwiki.org]";
        
        assertEquals( "[[http://www.jspwiki.org|JSPWiki]]", render(src) );
    }
    
    public void testLineBreak() throws Exception
    {
        String src = "a\nb\nc";
        
        assertEquals("a\nb\nc", render(src));
    }
    
    public void testPre() throws Exception
    {
        String src = "{{{\n test __foo__ \n}}}";
        
        assertEquals("{{{\n test __foo__ \n}}}", render(src));
    }

    public void testRule() throws Exception
    {
        String src = "a\n----\nb";
        
        assertEquals("a\n----\nb", render(src));
    }


    public static Test suite()
    {
        TestSuite suite = new TestSuite(CreoleRendererTest.class);

        return suite;
    }
}
