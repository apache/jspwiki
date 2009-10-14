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
    Properties m_props = new Properties();

    TestEngine m_engine;

    WikiContext m_context;

    PluginManager m_manager;

    public ReferredPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( m_props );

        m_engine.saveText( "SomeBodyPointsToMe", "Somebody points to this page" );
        m_engine.saveText( "IPointToSomeoneElse", "Reference to [SomeBodyPointsToMe]." );
        m_engine.saveText( "IPointToSomeoneElseToo", "Reference to [SomeBodyPointsToMe]." );
        m_engine.saveText( "SomeBodyPointsToMeToo", "Somebody points to this page too" );
        m_engine.saveText( "IPointToTwoPages", "Reference to [SomeBodyPointsToMe]  and   [SomeBodyPointsToMeToo]." );
        m_manager = new PluginManager( m_engine, m_props );
    }

    public void tearDown() throws Exception
    {
        m_engine.emptyRepository();
        TestEngine.emptyWorkDir();
        m_engine.shutdown();
    }

    /**
     * Plain test without parameters
     * 
     * @throws Exception
     */
    public void testReferredPage() throws Exception
    {
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "IPointToSomeoneElse" ) );

        String res = m_manager.execute( m_context, "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin}" );

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
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "IPointToSomeoneElse" ) );

        String res = m_manager.execute( m_context, "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin page=IPointToSomeoneElseToo}" );

        assertEquals(
                      "<div class=\"ReferredPagesPlugin\">\n<a class=\"wikipage\" href=\"/Wiki.jsp?page=IPointToSomeoneElseToo\" title=\"ReferredPagesPlugin: depth[1] include[.*] exclude[^$] format[compact]\">IPointToSomeoneElseToo</a>\n<ul>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMe\">SomeBodyPointsToMe</a></li>\n</ul>\n</div>\n",
                      res );
    }

    /**
     * Test with the include parameter (with and without the space name)
     * 
     * @throws Exception
     */
    public void testReferredPageParmInclude() throws Exception
    {
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "IPointToTwoPages" ) );
        String expected = "<div class=\"ReferredPagesPlugin\">\n<a class=\"wikipage\" href=\"/Wiki.jsp?page=IPointToTwoPages\" title=\"ReferredPagesPlugin: depth[1] include[Main:SomeBodyPointsToMe.*] exclude[^$] format[compact]\">IPointToTwoPages</a>\n<ul>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMe\">SomeBodyPointsToMe</a></li>\n<li><a class=\"wikipage\" href=\"/Wiki.jsp?page=SomeBodyPointsToMeToo\">SomeBodyPointsToMeToo</a></li>\n</ul>\n</div>\n";

        String res = m_manager.execute( m_context,
                                      "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin include='SomeBodyPointsToMe*'}" );
        assertEquals( expected, res );
        
        res = m_manager.execute( m_context,
                                      "{INSERT org.apache.wiki.plugin.ReferredPagesPlugin include='Main:SomeBodyPointsToMe*'}" );
        assertEquals( expected, res );
    }
    
    public void testSanitizePattern()
    {
        assertEquals( "Main:Foo", AbstractFilteredPlugin.sanitizePattern( "Foo" ) );
        assertEquals( "Main:Foo*", AbstractFilteredPlugin.sanitizePattern( "Foo*" ) );
        assertEquals( "Test:Foo", AbstractFilteredPlugin.sanitizePattern( "Test:Foo" ) );
        assertEquals( ".*?Foo", AbstractFilteredPlugin.sanitizePattern( ".*?Foo" ) );
    }

    public static Test suite()
    {
        return new TestSuite( ReferredPagesPluginTest.class );
    }
}
