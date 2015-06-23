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

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;

public class PageViewPluginTest extends TestCase

{
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;

    WikiContext context;

    PluginManager manager;

    public static Test suite()
    {
        return new TestSuite( PageViewPluginTest.class );
    }

    public void setUp() throws Exception
    {
        CacheManager.getInstance().removeAllCaches();
        testEngine = new TestEngine( props );

        // create pages that should be counted
        testEngine.saveText( "TestPage01", "this is test page 01 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage02", "this is test page 02 [{PageViewPlugin}]" );

        manager = new DefaultPluginManager( testEngine, props );
    }

    public void tearDown()
    {
        testEngine.deleteTestPage( "TestPage01" );
        testEngine.deleteTestPage( "TestPage02" );
        testEngine.deleteTestPage( "PageViews" );
        TestEngine.emptyWorkDir();
    }

    public void testShowCountsBasic() throws Exception
    {
        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list''\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
//        System.out.println( result );

        assertTrue( result.contains( "Test Page 01 (2 views)" ) );

        assertTrue( result.contains( "Test Page 02 (3 views)" ) );
    }

    public void testShowCountsExclude() throws Exception
    {
        testEngine.saveText( "TestPageExcluded", "this is test page that should be excluded [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' exclude='TestPageExcl*' '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
//        System.out.println( result );

        assertTrue( result.contains( "Test Page 01" ) );
        
        // this page should not have been shown:
        assertFalse( result.contains( "Test Page Excluded" ) );

        testEngine.deleteTestPage( "TestPageExcluded" );
    }

    public void testShowCountsSorted() throws Exception
    {
        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );

        // generate counts:
        testEngine.getHTML( context1, page1 );
        testEngine.getHTML( context2, page2 );
        testEngine.getHTML( context2, page2 );

        // mind the double \n in the following string:
        String pageViewPageContent = "[{PageViewPlugin show='list' sort=count '\n\n* {1} ({2} views)\n}]";
        testEngine.saveText( "PageViews", pageViewPageContent );

        WikiPage pageviews = testEngine.getPage( "PageViews" );
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
//        System.out.println( result );

        int start1 = result.indexOf( "Test Page 01" );
        int start2 = result.indexOf( "Test Page 02" );
        
        // page2 should be showed before page1
        assertTrue( start2 < start1 );
    }

    public void testShowCountEntries() throws Exception
    {
        // create pages that should be counted
        testEngine.saveText( "TestPage03", "this is test page 03 [{PageViewPlugin}]" );
        testEngine.saveText( "TestPage04", "this is test page 04 [{PageViewPlugin}]" );

        WikiPage page1 = testEngine.getPage( "TestPage01" );
        WikiContext context1 = new WikiContext( testEngine, page1 );
        WikiPage page2 = testEngine.getPage( "TestPage02" );
        WikiContext context2 = new WikiContext( testEngine, page2 );
        WikiPage page3 = testEngine.getPage( "TestPage03" );
        WikiContext context3 = new WikiContext( testEngine, page3 );
        WikiPage page4 = testEngine.getPage( "TestPage04" );
        WikiContext context4 = new WikiContext( testEngine, page4 );

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
        WikiContext contextPV = new WikiContext( testEngine, pageviews );

        String result = testEngine.getHTML( contextPV, pageviews );
//        System.out.println( result );

        assertTrue( result.contains( "Test Page 03" ) );

        assertFalse( result.contains( "Test Page 04" ) );
        
        testEngine.deleteTestPage( "TestPage03" );
        testEngine.deleteTestPage( "TestPage04" );
    }

}
