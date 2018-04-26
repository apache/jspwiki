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
package org.apache.wiki.render;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CreoleRendererTest
{
    protected TestEngine m_testEngine;

    @Before
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        m_testEngine = new TestEngine(props);
    }

    private String render(String s) throws IOException
    {
        WikiPage dummyPage = new WikiPage(m_testEngine,"TestPage");
        WikiContext ctx = new WikiContext(m_testEngine,dummyPage);

        StringReader in = new StringReader(s);

        JSPWikiMarkupParser p = new JSPWikiMarkupParser( ctx, in );
        WikiDocument d = p.parse();

        CreoleRenderer cr = new CreoleRenderer( ctx, d );

        return cr.getString();
    }

    @Test
    public void testItalic() throws Exception
    {
        String src = "123 ''test'' 456";

        Assert.assertEquals( "123 //test// 456", render(src) );
    }

    @Test
    public void testBold() throws Exception
    {
        String src = "123 __test__ 456";

        Assert.assertEquals( "123 **test** 456", render(src) );
    }

    @Test
    public void testBoldItalic() throws Exception
    {
        String src = "123 __''test''__ 456";

        Assert.assertEquals( "123 **//test//** 456", render(src) );
    }

    @Test
    public void testList() throws Exception
    {
        String src = "*one\r\n**two\r\n**three\r\n*four";

        Assert.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList2() throws Exception
    {
        String src = "* one\r\n**        two\r\n** three\r\n* four";

        Assert.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList3() throws Exception
    {
        String src = "*one\r\n**two\r\n**three\r\n*four";

        Assert.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList4() throws Exception
    {
        String src = "# one\r\n##        two\r\n## three\r\n#four";

        Assert.assertEquals( "# one\n## two\n## three\n# four", render(src) );
    }

    /*
    // FIXME: This class does not work.
    @Test
    public void testPara() throws Exception
    {
        String src = "aaa\n\nbbb\n\nccc";

        Assert.assertEquals( src, render(src) );
    }
    */
    @Test
    public void testInlineImages() throws Exception
    {
        String src = "Testing [{Image src='http://test/image.png'}] plugin.";

        Assert.assertEquals( "Testing {{http://test/image.png}} plugin.", render(src) );
    }

    @Test
    public void testPlugins() throws Exception
    {
        String src = "[{Counter}] [{Counter}]";

        Assert.assertEquals( "<<Counter 1>> <<Counter 2>>", render(src) );
    }
    /*
    // FIXME: These shouldn't really be Assert.failing.
    @Test
    public void testHeading1() throws Exception
    {
        String src = "!!!Hello";

        Assert.assertEquals( "== Hello ==", render(src) );
    }

    @Test
    public void testHeading2() throws Exception
    {
        String src = "!!Hello";

        Assert.assertEquals( "=== Hello ===", render(src) );
    }

    @Test
    public void testHeading3() throws Exception
    {
        String src = "!Hello";

        Assert.assertEquals( "==== Hello ====", render(src) );
    }
*/
    @Test
    public void testExternalAnchor() throws Exception
    {
        String src = "[http://jspwiki.apache.org]";

        Assert.assertEquals( "[[http://jspwiki.apache.org]]", render(src) );
    }

    @Test
    public void testExternalAnchor2() throws Exception
    {
        String src = "[JSPWiki|http://jspwiki.apache.org]";

        Assert.assertEquals( "[[http://jspwiki.apache.org|JSPWiki]]", render(src) );
    }

    @Test
    public void testLineBreak() throws Exception
    {
        String src = "a\nb\nc";

        Assert.assertEquals("a\nb\nc", render(src));
    }

    @Test
    public void testPre() throws Exception
    {
        String src = "{{{\n test __foo__ \n}}}";

        Assert.assertEquals("{{{\n test __foo__ \n}}}", render(src));
    }

    @Test
    public void testRule() throws Exception
    {
        String src = "a\n----\nb";

        Assert.assertEquals("a\n----\nb", render(src));
    }

}
