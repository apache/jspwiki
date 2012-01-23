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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;


public class RecentChangesPluginTest extends TestCase
{
    Properties m_props = new Properties();

    TestEngine m_engine;

    WikiContext m_context;

    PluginManager m_pluginmanager;

    public void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( m_props );

        m_engine.saveText( "RecentChangesPlugin01", "Some Text for testing 01" );
        m_engine.saveText( "RecentChangesPlugin02", "Some Text for testing 02" );
        m_engine.saveText( "RecentChangesPlugin03", "Some Text for testing 03" );

        m_pluginmanager = new PluginManager( m_engine, m_props );
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
    public void testSimple() throws Exception
    {
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "RecentChangesPlugin01" ) );

        String res = m_pluginmanager.execute( m_context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin}" );

        // we don't want to compare the complete html returned, but check if certain Strings are present and other 
        // Strings are not present
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin01'>RecentChangesPlugin01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin02'>RecentChangesPlugin02</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin03'>RecentChangesPlugin03</a>" ));

    }

    /**
     * Test with the include parameter
     * 
     * @throws Exception
     */
    public void testParmInclude() throws Exception
    {
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "RecentChangesPlugin02" ) );

        String res = m_pluginmanager.execute( m_context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin include='RecentChangesPlugin02*'}" );
        
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin01'>RecentChangesPlugin01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin02'>RecentChangesPlugin02</a>" ));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin03'>RecentChangesPlugin03</a>" ));

    }

    /**
     * Test with the exclude parameter
     * 
     * @throws Exception
     */
    public void testParmExclude() throws Exception
    {
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.getPage(  "RecentChangesPlugin03" ) );

        String res = m_pluginmanager.execute( m_context, "{INSERT RecentChangesPlugin exclude='RecentChangesPlugin03*'}" );
        
        assertTrue(res.contains( "<table cellpadding='4' class='recentchanges'>"));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin01'>RecentChangesPlugin01</a>" ));
        assertTrue(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin02'>RecentChangesPlugin02</a>" ));
        assertFalse(res.contains( "<a href='/Wiki.jsp?page=RecentChangesPlugin03'>RecentChangesPlugin03</a>" ));

    }

    public static Test suite()
    {
        return new TestSuite( RecentChangesPluginTest.class );
    }
}
