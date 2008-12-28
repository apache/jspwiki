package com.ecyrd.jspwiki.plugin;

import java.util.Collection;
import java.util.Properties;

import org.apache.jspwiki.api.PluginException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginManager.WikiPluginInfo;

public class PluginIndexPluginTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    WikiContext context;

    PluginManager manager;

    Collection<WikiPluginInfo> m_requiredPlugins;

    public static final String[] REQUIRED_COLUMNS = { "Name", "Class Name", "alias's", "author", "minVersion", "maxVersion",
                                                     "adminBean Class" };

    public PluginIndexPluginTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine( props );

        manager = new PluginManager( engine, props );

        context = engine.getWikiContextFactory().newViewContext( engine.createPage( "TestPage" ) );

        m_requiredPlugins = context.getEngine().getPluginManager().modules();
    }

    public void tearDown()
    {
        TestEngine.emptyWorkDir();
    }

    public static Test suite()
    {
        return new TestSuite( PluginIndexPluginTest.class );
    }

    /**
     * Test for the presence of all core plugins
     * 
     * @throws PluginException
     */
    public void testCorePluginsPresent() throws PluginException
    {
        String result = manager.execute( context, "{PluginIndexPlugin details=false}" );

        // test for the presence of each core plugin (this list can be expanded
        // as new plugins are added)
        for( WikiPluginInfo pluginInfo : m_requiredPlugins )
        {
            String name = pluginInfo.getName();
            assertTrue( "plugin '" + name + "' missing", result.contains( name ) );
        }
    }

    /**
     * Test for : PluginIndexPlugin details=true Shows the plugin names
     * including all attributes
     * 
     * @throws PluginException
     */
    public void testDetails() throws PluginException
    {
        String result = manager.execute( context, "{PluginIndexPlugin details=true}" );

        // check for the presence of all required columns:
        for( int i = 0; i < REQUIRED_COLUMNS.length; i++ )
        {
            assertTrue( "plugin '" + REQUIRED_COLUMNS[i] + "' missing", result.contains( REQUIRED_COLUMNS[i] ) );
        }
    }

    /**
     * Test for the number of rows returned (should be equal to the number of
     * plugins found)
     * 
     * @throws PluginException
     */
    public void testNumberOfRows() throws PluginException
    {
        String result = manager.execute( context, "{PluginIndexPlugin details=true}" );

        String row = "<tr";
        String[] pieces = result.split( row );
        int numRows = pieces.length - 2;
        assertEquals( "unexpected number of rows", m_requiredPlugins.size(), numRows );
    }
}
