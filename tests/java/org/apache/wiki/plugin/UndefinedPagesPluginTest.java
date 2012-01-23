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

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.plugin.PluginManager;


public class UndefinedPagesPluginTest extends TestCase
{
    Properties m_props = new Properties();
    TestEngine m_engine;
    WikiContext m_context;
    PluginManager m_manager;

    public UndefinedPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );
        m_props.setProperty( WikiEngine.PROP_BEAUTIFYTITLE, "false" );

        m_engine = new TestEngine(m_props);

        m_engine.saveText( "UndefinedPagesPluginTest", "Reference to [UndefinedRef]." );
        m_engine.saveText( "UndefinedRef", "Reference to [UndefinedRef2], [UndefinedRefs]" );

        m_context = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage( "UndefinedPagesPluginTest") );
        m_manager = new PluginManager( m_engine, m_props );
    }

    public void tearDown() throws Exception
    {
        m_engine.emptyRepository();
        TestEngine.emptyWorkDir();
        m_engine.shutdown();
    }

    private String wikitize( String s )
    {
        return m_engine.textToHTML( m_context, s );
    }

    /**
     *  Tests that only correct undefined links are found.
     *  We also check against plural forms here, which should not
     *  be listed as non-existant.
     */
    public void testSimpleUndefined()
        throws Exception
    {
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage( "UndefinedRef") );

        String res = m_manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.UndefinedPagesPlugin");

        String exp = "[UndefinedRef2]\\\\";

        assertEquals( wikitize(exp), res );
    }

    public void testCount() throws Exception
    {
        String result = null;
        result = m_manager.execute(m_context, "{UndefinedPagesPlugin show=count}");
        assertEquals("1", result);

        // test if the proper exception is thrown:
        String expectedExceptionString = "parameter is not valid for the UndefinedPagesPlugin : showLastModified";
        String exceptionString = null;
        try
        {
            result = m_manager.execute(m_context, "{UndefinedPagesPlugin,show=count,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        assertEquals(expectedExceptionString, exceptionString);
    }

    public static Test suite()
    {
        return new TestSuite( UndefinedPagesPluginTest.class );
    }
}
