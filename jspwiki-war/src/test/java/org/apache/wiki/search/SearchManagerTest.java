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

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.providers.AbstractFileProvider;

public class SearchManagerTest extends TestCase {
	
    private static final long SLEEP_TIME = 2000L;
    private static final int SLEEP_COUNT = 50;
    TestEngine m_engine;
    SearchManager m_mgr;
    Properties props;
    
    protected void setUp() throws Exception {
        Properties props = TestEngine.getTestProperties();
        String workDir = props.getProperty( "jspwiki.workDir" );
        String workRepo = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
        
        props.setProperty( SearchManager.PROP_SEARCHPROVIDER, "LuceneSearchProvider" );
        props.setProperty( "jspwiki.lucene.initialdelay", "1" );
        props.setProperty( "jspwiki.workDir", workDir + System.currentTimeMillis() );
        props.setProperty( AbstractFileProvider.PROP_PAGEDIR, workRepo + System.currentTimeMillis() );

        CacheManager.getInstance().removeAllCaches();
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getSearchManager();
    }

    protected void tearDown() throws Exception {
    	TestEngine.emptyWorkDir( props );
    }

    public void testDefaultProvider() {
        assertEquals( "org.apache.wiki.search.LuceneSearchProvider", 
                      m_mgr.getSearchEngine().getClass().getName() );    
    }
    
    /**
     * Should cover for both index and initial delay
     */
    Collection waitForIndex( String text, String testName ) throws Exception {
        Collection res = null;
        for( long l = 0; l < SLEEP_COUNT; l++ ) {
            if( res == null || res.isEmpty() ) {
                Thread.sleep( SLEEP_TIME );
            } else {
                break;
            }
            MockHttpServletRequest request = m_engine.newHttpRequest();
            WikiContext ctx = m_engine.createContext( request, WikiContext.EDIT );
            
            res = m_mgr.findPages( text, ctx );

//            debugSearchResults( res );
        }
        return res;
    }

	void debugSearchResults( Collection< SearchResult > res ) {
		Iterator< SearchResult > iterator = res.iterator();
		while( iterator.hasNext() ) {
			SearchResult next = iterator.next();
			System.out.println( "page: " + next.getPage() );
			for( String s : next.getContexts() ) {
				System.out.println( "snippet: " + s );
			}
		}
	}
    
    public void testSimpleSearch() throws Exception {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        
        m_engine.saveText("TestPage", txt);

        Thread.yield();
        Collection res = waitForIndex( "mankind" , "testSimpleSearch" );
     
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
        
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testSimpleSearch2() throws Exception {
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

    public void testSimpleSearch3() throws Exception {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
 
        MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );
        
        WikiContext ctx = m_engine.createContext( request, WikiContext.EDIT );
        
        m_engine.saveText( ctx, txt );

        m_engine.saveText( ctx, "The Babylon Project was a dream given form. Its goal: to prevent another war by creating a place where humans and aliens could work out their differences peacefully." );
     
        Thread.yield();
        Collection res = waitForIndex( "Babylon" , "testSimpleSearch3" ); // wait until 2nd m_engine.saveText() takes effect

        res = m_mgr.findPages( "mankind", ctx ); // check for text present in 1st m_engine.saveText() but not in 2nd

        assertNotNull( "found results", res );
        assertEquals( "empty results", 0, res.size() );
        
        res = m_mgr.findPages( "Babylon", ctx );
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
     
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testSimpleSearch4() throws Exception {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";

        MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );

        WikiContext ctx = m_engine.createContext( request, WikiContext.EDIT );

        m_engine.saveText( ctx, txt );

        Thread.yield();
        Collection res = waitForIndex( "mankind" , "testSimpleSearch4" );

        assertNotNull( "found results", res );
        assertEquals( "result not found", 1, res.size() );

        m_engine.saveText( ctx, "[{ALLOW view Authenticated}] It was the dawn of the third age of mankind... page is blocked" );

        res = m_mgr.findPages( "mankind" , ctx );
        assertNotNull( "null result", res );
        assertEquals( "result found, should be blocked", 0, res.size() );

        m_engine.deleteTestPage("TestPage");
    }

    public void testTitleSearch() throws Exception {
        String txt = "Nonsensical content that should not match";
 
        m_engine.saveText("TestPage", txt);
     
        Thread.yield();
        Collection res = waitForIndex( "Test" , "testTitleSearch" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
 
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public void testTitleSearch2() throws Exception { 
        String txt = "Nonsensical content that should not match";

        m_engine.saveText("TestPage", txt);
 
        Thread.yield();
        Collection res = waitForIndex( "TestPage" , "testTitleSearch2" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );

        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
        m_engine.deleteTestPage("TestPage");
    }

    public static Test suite() {
        return new TestSuite( SearchManagerTest.class );
    }

}