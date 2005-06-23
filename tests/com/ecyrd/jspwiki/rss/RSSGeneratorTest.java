/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.rss;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.plugin.WeblogEntryPlugin;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
import com.ecyrd.jspwiki.providers.FileSystemProvider;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class RSSGeneratorTest extends TestCase
{
    TestEngine m_testEngine;
    Properties props = new Properties();
    
    public RSSGeneratorTest( String arg0 )
    {
        super( arg0 );
    }

    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_BASEURL, "http://localhost/" );
        props.setProperty( RSSGenerator.PROP_GENERATE_RSS, "true" );
        m_testEngine = new TestEngine(props);
    }

    protected void tearDown() throws Exception
    {
        TestEngine.deleteAll( new File((String)props.getProperty( FileSystemProvider.PROP_PAGEDIR )) );
    }

    public void testBlogRSS()
        throws Exception
    {
        WeblogEntryPlugin plugin = new WeblogEntryPlugin();
        m_testEngine.saveText( "TestBlog", "Foo1" );
        
        String newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title1\r\nFoo" );
                
        newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );
        
        RSSGenerator gen = m_testEngine.getRSSGenerator();
        
        WikiContext context = new WikiContext( m_testEngine, m_testEngine.getPage("TestBlog") );
        
        WeblogPlugin blogplugin = new WeblogPlugin();
        
        List entries = blogplugin.findBlogEntries( m_testEngine.getPageManager(),
                                                   "TestBlog",
                                                   new Date(0),
                                                   new Date(Long.MAX_VALUE) );
        
        Feed feed = new RSS10Feed( context );
        String blog = gen.generateBlogRSS( context, entries, feed );
        
        assertTrue( "has Foo", blog.indexOf("<description>Foo</description>") != -1 );
        assertTrue( "has proper Bar", blog.indexOf("&lt;b&gt;Bar&lt;/b&gt;") != -1 );
    }
    
    public static Test suite()
    {
        return new TestSuite( RSSGeneratorTest.class );
    }

}
