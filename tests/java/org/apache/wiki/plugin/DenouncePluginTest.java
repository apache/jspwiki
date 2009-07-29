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

public class DenouncePluginTest extends TestCase 
{
    private Properties m_props = new Properties();
    private TestEngine m_engine;
    private WikiContext m_context;
    private PluginManager m_pluginmanager;
    private Properties m_denounceProps;
    private static final String PLUGINCMDLINE = "[{Denounce link='http://www.mobileasses.com' text='peoples asses'}]";

    public DenouncePluginTest(String s) 
    {
        super(s);
    }

    public void setUp()
            throws Exception 
    {
        m_props.load(TestEngine.findTestProperties());

        m_engine = new TestEngine(m_props);
        try 
        {
            ClassLoader loader = Denounce.class.getClassLoader();
            InputStream in = loader.getResourceAsStream("DenouncePlugin.properties");

            if (in == null) 
            {
                throw new IOException("No property file found! (Check the installation, it should be there.)");
            }
            m_denounceProps = new Properties();
            m_denounceProps.load(in);
        } 
        catch (IOException e) 
        {
            fail("failed to load DenouncePlugin.properties");
        }
    }

    private void setupHTTPRequest(String header) 
    {
        MockHttpServletRequest request = m_engine.newHttpRequest();
        if (header != null)
            request.addHeader("User-Agent", header);

        request.getParameterMap().put("page", new String[]{"TestPage"});
        m_context = m_engine.getWikiContextFactory().newViewContext( request, null, null );
        m_pluginmanager = new PluginManager(m_engine, m_props);
    }

    public void tearDown() throws Exception
    {
        TestEngine.emptyWorkDir();
        m_engine.emptyRepository();
        m_engine.shutdown();
    }

    public void testSLURPBot1() throws Exception
    {
        setupHTTPRequest( "Slurp/2.1" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertEquals( getDenounceText(), res );
    }

    public void testSLURPBot2() throws Exception
    {
        setupHTTPRequest( "ETSlurp/" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertEquals( getDenounceText(), res );
    }

    public void testSLURPBot3() throws Exception
    {
        setupHTTPRequest( "Slurp" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertFalse( getDenounceText().equalsIgnoreCase( res ) );
    }
    
    public void testGoogleBotWithWrongCase() throws Exception 
    {
        setupHTTPRequest("gOOglebot/2.1");
        String res = m_pluginmanager.execute(m_context, PLUGINCMDLINE);
        assertFalse(getDenounceText().equalsIgnoreCase(res));
    }
    
    public void testGoogleBot1() throws Exception
    {
        setupHTTPRequest( "Googlebot/2.1" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertEquals( getDenounceText(), res );
        //
    }

    public void testGoogleBot2() throws Exception
    {
        setupHTTPRequest( "ETSGooglebot/2.1" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertEquals( getDenounceText(), res );
    }

    public void testGoogleBot3() throws Exception
    {
        setupHTTPRequest( "ETSGooglebot" );
        String res = m_pluginmanager.execute( m_context, PLUGINCMDLINE );
        assertEquals( getDenounceText(), res );
    }

    public void testPlugin() throws Exception 
    {
        setupHTTPRequest(null);

        String res = m_pluginmanager.execute(m_context, PLUGINCMDLINE);

        assertEquals("<a href=\"http://www.mobileasses.com\">peoples asses</a>", res);

    }

    private String getDenounceText() 
    {
        return m_denounceProps.getProperty("denounce.denouncetext");
    }


    public static Test suite() 
    {
        return new TestSuite(DenouncePluginTest.class);
    }
}