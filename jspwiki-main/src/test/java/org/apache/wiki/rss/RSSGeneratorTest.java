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

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.UserManager;

import static org.apache.wiki.auth.UserManager.PROP_DATABASE;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.XMLGroupDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;


public class RSSGeneratorTest {

  
    @Test
    public void testBlogRSS() throws Exception {
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
        
        TestEngine m_testEngine = TestEngine.build( props);
        try{
        

            final WeblogEntryPlugin plugin = new WeblogEntryPlugin();

            m_testEngine.saveText( "testBlogRSS", "Foo1[{WeblogPlugin page='MyBlog' days='all' maxEntries='10'}]" );
            String newPage = plugin.getNewEntryPage( m_testEngine, "testBlogRSS" );
            m_testEngine.saveText( newPage, "!Title1\r\nFoo" );
            newPage = plugin.getNewEntryPage( m_testEngine, "testBlogRSS" );
            m_testEngine.saveText( newPage, "!Title2\r\n__Bar__" );

            //make a new page that only mike can see...
            newPage = plugin.getNewEntryPage( m_testEngine, "testBlogRSS" );
            m_testEngine.saveText( newPage, "[{ALLOW view admin,Mike}]\r\n!Title3\r\n__Tanaka__" );

            final RSSGenerator gen = m_testEngine.getManager( RSSGenerator.class );
            WikiContext context = (WikiContext) Wiki.context().create( m_testEngine, 
                    m_testEngine.getManager( PageManager.class ).getPage( "testBlogRSS" ) );

            //run as the admin...
            HttpServletRequest request = HttpMockFactory.createHttpRequest();
            m_testEngine.getManager( AuthenticationManager.class ).login( m_testEngine.adminSession(), request,
                    Users.ADMIN, Users.ADMIN_PASS );
            context = (WikiContext) Wiki.context().create( m_testEngine, request, "/wiki");
            final WeblogPlugin blogplugin = new WeblogPlugin();
            List< Page > entries = blogplugin.findBlogEntries( m_testEngine, "testBlogRSS", 
                    //get all pages since jan 1 1970 UTC and the heat death of the universe
                    new Date( 0 ), 
                    new Date( Long.MAX_VALUE ) );
            Assertions.assertTrue(!entries.isEmpty());
            Feed feed = new RSS10Feed( context );
            final String blog = gen.generateBlogRSS( context, entries, feed );
            final String fullWikiRssFeed = gen.generateBlogRSS( context, entries, feed );
            final String wikiPageRss = gen.generateWikiPageRSS(context, entries, feed);

            Feed feed20 = new RSS20Feed( context );
            
            final String blog20 = gen.generateBlogRSS( context, entries, feed20 );
            final String fullWikiRssFeed20 = gen.generateBlogRSS( context, entries, feed20 );
            final String wikiPageRss20 = gen.generateWikiPageRSS(context, entries, feed20);

            Feed feedAtom = new AtomFeed( context );
            final String blogAtom = gen.generateBlogRSS( context, entries, feedAtom );
            final String fullWikiRssFeedAtom = gen.generateBlogRSS( context, entries, feedAtom );
            final String wikiPageRssAtom = gen.generateWikiPageRSS(context, entries, feedAtom);
            Assertions.assertTrue( blog.contains( "<description>Foo</description>" ), "has Foo" );
            Assertions.assertTrue( blog20.contains( "Foo"), "has Foo" );
            Assertions.assertTrue( blogAtom.contains( "Foo" ), "has Foo" );
            
            Assertions.assertTrue( blog.contains( "&lt;b&gt;Bar&lt;/b&gt;" ), "has proper Bar" );
            Assertions.assertTrue( fullWikiRssFeed.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertTrue( wikiPageRss.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertTrue( fullWikiRssFeed20.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertTrue( wikiPageRss20.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertTrue( fullWikiRssFeedAtom.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertTrue( wikiPageRssAtom.contains( "Tanaka" ), "has hidden page" );


            //login as janne...
            request = HttpMockFactory.createHttpRequest();
            m_testEngine.getManager( AuthenticationManager.class ).login( m_testEngine.janneSession(), request,
                    Users.JANNE, Users.JANNE_PASS );
            
            context = (WikiContext) Wiki.context().create( m_testEngine, request, "/wiki");
            final WikiContext janneContext = (WikiContext) Wiki.context().create( m_testEngine, 
                    m_testEngine.getManager( PageManager.class ).getPage( "testBlogRSS" ) );
            entries = blogplugin.findBlogEntries( m_testEngine, "testBlogRSS", 
                    //get all pages since jan 1 1970 UTC and the heat death of the universe
                    new Date( 0 ), 
                    new Date( Long.MAX_VALUE ) );
            Assertions.assertTrue(!entries.isEmpty());
            feed = new RSS10Feed( janneContext );
            final String janneblog = gen.generateBlogRSS( janneContext, entries, feed );
            final String jannefullWikiRssFeed = gen.generateBlogRSS( janneContext, entries, feed );
            final String jannewikiPageRss = gen.generateWikiPageRSS(janneContext, entries, feed);

            feed20 = new RSS20Feed( janneContext );
            final String janneblog20 = gen.generateBlogRSS( janneContext, entries, feed20 );
            final String jannefullWikiRssFeed20 = gen.generateBlogRSS( janneContext, entries, feed20 );
            final String jannewikiPageRss20 = gen.generateWikiPageRSS(janneContext, entries, feed20);

            feedAtom = new AtomFeed( janneContext );
            final String janneblogAtom = gen.generateBlogRSS( janneContext, entries, feed20 );
            final String jannefullWikiRssFeedAtom = gen.generateBlogRSS( janneContext, entries, feed20 );
            final String jannewikiPageRssAtom = gen.generateWikiPageRSS(janneContext, entries, feed20);

            Assertions.assertTrue( janneblog.contains( "<description>Foo</description>" ), "has Foo" );
            Assertions.assertTrue( janneblog20.contains( "Foo"), "has Foo" );
            Assertions.assertTrue( janneblogAtom.contains( "Foo" ), "has Foo" );
            
            Assertions.assertTrue( janneblog.contains( "&lt;b&gt;Bar&lt;/b&gt;" ), "has proper Bar" );
            Assertions.assertFalse( jannefullWikiRssFeed.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertFalse( jannewikiPageRss.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertFalse( jannefullWikiRssFeed20.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertFalse( jannewikiPageRss20.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertFalse( jannefullWikiRssFeedAtom.contains( "Tanaka" ), "has hidden page" );
            Assertions.assertFalse( jannewikiPageRssAtom.contains( "Tanaka" ), "has hidden page" );

        } finally {
            m_testEngine.shutdown();
        }
       
        
        
    }
}
