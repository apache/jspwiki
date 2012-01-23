package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

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
        TestEngine.deleteTestPage( "TestPage01" );
        TestEngine.deleteTestPage( "TestPage02" );
        TestEngine.deleteTestPage( "PageViews" );
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

        TestEngine.deleteTestPage( "TestPageExcluded" );
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
        
        TestEngine.deleteTestPage( "TestPage03" );
        TestEngine.deleteTestPage( "TestPage04" );
    }


    public static Test suite()
    {
        return new TestSuite( PageViewPluginTest.class );
    }
}
