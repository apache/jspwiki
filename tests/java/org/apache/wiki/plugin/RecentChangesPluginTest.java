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

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.plugin.PluginManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class RecentChangesPluginTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    WikiContext context;

    PluginManager manager;

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine( props );

        engine.saveText( "TestPage01", "Some Text for testing 01" );
        engine.saveText( "TestPage02", "Some Text for testing 02" );
        engine.saveText( "TestPage03", "Some Text for testing 03" );

//        context = engine.getWikiContextFactory().newViewContext( null, null, engine.createPage( "TestPage01" ) );
        manager = new PluginManager( engine, props );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "TestPage01" );
        TestEngine.deleteTestPage( "TestPage02" );
        TestEngine.deleteTestPage( "TestPage03" );
        
        TestEngine.emptyWorkDir();
        
        engine.shutdown();
    }

    /**
     * Plain test without parameters
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.getPage(  "TestPage01" ) );

        String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin}" );

        // we don't want to compare the complete html returned, but check if certain Strings are present and other 
        // Strings are not present
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage01'>TestPage01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage02'>TestPage02</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage03'>TestPage03</a>" ));

    }

    /**
     * Test with the include parameter
     * 
     * @throws Exception
     */
    public void testParmInClude() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.getPage(  "TestPage02" ) );

        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin include='TestPage02*'}" );
        
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=TestPage01'>TestPage01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage02'>TestPage02</a>" ));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=TestPage03'>TestPage03</a>" ));

    }

    /**
     * Test with the exclude parameter
     * 
     * @throws Exception
     */
    public void testParmExClude() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.getPage(  "TestPage03" ) );

        String res = manager.execute( context, "{INSERT RecentChangesPlugin exclude='TestPage03*'}" );
        
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage01'>TestPage01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=TestPage02'>TestPage02</a>" ));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=TestPage03'>TestPage03</a>" ));

    }

    public static Test suite()
    {
        return new TestSuite( RecentChangesPluginTest.class );
    }
}
