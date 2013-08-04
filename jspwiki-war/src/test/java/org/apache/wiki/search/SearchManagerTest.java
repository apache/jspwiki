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
package org.apache.wiki.search;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import net.sourceforge.stripes.mock.MockHttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.wiki.Release;
import org.apache.wiki.SearchResult;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;

public class SearchManagerTest extends TestCase
{
    private static final long SLEEP_TIME = 2000L;
    private static final int SLEEP_COUNT = 50;
    TestEngine m_engine;
    SearchManager m_mgr;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        props.setProperty( SearchManager.PROP_SEARCHPROVIDER, "LuceneSearchProvider" );
        props.setProperty( "jspwiki.lucene.initialdelay", "1" );
        props.setProperty( "jspwiki.workDir", System.getProperty( "java.io.tmpdir" ) + File.separator + Release.APPNAME );
        
        TestEngine.emptyWorkDir(props);
        
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getSearchManager();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testDefaultProvider()
    {
        assertEquals( "org.apache.wiki.search.LuceneSearchProvider", 
                      m_mgr.getSearchEngine().getClass().getName() );    
    }
    
    /**
     * Should cover for both index and initial delay
     */
    Collection waitForIndex( String text, String testName ) throws Exception
    {
        Collection res = null;
        for( long l = 0; l < SLEEP_COUNT; l++ )
        {
            if( res == null || res.isEmpty() )
            {
                Thread.sleep( SLEEP_TIME );
                System.out.println( "SearchManagerTest.waitForIndex for " + testName + " sleeping " + l + " (out of " + SLEEP_COUNT + ")" );
            }
            else
            {
                break;
            }
            res = m_mgr.findPages( text );
        }
        return res;
    }
    
    public void testSimpleSearch()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        
        m_engine.saveText("TestPage", txt);

        Thread.yield();
        Collection res = waitForIndex( "mankind" , "testSimpleSearch" );
     
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
        
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testSimpleSearch2()
       throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
    
        m_engine.saveText("TestPage", txt);

        m_engine.saveText("TestPage", txt + " 2");
        
        Thread.yield();
        Collection res = waitForIndex( "mankind" , "testSimpleSearch2" );
 
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
    
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testSimpleSearch3()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
 
        MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );
        
        WikiContext ctx = m_engine.createContext( request, WikiContext.EDIT );
        
        m_engine.saveText( ctx, txt );

        m_engine.saveText( ctx, "The Babylon Project was a dream given form. Its goal: to prevent another war by creating a place where humans and aliens could work out their differences peacefully." );
     
        Thread.yield();
        Collection res = waitForIndex( "Babylon" , "testSimpleSearch3" ); // wait until 2nd m_engine.saveText() takes effect

        res = m_mgr.findPages( "mankind" ); // check for text present in 1st m_engine.saveText() but not in 2nd

        assertNotNull( "found results", res );
        assertEquals( "empty results", 0, res.size() );
        
        res = m_mgr.findPages( "Babylon" );
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
     
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testTitleSearch()
        throws Exception
    {
        String txt = "Nonsensical content that should not match";
 
        m_engine.saveText("TestPage", txt);
     
        Thread.yield();
        Collection res = waitForIndex( "Test" , "testTitleSearch" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
 
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testTitleSearch2()
        throws Exception
    { 
        String txt = "Nonsensical content that should not match";

        m_engine.saveText("TestPage", txt);
 
        Thread.yield();
        Collection res = waitForIndex( "TestPage" , "testTitleSearch2" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );

        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public static Test suite()
    {
        return new TestSuite( SearchManagerTest.class );
    }
}