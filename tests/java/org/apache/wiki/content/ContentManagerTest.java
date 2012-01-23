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
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.WikiPathResolver.PathRoot;
import org.apache.wiki.providers.ProviderException;

import stress.Benchmark;


public class ContentManagerTest extends TestCase
{
    ContentManager m_cm;
    TestEngine     m_engine;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);
        
        m_cm = m_engine.getContentManager();
    }

    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            WikiPage p = m_cm.getPage( WikiPath.valueOf("Main:TestPage") );
            
            if( p != null ) m_cm.deletePage( p );
        }
        catch ( PageNotFoundException e )
        {
            // No worries; it just means one of our unit tests never created TestPage...
        }
        finally
        {
            super.tearDown();
            m_engine.shutdown();
        }
    }

    public void testContentManagerGet() throws WikiException
    {
        assertNotNull(m_cm);
    }
    
    public void testBasicGet() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_cm.addPage( WikiPath.valueOf("Main:TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        m_cm.save( page );
        
        WikiPage page2 = m_cm.getPage( WikiPath.valueOf("Main:TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testBasicGetDefaultSpace() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_cm.addPage( WikiPath.valueOf("TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        m_cm.save( page );
        
        WikiPage page2 = m_cm.getPage( WikiPath.valueOf("TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testPaths() throws Exception
    {
        assertEquals( "One", "/pages/main/mainpage", WikiPathResolver.getJCRPath( WikiPath.valueOf("Main:MainPage"), PathRoot.PAGES ) );
        WikiPathResolver cache = WikiPathResolver.getInstance( m_cm );
        assertEquals( "Back", WikiPath.valueOf("Main:MainPage"), cache.getWikiPath( "/pages/main/mainpage", PathRoot.PAGES ) );
    }
    
    public void getAllPages() throws Exception
    {
        m_engine.emptyRepository();
        Collection<WikiPage> allPages = m_cm.getAllPages( ContentManager.DEFAULT_SPACE );
        assertEquals( 0, allPages.size() );

        // Add 2 pages to space Main
        m_engine.saveText( "Test1", "This is a test." );
        m_engine.saveText( "Test2", "This is a test." );

        allPages = m_cm.getAllPages( null );
        assertEquals( 2, allPages.size() );
    }

    public void testPageExists() throws Exception
    {
        WikiPath path = WikiPath.valueOf( "ContentManagerTest-PageExists" );

        // Save a new page
        m_engine.saveText( path.toString(), "This is the first version" );
        assertTrue( m_cm.pageExists( path, WikiProvider.LATEST_VERSION ) );
        assertTrue( m_cm.pageExists( path, 1 ) );

        // Save another version
        m_engine.saveText( path.toString(), "This is the second version" );
        assertTrue( m_cm.pageExists( path, WikiProvider.LATEST_VERSION ) );
        assertTrue( m_cm.pageExists( path, 2 ) );
        assertTrue( m_cm.pageExists( path, 1 ) );

        m_engine.deletePage( path.toString() );
        
        assertFalse( m_cm.pageExists( path ) );
    }

    public void testVersions() throws Exception
    {
        String content = "Test Content";

        WikiPage page = m_cm.addPage( WikiPath.valueOf("TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );

        page.setContent( content );
        
        page.save();
        
        page.setContent( "New Test Content" );
        
        page.save();
        
        page.setContent( "Even newer Test Content" );
        
        page.save();
        
        assertEquals( "origpage version", 3, page.getVersion() );
        
        WikiPage p2 = m_cm.getPage( WikiPath.valueOf("TestPage") );
        
        assertEquals( "fetched page version", 3, p2.getVersion() );
        
        assertEquals( "content", "Even newer Test Content", p2.getContentAsString() );
        
        assertEquals( "content type", ContentManager.JSPWIKI_CONTENT_TYPE, p2.getContentType() );
        
        // Test get version

        p2 = m_cm.getPage( WikiPath.valueOf("TestPage"), 1 );
        
        assertEquals( "v1 content", "Test Content", p2.getContentAsString() );
        assertEquals( "v1 version", 1, p2.getVersion() );

        assertFalse( "content", page.getContentAsString().equals(p2.getContentAsString()));
        assertFalse( "uuid", page.getAttribute( "jcr:uuid" ).equals( p2.getAttribute( "jcr:uuid" )));
        
        p2 = m_cm.getPage( WikiPath.valueOf("TestPage"), 2 );
        
        assertEquals( "v2 content", "New Test Content", p2.getContentAsString() );
        assertEquals( "v2 version", 2, p2.getVersion() );

        p2 = m_cm.getPage( WikiPath.valueOf("TestPage"), 3 );
        
        assertEquals( "v3 content", "Even newer Test Content", p2.getContentAsString() );
        assertEquals( "v3 version", 3, p2.getVersion() );

        p2 = m_cm.getPage( WikiPath.valueOf("TestPage"), -1 );
        
        assertEquals( "v3 content", "Even newer Test Content", p2.getContentAsString() );
        assertEquals( "v3 version", 3, p2.getVersion() );
    }
    
    private void storeVersions( WikiPage p, int howMany ) throws ProviderException
    {
        for( int i = 1; i <= howMany; i++ )
        {
            p.setContent( "Test "+i );
            p.save();
        }        
    }
    
    public void testZillionVersions() throws Exception
    {
        WikiPage p = m_cm.addPage( WikiPath.valueOf( "TestPage" ), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        Benchmark b = new Benchmark();
        b.start();
        
        storeVersions( p, 100 );
        
        b.stop();
        
        System.out.println( "100 versions stored in "+b+", "+b.toString( 100 ) + " stores/second");
        
        p = m_engine.getPage( "TestPage", 100 );
        assertEquals( "content 100","Test 100", p.getContentAsString() );
        assertEquals( "content 100/2", p.getContentAsString(), p.getContentAsString() );
        assertEquals( "version 100", 100, p.getVersion() );
        
        p = m_engine.getPage( "TestPage", 1 );
        assertEquals( "content 1","Test 1", p.getContentAsString() );
        assertEquals( "content 1/2", p.getContentAsString(), p.getContentAsString() );
        assertEquals( "version 1", 1, p.getVersion() );
        
        p = m_engine.getPage( "TestPage", 51 );
        assertEquals( "content 51","Test 51", p.getContentAsString() );
        assertEquals( "content 51/2", p.getContentAsString(), p.getContentAsString() );
        assertEquals( "version 51", 51, p.getVersion() );
    }
    
    public void testDeleteLastVersion() throws Exception
    {
        WikiPage p = m_engine.createPage("TestPage");
        
        storeVersions( p, 3 );
        
        WikiPage current = m_engine.getPage( "TestPage" );
        
        assertEquals( current.getVersion(), 3 );
        
        m_engine.deleteVersion( current );
        
        assertFalse( m_engine.pageExists( "TestPage", 3 ));
        
        current = m_engine.getPage("TestPage");
        
        assertEquals( current.getVersion(), 2 );
        
        try
        {
            m_engine.getPage("TestPage",3);
            fail("Got v3");
        }
        catch(PageNotFoundException e) {}
    }
    
    public void testDeleteInTheMiddle() throws Exception
    {
        WikiPage p = m_engine.createPage("TestPage");
        
        storeVersions( p, 3 );
        
        WikiPage current = m_engine.getPage( "TestPage", 2 );
        
        assertEquals( current.getVersion(), 2 );
        
        m_engine.deleteVersion( current );
        
        assertFalse( m_engine.pageExists( "TestPage", 2 ));
        
        current = m_engine.getPage("TestPage");
        
        assertEquals( current.getVersion(), 3 );
        
        try
        {
            m_engine.getPage("TestPage",2);
            fail("Got v2");
        }
        catch(PageNotFoundException e) {}
        
        List<WikiPage> vh = m_engine.getVersionHistory( "TestPage" );
        assertEquals( 2, vh.size() );
        assertEquals( 1, vh.get(0).getVersion() );
        assertEquals( 3, vh.get(1).getVersion() );
    }

    public void testDeleteFirst() throws Exception
    {
        WikiPage p = m_engine.createPage("TestPage");
        
        storeVersions( p, 3 );
        
        WikiPage current = m_engine.getPage( "TestPage", 1 );
        
        assertEquals( current.getVersion(), 1 );
        
        m_engine.deleteVersion( current );
        
        assertFalse( m_engine.pageExists( "TestPage", 1 ));
        
        current = m_engine.getPage("TestPage");
        
        assertEquals( current.getVersion(), 3 );
        
        try
        {
            m_engine.getPage("TestPage",1);
            fail("Got v1");
        }
        catch(PageNotFoundException e) {}
        
        List<WikiPage> vh = m_engine.getVersionHistory( "TestPage" );
        assertEquals( 2, vh.size() );
        assertEquals( 2, vh.get(0).getVersion() );
        assertEquals( 3, vh.get(1).getVersion() );
    }
    
    public void testDeleteAllVersions() throws Exception
    {
        WikiPage p = m_engine.createPage( "TestPage" );
        
        storeVersions( p, 3 );
        
        for( int i = 1; i <= 3; i++ )
        {
            p = m_engine.getPage( "TestPage", i );
            
            m_engine.deleteVersion( p );
            
            assertFalse( "exists "+i, m_engine.pageExists( "TestPage", i ) );

            try
            {
                p = m_engine.getPage( "TestPage", i );
                fail("Didn't get exception for "+i);
            }
            catch( PageNotFoundException e ) {} // Expected

        }
    
        assertFalse( m_engine.pageExists( "TestPage" ) );
        
        try
        {
            p = m_engine.getPage( "TestPage" );
            fail("Didn't get exception for the whole page");
        }
        catch( PageNotFoundException e ) {} // Expected
    }
    
    public static Test suite()
    {
        return new TestSuite( ContentManagerTest.class );
    }
    
}
