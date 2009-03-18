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
import org.apache.wiki.WikiEngine;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.plugin.PluginManager.WikiPluginInfo;
import org.apache.wiki.providers.ProviderException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


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
        engine.deletePage( "Testpage" );
        context = engine.getWikiContextFactory().newViewContext( engine.createPage( "Testpage" ) );
        manager = new PluginManager( engine, props );
    }

    public void tearDown()
    {
        try
        {
            engine.deletePage("Testpage");
        }
        catch ( ProviderException e )
        {
        }
        engine.shutdown();
    }

    public void testSimpleInsert()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.SamplePlugin WHERE text=foobar}");

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

    /** Check that in all cases org.apache.wiki.plugin is searched. */
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
                                      "{INSERT   org.apache.wiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat}");

        assertEquals( "foobar2",
                      res );
    }

    /** Missing closing brace */
    public void testSimpleInsert3()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT   org.apache.wiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat");

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
