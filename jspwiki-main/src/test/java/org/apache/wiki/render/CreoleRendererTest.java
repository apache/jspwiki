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

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class CreoleRendererTest
{
    protected TestEngine m_testEngine;

    @BeforeEach
    public void setUp() throws Exception
    {
        final Properties props = TestEngine.getTestProperties();
        m_testEngine = new TestEngine(props);
    }

    private String render( final String s) throws IOException
    {
        final Page dummyPage = Wiki.contents().page(m_testEngine,"TestPage");
        final Context ctx = Wiki.context().create(m_testEngine,dummyPage);

        final StringReader in = new StringReader(s);

        final JSPWikiMarkupParser p = new JSPWikiMarkupParser( ctx, in );
        final WikiDocument d = p.parse();

        final CreoleRenderer cr = new CreoleRenderer( ctx, d );

        return cr.getString();
    }

    @Test
    public void testItalic() throws Exception
    {
        final String src = "123 ''test'' 456";

        Assertions.assertEquals( "123 //test// 456", render(src) );
    }

    @Test
    public void testBold() throws Exception
    {
        final String src = "123 __test__ 456";

        Assertions.assertEquals( "123 **test** 456", render(src) );
    }

    @Test
    public void testBoldItalic() throws Exception
    {
        final String src = "123 __''test''__ 456";

        Assertions.assertEquals( "123 **//test//** 456", render(src) );
    }

    @Test
    public void testList() throws Exception
    {
        final String src = "*one\r\n**two\r\n**three\r\n*four";

        Assertions.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList2() throws Exception
    {
        final String src = "* one\r\n**        two\r\n** three\r\n* four";

        Assertions.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList3() throws Exception
    {
        final String src = "*one\r\n**two\r\n**three\r\n*four";

        Assertions.assertEquals( "* one\n** two\n** three\n* four", render(src) );
    }

    @Test
    public void testList4() throws Exception
    {
        final String src = "# one\r\n##        two\r\n## three\r\n#four";

        Assertions.assertEquals( "# one\n## two\n## three\n# four", render(src) );
    }

    /*
    // FIXME: This class does not work.
    @Test
    public void testPara() throws Exception
    {
        String src = "aaa\n\nbbb\n\nccc";

        Assertions.assertEquals( src, render(src) );
    }
    */
    @Test
    public void testInlineImages() throws Exception
    {
        final String src = "Testing [{Image src='http://test/image.png'}] plugin.";

        Assertions.assertEquals( "Testing {{http://test/image.png}} plugin.", render(src) );
    }

    @Test
    public void testPlugins() throws Exception
    {
        final String src = "[{Counter}] [{Counter}]";

        Assertions.assertEquals( "<<Counter 1>> <<Counter 2>>", render(src) );
    }
    /*
    // FIXME: These shouldn't really be Assertions.failing.
    @Test
    public void testHeading1() throws Exception
    {
        String src = "!!!Hello";

        Assertions.assertEquals( "== Hello ==", render(src) );
    }

    @Test
    public void testHeading2() throws Exception
    {
        String src = "!!Hello";

        Assertions.assertEquals( "=== Hello ===", render(src) );
    }

    @Test
    public void testHeading3() throws Exception
    {
        String src = "!Hello";

        Assertions.assertEquals( "==== Hello ====", render(src) );
    }
*/
    @Test
    public void testExternalAnchor() throws Exception
    {
        final String src = "[http://jspwiki.apache.org]";

        Assertions.assertEquals( "[[http://jspwiki.apache.org]]", render(src) );
    }

    @Test
    public void testExternalAnchor2() throws Exception
    {
        final String src = "[JSPWiki|http://jspwiki.apache.org]";

        Assertions.assertEquals( "[[http://jspwiki.apache.org|JSPWiki]]", render(src) );
    }

    @Test
    public void testLineBreak() throws Exception
    {
        final String src = "a\nb\nc";

        Assertions.assertEquals("a\nb\nc", render(src));
    }

    @Test
    public void testPre() throws Exception
    {
        final String src = "{{{\n test __foo__ \n}}}";

        Assertions.assertEquals("{{{\n test __foo__ \n}}}", render(src));
    }

    @Test
    public void testRule() throws Exception
    {
        final String src = "a\n----\nb";

        Assertions.assertEquals("a\n----\nb", render(src));
    }

}
