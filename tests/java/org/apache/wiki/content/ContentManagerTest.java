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

package org.apache.wiki.content;

import java.util.Collection;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;


public class ContentManagerTest extends TestCase
{
    ContentManager m_mgr;
    TestEngine     m_engine;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);
        
        m_mgr = m_engine.getContentManager();
    }

    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            WikiPage p = m_mgr.getPage( WikiPath.valueOf("Main:TestPage") );
            
            if( p != null ) m_mgr.deletePage( p );
        }
        catch ( PageNotFoundException e )
        {
            // No worries; it just means one of our unit tests never created TestPage...
        }
        
        super.tearDown();
    }

    public void testContentManagerGet() throws WikiException
    {
        assertNotNull(m_mgr);
    }
    
    public void testBasicGet() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_mgr.addPage( WikiPath.valueOf("Main:TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        m_mgr.save( page );
        
        WikiPage page2 = m_mgr.getPage( WikiPath.valueOf("Main:TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testBasicGetDefaultSpace() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_mgr.addPage( WikiPath.valueOf("TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        m_mgr.save( page );
        
        WikiPage page2 = m_mgr.getPage( WikiPath.valueOf("TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testPaths() throws Exception
    {
        assertEquals( "One", "/pages/Main/MainPage", ContentManager.getJCRPath( WikiPath.valueOf("Main:MainPage") ) );
        
        assertEquals( "Back", WikiPath.valueOf("Main:MainPage"), ContentManager.getWikiPath( "/pages/Main/MainPage" ) );
    }
    
    public void getAllPages() throws Exception
    {
        m_engine.emptyRepository();
        Collection<WikiPage> allPages = m_mgr.getAllPages( ContentManager.DEFAULT_SPACE );
        assertEquals( 0, allPages.size() );

        // Add 2 pages to space Main
        m_engine.saveText( "Test1", "This is a test." );
        m_engine.saveText( "Test2", "This is a test." );

        allPages = m_mgr.getAllPages( null );
        assertEquals( 2, allPages.size() );
    }

    public static Test suite()
    {
        return new TestSuite( ContentManagerTest.class );
    }
    
}
