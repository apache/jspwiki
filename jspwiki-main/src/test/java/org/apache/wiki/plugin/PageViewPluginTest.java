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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
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
        final Page page1 = testEngine.getManager( PageManager.class ).getPage( "TestPage01" );
        final Context context1 = Wiki.context().create( testEngine, page1 );
        final Page page2 = testEngine.getManager( PageManager.class ).getPage( "TestPage02" );
        final Context context2 = Wiki.context().create( testEngine, page2 );

        // generate counts:
        testEngine.getManager( RenderingManager.class ).getHTML( context1, page1 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );

        // mind the double \n in the following string:
        final String pageViewPageContent = "[{PageViewPlugin show='list''\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        final Page pageviews = testEngine.getManager( PageManager.class ).getPage( "PageViews" );
        final Context contextPV = Wiki.context().create( testEngine, pageviews );

        final String result = testEngine.getManager( RenderingManager.class ).getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 01 (2 views)" ) );

        Assertions.assertTrue( result.contains( "Test Page 02 (3 views)" ) );
    }

    @Test
    public void testShowCountsExclude() throws Exception
    {
        testEngine.saveText( "TestPageExcluded", "this is test page that should be excluded [{PageViewPlugin}]" );

        final Page page1 = testEngine.getManager( PageManager.class ).getPage( "TestPage01" );
        final Context context1 = Wiki.context().create( testEngine, page1 );
        final Page page2 = testEngine.getManager( PageManager.class ).getPage( "TestPage02" );
        final Context context2 = Wiki.context().create( testEngine, page2 );

        // generate counts:
        testEngine.getManager( RenderingManager.class ).getHTML( context1, page1 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );

        // mind the double \n in the following string:
        final String pageViewPageContent = "[{PageViewPlugin show='list' exclude='TestPageExcl*' '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        final Page pageviews = testEngine.getManager( PageManager.class ).getPage( "PageViews" );
        final Context contextPV = Wiki.context().create( testEngine, pageviews );

        final String result = testEngine.getManager( RenderingManager.class ).getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 01" ) );

        // this page should not have been shown:
        Assertions.assertFalse( result.contains( "Test Page Excluded" ) );

        testEngine.deleteTestPage( "TestPageExcluded" );
    }

    @Test
    public void testShowCountsSorted() throws Exception
    {
        final Page page1 = testEngine.getManager( PageManager.class ).getPage( "TestPage01" );
        final Context context1 = Wiki.context().create( testEngine, page1 );
        final Page page2 = testEngine.getManager( PageManager.class ).getPage( "TestPage02" );
        final Context context2 = Wiki.context().create( testEngine, page2 );

        // generate counts:
        testEngine.getManager( RenderingManager.class ).getHTML( context1, page1 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );

        // mind the double \n in the following string:
        final String pageViewPageContent = "[{PageViewPlugin show='list' sort=count '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        final Page pageviews = testEngine.getManager( PageManager.class ).getPage( "PageViews" );
        final Context contextPV = Wiki.context().create( testEngine, pageviews );

        final String result = testEngine.getManager( RenderingManager.class ).getHTML( contextPV, pageviews );
//        System.out.println( result );

        final int start1 = result.indexOf( "Test Page 01" );
        final int start2 = result.indexOf( "Test Page 02" );

        // page2 should be showed before page1
        Assertions.assertTrue( start2 < start1 );
    }

    @Test
    public void testShowCountEntries() throws Exception
    {
        // create pages that should be counted
        testEngine.saveText( "TestPage03", "this is test page 03 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage04", "this is test page 04 [{PageViewPlugin}]" );

        final Page page1 = testEngine.getManager( PageManager.class ).getPage( "TestPage01" );
        final Context context1 = Wiki.context().create( testEngine, page1 );
        final Page page2 = testEngine.getManager( PageManager.class ).getPage( "TestPage02" );
        final Context context2 = Wiki.context().create( testEngine, page2 );
        final Page page3 = testEngine.getManager( PageManager.class ).getPage( "TestPage03" );
        final Context context3 = Wiki.context().create( testEngine, page3 );
        final Page page4 = testEngine.getManager( PageManager.class ).getPage( "TestPage04" );
        final Context context4 = Wiki.context().create( testEngine, page4 );

        // generate counts:
        testEngine.getManager( RenderingManager.class ).getHTML( context1, page1 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );
        testEngine.getManager( RenderingManager.class ).getHTML( context2, page2 );
        testEngine.getManager( RenderingManager.class ).getHTML( context3, page3 );
        testEngine.getManager( RenderingManager.class ).getHTML( context4, page4 );

        // mind the double \n in the following string:
        final String pageViewPageContent = "[{PageViewPlugin show='list' entries=3'\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        final Page pageviews = testEngine.getManager( PageManager.class ).getPage( "PageViews" );
        final Context contextPV = Wiki.context().create( testEngine, pageviews );

        final String result = testEngine.getManager( RenderingManager.class ).getHTML( contextPV, pageviews );
//        System.out.println( result );

        Assertions.assertTrue( result.contains( "Test Page 03" ) );

        Assertions.assertFalse( result.contains( "Test Page 04" ) );

        testEngine.deleteTestPage( "TestPage03" );
        testEngine.deleteTestPage( "TestPage04" );
    }

}
