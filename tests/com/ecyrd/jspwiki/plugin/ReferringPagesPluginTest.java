
package com.ecyrd.jspwiki.plugin;

import java.text.SimpleDateFormat;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

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
        props.load( TestEngine.findTestProperties() );

        props.setProperty( "jspwiki.breakTitleWithSpaces", "false" );
        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [TestPage]." );
        engine.saveText( "Foobar2", "Reference to [TestPage]." );
        engine.saveText( "Foobar3", "Reference to [TestPage]." );
        engine.saveText( "Foobar4", "Reference to [TestPage]." );
        engine.saveText( "Foobar5", "Reference to [TestPage]." );
        engine.saveText( "Foobar6", "Reference to [TestPage]." );
        engine.saveText( "Foobar7", "Reference to [TestPage]." );

        context = new WikiContext( engine, engine.newHttpRequest(), new WikiPage(engine,"TestPage") );
        manager = new PluginManager( engine, props );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "TestPage" );
        TestEngine.deleteTestPage( "Foobar" );
        TestEngine.deleteTestPage( "Foobar2" );
        TestEngine.deleteTestPage( "Foobar3" );
        TestEngine.deleteTestPage( "Foobar4" );
        TestEngine.deleteTestPage( "Foobar5" );
        TestEngine.deleteTestPage( "Foobar6" );
        TestEngine.deleteTestPage( "Foobar7" );
    }

    private String mkLink( String page )
    {
        return mkFullLink( page, page );
    }

    private String mkFullLink( String page, String link )
    {
        return "<a class=\"wikipage\" href=\"/Wiki.jsp?page="+link+"\">"+page+"</a>";
    }

    public void testSingleReferral()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE max=5}");

        assertEquals( mkLink( "TestPage" )+"<br />",
                      res );
    }

    public void testMaxReferences()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE max=5}");
    
        int count = 0;
        int index = -1;
    
        // Count the number of hyperlinks.  We could check their
        // correctness as well, though.
    
        while( (index = res.indexOf("<a",index+1)) != -1 )
        {
            count++;
        }
    
        // there is one extra "<a" in the result 
        assertEquals( 5+1, count );
    
        String expected = ">...and 2 more</a>";
        count =0;
        while( (index = res.indexOf(expected,index+1)) != -1 )
        {
            count++;
        }
        assertEquals("End", 1, count );
    }

    public void testReferenceWidth()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.ReferringPagesPlugin WHERE maxwidth=5}");

        assertEquals( mkFullLink( "TestP...", "TestPage" )+"<br />",
                      res );
    }

    public void testInclude()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin include='*7'}" );

        assertTrue( "7", res.indexOf("Foobar7") != -1 );
        assertTrue( "6", res.indexOf("Foobar6") == -1 );
        assertTrue( "5", res.indexOf("Foobar5") == -1 );
        assertTrue( "4", res.indexOf("Foobar4") == -1 );
        assertTrue( "3", res.indexOf("Foobar3") == -1 );
        assertTrue( "2", res.indexOf("Foobar2") == -1 );
    }

    public void testExclude()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin exclude='*'}");

        assertEquals( "...nobody",
                      res );
    }

    public void testExclude2()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin exclude='*7'}");

        assertTrue( res.indexOf("Foobar7") == -1 );
    }

    public void testExclude3()
       throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin exclude='*7,*5,*4'}");

        assertTrue( "7", res.indexOf("Foobar7") == -1 );
        assertTrue( "6", res.indexOf("Foobar6") != -1 );
        assertTrue( "5", res.indexOf("Foobar5") == -1 );
        assertTrue( "4", res.indexOf("Foobar4") == -1 );
        assertTrue( "3", res.indexOf("Foobar3") != -1 );
        assertTrue( "2", res.indexOf("Foobar2") != -1 );
    }

    public void testCount() throws Exception
    {
        String result = null;
        result = manager.execute(context, "{ReferringPagesPlugin show=count}");
        assertEquals("7",result);
        
        result = manager.execute(context, "{ReferringPagesPlugin,exclude='*7',show=count}");
        assertEquals("6",result);
        
        result = manager.execute(context, "{ReferringPagesPlugin,exclude='*7',show=count,showLastModified=true}");
        String numberResult=result.substring(0,result.indexOf(" "));
        assertEquals("6",numberResult);
        
        String dateString = result.substring(result.indexOf("(")+1,result.indexOf(")"));
        // the date should be parseable:
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy zzz", engine.newHttpRequest().getLocale());
        df.parse(dateString);

        // test if the proper exception is thrown:
        String expectedExceptionString = "showLastModified=true is only valid if show=count is also specified";
        String exceptionString = null;
        try
        {
            result = manager.execute(context, "{ReferringPagesPlugin,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        assertEquals(expectedExceptionString, exceptionString);
    }

    public static Test suite()
    {
        return new TestSuite( ReferringPagesPluginTest.class );
    }
}
