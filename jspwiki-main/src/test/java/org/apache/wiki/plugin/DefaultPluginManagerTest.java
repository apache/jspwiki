/*
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

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class DefaultPluginManagerTest {

    Properties props = TestEngine.getTestProperties();
    WikiEngine engine = TestEngine.build();
    DefaultPluginManager manager = new DefaultPluginManager( engine, props );

    WikiContext context;

    @BeforeEach
    public void setUp() throws Exception {
        context = new WikiContext( engine, new WikiPage(engine, "Testpage") );
    }

    @AfterEach
    public void tearDown() throws ProviderException {
        engine.getPageManager().deletePage("Testpage");
    }

    @Test
    public void testSimpleInsert() throws Exception {
        final String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.SamplePlugin WHERE text=foobar}");
        Assertions.assertEquals( "foobar", res );
    }

    @Test
    public void testSimpleInsertNoPackage() throws Exception {
        final String res = manager.execute( context, "{INSERT SamplePlugin WHERE text=foobar}");
        Assertions.assertEquals( "foobar", res );
    }


    @Test
    public void testSimpleInsertNoPackage2() throws Exception {
        props.setProperty( DefaultPluginManager.PROP_SEARCHPATH, "com.foo" );
        final DefaultPluginManager m = new DefaultPluginManager( engine, props );
        final String res = m.execute( context,"{INSERT SamplePlugin2 WHERE text=foobar}" );
        Assertions.assertEquals( "foobar", res );
    }

    @Test
    public void testSimpleInsertNoPackage3() throws Exception {
        props.setProperty( DefaultPluginManager.PROP_SEARCHPATH, "com.foo" );
        final DefaultPluginManager m = new DefaultPluginManager( engine, props );
        final String res = m.execute( context,"{INSERT SamplePlugin3 WHERE text=foobar}" );
        Assertions.assertEquals( "foobar", res );
    }

    /** Check that in all cases org.apache.wiki.plugin is searched. */
    @Test
    public void testSimpleInsertNoPackage4() throws Exception {
        props.setProperty( DefaultPluginManager.PROP_SEARCHPATH, "com.foo,blat.blaa" );
        final DefaultPluginManager m = new DefaultPluginManager( engine, props );
        final String res = m.execute( context,"{INSERT SamplePlugin WHERE text=foobar}" );
        Assertions.assertEquals( "foobar", res );
    }


    @Test
    public void testSimpleInsert2() throws Exception {
        final String res = manager.execute( context,"{INSERT   org.apache.wiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat}");
        Assertions.assertEquals( "foobar2", res );
    }

    /** Missing closing brace */
    @Test
    public void testSimpleInsert3() throws Exception {
        final String res = manager.execute( context, "{INSERT   org.apache.wiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat");
        Assertions.assertEquals( "foobar2", res );
    }

    @Test
    public void testQuotedArgs() throws Exception {
        final String res = manager.execute( context, "{INSERT SamplePlugin WHERE text='this is a space'}");
        Assertions.assertEquals( "this is a space", res );
    }

    @Test
    public void testQuotedArgs2() throws Exception {
        final String res = manager.execute( context, "{INSERT SamplePlugin WHERE text='this \\'is a\\' space'}" );
        Assertions.assertEquals( "this 'is a' space", res );
    }

    @Test
    public void testNumberArgs() throws Exception {
        final String res = manager.execute( context, "{INSERT SamplePlugin WHERE text=15}" );
        Assertions.assertEquals( "15", res );
    }

    @Test
    public void testNoInsert() throws Exception {
        final String res = manager.execute( context, "{SamplePlugin WHERE text=15}" );
        Assertions.assertEquals( "15", res );
    }

    // This should be read from tests/etc/ini/jspwiki_module.xml
    @Test
    public void testAlias() throws Exception {
        final String res = manager.execute( context, "{samplealias text=15}");
        Assertions.assertEquals( "15", res );
    }

    @Test
    public void testAlias2() throws Exception {
        final String res = manager.execute( context, "{samplealias2 text=xyzzy}");
        Assertions.assertEquals( "xyzzy", res );
    }

    @Test
    public void testInitPlugin() throws Exception {
        manager.execute( context, "{JavaScriptPlugin}");
        Assertions.assertTrue( JavaScriptPlugin.c_inited );
    }

    @Test
    public void testParserPlugin() throws Exception {
        engine.getPageManager().saveText(context, "[{SamplePlugin render=true}]");
        engine.getRenderingManager().getHTML( "Testpage" );
        Assertions.assertTrue( SamplePlugin.c_rendered );
    }

}
