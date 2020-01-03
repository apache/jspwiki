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
/*
 * (C) Janne Jalkanen 2005
 *
 */
package org.apache.wiki.plugin;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 *  @since
 */
public class TableOfContentsTest
{
    TestEngine testEngine = TestEngine.build();

    @AfterEach
    public void tearDown() throws Exception {
        testEngine.getPageManager().deletePage( "Test" );
    }

    @Test
    public void testHeadingVariables()
        throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{TableOfContents}]\n\n!!!Heading [{$foo}]";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        // FIXME: The <p> should not be here.
        Assertions.assertEquals( "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
                      "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
                      "<ul>\n"+
                      "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-HeadingBar\">Heading bar</a></li>\n"+
                      "</ul>\n</div>\n</div>\n\n</p>"+
                      "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>\n",
                      res );
    }

    @Test
    public void testNumberedItems()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
                "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
                "<ul>\n"+
                "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-HeadingBar\">3 Heading bar</a></li>\n"+
                "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading\">3.1 Subheading</a></li>\n"+
                "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading\">3.1.1 Subsubheading</a></li>\n"+
                "</ul>\n</div>\n</div>\n\n</p>"+
                "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
                "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
                "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>\n";

        Assertions.assertEquals(expecting, res );
    }

    @Test
    public void testNumberedItemsComplex()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading\n\n!Subsubheading2\n\n!!Subheading2\n\n!Subsubheading3\n\n!!!Heading\n\n!!Subheading3";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-HeadingBar\">3 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading\">3.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading\">3.1.1 Subsubheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading2\">3.1.2 Subsubheading2</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading2\">3.2 Subheading2</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading3\">3.2.1 Subsubheading3</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-Heading\">4 Heading</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading3\">4.1 Subheading3</a></li>\n"+
        "</ul>\n</div>\n</div>\n\n</p>"+
        "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>"+
        "\n<h4 id=\"section-Test-Subsubheading2\">Subsubheading2<a class=\"hashlink\" href=\"#section-Test-Subsubheading2\">#</a></h4>"+
        "\n<h3 id=\"section-Test-Subheading2\">Subheading2<a class=\"hashlink\" href=\"#section-Test-Subheading2\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading3\">Subsubheading3<a class=\"hashlink\" href=\"#section-Test-Subsubheading3\">#</a></h4>"+
        "\n<h2 id=\"section-Test-Heading\">Heading<a class=\"hashlink\" href=\"#section-Test-Heading\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading3\">Subheading3<a class=\"hashlink\" href=\"#section-Test-Subheading3\">#</a></h3>\n";

        Assertions.assertEquals(expecting,
                res );
    }

    @Test
    public void testNumberedItemsComplex2()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3}]\n\n!!Subheading0\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading\n\n!Subsubheading2\n\n!!Subheading2\n\n!Subsubheading3\n\n!!!Heading\n\n!!Subheading3";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading0\">3.1 Subheading0</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-HeadingBar\">4 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading\">4.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading\">4.1.1 Subsubheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading2\">4.1.2 Subsubheading2</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading2\">4.2 Subheading2</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading3\">4.2.1 Subsubheading3</a></li>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-Heading\">5 Heading</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading3\">5.1 Subheading3</a></li>\n"+
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

        Assertions.assertEquals(expecting,
                     res );
    }

    @Test
    public void testNumberedItemsWithPrefix()
    throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{INSERT TableOfContents WHERE numbered=true,start=3,prefix=FooBar-}]\n\n!!!Heading [{$foo}]\n\n!!Subheading\n\n!Subsubheading";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        // FIXME: The <p> should not be here.
        String expecting = "<p><div class=\"toc\">\n<div class=\"collapsebox\">\n"+
        "<h4 id=\"section-TOC\">Table of Contents</h4>\n"+
        "<ul>\n"+
        "<li class=\"toclevel-1\"><a class=\"wikipage\" href=\"#section-Test-HeadingBar\">FooBar-3 Heading bar</a></li>\n"+
        "<li class=\"toclevel-2\"><a class=\"wikipage\" href=\"#section-Test-Subheading\">FooBar-3.1 Subheading</a></li>\n"+
        "<li class=\"toclevel-3\"><a class=\"wikipage\" href=\"#section-Test-Subsubheading\">FooBar-3.1.1 Subsubheading</a></li>\n"+
        "</ul>\n</div>\n</div>\n\n</p>"+
        "\n<h2 id=\"section-Test-HeadingBar\">Heading bar<a class=\"hashlink\" href=\"#section-Test-HeadingBar\">#</a></h2>"+
        "\n<h3 id=\"section-Test-Subheading\">Subheading<a class=\"hashlink\" href=\"#section-Test-Subheading\">#</a></h3>"+
        "\n<h4 id=\"section-Test-Subsubheading\">Subsubheading<a class=\"hashlink\" href=\"#section-Test-Subsubheading\">#</a></h4>\n";

        Assertions.assertEquals(expecting, res );
    }

    /**
     *  Tests BugTableOfContentsCausesHeapdump
     *
     * @throws Exception
     */
    @Test
    public void testSelfReference()
        throws Exception
    {
        String src = "!!![{TableOfContents}]";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        Assertions.assertTrue( res.indexOf("Table of Contents") != -1 );
    }

    @Test
    public void testHTML()
        throws Exception
    {
        String src = "[{TableOfContents}]\n\n!<i>test</i>";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        Assertions.assertTrue( res.indexOf("<i>") == -1, "<i>" ); // Check that there is no HTML left
        Assertions.assertTrue( res.indexOf("</i>") == -1, "</i>" ); // Check that there is no HTML left
    }

    @Test
    public void testSimilarNames() throws WikiException
    {
        String src = "[{TableOfContents}]\n\n!Test\n\n!Test\n\n";

        testEngine.saveText( "Test", src );

        String res = testEngine.getI18nHTML( "Test" );

        Assertions.assertTrue( res.indexOf(  "id=\"section-Test-Test\"" ) != -1, "Final HTML 1" );
        Assertions.assertTrue( res.indexOf(  "id=\"section-Test-Test-2\"" ) != -1, "Final HTML 2" );
        Assertions.assertTrue( res.indexOf( "#section-Test-Test" ) != -1, "First test" );
        Assertions.assertTrue( res.indexOf( "#section-Test-Test-2" ) != -1, "2nd test" );
    }

}
