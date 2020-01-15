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

import org.apache.wiki.TestEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class InsertPageTest
{
    protected TestEngine testEngine;
    Properties props = TestEngine.getTestProperties();

    @BeforeEach
    public void setUp() throws Exception
    {
        testEngine = new TestEngine(props);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEngine.deleteTestPage( "ThisPage" );
        testEngine.deleteTestPage( "ThisPage2" );
        testEngine.deleteTestPage( "Test_Page" );
        testEngine.deleteTestPage( "TestPage" );
        testEngine.deleteTestPage( "Test Page" );
    }

    @Test
    public void testRecursive() throws Exception
    {
        String src = "[{InsertPage page='ThisPage'}] [{ALLOW view Anonymous}]";

        testEngine.saveText("ThisPage",src);

        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        String res = testEngine.getRenderingManager().getHTML("ThisPage");
        Assertions.assertTrue( res.indexOf("Circular reference") != -1 );
    }

    @Test
    public void testRecursive2() throws Exception
    {
        String src  = "[{InsertPage page='ThisPage2'}]";
        String src2 = "[{InsertPage page='ThisPage'}]";

        testEngine.saveText("ThisPage",src);
        testEngine.saveText("ThisPage2",src2);

        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        Assertions.assertTrue( testEngine.getRenderingManager().getHTML("ThisPage").indexOf("Circular reference") != -1 );
    }

    @Test
    public void testMultiInvocation() throws Exception {
        String src  = "[{InsertPage page='ThisPage2'}] [{InsertPage page='ThisPage2'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        testEngine.saveText("ThisPage",src);
        testEngine.saveText("ThisPage2",src2);

        Assertions.assertTrue( testEngine.getRenderingManager().getHTML("ThisPage").indexOf("Circular reference") == -1, "got circ ref" );
        Assertions.assertEquals( "<div class=\"inserted-page \" >foo\n</div> <div class=\"inserted-page \" >foo\n</div>\n",
                                 testEngine.getRenderingManager().getHTML("ThisPage"), "found != 2" );
    }

    @Test
    public void testUnderscore() throws Exception {
        String src  = "[{InsertPage page='Test_Page'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        testEngine.saveText("ThisPage",src);
        testEngine.saveText("Test_Page",src2);

        Assertions.assertTrue( testEngine.getRenderingManager().getHTML("ThisPage").indexOf("Circular reference") == -1, "got circ ref" );
        Assertions.assertEquals( "<div class=\"inserted-page \" >foo\n</div>\n", testEngine.getRenderingManager().getHTML("ThisPage"), "found != 1" );
    }

    /**
     * a link containing a blank should work if there is a page with exact the
     * same name ('Test Page')
     */
    @Test
    public void testWithBlanks1() throws Exception {
        testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        testEngine.saveText( "Test Page", "foo[{ALLOW view Anonymous}]" );

        Assertions.assertEquals( "<div class=\"inserted-page \" >foo\n</div>\n", testEngine.getRenderingManager().getHTML( "ThisPage" ), "found != 1" );
    }

    /**
     * same as testWithBlanks1, but it should still work if the page does not have the blank in it ( 'Test Page' should work if the
     * included page is called 'TestPage')
     */
    @Test
    public void testWithBlanks2() throws Exception {
        testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        testEngine.saveText( "TestPage", "foo[{ALLOW view Anonymous}]" );

        Assertions.assertEquals( "<div class=\"inserted-page \" >foo\n</div>\n", testEngine.getRenderingManager().getHTML( "ThisPage" ), "found != 1" );
    }

}
