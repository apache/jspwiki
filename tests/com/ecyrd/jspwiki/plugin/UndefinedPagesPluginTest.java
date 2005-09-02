
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.util.*;

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
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars]" );

        context = new WikiContext( engine, new WikiPage(engine, "TestPage") );
        manager = new PluginManager( props );
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
        WikiContext context2 = new WikiContext( engine, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT com.ecyrd.jspwiki.plugin.UndefinedPagesPlugin");

        String exp = "[Foobar 2]\\\\";

        assertEquals( wikitize(exp), res );
    }

    public static Test suite()
    {
        return new TestSuite( UndefinedPagesPluginTest.class );
    }
}
