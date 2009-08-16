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
package org.apache.wiki.search;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.search.SearchResult;

import net.sourceforge.stripes.mock.MockHttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class SearchManagerTest extends TestCase
{
    TestEngine m_engine;
    SearchManager m_mgr;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        props.setProperty( SearchManager.PROP_SEARCHPROVIDER, "LuceneSearchProvider" );
        props.setProperty( "jspwiki.lucene.initialdelay", "1" );

        String tmpdir = props.getProperty("jspwiki.workDir");
        
        assertNotNull(tmpdir);
        // Empty the lucene work directory
        TestEngine.deleteAll( new File(tmpdir, "lucene") );
        
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getSearchManager();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        m_engine.emptyRepository();
        
        m_engine.shutdown();
    }

    public void testDefaultProvider()
    {
        assertEquals( "org.apache.wiki.search.LuceneSearchProvider", 
                      m_mgr.getSearchProvider().getClass().getName() );    
    }
    
    public void testSimpleSearch()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        
        m_engine.saveText("TestPage", txt);

        Thread.yield();

        Thread.sleep( 10000L ); // Should cover for both index and initial delay
        
        Collection<SearchResult> res = m_mgr.findPages( "mankind" );
     
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
        
        assertEquals( "page","TestPage", res.iterator().next().getPage().getName() );
    }

    public void testSimpleSearch2()
       throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
    
        m_engine.saveText("TestPage", txt);

        m_engine.saveText("TestPage", txt + " 2");
        
        Thread.yield();

        Thread.sleep( 10000L ); // Should cover for both index and initial delay
    
        Collection<SearchResult> res = m_mgr.findPages( "mankind" );
 
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
    
        assertEquals( "page","TestPage", res.iterator().next().getPage().getName() );
    }

    public void testSimpleSearch3()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
 
        MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );
        
        WikiContext ctx = m_engine.getWikiContextFactory().newContext( request, null, WikiContext.EDIT );
        
        m_engine.saveText( ctx, txt );

        m_engine.saveText( ctx, "The Babylon Project was a dream given form. Its goal: to prevent another war by creating a place where humans and aliens could work out their differences peacefully." );
     
        Thread.yield();

        Thread.sleep( 10000L ); // Should cover for both index and initial delay
 
        Collection<SearchResult> res = m_mgr.findPages( "mankind" );

        assertNotNull( "found results", res );
        assertEquals( "empty results", 0, res.size() );
        
        res = m_mgr.findPages( "Babylon" );
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
     
        assertEquals( "page","TestPage", res.iterator().next().getPage().getName() );
    }

    public void testTitleSearch()
        throws Exception
    {
        String txt = "Nonsensical content that should not match";
 
        m_engine.saveText("TestPage", txt);
     
        Thread.yield();

        Thread.sleep( 5000L ); // Should cover for both index and initial delay
 
        Collection<SearchResult> res = m_mgr.findPages( "Test" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
 
        assertEquals( "page","TestPage", res.iterator().next().getPage().getName() );
    }

    public void testTitleSearch2()
        throws Exception
    { 
        String txt = "Nonsensical content that should not match";

        m_engine.saveText("TestPage", txt);
 
        Thread.yield();

        Thread.sleep( 5000L ); // Should cover for both index and initial delay

        Collection<SearchResult> res = m_mgr.findPages( "TestPage" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );

        assertEquals( "page","TestPage", res.iterator().next().getPage().getName() );
    }

    public static Test suite()
    {
        return new TestSuite( SearchManagerTest.class );
    }
}
