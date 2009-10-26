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

import java.io.File;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.providers.ProviderException;

public class PageViewPluginTest extends TestCase

{
    private TestEngine m_engine;

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( props );
        
        // Clean page counters
        File file = new File ( m_engine.getWorkDir(), PageViewPlugin.COUNTER_PAGE );
        if ( file.exists() )
        {
            file.delete();
        }

        // Create pages that should be counted
        m_engine.deletePage( "PageViewPluginTest01" );
        m_engine.deletePage( "PageViewPluginTest02" );
        m_engine.deletePage( "PageViewPluginTest03" );
        m_engine.deletePage( "PageViewPluginTest04" );
        m_engine.deletePage( "PageViews" );
        m_engine.saveText( "PageViewPluginTest01", "this is test page 01 [{PageViewPlugin}]" );
        m_engine.saveText( "PageViewPluginTest02", "this is test page 02 [{PageViewPlugin}]" );

        new PluginManager( m_engine, props );
    }

    public void tearDown()
    {
        try
        {
            m_engine.deletePage( "PageViewPluginTest01" );
            m_engine.deletePage( "PageViewPluginTest02" );
            m_engine.deletePage( "PageViews" );
            m_engine.shutdown();
        }
        catch( ProviderException e )
        {
            e.printStackTrace();
        }
        TestEngine.emptyWorkDir();
    }

    public void testShowCountsBasic() throws Exception
    {
        WikiPage page1 = m_engine.getPage( "PageViewPluginTest01" );
        WikiContext context1 = m_engine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = m_engine.getPage( "PageViewPluginTest02" );
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        m_engine.getHTML( context1, page1 );
        m_engine.getHTML( context2, page2 );
        m_engine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list''\n\n* {1} ({2} views)\n}]";
        m_engine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = m_engine.getPage( "PageViews" );
        WikiContext contextPV = m_engine.getWikiContextFactory().newViewContext( pageviews );

        String result = m_engine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "PageViewPluginTest01</a> (2 views)" ) );

        assertTrue( result.contains( "PageViewPluginTest02</a> (3 views)" ) );
    }

    public void testShowCountsExclude() throws Exception
    {
        m_engine.saveText( "TestPageExcluded", "this is test page that should be excluded [{PageViewPlugin}]" );

        WikiPage page1 = m_engine.getPage( "PageViewPluginTest01" );
        WikiContext context1 = m_engine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = m_engine.getPage( "PageViewPluginTest02" );
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        m_engine.getHTML( context1, page1 );
        m_engine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' exclude='TestPageExcl*' '\n\n* {1} ({2} views)\n}]";
        m_engine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = m_engine.getPage( "PageViews" );
        WikiContext contextPV = m_engine.getWikiContextFactory().newViewContext( pageviews );

        String result = m_engine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "PageViewPluginTest01" ) );

        // this page should not have been shown:
        assertFalse( result.contains( "TestPageExcluded" ) );

        m_engine.deletePage( "TestPageExcluded" );
    }

    public void testShowCountsSorted() throws Exception
    {
        WikiPage page1 = m_engine.getPage( "PageViewPluginTest01" );
        WikiContext context1 = m_engine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = m_engine.getPage( "PageViewPluginTest02" );
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        m_engine.getHTML( context1, page1 );
        m_engine.getHTML( context2, page2 );
        m_engine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' sort=count '\n\n* {1} ({2} views)\n}]";
        m_engine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = m_engine.getPage( "PageViews" );
        WikiContext contextPV = m_engine.getWikiContextFactory().newViewContext( pageviews );

        String result = m_engine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        int start1 = result.indexOf( "PageViewPluginTest01" );
        int start2 = result.indexOf( "PageViewPluginTest02" );

        // page2 should be showed before page1
        assertTrue( start2 < start1 );
    }

    public void testShowCountEntries() throws Exception
    {
        // create pages that should be counted
        m_engine.saveText( "PageViewPluginTest03", "this is test page 03 [{PageViewPlugin}]" );
        m_engine.saveText( "PageViewPluginTest04", "this is test page 04 [{PageViewPlugin}]" );

        WikiPage page1 = m_engine.getPage( "PageViewPluginTest01" );
        WikiContext context1 = m_engine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = m_engine.getPage( "PageViewPluginTest02" );
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( page2 );
        WikiPage page3 = m_engine.getPage( "PageViewPluginTest03" );
        WikiContext context3 = m_engine.getWikiContextFactory().newViewContext( page3 );
        WikiPage page4 = m_engine.getPage( "PageViewPluginTest04" );
        WikiContext context4 = m_engine.getWikiContextFactory().newViewContext( page4 );

        // generate counts:
        m_engine.getHTML( context1, page1 );
        m_engine.getHTML( context2, page2 );
        m_engine.getHTML( context2, page2 );
        m_engine.getHTML( context3, page3 );
        m_engine.getHTML( context4, page4 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' entries=3'\n\n* {1} ({2} views)\n}]";
        m_engine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = m_engine.getPage( "PageViews" );
        WikiContext contextPV = m_engine.getWikiContextFactory().newViewContext( pageviews );

        String result = m_engine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "PageViewPluginTest03" ) );

        assertFalse( result.contains( "PageViewPluginTest04" ) );

        m_engine.deletePage( "PageViewPluginTest03" );
        m_engine.deletePage( "PageViewPluginTest04" );
    }

    public static Test suite()
    {
        return new TestSuite( PageViewPluginTest.class );
    }
}
