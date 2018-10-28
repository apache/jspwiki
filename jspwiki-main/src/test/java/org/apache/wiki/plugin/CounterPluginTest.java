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
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.render.WikiRenderer;
import org.apache.wiki.render.XHTMLRenderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CounterPluginTest
{
    Properties props = TestEngine.getTestProperties();
    TestEngine testEngine;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        testEngine = new TestEngine(props);
    }

    private String translate( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = new WikiContext( testEngine,
                                               new WikiPage(testEngine, "TestPage") );

        MarkupParser p = new JSPWikiMarkupParser( context, new StringReader(src) );

        WikiDocument dom = p.parse();

        WikiRenderer r = new XHTMLRenderer( context, dom );

        return r.getString();
    }

    @Test
    public void testSimpleCount()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}]";

        Assertions.assertEquals( "1, 2", translate(src) );
    }

    @Test
    public void testSimpleVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}], [{$counter}]";

        Assertions.assertEquals( "1, 2, 2",
                      translate(src) );
    }

    @Test
    public void testTwinVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter name=aa}], [{$counter-aa}]";

        Assertions.assertEquals( "1, 1, 1",
                      translate(src) );
    }

    @Test
    public void testIncrement()
        throws Exception
    {
        String src = "[{Counter}], [{Counter increment=9}]";

        Assertions.assertEquals( "1, 10", translate(src) );

        src = "[{Counter}],[{Counter}], [{Counter increment=-8}]";

        Assertions.assertEquals( "1,2, -6", translate(src) );
    }

    @Test
    public void testIncrement2() throws Exception
    {
        String src = "[{Counter start=5}], [{Counter increment=-1}], [{Counter increment=-1}]";

        Assertions.assertEquals( "5, 4, 3", translate(src) );

        src = "[{Counter}],[{Counter start=11}], [{Counter increment=-8}]";

        Assertions.assertEquals( "1,11, 3", translate(src) );
    }

    @Test
    public void testShow()
        throws Exception
    {
        String src = "[{Counter}],[{Counter showResult=false}],[{Counter}]";

        Assertions.assertEquals( "1,,3", translate(src) );

        src = "[{Counter}],[{Counter showResult=true}],[{Counter}]";

        Assertions.assertEquals( "1,2,3", translate(src) );
    }

}
