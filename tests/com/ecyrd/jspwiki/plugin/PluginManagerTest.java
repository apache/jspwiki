
package com.ecyrd.jspwiki.plugin;

import java.util.Collection;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.plugin.PluginManager.WikiPluginInfo;
import com.ecyrd.jspwiki.providers.ProviderException;

public class PluginManagerTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine engine;

    WikiContext context;

    PluginManager manager;

    public PluginManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine(props);
        context = engine.getWikiActionBeanFactory().newViewActionBean( null, null, new WikiPage(engine, "Testpage") );
        manager = new PluginManager( engine, props );
    }

    public void tearDown() throws ProviderException
    {
        engine.deletePage("Testpage");
    }

    public void testSimpleInsert()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT com.ecyrd.jspwiki.plugin.SamplePlugin WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    public void testSimpleInsertNoPackage()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }


    public void testSimpleInsertNoPackage2()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo" );
        PluginManager m = new PluginManager( engine, props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin2 WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    public void testSimpleInsertNoPackage3()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo" );
        PluginManager m = new PluginManager( engine, props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin3 WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    /** Check that in all cases com.ecyrd.jspwiki.plugin is searched. */
    public void testSimpleInsertNoPackage4()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo,blat.blaa" );
        PluginManager m = new PluginManager( engine, props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }


    public void testSimpleInsert2()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT   com.ecyrd.jspwiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat}");

        assertEquals( "foobar2",
                      res );
    }

    /** Missing closing brace */
    public void testSimpleInsert3()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT   com.ecyrd.jspwiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat");

        assertEquals( "foobar2",
                      res );
    }

    public void testQuotedArgs()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text='this is a space'}");

        assertEquals( "this is a space",
                      res );
    }

    public void testQuotedArgs2()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text='this \\'is a\\' space'}");

        assertEquals( "this 'is a' space",
                      res );
    }

    public void testNumberArgs()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text=15}");

        assertEquals( "15",
                      res );
    }

    public void testNoInsert()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{SamplePlugin WHERE text=15}");

        assertEquals( "15",
                      res );
    }

    // This should be read from tests/etc/ini/jspwiki_module.xml
    public void testAlias()
        throws Exception
    {
        String res = manager.execute( context, "{samplealias text=15}");

        assertEquals( "15", res );
    }

    public void testAlias2()
        throws Exception
    {
        String res = manager.execute( context, "{samplealias2 text=xyzzy}");

        assertEquals( "xyzzy", res );
    }

    public void testInitPlugin() throws Exception
    {
        manager.execute( context, "{JavaScriptPlugin}");

        assertTrue( JavaScriptPlugin.c_inited );
    }

    public void testParserPlugin() throws Exception
    {
        engine.saveText(context, "[{SamplePlugin render=true}]");

        engine.getHTML( "Testpage" );

        assertTrue( SamplePlugin.c_rendered );
    }

    public void testAnnotations() throws Exception
    {
        Collection<WikiPluginInfo> plugins = manager.modules();
        
        for( WikiPluginInfo wpi : plugins )
        {
            if( wpi.getName().equals( "SamplePlugin" ) )
            {
                assertEquals("author", "Urgle Burgle", wpi.getAuthor());
                String[] aliases = wpi.getAliases();
                
                assertNotNull("aliases",aliases);
                assertEquals( "aliases len", 2, aliases.length );
                assertTrue( "data", ( aliases[0].equals( "samplealias2" ) && aliases[1].equals( "samplealias" ) )
                            || (aliases[0].equals("samplealias") && aliases[1].equals("samplealias2")) );
                return; // We're done
            }
        }
        
        fail("No SamplePlugin found");
    }
    
    public static Test suite()
    {
        return new TestSuite( PluginManagerTest.class );
    }
}
