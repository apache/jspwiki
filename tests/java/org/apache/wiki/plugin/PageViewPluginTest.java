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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.providers.ProviderException;

public class PageViewPluginTest extends TestCase

{
    Properties props = new Properties();

    TestEngine testEngine;

    WikiContext context;

    PluginManager manager;

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        testEngine = new TestEngine( props );

        // create pages that should be counted
        testEngine.saveText( "TestPage01", "this is test page 01 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage02", "this is test page 02 [{PageViewPlugin}]" );

        manager = new PluginManager( testEngine, props );
    }

    public void tearDown()
    {
        try
        {
            testEngine.deletePage( "TestPage01" );
            testEngine.deletePage( "TestPage02" );
            testEngine.deletePage( "PageViews" );
        }
        catch( ProviderException e )
        {
            e.printStackTrace();
        }
        TestEngine.emptyWorkDir();
    }

    public void testShowCountsBasic() throws Exception
    {
        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = testEngine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = testEngine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list''\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = testEngine.getWikiContextFactory().newViewContext( pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "TestPage01</a> (2 views)" ) );

        assertTrue( result.contains( "TestPage02</a> (3 views)" ) );
    }

    public void testShowCountsExclude() throws Exception
    {
        testEngine.saveText( "TestPageExcluded", "this is test page that should be excluded [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = testEngine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = testEngine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' exclude='TestPageExcl*' '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = testEngine.getWikiContextFactory().newViewContext( pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "TestPage01" ) );

        // this page should not have been shown:
        assertFalse( result.contains( "TestPageExcluded" ) );

        testEngine.deletePage( "TestPageExcluded" );
    }

    public void testShowCountsSorted() throws Exception
    {
        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = testEngine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = testEngine.getWikiContextFactory().newViewContext( page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' sort=count '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = testEngine.getWikiContextFactory().newViewContext( pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        int start1 = result.indexOf( "TestPage01" );
        int start2 = result.indexOf( "TestPage02" );

        // page2 should be showed before page1
        assertTrue( start2 < start1 );
    }

    public void testShowCountEntries() throws Exception
    {
        // create pages that should be counted
        testEngine.saveText( "TestPage03", "this is test page 03 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage04", "this is test page 04 [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = testEngine.getWikiContextFactory().newViewContext( page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = testEngine.getWikiContextFactory().newViewContext( page2 );
        WikiPage page3 = testEngine.getPage( "TestPage03" );
        WikiContext context3 = testEngine.getWikiContextFactory().newViewContext( page3 );
        WikiPage page4 = testEngine.getPage( "TestPage04" );
        WikiContext context4 = testEngine.getWikiContextFactory().newViewContext( page4 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context3, page3 );
        testEngine.getHTML( context4, page4 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' entries=3'\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = testEngine.getWikiContextFactory().newViewContext( pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
        // System.out.println( result );

        assertTrue( result.contains( "TestPage03" ) );

        assertFalse( result.contains( "TestPage04" ) );

        testEngine.deletePage( "TestPage03" );
        testEngine.deletePage( "TestPage04" );
    }

    public static Test suite()
    {
        return new TestSuite( PageViewPluginTest.class );
    }
}
