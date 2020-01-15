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

package org.apache.wiki.pages;

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.CachingProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.providers.VerySimpleProvider;
import org.apache.wiki.util.TextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

public class DefaultPageManagerTest {

    static final String NAME1 = "Test1";

    TestEngine engine = TestEngine.build();

    @AfterEach
    public void tearDown() {
        final String files = engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );

        if( files != null ) {
            final File f = new File( files );
            TestEngine.deleteAll( f );
        }

        TestEngine.emptyWorkDir();
        CacheManager.getInstance().removeAllCaches();
    }

    /**
     *  Check that calling pageExists( String ) works.
     */
    @Test
    public void testNonExistentPage() {
        Assertions.assertFalse( engine.getPageManager().wikiPageExists( NAME1 ), "Page already exists" );
    }

    /**
     *  Check that calling pageExists( WikiPage ) works.
     */
    @Test
    public void testNonExistentPage2() throws Exception {
        final WikiPage page = new WikiPage( engine, NAME1 );
        Assertions.assertFalse( engine.getPageManager().wikiPageExists( page ), "Page already exists" );
    }

    @Test
    public void testPageCacheExists() throws Exception {
        engine.getWikiProperties().setProperty( "jspwiki.usePageCache", "true" );
        final PageManager m = new DefaultPageManager( engine, engine.getWikiProperties() );

        Assertions.assertTrue( m.getProvider() instanceof CachingProvider );
    }

    @Test
    public void testPageCacheNotInUse() throws Exception {
        engine.getWikiProperties().setProperty( "jspwiki.usePageCache", "false" );
        final PageManager m = new DefaultPageManager( engine, engine.getWikiProperties() );

        Assertions.assertFalse( m.getProvider() instanceof CachingProvider );
    }

    @Test
    public void testDeletePage() throws Exception {
        engine.saveText( NAME1, "Test" );
        final String files = engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );
        final File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );
        Assertions.assertTrue( saved.exists(), "Didn't create it!" );

        final WikiPage page = engine.getPageManager().getPage( NAME1, WikiProvider.LATEST_VERSION );
        engine.getPageManager().deletePage( page.getName() );
        Assertions.assertFalse( saved.exists(), "Page has not been removed!" );
    }

    @Test
    public void testDeletePageAndAttachments() throws Exception {
        engine.saveText( NAME1, "Test" );
        final Attachment att = new Attachment( engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        engine.getAttachmentManager().storeAttachment( att, engine.makeAttachmentFile() );

        final String files = engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );
        final File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        final String atts = engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        final File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );

        Assertions.assertTrue( saved.exists(), "Didn't create it!" );
        Assertions.assertTrue( attfile.exists(), "Attachment dir does not exist" );

        final WikiPage page = engine.getPageManager().getPage( NAME1, WikiProvider.LATEST_VERSION );

        engine.getPageManager().deletePage( page.getName() );

        Assertions.assertFalse( saved.exists(), "Page has not been removed!" );
        Assertions.assertFalse( attfile.exists(), "Attachment has not been removed" );
    }

    @Test
    public void testDeletePageAndAttachments2() throws Exception {
        engine.saveText( NAME1, "Test" );
        Attachment att = new Attachment( engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        engine.getAttachmentManager().storeAttachment( att, engine.makeAttachmentFile() );

        final String files = engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );
        final File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        final String atts = engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        final File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );

        Assertions.assertTrue( saved.exists(), "Didn't create it!" );
        Assertions.assertTrue( attfile.exists(), "Attachment dir does not exist" );

        final WikiPage page = engine.getPageManager().getPage( NAME1, WikiProvider.LATEST_VERSION );
        Assertions.assertNotNull( page, "page" );

        att = engine.getAttachmentManager().getAttachmentInfo(NAME1+"/TestAtt.txt");
        engine.getPageManager().deletePage(att.getName());
        engine.getPageManager().deletePage( NAME1 );
        Assertions.assertNull( engine.getPageManager().getPage(NAME1), "Page not removed" );
        Assertions.assertNull( engine.getPageManager().getPage(NAME1+"/TestAtt.txt"), "Att not removed" );

        final Collection< String > refs = engine.getReferenceManager().findReferrers(NAME1);
        Assertions.assertNull( refs, "referrers" );
    }

    @Test
    public void testDeleteVersion() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );
        final TestEngine engine = new TestEngine( props );
        engine.saveText( NAME1, "Test1" );
        engine.saveText( NAME1, "Test2" );
        engine.saveText( NAME1, "Test3" );

        final WikiPage page = engine.getPageManager().getPage( NAME1, 3 );
        engine.getPageManager().deleteVersion( page );
        Assertions.assertNull( engine.getPageManager().getPage( NAME1, 3 ), "got page" );

        final String content = engine.getPageManager().getText( NAME1, WikiProvider.LATEST_VERSION );
        Assertions.assertEquals( "Test2", content.trim(), "content" );
    }

    @Test
    public void testDeleteVersion2() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );
        final TestEngine engine = new TestEngine( props );
        engine.saveText( NAME1, "Test1" );
        engine.saveText( NAME1, "Test2" );
        engine.saveText( NAME1, "Test3" );

        final WikiPage page = engine.getPageManager().getPage( NAME1, 1 );
        engine.getPageManager().deleteVersion( page );
        Assertions.assertNull( engine.getPageManager().getPage( NAME1, 1 ), "got page" );

        final String content = engine.getPageManager().getText( NAME1, WikiProvider.LATEST_VERSION );
        Assertions.assertEquals( "Test3", content.trim(), "content" );
        Assertions.assertEquals( "", engine.getPageManager().getText(NAME1, 1).trim(), "content1" );
    }

    @Test
    public void testLatestGet() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );
        final WikiEngine engine = new TestEngine( props );
        final WikiPage p = engine.getPageManager().getPage( "test", -1 );
        final VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet2() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );
        final WikiEngine engine = new TestEngine( props );
        final String p = engine.getPageManager().getText( "test", -1 );
        final VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet3() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );
        final WikiEngine engine = new TestEngine( props );
        final String p = engine.getRenderingManager().getHTML( "test", -1 );
        final VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( 5, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet4() throws Exception {
        final Properties props = engine.getWikiProperties();
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "true" );
        final WikiEngine engine = new TestEngine( props );
        final String p = engine.getRenderingManager().getHTML( VerySimpleProvider.PAGENAME, -1 );
        final CachingProvider cp = (CachingProvider)engine.getPageManager().getProvider();
        final VerySimpleProvider vsp = (VerySimpleProvider) cp.getRealProvider();

        Assertions.assertEquals( VerySimpleProvider.PAGENAME, vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers,  "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testCreatePage() throws Exception {
        final String text = "Foobar.\r\n";
        final String name = "mrmyxpltz";
        Assertions.assertFalse( engine.getPageManager().wikiPageExists( name ), "page should not exist right now" );

        engine.saveText( name, text );
        Assertions.assertTrue( engine.getPageManager().wikiPageExists( name ), "page does not exist" );
    }

    @Test
    public void testCreateEmptyPage() throws Exception {
        final String text = "";
        final String name = "mrmxyzptlk";
        Assertions.assertFalse( engine.getPageManager().wikiPageExists( name ), "page should not exist right now" );

        engine.saveText( name, text );
        Assertions.assertFalse( engine.getPageManager().wikiPageExists( name ), "page should not exist right now neither" );
    }

    @Test
    public void testPutPage() throws Exception {
        final String text = "Foobar.\r\n";
        final String name = NAME1;
        engine.saveText( name, text );

        Assertions.assertTrue( engine.getPageManager().wikiPageExists( name ), "page does not exist" );
        Assertions.assertEquals( text, engine.getPageManager().getText( name ), "wrong content" );
    }

    @Test
    public void testPutPageEntities() throws Exception {
        final String text = "Foobar. &quot;\r\n";
        final String name = NAME1;
        engine.saveText( name, text );

        Assertions.assertTrue( engine.getPageManager().wikiPageExists( name ), "page does not exist" );
        Assertions.assertEquals( "Foobar. &amp;quot;\r\n", engine.getPageManager().getText( name ), "wrong content" );
    }

    /**
     *  Check that basic " is changed.
     */
    @Test
    public void testPutPageEntities2() throws Exception {
        final String text = "Foobar. \"\r\n";
        final String name = NAME1;
        engine.saveText( name, text );

        Assertions.assertTrue( engine.getPageManager().wikiPageExists( name ), "page does not exist" );
        Assertions.assertEquals( "Foobar. &quot;\r\n", engine.getPageManager().getText( name ), "wrong content" );
    }

    @Test
    public void testSaveExistingPageWithEmptyContent() throws Exception {
        final String text = "Foobar.\r\n";
        final String name = NAME1;
        engine.saveText( name, text );

        Assertions.assertTrue( engine.getPageManager().wikiPageExists( name ), "page does not exist" );
        // saveText uses normalizePostData to assure it conforms to certain rules
        Assertions.assertEquals( TextUtil.normalizePostData( text ), engine.getPageManager().getText( name ), "wrong content" );

        engine.saveText( name, "" );
        Assertions.assertEquals( TextUtil.normalizePostData( "" ), engine.getPageManager().getText( name ), "wrong content" );
    }

}
