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

import net.sf.ehcache.CacheManager;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;


public class SearchManagerTest {

    TestEngine m_engine;
    SearchManager m_mgr;
    Properties props;

    @BeforeEach
    public void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final String workDir = props.getProperty( "jspwiki.workDir" );
        final String workRepo = props.getProperty( "jspwiki.fileSystemProvider.pageDir" );

        props.setProperty( SearchManager.PROP_SEARCHPROVIDER, "LuceneSearchProvider" );
        props.setProperty( "jspwiki.lucene.initialdelay", "1" );
        props.setProperty( "jspwiki.workDir", workDir + System.currentTimeMillis() );
        props.setProperty( "jspwiki.fileSystemProvider.pageDir", workRepo + System.currentTimeMillis() );

        CacheManager.getInstance().removeAllCaches();
        m_engine = new TestEngine( props );
        m_mgr = m_engine.getSearchManager();
    }

    @AfterEach
    public void tearDown() {
    	TestEngine.emptyWorkDir( props );
    }

    @Test
    public void testDefaultProvider() {
        Assertions.assertEquals( "org.apache.wiki.search.LuceneSearchProvider", m_mgr.getSearchEngine().getClass().getName() );
    }

    void debugSearchResults( final Collection< SearchResult > res ) {
        res.forEach( next -> {
            System.out.println( "page: " + next.getPage() );
            for( final String s : next.getContexts() ) {
                System.out.println( "snippet: " + s );
            }
        } );
    }

    Callable< Boolean > findsResultsFor( final Collection< SearchResult > res, final String text ) {
        return () -> {
            final MockHttpServletRequest request = m_engine.newHttpRequest();
            final WikiContext ctx = new WikiContext( m_engine, request, WikiContext.EDIT );
            final Collection< SearchResult > search = m_mgr.findPages( text, ctx );
            if( search != null && search.size() > 0 ) {
                // debugSearchResults( search );
                res.addAll( search );
                return true;
            }
            return false;
        };
    }

    @Test
    public void testSimpleSearch() throws Exception {
        final String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        m_engine.saveText("TestPage", txt);

        final Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testSimpleSearch" ).until( findsResultsFor( res, "mankind" ) );

        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage("TestPage");
    }

    @Test
    public void testSimpleSearch2() throws Exception {
        final String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        m_engine.saveText("TestPage", txt);
        m_engine.saveText("TestPage", txt + " 2");

        final Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testSimpleSearch2" ).until( findsResultsFor( res,"mankind" ) );

        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage( "TestPage" );
    }

    @Test
    public void testSimpleSearch3() throws Exception {
        final String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        final MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );
        final WikiContext ctx = new WikiContext( m_engine, request, WikiContext.EDIT );
        m_engine.getPageManager().saveText( ctx, txt );
        m_engine.getPageManager().saveText( ctx, "The Babylon Project was a dream given form. Its goal: to prevent another war by creating a place where humans and aliens could work out their differences peacefully." );

        Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testSimpleSearch3" ).until( findsResultsFor( res, "Babylon" ) );
        res = m_mgr.findPages( "mankind", ctx ); // check for text present in 1st m_engine.saveText() but not in 2nd

        Assertions.assertEquals( 0, res.size(), "empty results" );

        Awaitility.await( "testSimpleSearch3" ).until( findsResultsFor( res,"Babylon" ) );
        Assertions.assertNotNull( res, "null result" );
        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage("TestPage");
    }

    @Test
    public void testSimpleSearch4() throws Exception {
        final String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        final MockHttpServletRequest request = m_engine.newHttpRequest();
        request.getParameterMap().put( "page", new String[]{ "TestPage" } );
        final WikiContext ctx = new WikiContext( m_engine, request, WikiContext.EDIT );
        m_engine.getPageManager().saveText( ctx, txt );

        Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testSimpleSearch4" ).until( findsResultsFor( res, "mankind" ) );

        Assertions.assertEquals( 1, res.size(), "result not found" );

        m_engine.getPageManager().saveText( ctx, "[{ALLOW view Authenticated}] It was the dawn of the third age of mankind... page is blocked" );

        res = m_mgr.findPages( "mankind" , ctx );
        Assertions.assertNotNull( res, "null result" );
        Assertions.assertEquals( 0, res.size(), "result found, should be blocked" );

        m_engine.deleteTestPage("TestPage");
    }

    @Test
    public void testTitleSearch() throws Exception {
        final String txt = "Nonsensical content that should not match";
        m_engine.saveText("TestPage", txt);

        final Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testTitleSearch" ).until( findsResultsFor( res, "Test" ) );

        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage("TestPage");
    }

    @Test
    public void testTitleSearch2() throws Exception {
        final String txt = "Nonsensical content that should not match";
        m_engine.saveText("TestPage", txt);

        final Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testTitleSearch2" ).until( findsResultsFor( res, "TestPage" ) );

        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage("TestPage");
    }

    @Test
    public void testKeywordsSearch() throws Exception {
        final String txt = "[{SET keywords=perry,mason,attorney,law}] Nonsensical content that should not match";

        m_engine.saveText("TestPage", txt);

        final Collection< SearchResult > res = new ArrayList<>();
        Awaitility.await( "testKeywordsSearch" ).until( findsResultsFor( res, "perry" ) );

        Assertions.assertEquals( 1, res.size(), "no pages" );
        Assertions.assertEquals( "TestPage", res.iterator().next().getPage().getName(), "page" );
        m_engine.deleteTestPage("TestPage");
    }

}