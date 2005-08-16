package com.ecyrd.jspwiki.render;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.time.StopWatch;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;

public class RenderingManagerTest extends TestCase
{
    RenderingManager m_manager;
    TestEngine       m_engine;
    
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine( props );
        
        m_manager = new RenderingManager();
        m_manager.initialize( m_engine, props );
    }

    protected void tearDown() throws Exception
    {
        m_engine.deletePage( "TestPage" );
    }
        
    /**
     * Tests the relative speed of the DOM cache with respect to
     * page being parsed every single time.
     * @throws Exception
     */
    public void testCache()
        throws Exception
    {
        m_engine.saveText( "TestPage", TEST_TEXT );
        
        StopWatch sw = new StopWatch();
        
        System.out.println("DOM cache speed test:");
        sw.start();
        
        for( int i = 0; i < 100; i++ )
        {
            WikiPage page = m_engine.getPage( "TestPage" );
            String pagedata = m_engine.getPureText( page );
            
            WikiContext context = new WikiContext( m_engine, page );
            
            MarkupParser p = m_manager.getParser( context, pagedata );
            
            WikiDocument d = p.parse();
            
            String html = m_manager.getHTML( context, d );
            assertNotNull( "noncached got null response",html);
        }
        
        sw.stop();
        System.out.println("  Nocache took "+sw);

        long nocachetime = sw.getTime();
        
        sw.reset();
        sw.start();
        
        for( int i = 0; i < 100; i++ )
        {
            WikiPage page = m_engine.getPage( "TestPage" );
            String pagedata = m_engine.getPureText( page );
            
            WikiContext context = new WikiContext( m_engine, page );
            
            String html = m_manager.getHTML( context, pagedata );
            
            assertNotNull("cached got null response",html);
        }
        
        sw.stop();
        System.out.println("  Cache took "+sw);
        
        long speedup = nocachetime / sw.getTime();
        System.out.println("  Approx speedup: "+speedup+"x");
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( RenderingManagerTest.class );
        
        return suite;
    }
    
    private static final String TEST_TEXT =
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
}
