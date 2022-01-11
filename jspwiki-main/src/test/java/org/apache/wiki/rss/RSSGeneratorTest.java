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

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.apache.wiki.providers.FileSystemProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;


/**
 *
 *  @since
 */
public class RSSGeneratorTest {

    TestEngine m_testEngine;
    Properties props = TestEngine.getTestProperties();

    @BeforeEach
    public void setUp() {
        props.setProperty( RSSGenerator.PROP_GENERATE_RSS, "true" );
        m_testEngine = TestEngine.build( props );
    }

    @AfterEach
    public void tearDown() {
        TestEngine.deleteAll( new File(props.getProperty( FileSystemProvider.PROP_PAGEDIR )) );
        CacheManager.getInstance().removeAllCaches();
    }

    @Test
    public void testBlogRSS()
        throws Exception
    {
        final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
        m_testEngine.saveText( "TestBlog", "Foo1" );

        String newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title1\r\nFoo" );

        newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );

        final RSSGenerator gen = m_testEngine.getManager( RSSGenerator.class );

        final Context context = Wiki.context().create( m_testEngine, m_testEngine.getManager( PageManager.class ).getPage("TestBlog") );

        final WeblogPlugin blogplugin = new WeblogPlugin();

        final List< Page > entries = blogplugin.findBlogEntries( m_testEngine,
                                                               "TestBlog",
                                                               new Date(0),
                                                               new Date(Long.MAX_VALUE) );

        final Feed feed = new RSS10Feed( context );
        final String blog = gen.generateBlogRSS( context, entries, feed );

        Assertions.assertTrue( blog.contains( "<description>Foo</description>" ), "has Foo" );
        Assertions.assertTrue( blog.contains( "&lt;b&gt;Bar&lt;/b&gt;" ), "has proper Bar" );
    }

    @Test
    public void testBlogRSS2()
        throws Exception
    {
        final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
        m_testEngine.saveText( "TestBlog", "Foo1" );

        String newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title1\r\nFoo \"blah\"." );

        newPage = plugin.getNewEntryPage( m_testEngine, "TestBlog" );
        m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );

        final RSSGenerator gen = m_testEngine.getManager( RSSGenerator.class );

        final Context context = Wiki.context().create( m_testEngine, m_testEngine.getManager( PageManager.class ).getPage("TestBlog") );

        final WeblogPlugin blogplugin = new WeblogPlugin();

        final List< Page > entries = blogplugin.findBlogEntries( m_testEngine,
                                                               "TestBlog",
                                                               new Date(0),
                                                               new Date(Long.MAX_VALUE) );

        final Feed feed = new RSS20Feed( context );
        final String blog = gen.generateBlogRSS( context, entries, feed );

        Assertions.assertTrue( blog.contains( "<description>Foo &amp;quot;blah&amp;quot;.</description>" ), "has Foo" );
        Assertions.assertTrue( blog.contains( "&lt;b&gt;Bar&lt;/b&gt;" ), "has proper Bar" );
    }

}
