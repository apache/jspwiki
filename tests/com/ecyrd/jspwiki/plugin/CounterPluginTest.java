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

package com.ecyrd.jspwiki.plugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.jspwiki.api.WikiException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.WikiRenderer;
import com.ecyrd.jspwiki.render.XHTMLRenderer;

public class CounterPluginTest extends TestCase
{
    Properties props = new Properties();
    TestEngine testEngine;
    
    public CounterPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        testEngine = new TestEngine(props);
    }

    public void tearDown()
    {
    }

    private String translate( String src )
        throws IOException,
               ServletException, WikiException
    {
        WikiContext context = testEngine.getWikiContextFactory().newViewContext( testEngine.createPage( "TestPage" ) );
        
        MarkupParser p = new JSPWikiMarkupParser( context, new StringReader(src) );
        
        WikiDocument dom = p.parse();
        
        WikiRenderer r = new XHTMLRenderer( context, dom );
        
        return r.getString();
    }

    public void testSimpleCount()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}]";

        assertEquals( "1, 2",
                      translate(src) );
    }

    public void testSimpleVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}], [{$counter}]";

        assertEquals( "1, 2, 2",
                      translate(src) );
    }

    public void testTwinVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter name=aa}], [{$counter-aa}]";

        assertEquals( "1, 1, 1",
                      translate(src) );
    }

    public void testIncrement()
        throws Exception
    {
        String src = "[{Counter}], [{Counter increment=9}]";

        assertEquals( "1, 10", translate(src) );

        src = "[{Counter}],[{Counter}], [{Counter increment=-8}]";

        assertEquals( "1,2, -6", translate(src) );
    }

    public void testIncrement2() throws Exception
    {
        String src = "[{Counter start=5}], [{Counter increment=-1}], [{Counter increment=-1}]";

        assertEquals( "5, 4, 3", translate(src) );

        src = "[{Counter}],[{Counter start=11}], [{Counter increment=-8}]";

        assertEquals( "1,11, 3", translate(src) );
    }

    public void testShow()
        throws Exception
    {
        String src = "[{Counter}],[{Counter showResult=false}],[{Counter}]";

        assertEquals( "1,,3", translate(src) );

        src = "[{Counter}],[{Counter showResult=true}],[{Counter}]";

        assertEquals( "1,2,3", translate(src) );
    }

    public static Test suite()
    {
        return new TestSuite( CounterPluginTest.class );
    }
}
