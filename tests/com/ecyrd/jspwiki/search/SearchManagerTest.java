package com.ecyrd.jspwiki.search;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.WikiContext;

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
        
        TestEngine.deleteTestPage("TestPage");
    }

    public void testDefaultProvider()
    {
        assertEquals( "com.ecyrd.jspwiki.search.LuceneSearchProvider", 
                      m_mgr.getSearchEngine().getClass().getName() );    
    }
    
    public void testSimpleSearch()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
        
        m_engine.saveText("TestPage", txt);

        Thread.yield();

        Thread.sleep( 5000L ); // Should cover for both index and initial delay
        
        Collection res = m_mgr.findPages( "mankind" );
     
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
        
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
    }

    public void testSimpleSearch2()
       throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
    
        m_engine.saveText("TestPage", txt);

        m_engine.saveText("TestPage", txt + " 2");
        
        Thread.yield();

        Thread.sleep( 2000L ); // Should cover for both index and initial delay
    
        Collection res = m_mgr.findPages( "mankind" );
 
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
    
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
    }

    public void testSimpleSearch3()
        throws Exception
    {
        String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
 
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setParameter("page","TestPage");
        
        WikiContext ctx = m_engine.createContext( request, WikiContext.EDIT );
        
        m_engine.saveText( ctx, txt );

        m_engine.saveText( ctx, "The Babylon Project was a dream given form. Its goal: to prevent another war by creating a place where humans and aliens could work out their differences peacefully." );
     
        Thread.yield();

        Thread.sleep( 2000L ); // Should cover for both index and initial delay
 
        Collection res = m_mgr.findPages( "mankind" );

        assertNotNull( "found results", res );
        assertEquals( "empty results", 0, res.size() );
        
        res = m_mgr.findPages( "Babylon" );
        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
     
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
    }

    public void testTitleSearch()
        throws Exception
    {
        String txt = "Nonsensical content that should not match";
 
        m_engine.saveText("TestPage", txt);
     
        Thread.yield();

        Thread.sleep( 2000L ); // Should cover for both index and initial delay
 
        Collection res = m_mgr.findPages( "Test" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );
 
        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
    }

    public void testTitleSearch2()
        throws Exception
    { 
        String txt = "Nonsensical content that should not match";

        m_engine.saveText("TestPage", txt);
 
        Thread.yield();

        Thread.sleep( 2000L ); // Should cover for both index and initial delay

        Collection res = m_mgr.findPages( "TestPage" );

        assertNotNull( "null result", res );
        assertEquals( "no pages", 1, res.size() );

        assertEquals( "page","TestPage", ((SearchResult)res.iterator().next()).getPage().getName() );
    }

    public static Test suite()
    {
        return new TestSuite( SearchManagerTest.class );
    }
}
