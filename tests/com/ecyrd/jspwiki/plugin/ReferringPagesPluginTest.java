
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;

public class ReferringPagesPluginTest extends TestCase
{
    Properties props = new Properties();
    TestEngine engine;
    WikiContext context;
    PluginManager manager;
    
    public ReferringPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [TestPage]." );
        engine.saveText( "Foobar2", "Reference to [TestPage]." );
        engine.saveText( "Foobar3", "Reference to [TestPage]." );
        engine.saveText( "Foobar4", "Reference to [TestPage]." );
        engine.saveText( "Foobar5", "Reference to [TestPage]." );
        engine.saveText( "Foobar6", "Reference to [TestPage]." );
        engine.saveText( "Foobar7", "Reference to [TestPage]." );

        context = new WikiContext( engine, "TestPage" );
        manager = new PluginManager( props );
    }

    public void tearDown()
    {
        engine.deletePage( "TestPage" );
        engine.deletePage( "Foobar" );
        engine.deletePage( "Foobar2" );
        engine.deletePage( "Foobar3" );
        engine.deletePage( "Foobar4" );
        engine.deletePage( "Foobar5" );
        engine.deletePage( "Foobar6" );
        engine.deletePage( "Foobar7" );
    }

    private String mkLink( String page )
    {
        return mkFullLink( page, page );
    }

    private String mkFullLink( String page, String link )
    {
        return "<A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page="+link+"\">"+page+"</A>";        
    }

    public void testSingleReferral()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, "Foobar" );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE max=5}");

        assertEquals( mkLink( "TestPage" )+"\n<BR />",
                      res );
    }

    public void testMaxReferences()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE max=5}");

        int count = 0, index = -1;

        // Count the number of hyperlinks.  We could check their
        // correctness as well, though.

        while( (index = res.indexOf("<A",index+1)) != -1 )
        {
            count++;
        }

        assertEquals( 5, count );

        String expected = "...and 2 more<BR />";

        assertEquals( "End", expected, 
                      res.substring( res.length()-expected.length() ) );
    }

    public void testReferenceWidth()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, "Foobar" );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE maxwidth=5}");

        assertEquals( mkFullLink( "TestP...", "TestPage" )+"\n<BR />",
                      res );        
    }

    public static Test suite()
    {
        return new TestSuite( ReferringPagesPluginTest.class );
    }
}
