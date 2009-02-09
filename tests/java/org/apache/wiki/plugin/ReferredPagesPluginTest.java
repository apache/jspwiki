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
import org.apache.wiki.plugin.PluginManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ReferredPagesPluginTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    WikiContext context;

    PluginManager manager;

    public ReferredPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine( props );

        engine.saveText( "SomeBodyPointsToMe", "Somebody points to this page" );
        engine.saveText( "IPointToSomeoneElse", "Reference to [SomeBodyPointsToMe]." );
        engine.saveText( "IPointToSomeoneElseToo", "Reference to [SomeBodyPointsToMe]." );
        engine.saveText( "SomeBodyPointsToMeToo", "Somebody points to this page too" );
        engine.saveText( "IPointToTwoPages", "Reference to [SomeBodyPointsToMe]  and   [SomeBodyPointsToMeToo]." );

//        context = engine.getWikiContextFactory().newViewContext( null, null, engine.createPage( "IPointToSomeoneElse" ) );
        manager = new PluginManager( engine, props );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "SomeBodyPointsToMe" );
        TestEngine.deleteTestPage( "IPointToSomeoneElse" );
        TestEngine.deleteTestPage( "IPointToSomeoneElseToo" );
        TestEngine.deleteTestPage( "SomeBodyPointsToMeToo");
        TestEngine.deleteTestPage( "IPointToTwoPages" );
        
        TestEngine.emptyWorkDir();
        
        engine.shutdown();
    }

    /**
     * Plain test without parameters
     * 
     * @throws Exception
     */
    public void testReferredPage() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.createPage(  "IPointToSomeoneElse" ) );

        String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin}" );

        assertEquals(
                      "<div class=\"ReferredPagesPlugin\">\n<a class=\"wikipage\" href=\"/Wiki.jsp?page=IPointToSomeoneElse\" title=\"ReferredPagesPlugin: depth[1] include[.*] exclude[^$] format[compact]\">IPointToSomeoneElse</a>\n<ul>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMe\">SomeBodyPointsToMe</a></li>\n</ul>\n</div>\n",
                      res );
    }

    /**
     * Test with the page parameter (page A tells us which pages page B points
     * to)
     * 
     * @throws Exception
     */
    public void testReferredPageParmPage() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.createPage(  "IPointToSomeoneElse" ) );

        String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin page=IPointToSomeoneElseToo}" );

        assertEquals(
                      "<div class=\"ReferredPagesPlugin\">\n<a class=\"wikipage\" href=\"/Wiki.jsp?page=IPointToSomeoneElseToo\" title=\"ReferredPagesPlugin: depth[1] include[.*] exclude[^$] format[compact]\">IPointToSomeoneElseToo</a>\n<ul>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMe\">SomeBodyPointsToMe</a></li>\n</ul>\n</div>\n",
                      res );
    }

    /**
     * Test with the include parameter
     * 
     * @throws Exception
     */
    public void testReferredPageParmInClude() throws Exception
    {
        context = engine.getWikiContextFactory().newViewContext( null, null, engine.createPage(  "IPointToTwoPages" ) );

        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin include='SomeBodyPointsToMe*'}" );

        assertEquals(
                      "<div class=\"ReferredPagesPlugin\">\n<a class=\"wikipage\" href=\"/Wiki.jsp?page=IPointToTwoPages\" title=\"ReferredPagesPlugin: depth[1] include[SomeBodyPointsToMe*] exclude[^$] format[compact]\">IPointToTwoPages</a>\n<ul>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMe\">SomeBodyPointsToMe</a></li>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMeToo\">SomeBodyPointsToMeToo</a></li>\n</ul>\n</div>\n",
                      res );

    }

    public static Test suite()
    {
        return new TestSuite( ReferredPagesPluginTest.class );
    }
}
