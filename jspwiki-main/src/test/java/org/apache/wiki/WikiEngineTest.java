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

package org.apache.wiki;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.CachingProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.providers.VerySimpleProvider;
import org.apache.wiki.util.TextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.ehcache.CacheManager;

public class WikiEngineTest
{
    public static final String NAME1 = "Test1";
    public static final long PAGEPROVIDER_RESCAN_PERIOD = 2;

    Properties props = TestEngine.getTestProperties();

    TestEngine m_engine;


    @BeforeEach
    public void setUp()
        throws Exception
    {
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );

        CacheManager.getInstance().removeAllCaches();

        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);
    }

    @AfterEach
    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        if( files != null )
        {
            File f = new File( files );

            TestEngine.deleteAll( f );
        }

        TestEngine.emptyWorkDir();
    }

    @Test
    public void testNonExistentDirectory()
        throws Exception
    {
        String tmpdir = "./target";
        String dirname = "non-existent-directory";
        String newdir = tmpdir + File.separator + dirname;

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, newdir );
        new TestEngine( props );

        File f = new File( props.getProperty( FileSystemProvider.PROP_PAGEDIR ) );
        Assertions.assertTrue( f.exists(), "didn't create it" );
        Assertions.assertTrue( f.isDirectory(), "isn't a dir" );

        f.delete();
    }

    /**
     *  Check that calling pageExists( String ) works.
     */
    @Test
    public void testNonExistentPage()
        throws Exception
    {
        String pagename = "Test1";

        Assertions.assertEquals( false, m_engine.pageExists( pagename ), "Page already exists" );
    }

    /**
     *  Check that calling pageExists( WikiPage ) works.
     */
    @Test
    public void testNonExistentPage2()
        throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test1");

        Assertions.assertEquals( false, m_engine.pageExists( page ), "Page already exists" );
    }

    @Test
    public void testFinalPageName()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );
        m_engine.saveText( "Foobars", "2" );

        Assertions.assertEquals( "Foobars",m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testFinalPageNameSingular()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );

        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testFinalPageNamePlural()
        throws Exception
    {
        m_engine.saveText( "Foobars", "1" );

        Assertions.assertEquals( "Foobars", m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobars", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testPutPage()
        throws Exception
    {
        String text = "Foobar.\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assertions.assertEquals( true, m_engine.pageExists( name ), "page does not exist" );
        Assertions.assertEquals( text, m_engine.getText( name ), "wrong content" );
    }

    @Test
    public void testPutPageEntities()
        throws Exception
    {
        String text = "Foobar. &quot;\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assertions.assertEquals( true, m_engine.pageExists( name ), "page does not exist" );
        Assertions.assertEquals( "Foobar. &amp;quot;\r\n", m_engine.getText( name ), "wrong content" );
    }

    /**
     *  Cgeck that basic " is changed.
     */
    @Test
    public void testPutPageEntities2()
        throws Exception
    {
        String text = "Foobar. \"\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assertions.assertEquals( true, m_engine.pageExists( name ), "page does not exist" );
        Assertions.assertEquals( "Foobar. &quot;\r\n", m_engine.getText( name ), "wrong content" );
    }

    @Test
    public void testGetHTML()
        throws Exception
    {
        String text = "''Foobar.''";
        String name = NAME1;

        m_engine.saveText( name, text );

        String data = m_engine.getHTML( name );

        Assertions.assertEquals( "<i>Foobar.</i>\n", data );
    }

    @Test
    public void testEncodeNameLatin1()
    {
        String name = "abc\u00e5\u00e4\u00f6";

        Assertions.assertEquals( "abc%E5%E4%F6", m_engine.encodeName(name) );
    }

    @Test
    public void testEncodeNameUTF8()
        throws Exception
    {
        String name = "\u0041\u2262\u0391\u002E";

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );

        WikiEngine engine = new TestEngine( props );

        Assertions.assertEquals( "A%E2%89%A2%CE%91.", engine.encodeName(name) );
    }

    @Test
    public void testReadLinks()
        throws Exception
    {
        String src="Foobar. [Foobar].  Frobozz.  [This is a link].";

        Object[] result = m_engine.scanWikiLinks( new WikiPage(m_engine, "Test"), src ).toArray();

        Assertions.assertEquals( "Foobar", result[0], "item 0" );
        Assertions.assertEquals( "This is a link", result[1], "item 1" );
    }

    @Test
    public void testBeautifyTitle()
    {
        String src = "WikiNameThingy";

        Assertions.assertEquals("Wiki Name Thingy", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    @Test
    public void testBeautifyTitleAcronym()
    {
        String src = "JSPWikiPage";

        Assertions.assertEquals("JSP Wiki Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    @Test
    public void testBeautifyTitleAcronym2()
    {
        String src = "DELETEME";

        Assertions.assertEquals("DELETEME", m_engine.beautifyTitle( src ) );
    }

    @Test
    public void testBeautifyTitleAcronym3()
    {
        String src = "JSPWikiFAQ";

        Assertions.assertEquals("JSP Wiki FAQ", m_engine.beautifyTitle( src ) );
    }

    @Test
    public void testBeautifyTitleNumbers()
    {
        String src = "TestPage12";

        Assertions.assertEquals("Test Page 12", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too.
     */
    @Test
    public void testBeautifyTitleArticle()
    {
        String src = "ThisIsAPage";

        Assertions.assertEquals("This Is A Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too, pathological case...
     */
    /*
    @Test
    public void testBeautifyTitleArticle2()
    {
        String src = "ThisIsAJSPWikiPage";

        Assertions.assertEquals("This Is A JSP Wiki Page", m_engine.beautifyTitle( src ) );
    }
    */

    @Test
    public void testLatestGet()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider",
                           "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        WikiPage p = engine.getPage( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet2()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider",
                           "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getText( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet3()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider",
                           "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getHTML( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        Assertions.assertEquals( "test", vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( 5, vsp.m_latestVers, "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    @Test
    public void testLatestGet4()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider",
                           "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "true" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getHTML( VerySimpleProvider.PAGENAME, -1 );

        CachingProvider cp = (CachingProvider)engine.getPageManager().getProvider();
        VerySimpleProvider vsp = (VerySimpleProvider) cp.getRealProvider();

        Assertions.assertEquals( VerySimpleProvider.PAGENAME, vsp.m_latestReq, "wrong page" );
        Assertions.assertEquals( -1, vsp.m_latestVers,  "wrong version" );
        Assertions.assertNotNull( p, "null" );
    }

    /**
     *  Checks, if ReferenceManager is informed of new attachments.
     */
    @Test
    public void testAttachmentRefs()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "fooBar");

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

        try
        {
            // and check post-conditions
            Collection< String > c = refMgr.findUncreated();
            Assertions.assertTrue( c==null || c.size()==0, "attachment exists: "+c );

            c = refMgr.findUnreferenced();
            Assertions.assertEquals( 2, c.size(), "unreferenced count" );
            Iterator< String > i = c.iterator();
            String first = i.next();
            String second = i.next();
            Assertions.assertTrue(  (first.equals( NAME1 ) && second.equals( NAME1+"/TestAtt.txt"))
                                 || (first.equals( NAME1+"/TestAtt.txt" ) && second.equals( NAME1 )), "unreferenced" );
        }
        finally
        {
            // do cleanup
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
        }
    }

    /**
     *  Is ReferenceManager updated properly if a page references
     *  its own attachments?
     */

    /*
      FIXME: This is a deep problem.  The real problem is that the reference
      manager cannot know when it encounters a link like "testatt.txt" that it
      is really a link to an attachment IF the link is created before
      the attachment.  This means that when the attachment is created,
      the link will stay in the "uncreated" list.

      There are two issues here: first of all, TranslatorReader should
      able to return the proper attachment references (which I think
      it does), and second, the ReferenceManager should be able to
      remove any links that are not referred to, nor they are created.

      However, doing this in a relatively sane timeframe can be a problem.
    */

    @Test
    public void testAttachmentRefs2()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "[TestAtt.txt]");

        // check a few pre-conditions

        Collection< String > c = refMgr.findReferrers( "TestAtt.txt" );
        Assertions.assertTrue( c!=null && c.iterator().next().equals( NAME1 ), "normal, unexisting page" );

        c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
        Assertions.assertTrue( c==null || c.size()==0, "no attachment" );

        c = refMgr.findUncreated();
        Assertions.assertTrue( c!=null && c.size()==1 && c.iterator().next().equals( "TestAtt.txt" ), "unknown attachment" );

        // now we create the attachment

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );
        try
        {
            // and check post-conditions
            c = refMgr.findUncreated();
            Assertions.assertTrue( c==null || c.size()==0, "attachment exists: " );

            c = refMgr.findReferrers( "TestAtt.txt" );
            Assertions.assertTrue( c==null || c.size()==0, "no normal page" );

            c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
            Assertions.assertTrue( c!=null && c.iterator().next().equals( NAME1 ), "attachment exists now" );

            c = refMgr.findUnreferenced();
            Assertions.assertTrue( c.size()==1 && c.iterator().next().equals( NAME1 ), "unreferenced" );
        }
        finally
        {
            // do cleanup
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
        }
    }

    /**
     *  Checks, if ReferenceManager is informed if a link to an attachment is added.
     */
    @Test
    public void testAttachmentRefs3()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "fooBar");

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

        m_engine.saveText( NAME1, " ["+NAME1+"/TestAtt.txt] ");

        try
        {
            // and check post-conditions
            Collection< String > c = refMgr.findUncreated();
            Assertions.assertTrue( c==null || c.size()==0, "attachment exists" );

            c = refMgr.findUnreferenced();
            Assertions.assertEquals( c.size(), 1, "unreferenced count" );
            Assertions.assertTrue( c.iterator().next().equals( NAME1 ), "unreferenced" );
        }
        finally
        {
            // do cleanup
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
        }
    }

    /**
     *  Checks, if ReferenceManager is informed if a third page references an attachment.
     */
    @Test
    public void testAttachmentRefs4()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "[TestPage2]");

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

        m_engine.saveText( "TestPage2", "["+NAME1+"/TestAtt.txt]");

        try
        {
            // and check post-conditions
            Collection< String > c = refMgr.findUncreated();
            Assertions.assertTrue( c==null || c.size()==0, "attachment exists" );

            c = refMgr.findUnreferenced();
            Assertions.assertEquals( c.size(), 1, "unreferenced count" );
            Assertions.assertTrue( c.iterator().next().equals( NAME1 ), "unreferenced" );
        }
        finally
        {
            // do cleanup
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
            new File( files, "TestPage2"+FileSystemProvider.FILE_EXT ).delete();
        }
    }




    @Test
    public void testDeletePage()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        Assertions.assertTrue( saved.exists(), "Didn't create it!" );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        Assertions.assertFalse( saved.exists(), "Page has not been removed!" );
    }


    @Test
    public void testDeletePageAndAttachments()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        String atts = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );

        Assertions.assertTrue( saved.exists(), "Didn't create it!" );

        Assertions.assertTrue( attfile.exists(), "Attachment dir does not exist" );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        Assertions.assertFalse( saved.exists(), "Page has not been removed!" );
        Assertions.assertFalse( attfile.exists(), "Attachment has not been removed" );
    }

    @Test
    public void testDeletePageAndAttachments2()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        String atts = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );

        Assertions.assertTrue( saved.exists(), "Didn't create it!" );

        Assertions.assertTrue( attfile.exists(), "Attachment dir does not exist" );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        Assertions.assertNotNull( page, "page" );

        att = m_engine.getAttachmentManager().getAttachmentInfo(NAME1+"/TestAtt.txt");

        m_engine.deletePage(att.getName());

        m_engine.deletePage( NAME1 );

        Assertions.assertNull( m_engine.getPage(NAME1), "Page not removed" );
        Assertions.assertNull( m_engine.getPage(NAME1+"/TestAtt.txt"), "Att not removed" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers(NAME1);

        Assertions.assertNull( refs, "referrers" );
    }

    @Test
    public void testDeleteVersion()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );

        TestEngine engine = new TestEngine( props );
        engine.saveText( NAME1, "Test1" );
        engine.saveText( NAME1, "Test2" );
        engine.saveText( NAME1, "Test3" );

        WikiPage page = engine.getPage( NAME1, 3 );

        engine.deleteVersion( page );

        Assertions.assertNull( engine.getPage( NAME1, 3 ), "got page" );

        String content = engine.getText( NAME1, WikiProvider.LATEST_VERSION );

        Assertions.assertEquals( "Test2", content.trim(), "content" );
    }

    @Test
    public void testDeleteVersion2()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );

        TestEngine engine = new TestEngine( props );
        engine.saveText( NAME1, "Test1" );
        engine.saveText( NAME1, "Test2" );
        engine.saveText( NAME1, "Test3" );

        WikiPage page = engine.getPage( NAME1, 1 );

        engine.deleteVersion( page );

        Assertions.assertNull( engine.getPage( NAME1, 1 ), "got page" );

        String content = engine.getText( NAME1, WikiProvider.LATEST_VERSION );

        Assertions.assertEquals( "Test3", content.trim(), "content" );

        Assertions.assertEquals( "", engine.getText(NAME1, 1).trim(), "content1" );
    }


    /**
     *  Tests BugReadingOfVariableNotWorkingForOlderVersions
     * @throws Exception
     */
    @Test
    public void testOldVersionVars()
        throws Exception
    {
        Properties props = TestEngine.getTestProperties("/jspwiki-vers-custom.properties");

        props.setProperty( PageManager.PROP_USECACHE, "true" );

        TestEngine engine = new TestEngine( props );

        engine.saveText( NAME1, "[{SET foo=bar}]" );

        engine.saveText( NAME1, "[{SET foo=notbar}]");

        WikiPage v1 = engine.getPage( NAME1, 1 );

        WikiPage v2 = engine.getPage( NAME1, 2 );

        Assertions.assertEquals( "bar", v1.getAttribute("foo"), "V1" );

        // FIXME: The following must run as well
        Assertions.assertEquals( "notbar", v2.getAttribute("foo"), "V2" );

        engine.deletePage( NAME1 );
    }

    @Test
    public void testSpacedNames1()
        throws Exception
    {
        m_engine.saveText("This is a test", "puppaa");

        Assertions.assertEquals( "puppaa", m_engine.getText("This is a test").trim(), "normal" );
    }


    @Test
    public void testParsedVariables() throws Exception
    {
        m_engine.saveText( "TestPage", "[{SET foo=bar}][{SamplePlugin text='{$foo}'}]");

        String res = m_engine.getHTML( "TestPage" );

        Assertions.assertEquals( "bar\n", res );
    }

    /**
     * Tests BugReferenceToRenamedPageNotCleared
     *
     * @throws Exception
     */
    @Test
    public void testRename() throws Exception
    {
        m_engine.saveText( "RenameBugTestPage", "Mary had a little generic object" );
        m_engine.saveText( "OldNameTestPage", "Linked to RenameBugTestPage" );

        Collection< String > pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );
        Assertions.assertEquals( "OldNameTestPage", pages.iterator().next(), "has one" );

        WikiContext ctx = new WikiContext( m_engine, m_engine.getPage("OldNameTestPage") );

        m_engine.renamePage( ctx, "OldNameTestPage", "NewNameTestPage", true );

        Assertions.assertFalse( m_engine.pageExists( "OldNameTestPage"), "did not vanish" );
        Assertions.assertTrue( m_engine.pageExists( "NewNameTestPage"), "did not appear" );

        pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );

        Assertions.assertEquals( 1, pages.size(),  "wrong # of referrers" );

        Assertions.assertEquals( "NewNameTestPage", pages.iterator().next(), "has wrong referrer" );
    }

    @Test
    public void testChangeNoteOldVersion2() throws Exception
    {
        WikiPage p = new WikiPage( m_engine, NAME1 );

        WikiContext context = new WikiContext(m_engine,p);

        context.getPage().setAttribute( WikiPage.CHANGENOTE, "Test change" );

        m_engine.saveText( context, "test" );

        for( int i = 0; i < 5; i++ )
        {
            WikiPage p2 = (WikiPage)m_engine.getPage( NAME1 ).clone();
            p2.removeAttribute(WikiPage.CHANGENOTE);

            context.setPage( p2 );

            m_engine.saveText( context, "test"+i );
        }

        WikiPage p3 = m_engine.getPage( NAME1, -1 );

        Assertions.assertEquals( null, p3.getAttribute(WikiPage.CHANGENOTE) );
    }

    @Test
    public void testCreatePage() throws Exception
    {
        String text = "Foobar.\r\n";
        String name = "mrmyxpltz";

        Assertions.assertEquals( false, m_engine.pageExists( name ), "page should not exist right now" );

        m_engine.saveText( name, text );

        Assertions.assertEquals( true, m_engine.pageExists( name ), "page does not exist" );
    }

    @Test
    public void testCreateEmptyPage() throws Exception
    {
        String text = "";
        String name = "mrmxyzptlk";

        Assertions.assertEquals( false, m_engine.pageExists( name ), "page should not exist right now" );

        m_engine.saveText( name, text );

        Assertions.assertEquals( false, m_engine.pageExists( name ), "page should not exist right now neither" );
    }

    @Test
    public void testSaveExistingPageWithEmptyContent() throws Exception
    {
        String text = "Foobar.\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assertions.assertEquals( true, m_engine.pageExists( name ), "page does not exist" );

        // saveText uses normalizePostData to assure it conforms to certain rules
        Assertions.assertEquals( TextUtil.normalizePostData( text ), m_engine.getText( name ), "wrong content" );

        m_engine.saveText( name, "" );

        Assertions.assertEquals( TextUtil.normalizePostData( "" ), m_engine.getText( name ), "wrong content" );
    }
    
    @Test
    public void testGetRequiredProperty() throws Exception
    {
        String[] vals = { "foo", " this is a property ", "bar", "60" };
        Properties props = TextUtil.createProperties(vals);
        Assertions.assertEquals( "60", m_engine.getRequiredProperty( props, "bar" ) );
    }

    @Test
    public void testGetRequiredPropertyNRPE()
    {
        String[] vals = { "foo", " this is a property ", "bar", "60" };
        Properties props = TextUtil.createProperties(vals);
        try
        {
            m_engine.getRequiredProperty( props, "ber" );
            Assertions.fail( "NoRequiredPropertyException should've been thrown!" );
        }
        catch (NoRequiredPropertyException nrpe) {}
    }

}
