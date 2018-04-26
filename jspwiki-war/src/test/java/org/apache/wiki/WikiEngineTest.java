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
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.providers.BasicAttachmentProvider;
import org.apache.wiki.providers.CachingProvider;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.providers.VerySimpleProvider;
import org.apache.wiki.util.TextUtil;

import org.junit.Test;
import org.junit.Assert;
import net.sf.ehcache.CacheManager;

public class WikiEngineTest
{
    public static final String NAME1 = "Test1";
    public static final long PAGEPROVIDER_RESCAN_PERIOD = 2;

    Properties props = TestEngine.getTestProperties();

    TestEngine m_engine;


    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[] { WikiEngineTest.class.getName() } );
    }

    @Before
    public void setUp()
        throws Exception
    {
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );

        CacheManager.getInstance().removeAllCaches();

        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);
    }

    @After
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
        Assert.assertTrue( "didn't create it", f.exists() );
        Assert.assertTrue( "isn't a dir", f.isDirectory() );

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

        Assert.assertEquals( "Page already exists",
                      false,
                      m_engine.pageExists( pagename ) );
    }

    /**
     *  Check that calling pageExists( WikiPage ) works.
     */
    @Test
    public void testNonExistentPage2()
        throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test1");

        Assert.assertEquals( "Page already exists",
                      false,
                      m_engine.pageExists( page ) );
    }

    @Test
    public void testFinalPageName()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );
        m_engine.saveText( "Foobars", "2" );

        Assert.assertEquals( "plural mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobars" ) );

        Assert.assertEquals( "singular mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobar" ) );
    }

    @Test
    public void testFinalPageNameSingular()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );

        Assert.assertEquals( "plural mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobars" ) );
        Assert.assertEquals( "singular mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobar" ) );
    }

    @Test
    public void testFinalPageNamePlural()
        throws Exception
    {
        m_engine.saveText( "Foobars", "1" );

        Assert.assertEquals( "plural mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobars" ) );
        Assert.assertEquals( "singular mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobar" ) );
    }

    @Test
    public void testPutPage()
        throws Exception
    {
        String text = "Foobar.\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assert.assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        Assert.assertEquals( "wrong content",
                      text,
                      m_engine.getText( name ) );
    }

    @Test
    public void testPutPageEntities()
        throws Exception
    {
        String text = "Foobar. &quot;\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assert.assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        Assert.assertEquals( "wrong content",
                      "Foobar. &amp;quot;\r\n",
                      m_engine.getText( name ) );
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

        Assert.assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        Assert.assertEquals( "wrong content",
                      "Foobar. &quot;\r\n",
                      m_engine.getText( name ) );
    }

    @Test
    public void testGetHTML()
        throws Exception
    {
        String text = "''Foobar.''";
        String name = NAME1;

        m_engine.saveText( name, text );

        String data = m_engine.getHTML( name );

        Assert.assertEquals( "<i>Foobar.</i>\n",
                       data );
    }

    @Test
    public void testEncodeNameLatin1()
    {
        String name = "abc\u00e5\u00e4\u00f6";

        Assert.assertEquals( "abc%E5%E4%F6",
                      m_engine.encodeName(name) );
    }

    @Test
    public void testEncodeNameUTF8()
        throws Exception
    {
        String name = "\u0041\u2262\u0391\u002E";

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );

        WikiEngine engine = new TestEngine( props );

        Assert.assertEquals( "A%E2%89%A2%CE%91.",
                      engine.encodeName(name) );
    }

    @Test
    public void testReadLinks()
        throws Exception
    {
        String src="Foobar. [Foobar].  Frobozz.  [This is a link].";

        Object[] result = m_engine.scanWikiLinks( new WikiPage(m_engine, "Test"), src ).toArray();

        Assert.assertEquals( "item 0", "Foobar", result[0] );
        Assert.assertEquals( "item 1", "This is a link", result[1] );
    }

    @Test
    public void testBeautifyTitle()
    {
        String src = "WikiNameThingy";

        Assert.assertEquals("Wiki Name Thingy", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    @Test
    public void testBeautifyTitleAcronym()
    {
        String src = "JSPWikiPage";

        Assert.assertEquals("JSP Wiki Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    @Test
    public void testBeautifyTitleAcronym2()
    {
        String src = "DELETEME";

        Assert.assertEquals("DELETEME", m_engine.beautifyTitle( src ) );
    }

    @Test
    public void testBeautifyTitleAcronym3()
    {
        String src = "JSPWikiFAQ";

        Assert.assertEquals("JSP Wiki FAQ", m_engine.beautifyTitle( src ) );
    }

    @Test
    public void testBeautifyTitleNumbers()
    {
        String src = "TestPage12";

        Assert.assertEquals("Test Page 12", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too.
     */
    @Test
    public void testBeautifyTitleArticle()
    {
        String src = "ThisIsAPage";

        Assert.assertEquals("This Is A Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too, pathological case...
     */
    /*
    @Test
    public void testBeautifyTitleArticle2()
    {
        String src = "ThisIsAJSPWikiPage";

        Assert.assertEquals("This Is A JSP Wiki Page", m_engine.beautifyTitle( src ) );
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

        Assert.assertEquals( "wrong page", "test", vsp.m_latestReq );
        Assert.assertEquals( "wrong version", -1, vsp.m_latestVers );
        Assert.assertNotNull("null", p);
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

        Assert.assertEquals( "wrong page", "test", vsp.m_latestReq );
        Assert.assertEquals( "wrong version", -1, vsp.m_latestVers );
        Assert.assertNotNull("null", p);
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

        Assert.assertEquals( "wrong page", "test", vsp.m_latestReq );
        Assert.assertEquals( "wrong version", 5, vsp.m_latestVers );
        Assert.assertNotNull("null", p);
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

        Assert.assertEquals( "wrong page", VerySimpleProvider.PAGENAME, vsp.m_latestReq );
        Assert.assertEquals( "wrong version", -1, vsp.m_latestVers );
        Assert.assertNotNull("null", p);
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
            Collection c = refMgr.findUncreated();
            Assert.assertTrue("attachment exists: "+c,
                       c==null || c.size()==0 );

            c = refMgr.findUnreferenced();
            Assert.assertEquals( "unreferenced count", 2, c.size() );
            Iterator< String > i = c.iterator();
            String first = i.next();
            String second = i.next();
            Assert.assertTrue( "unreferenced",
                        (first.equals( NAME1 ) && second.equals( NAME1+"/TestAtt.txt"))
                        || (first.equals( NAME1+"/TestAtt.txt" ) && second.equals( NAME1 )) );
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

        Collection c = refMgr.findReferrers( "TestAtt.txt" );
        Assert.assertTrue( "normal, unexisting page",
                    c!=null && ((String)c.iterator().next()).equals( NAME1 ) );

        c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
        Assert.assertTrue( "no attachment", c==null || c.size()==0 );

        c = refMgr.findUncreated();
        Assert.assertTrue( "unknown attachment",
                    c!=null &&
                    c.size()==1 &&
                    ((String)c.iterator().next()).equals( "TestAtt.txt" ) );

        // now we create the attachment

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );
        try
        {
            // and check post-conditions
            c = refMgr.findUncreated();
            Assert.assertTrue( "attachment exists: ",
                        c==null || c.size()==0 );

            c = refMgr.findReferrers( "TestAtt.txt" );
            Assert.assertTrue( "no normal page", c==null || c.size()==0 );

            c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
            Assert.assertTrue( "attachment exists now", c!=null && ((String)c.iterator().next()).equals( NAME1 ) );

            c = refMgr.findUnreferenced();
            Assert.assertTrue( "unreferenced",
                        c.size()==1 && ((String)c.iterator().next()).equals( NAME1 ));
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
            Collection c = refMgr.findUncreated();
            Assert.assertTrue( "attachment exists",
                        c==null || c.size()==0 );

            c = refMgr.findUnreferenced();
            Assert.assertEquals( "unreferenced count", c.size(), 1 );
            Assert.assertTrue( "unreferenced",
                        ((String)c.iterator().next()).equals( NAME1 ) );
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
            Collection c = refMgr.findUncreated();
            Assert.assertTrue( "attachment exists",
                        c==null || c.size()==0 );

            c = refMgr.findUnreferenced();
            Assert.assertEquals( "unreferenced count", c.size(), 1 );
            Assert.assertTrue( "unreferenced",
                        ((String)c.iterator().next()).equals( NAME1 ) );
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

        Assert.assertTrue( "Didn't create it!", saved.exists() );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        Assert.assertFalse( "Page has not been removed!", saved.exists() );
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

        Assert.assertTrue( "Didn't create it!", saved.exists() );

        Assert.assertTrue( "Attachment dir does not exist", attfile.exists() );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        Assert.assertFalse( "Page has not been removed!", saved.exists() );
        Assert.assertFalse( "Attachment has not been removed", attfile.exists() );
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

        Assert.assertTrue( "Didn't create it!", saved.exists() );

        Assert.assertTrue( "Attachment dir does not exist", attfile.exists() );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        Assert.assertNotNull( "page", page );

        att = m_engine.getAttachmentManager().getAttachmentInfo(NAME1+"/TestAtt.txt");

        m_engine.deletePage(att.getName());

        m_engine.deletePage( NAME1 );

        Assert.assertNull( "Page not removed", m_engine.getPage(NAME1) );
        Assert.assertNull( "Att not removed", m_engine.getPage(NAME1+"/TestAtt.txt") );

        Collection refs = m_engine.getReferenceManager().findReferrers(NAME1);

        Assert.assertNull( "referrers", refs );
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

        Assert.assertNull( "got page", engine.getPage( NAME1, 3 ) );

        String content = engine.getText( NAME1, WikiProvider.LATEST_VERSION );

        Assert.assertEquals( "content", "Test2", content.trim() );
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

        Assert.assertNull( "got page", engine.getPage( NAME1, 1 ) );

        String content = engine.getText( NAME1, WikiProvider.LATEST_VERSION );

        Assert.assertEquals( "content", "Test3", content.trim() );

        Assert.assertEquals( "content1", "", engine.getText(NAME1, 1).trim() );
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

        Assert.assertEquals( "V1", "bar", v1.getAttribute("foo") );

        // FIXME: The following must run as well
        Assert.assertEquals( "V2", "notbar", v2.getAttribute("foo") );

        engine.deletePage( NAME1 );
    }

    @Test
    public void testSpacedNames1()
        throws Exception
    {
        m_engine.saveText("This is a test", "puppaa");

        Assert.assertEquals( "normal", "puppaa", m_engine.getText("This is a test").trim() );
    }


    @Test
    public void testParsedVariables() throws Exception
    {
        m_engine.saveText( "TestPage", "[{SET foo=bar}][{SamplePlugin text='{$foo}'}]");

        String res = m_engine.getHTML( "TestPage" );

        Assert.assertEquals( "bar\n", res );
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

        Collection pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );
        Assert.assertEquals( "has one", "OldNameTestPage", pages.iterator().next() );

        WikiContext ctx = new WikiContext( m_engine, m_engine.getPage("OldNameTestPage") );

        m_engine.renamePage( ctx, "OldNameTestPage", "NewNameTestPage", true );

        Assert.assertFalse( "did not vanish", m_engine.pageExists( "OldNameTestPage") );
        Assert.assertTrue( "did not appear", m_engine.pageExists( "NewNameTestPage") );

        pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );

        Assert.assertEquals( "wrong # of referrers", 1, pages.size() );

        Assert.assertEquals( "has wrong referrer", "NewNameTestPage", pages.iterator().next() );
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

        Assert.assertEquals( null, p3.getAttribute(WikiPage.CHANGENOTE) );
    }

    @Test
    public void testCreatePage() throws Exception
    {
        String text = "Foobar.\r\n";
        String name = "mrmyxpltz";

        Assert.assertEquals( "page should not exist right now",
                      false,
                      m_engine.pageExists( name ) );

        m_engine.saveText( name, text );

        Assert.assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );
    }

    @Test
    public void testCreateEmptyPage() throws Exception
    {
        String text = "";
        String name = "mrmxyzptlk";

        Assert.assertEquals( "page should not exist right now",
                      false,
                      m_engine.pageExists( name ) );

        m_engine.saveText( name, text );

        Assert.assertEquals( "page should not exist right now neither",
                      false,
                      m_engine.pageExists( name ) );
    }

    @Test
    public void testSaveExistingPageWithEmptyContent() throws Exception
    {
        String text = "Foobar.\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        Assert.assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        // saveText uses normalizePostData to assure it conforms to certain rules
        Assert.assertEquals( "wrong content",
                      TextUtil.normalizePostData( text ),
                      m_engine.getText( name ) );

        m_engine.saveText( name, "" );

        Assert.assertEquals( "wrong content",
                      TextUtil.normalizePostData( "" ),
                      m_engine.getText( name ) );

    }

}
