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

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class PageViewPluginTest
{
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;

    WikiContext context;

    PluginManager manager;

    @BeforeEach
    public void setUp() throws Exception
    {
        CacheManager.getInstance().removeAllCaches();
        testEngine = new TestEngine( props );

        // create pages that should be counted
        testEngine.saveText( "TestPage01", "this is test page 01 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage02", "this is test page 02 [{PageViewPlugin}]" );

        manager = new DefaultPluginManager( testEngine, props );
    }

    @AfterEach
    public void tearDown()
    {
        testEngine.deleteTestPage( "TestPage01" );
        testEngine.deleteTestPage( "TestPage02" );
        testEngine.deleteTestPage( "PageViews" );
        TestEngine.emptyWorkDir();
    }

    @Test
    public void testShowCountsBasic() throws Exception
    {
        WikiPage page1 = testEngine.getPageManager().getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPageManager().getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getRenderingManager().getHTML( context1, page1 );
        testEngine.getRenderingManager().getHTML( context2, page2 );
        testEngine.getRenderingManager().getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list''\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPageManager().getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getRenderingManager().getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 01 (2 views)" ) );

        Assertions.assertTrue( result.contains( "Test Page 02 (3 views)" ) );
    }

    @Test
    public void testShowCountsExclude() throws Exception
    {
        testEngine.saveText( "TestPageExcluded", "this is test page that should be excluded [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPageManager().getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPageManager().getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getRenderingManager().getHTML( context1, page1 );
        testEngine.getRenderingManager().getHTML( context2, page2 );
        testEngine.getRenderingManager().getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' exclude='TestPageExcl*' '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPageManager().getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getRenderingManager().getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 01" ) );

        // this page should not have been shown:
        Assertions.assertFalse( result.contains( "Test Page Excluded" ) );

        testEngine.deleteTestPage( "TestPageExcluded" );
    }

    @Test
    public void testShowCountsSorted() throws Exception
    {
        WikiPage page1 = testEngine.getPageManager().getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPageManager().getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getRenderingManager().getHTML( context1, page1 );
        testEngine.getRenderingManager().getHTML( context2, page2 );
        testEngine.getRenderingManager().getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' sort=count '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPageManager().getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getRenderingManager().getHTML( contextPV, pageviews );
//        System.out.println( result );

        int start1 = result.indexOf( "Test Page 01" );
        int start2 = result.indexOf( "Test Page 02" );

        // page2 should be showed before page1
        Assertions.assertTrue( start2 < start1 );
    }

    @Test
    public void testShowCountEntries() throws Exception
    {
        // create pages that should be counted
        testEngine.saveText( "TestPage03", "this is test page 03 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage04", "this is test page 04 [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPageManager().getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPageManager().getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );
        WikiPage page3 = testEngine.getPageManager().getPage( "TestPage03" );
        WikiContext context3 = new WikiContext( testEngine, page3 );
        WikiPage page4 = testEngine.getPageManager().getPage( "TestPage04" );
        WikiContext context4 = new WikiContext( testEngine, page4 );

        // generate counts:
        testEngine.getRenderingManager().getHTML( context1, page1 );
        testEngine.getRenderingManager().getHTML( context2, page2 );
        testEngine.getRenderingManager().getHTML( context2, page2 );
        testEngine.getRenderingManager().getHTML( context3, page3 );
        testEngine.getRenderingManager().getHTML( context4, page4 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' entries=3'\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPageManager().getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getRenderingManager().getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 03" ) );

        Assertions.assertFalse( result.contains( "Test Page 04" ) );

        testEngine.deleteTestPage( "TestPage03" );
        testEngine.deleteTestPage( "TestPage04" );
    }

}
