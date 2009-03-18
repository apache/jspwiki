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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;

public class InterWikiLinksPluginTest extends TestCase
{
    private Properties props = new Properties();

    private TestEngine engine;

    private WikiContext context;

    private PluginManager manager;

    private Collection<String> m_links = null;

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine( props );

        manager = new PluginManager( engine, props );

        engine.deletePage( "TestPage" );
        context = engine.getWikiContextFactory().newViewContext( engine.createPage( "TestPage" ) );

        m_links = engine.getAllInterWikiLinks();

    }

    public void tearDown()
    {
        TestEngine.emptyWorkDir();

        engine.shutdown();
    }

    public static Test suite()
    {
        return new TestSuite( InterWikiLinksPluginTest.class );
    }

    /**
     * Test if all interwikilinks are shown
     * 
     * @throws PluginException
     */
    public void testLinkCollection() throws PluginException
    {
        String result = manager.execute( context, "{InterWikiLinksPlugin}" );

        boolean allLinksFound = true;
        for( String link : m_links )
        {
            if( !result.contains( link ) )
            {
                allLinksFound = false;
            }
        }

        assertTrue( "none, or not all wikiLinks found", allLinksFound );
    }

    /**
     * Test if a table format is presented
     * 
     * @throws PluginException
     */
    public void testTableFormat() throws PluginException
    {
        String title = "DirtyHarryWikiLinks";
        String result = manager.execute( context, "{InterWikiLinksPlugin type=TABLE,tabletitle=" + title + "}" );

        assertTrue( "no table found", result.contains( "<table>" ) );

        assertTrue( "no (correct) table title found", result.contains( title ) );
    }
}
