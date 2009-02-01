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
package org.apache.wiki.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.plugin.Denounce;
import org.apache.wiki.plugin.PluginManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;


public class DenouncePluginTest extends TestCase {
    Properties props = new Properties();
    TestEngine engine;
    WikiContext context;
    PluginManager manager;
    Properties denounceProps;
    private final String pluginCmdLine = "[{Denounce link='http://www.mobileasses.com' text='peoples asses'}]";

    public DenouncePluginTest(String s) {
        super(s);
    }

    public void setUp()
            throws Exception {
        props.load(TestEngine.findTestProperties());

        engine = new TestEngine(props);
        try {


            ClassLoader loader = Denounce.class.getClassLoader();
            InputStream in = loader.getResourceAsStream("com/ecyrd/jspwiki/plugin/denounce.properties");

            if (in == null) {
                throw new IOException("No property file found! (Check the installation, it should be there.)");
            }
            denounceProps = new Properties();
            denounceProps.load(in);
        } catch (IOException e) {
            fail("failed to load denounce.properties");
        }


    }

    private void setupHTTPRequest(String host, String header) {
        MockHttpServletRequest request = engine.newHttpRequest();
        if (header != null)
            request.addHeader("User-Agent", header);
        //if(host != null)

        request.getParameterMap().put("page", new String[]{"TestPage"});
        context = engine.getWikiContextFactory().newViewContext( request, null, null );
        manager = new PluginManager(engine, props);
    }

    public void tearDown() {
        TestEngine.deleteTestPage("TestPage");
        TestEngine.deleteTestPage("Foobar");
        TestEngine.emptyWorkDir();
        engine.shutdown();
    }

    public void testSLURPBot() throws Exception {
        setupHTTPRequest(null, "Slurp/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);
        //
        setupHTTPRequest(null, "ETSlurp/");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

        setupHTTPRequest(null, "Slurp");
        res = manager.execute(context, pluginCmdLine);
        assertFalse(getDenounceText().equalsIgnoreCase(res));

    }
      public void testGoogleBotWithWrongCase() throws Exception {
        setupHTTPRequest(null, "gOOglebot/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertFalse(getDenounceText().equalsIgnoreCase(res));
      }
    public void testGoogleBot() throws Exception {
        setupHTTPRequest(null, "Googlebot/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);
        //
        setupHTTPRequest(null, "ETSGooglebot/2.1");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

        setupHTTPRequest(null, "ETSGooglebot");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

    }

    public void testPlugin() throws Exception {
        setupHTTPRequest(null, null);

        String res = manager.execute(context, pluginCmdLine);

        assertEquals("<a href=\"http://www.mobileasses.com\">peoples asses</a>", res);

    }

    private String getDenounceText() {
        return denounceProps.getProperty("denounce.denouncetext");
    }


    public static Test suite() {
        return new TestSuite(DenouncePluginTest.class);
    }
}