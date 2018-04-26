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
/*
 * (C) Janne Jalkanen 2005
 *
 */
package org.apache.wiki.rss;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.apache.wiki.providers.FileSystemProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.ehcache.CacheManager;


/**
 *
 *  @since
 */
public class RSSGeneratorTest
{
    TestEngine m_testEngine;
    Properties props = TestEngine.getTestProperties();

    @Before
    public void setUp() throws Exception
    {
        props.setProperty( RSSGenerator.PROP_GENERATE_RSS, "true" );
        CacheManager.getInstance().removeAllCaches();
        m_testEngine = new TestEngine(props);
    }

    @After
    public void tearDown() throws Exception
    {
        TestEngine.deleteAll( new File(props.getProperty( FileSystemProvider.PROP_PAGEDIR )) );
    }

    @Test
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

        List< ? > entries = blogplugin.findBlogEntries( m_testEngine,
                                                       "TestBlog",
                                                        new Date(0),
                                                        new Date(Long.MAX_VALUE) );

        Feed feed = new RSS10Feed( context );
        String blog = gen.generateBlogRSS( context, entries, feed );

        Assert.assertTrue( "has Foo", blog.indexOf("<description>Foo</description>") != -1 );
        Assert.assertTrue( "has proper Bar", blog.indexOf("&lt;b&gt;Bar&lt;/b&gt;") != -1 );
    }

    @Test
    public void testBlogRSS2()
        throws Exception
    {
        WeblogEntryPlugin plugin = new WeblogEntryPlugin();
        m_testEngine.saveText( "TestBlog", "Foo1" );

        String newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title1\r\nFoo \"blah\"." );

        newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );

        RSSGenerator gen = m_testEngine.getRSSGenerator();

        WikiContext context = new WikiContext( m_testEngine, m_testEngine.getPage("TestBlog") );

        WeblogPlugin blogplugin = new WeblogPlugin();

        List< ? > entries = blogplugin.findBlogEntries( m_testEngine,
                                                       "TestBlog",
                                                        new Date(0),
                                                        new Date(Long.MAX_VALUE) );

        Feed feed = new RSS20Feed( context );
        String blog = gen.generateBlogRSS( context, entries, feed );

        Assert.assertTrue( "has Foo", blog.indexOf("<description>Foo &amp;quot;blah&amp;quot;.</description>") != -1 );
        Assert.assertTrue( "has proper Bar", blog.indexOf("&lt;b&gt;Bar&lt;/b&gt;") != -1 );
    }

}
