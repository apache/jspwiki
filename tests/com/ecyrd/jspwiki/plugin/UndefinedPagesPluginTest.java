
package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

public class UndefinedPagesPluginTest extends TestCase
{
    Properties props = new Properties();
    TestEngine engine;
    WikiContext context;
    PluginManager manager;

    public UndefinedPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar 2], [Foobars]" );

        context = engine.getWikiActionBeanFactory().newViewWikiContext( null, null, new WikiPage(engine, "TestPage") );
        manager = new PluginManager( engine, props );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "TestPage" );
        TestEngine.deleteTestPage( "Foobar" );
        TestEngine.emptyWorkDir();
    }

    private String wikitize( String s )
    {
        return engine.textToHTML( context, s );
    }

    /**
     *  Tests that only correct undefined links are found.
     *  We also check against plural forms here, which should not
     *  be listed as non-existant.
     */
    public void testSimpleUndefined()
        throws Exception
    {
        WikiContext context2 = engine.getWikiActionBeanFactory().newViewWikiContext( null, null, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.UndefinedPagesPlugin");

        String exp = "[Foobar 2]\\\\";

        assertEquals( wikitize(exp), res );
    }

    public void testCount() throws Exception
    {
        String result = null;
        result = manager.execute(context, "{UndefinedPagesPlugin show=count}");
        assertEquals("1", result);

        // test if the proper exception is thrown:
        String expectedExceptionString = "parameter showLastModified is not valid for the UndefinedPagesPlugin";
        String exceptionString = null;
        try
        {
            result = manager.execute(context, "{UndefinedPagesPlugin,show=count,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        assertEquals(expectedExceptionString, exceptionString);
    }

    public static Test suite()
    {
        return new TestSuite( UndefinedPagesPluginTest.class );
    }
}
