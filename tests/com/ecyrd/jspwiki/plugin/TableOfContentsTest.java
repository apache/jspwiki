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
/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import org.apache.jspwiki.api.WikiException;

import com.ecyrd.jspwiki.TestEngine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 *  @since 
 */
public class TableOfContentsTest extends TestCase
{
    TestEngine testEngine;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load(TestEngine.findTestProperties());
        
        testEngine = new TestEngine( props );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        testEngine.deletePage( "Test" );
        
        testEngine.shutdown();
    }

    public void testHeadingVariables()
        throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{TableOfContents}]\n\n!!!Heading [{$foo}]";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: The <p> should not be here.
        assertEquals( "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
                      "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
                      "<ul>\n"+
                      "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">Heading bar</a></li>\n"+
                      "</ul>\n</div>\n</div>\n\n</p>"+
                      "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>\n",
                      res );
    }

    public void testNumberedItems()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
                "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
                "<ul>\n"+
                "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">3 Heading bar</a></li>\n"+
                "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading\">3.1 Subheading</a></li>\n"+
                "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading\">3.1.1 Subsubheading</a></li>\n"+
                "</ul>\n</div>\n</div>\n\n</p>"+
                "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
                "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
                "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>\n";
                
        assertEquals(expecting,
                res );
    }
    
    public void testNumberedItemsComplex()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading\n\n!Subsubheading2\n\n!!Subheading2\n\n!Subsubheading3\n\n!!!Heading\n\n!!Subheading3";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">3 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading\">3.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading\">3.1.1 Subsubheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading2\">3.1.2 Subsubheading2</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading2\">3.2 Subheading2</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading3\">3.2.1 Subsubheading3</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Heading\">4 Heading</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading3\">4.1 Subheading3</a></li>\n"+
        "</ul>\n</div>\n</div>\n\n</p>"+
        "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>"+
        "\n<h4 id=\"section-Test-Subsubheading2\">Subsubheading2<a class=\"hashlink\" href=\"#section-Test-Subsubheading2\">#</a></h4>"+
        "\n<h3 id=\"section-Test-Subheading2\">Subheading2<a class=\"hashlink\" href=\"#section-Test-Subheading2\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading3\">Subsubheading3<a class=\"hashlink\" href=\"#section-Test-Subsubheading3\">#</a></h4>"+
        "\n<h2 id=\"section-Test-Heading\">Heading<a class=\"hashlink\" href=\"#section-Test-Heading\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading3\">Subheading3<a class=\"hashlink\" href=\"#section-Test-Subheading3\">#</a></h3>\n";
        
        assertEquals(expecting,
                res );
    }
    
    public void testNumberedItemsComplex2()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!Subheading0\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading\n\n!Subsubheading2\n\n!!Subheading2\n\n!Subsubheading3\n\n!!!Heading\n\n!!Subheading3";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading0\">3.1 Subheading0</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">4 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading\">4.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading\">4.1.1 Subsubheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading2\">4.1.2 Subsubheading2</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading2\">4.2 Subheading2</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading3\">4.2.1 Subsubheading3</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Heading\">5 Heading</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading3\">5.1 Subheading3</a></li>\n"+
        "</ul>\n</div>\n</div>\n\n</p>"+
        "\n<h3 id=\"section-Test-Subheading0\">Subheading0<a class=\"hashlink\" href=\"#section-Test-Subheading0\">#</a></h3>"+
        "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>"+
        "\n<h4 id=\"section-Test-Subsubheading2\">Subsubheading2<a class=\"hashlink\" href=\"#section-Test-Subsubheading2\">#</a></h4>"+
        "\n<h3 id=\"section-Test-Subheading2\">Subheading2<a class=\"hashlink\" href=\"#section-Test-Subheading2\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading3\">Subsubheading3<a class=\"hashlink\" href=\"#section-Test-Subsubheading3\">#</a></h4>"+
        "\n<h2 id=\"section-Test-Heading\">Heading<a class=\"hashlink\" href=\"#section-Test-Heading\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading3\">Subheading3<a class=\"hashlink\" href=\"#section-Test-Subheading3\">#</a></h3>\n";
        
        assertEquals(expecting,
                     res );
    }
    
    public void testNumberedItemsWithPrefix()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3,prefix=FooBar-}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">FooBar-3 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subheading\">FooBar-3.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-Subsubheading\">FooBar-3.1.1 Subsubheading</a></li>\n"+
        "</ul>\n</div>\n</div>\n\n</p>"+
        "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>\n";
        
        assertEquals(expecting,
                res );
    }
    
    /**
     *  Tests BugTableOfContentsCausesHeapdump
     *  
     * @throws Exception
     */
    public void testSelfReference()
        throws Exception
    {
        String src = "!!![{TableOfContents}]";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        assertTrue( res.indexOf("Table of Contents") != -1 );
    }
    
    public void testHTML()
        throws Exception
    {
        String src = "[{TableOfContents}]\n\n!<i>test</i>";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        assertTrue( "<i>", res.indexOf("<i>") == -1 ); // Check that there is no HTML left
        assertTrue( "</i>", res.indexOf("</i>") == -1 ); // Check that there is no HTML left
        
    }
    
    public void testSimilarNames() throws WikiException
    {
        String src = "[{TableOfContents}]\n\n!Test\n\n!Test\n\n";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );

        assertTrue( "Final HTML 1", res.indexOf(  "id=\"section-Test-Test\"" ) != -1 );
        assertTrue( "Final HTML 2", res.indexOf(  "id=\"section-Test-Test-2\"" ) != -1 );

        assertTrue( "First test", res.indexOf( "Wiki.jsp?page=Test#section-Test-Test" ) != -1 );
        assertTrue( "2nd test",   res.indexOf( "Wiki.jsp?page=Test#section-Test-Test-2" ) != -1 );
        
    }
    public static Test suite()
    {
        return new TestSuite( TableOfContentsTest.class );
    }
    
}
