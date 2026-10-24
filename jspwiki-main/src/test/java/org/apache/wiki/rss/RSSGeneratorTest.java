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
import jakarta.servlet.http.HttpServletRequest;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.auth.UserManager;

import static org.apache.wiki.auth.UserManager.PROP_DATABASE;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.XMLGroupDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;


public class RSSGeneratorTest {


    private static TestEngine buildRssEngine() throws Exception {
        Properties props = TestEngine.getTestProperties();
        File userTarget = new File("target/" + UUID.randomUUID() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/userdatabase.xml"), userTarget);
        props.setProperty(XMLUserDatabase.PROP_USERDATABASE, userTarget.getAbsolutePath());
        props.put(RSSGenerator.PROP_GENERATE_RSS, "true");
        File groupTarget = new File("target/" + UUID.randomUUID() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/groupdatabase.xml"), groupTarget);
        props.setProperty(XMLGroupDatabase.PROP_DATABASE, groupTarget.getAbsolutePath());
        props.setProperty(GroupManager.PROP_GROUPDATABASE, XMLGroupDatabase.class.getCanonicalName());
        props.setProperty(UserManager.PROP_DATABASE, XMLUserDatabase.class.getCanonicalName());
        
        return TestEngine.build( props );
    }

    @Test
    public void testBlogRSS() throws Exception {

        final TestEngine m_testEngine = buildRssEngine();

        final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
        m_testEngine.saveText( "testBlogRSS", "Foo1" );
        String newPage = plugin.getNewEntryPage( m_testEngine, "testBlogRSS" );
        m_testEngine.saveText( newPage, "!Title1\r\nFoo" );
        newPage = plugin.getNewEntryPage( m_testEngine, "testBlogRSS" );
        m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );

        final RSSGenerator gen = m_testEngine.getManager( RSSGenerator.class );
        final Context context = Wiki.context().create( m_testEngine, m_testEngine.getManager( PageManager.class ).getPage( "testBlogRSS" ) );
        final WeblogPlugin blogplugin = new WeblogPlugin();
        final List< Page > entries = blogplugin.findBlogEntries( m_testEngine, "testBlogRSS", new Date( 0 ), new Date( Long.MAX_VALUE ) );
        final Feed feed = new RSS10Feed( context );
        final String blog = gen.generateBlogRSS( context, entries, feed );
        m_testEngine.shutdown();
        Assertions.assertTrue( blog.contains( "<description>Foo</description>" ), "has Foo" );
        Assertions.assertTrue( blog.contains( "&lt;b&gt;Bar&lt;/b&gt;" ), "has proper Bar" );
        
        
    }

    @Test
    public void testEntryLinksAreAbsolute() throws Exception {
        final TestEngine engine = buildRssEngine();
        engine.saveText( "RssAbsoluteLink", "Foo" );

        final HttpServletRequest request = HttpMockFactory.createHttpRequest();
        Mockito.doReturn( "http" ).when( request ).getScheme();
        Mockito.doReturn( "localhost" ).when( request ).getServerName();
        Mockito.doReturn( 8080 ).when( request ).getServerPort();
        final Page page = engine.getManager( PageManager.class ).getPage( "RssAbsoluteLink" );
        final Context context = Wiki.context().create( engine, request, page );
        final RSSGenerator gen = engine.getManager( RSSGenerator.class );

        for( final Feed feed : List.of( new RSS10Feed( context ), new RSS20Feed( context ), new AtomFeed( context ) ) ) {
            final String xml = gen.generateWikiPageRSS( context, new ArrayList<>( List.of( page ) ), feed );

            Assertions.assertTrue( xml.contains( "http://localhost:8080/test/Wiki.jsp?page=RssAbsoluteLink" ), xml );
            Assertions.assertFalse( xml.contains( ">/test/Wiki.jsp" ) || xml.contains( "\"/test/Wiki.jsp" ), xml );
        }
        engine.shutdown();
    }
}
