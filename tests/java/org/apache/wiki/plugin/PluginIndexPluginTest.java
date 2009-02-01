/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.plugin;

import java.util.Collection;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.plugin.PluginManager.WikiPluginInfo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


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
        
        engine.shutdown();
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
