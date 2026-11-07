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
package org.apache.wiki.rss;

import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.diff.DifferenceManager;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tests that the RSS feed of a page diffs a version against the version before it, also when versions have been deleted.
 */
class RSSGeneratorVersionsTest {

    static final String PAGE = "RSSVersionsPage";

    /** The rows of the diff table, as they appear in the escaped description of an item. */
    static final String UNCHANGED_LINE_ONE = "class=\"diff\"&gt;&lt;td&gt;1&lt;/td&gt;&lt;td&gt;line one";
    static final String ADDED_LINE_ONE = "class=\"diffadd\"&gt;&lt;td&gt;1&lt;/td&gt;&lt;td&gt;line one";

    TestEngine engine;
    PageManager pageManager;

    @BeforeEach
    void setUp() {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );
        props.setProperty( "jspwiki.fileSystemProvider.pageDir", props.getProperty( "jspwiki.fileSystemProvider.pageDir" ) + System.currentTimeMillis() );
        props.setProperty( DifferenceManager.PROP_DIFF_PROVIDER, "org.apache.wiki.diff.SvnStyleDiffProvider" );
        props.setProperty( RSSGenerator.PROP_GENERATE_RSS, "true" );
        engine = TestEngine.build( props );
        pageManager = engine.getManager( PageManager.class );
    }

    @AfterEach
    void tearDown() {
        engine.stop();
    }

    String feedOfLatestVersion() throws Exception {
        final Page latest = pageManager.getPage( PAGE );
        final Context context = Wiki.context().create( engine, HttpMockFactory.createHttpRequest(), latest );
        final List< Page > entries = new ArrayList<>();
        entries.add( latest );
        return engine.getManager( RSSGenerator.class ).generateWikiPageRSS( context, entries, new RSS20Feed( context ) );
    }

    @Test
    void testDiffsAgainstThePreviousVersion() throws Exception {
        engine.saveText( PAGE, "line one" );
        engine.saveText( PAGE, "line one\nline two" );

        final String feed = feedOfLatestVersion();

        Assertions.assertTrue( feed.contains( "changed this page" ), feed );
        Assertions.assertTrue( feed.contains( "line two" ), feed );
        Assertions.assertTrue( feed.contains( UNCHANGED_LINE_ONE ), "line one did not change: " + feed );
        Assertions.assertFalse( feed.contains( ADDED_LINE_ONE ), "line one was not added: " + feed );
    }

    @Test
    void testDiffsAgainstTheLatestExistingVersionWhenOneWasDeleted() throws Exception {
        engine.saveText( PAGE, "line one" );
        engine.saveText( PAGE, "line one\nline two" );
        engine.saveText( PAGE, "line one\nline two\nline three" );
        pageManager.deleteVersion( pageManager.getPage( PAGE, 2 ) );

        final String feed = feedOfLatestVersion();

        Assertions.assertTrue( feed.contains( "changed this page" ), feed );
        Assertions.assertTrue( feed.contains( "line two" ), feed );
        Assertions.assertTrue( feed.contains( "line three" ), feed );
        Assertions.assertTrue( feed.contains( UNCHANGED_LINE_ONE ), "line one did not change: " + feed );
        Assertions.assertFalse( feed.contains( ADDED_LINE_ONE ), "line one was not added: " + feed );
    }

    @Test
    void testFirstVersionIsDescribedAsCreated() throws Exception {
        engine.saveText( PAGE, "line one" );

        final String feed = feedOfLatestVersion();

        Assertions.assertTrue( feed.contains( "created this page" ), feed );
    }

}
