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

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.util.FileUtil;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IndexPluginTest extends TestCase
{
    Properties m_props = new Properties();

    TestEngine m_engine;

    AttachmentManager m_attManager;

    WikiContext m_context;

    PluginManager m_manager;

    public void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( m_props );
        m_attManager = m_engine.getAttachmentManager();
        m_context = m_engine.getWikiContextFactory().newViewContext( null, null, m_engine.createPage( "WhatEver" ) );
        m_manager = new PluginManager( m_engine, m_props );
    }

    public void tearDown()
    {
        TestEngine.deleteTestPage( "TestPage" );
        TestEngine.emptyWorkDir();
        m_engine.shutdown();
    }

    /**
     * Plain test without parameters
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception
    {
        m_engine.saveText( "TestPage", "Content of TestPage" );

        String res = m_manager.execute( m_context, "{INSERT org.apache.wiki.plugin.IndexPlugin}" );

        assertTrue( "TestPage not found in Index", res.contains( "<a href='/Wiki.jsp?page=TestPage'>TestPage</a>" ) );
    }

    /**
     * Plain test without parameters, but with an attachment
     * 
     * @throws Exception
     */
    /*
    public void testAttachment() throws Exception
    {
        m_engine.saveText( "TestPage", "Content of TestPage" );

        Attachment att = new Attachment( m_engine, "TestPage", "test1.txt" );
        att.setAuthor( "OmeJoop" );
        m_attManager.storeAttachment( att, makeAttachmentFile() );

        String res = m_manager.execute( m_context, "{INSERT org.apache.wiki.plugin.IndexPlugin}" );

        assertTrue( "TestPage not found in Index", res.contains( "<a href='/Wiki.jsp?page=TestPage'>TestPage</a>" ) );

        assertTrue( "attachment not found in Index", res
            .contains( "<a href='/Wiki.jsp?page=TestPage/test1.txt'>TestPage/test1.txt</a>" ) );
    }
*/
    /**
     * Test with showAttachment=false parameter
     * 
     * @throws Exception
     */
    /*
    public void testAttachmentDoNotShow() throws Exception
    {
        m_engine.saveText( "TestPage", "Content of TestPage" );

        Attachment att = new Attachment( m_engine, "TestPage", "test1.txt" );
        att.setAuthor( "OmeJoop" );
        m_attManager.storeAttachment( att, makeAttachmentFile() );

        String res = m_manager.execute( m_context, "{INSERT org.apache.wiki.plugin.IndexPlugin showAttachments=false}" );

        assertTrue( "TestPage not found in Index", res.contains( "<a href='/Wiki.jsp?page=TestPage'>TestPage</a>" ) );

        assertFalse( "attachment should not be in Index", res
            .contains( "<a href='/Wiki.jsp?page=TestPage/test1.txt'>TestPage/test1.txt</a>" ) );
    }
*/
    private File makeAttachmentFile() throws Exception
    {
        File tmpFile = File.createTempFile( "test", "txt" );
        tmpFile.deleteOnExit();
        FileWriter out = new FileWriter( tmpFile );
        FileUtil.copyContents( new StringReader( "contents of attachment file" ), out );
        out.close();
        return tmpFile;
    }

    public static Test suite()
    {
        return new TestSuite( IndexPluginTest.class );
    }
}
