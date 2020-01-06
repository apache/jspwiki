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
package org.apache.wiki.content;
import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Properties;

public class PageRenamerTest
{
    TestEngine m_engine;

    @BeforeEach
    public void setUp() throws Exception {
        Properties props = TestEngine.getTestProperties();
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        CacheManager.getInstance().removeAllCaches();
        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);
    }

    @AfterEach
    public void tearDown() {
        m_engine.deleteTestPage("TestPage");
        m_engine.deleteTestPage("TestPage2");
        m_engine.deleteTestPage("FooTest");
        m_engine.deleteTestPage("Test");
        m_engine.deleteTestPage("CdauthNew");
        m_engine.deleteTestPage("Cdauth");
        m_engine.deleteTestPage("TestPageReferring");
        m_engine.deleteTestPage("TestPageReferredNew");
        m_engine.deleteTestPage("Main");
        m_engine.deleteTestPage("Main8887");
        m_engine.deleteTestPage("TestPage1234");
        m_engine.deleteTestPage("TestPageReferred");
        m_engine.deleteTestPage("RenameTest");
        m_engine.deleteTestPage("Link one");
        m_engine.deleteTestPage("Link uno");
        m_engine.deleteTestPage("Link two");

        TestEngine.emptyWorkDir();
    }

    @Test
    public void testSimpleRename() throws Exception {
        // Count the number of existing references
        int refCount = m_engine.getReferenceManager().findCreated().size();

        m_engine.saveText("TestPage", "the big lazy dog thing" );

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", false);

        WikiPage newpage = m_engine.getPageManager().getPage("FooTest");

        Assertions.assertNotNull( newpage, "no new page" );
        Assertions.assertNull( m_engine.getPageManager().getPage("TestPage"), "old page not gone" );

        // Refmgr
        Collection< String > refs = m_engine.getReferenceManager().findCreated();

        Assertions.assertTrue( refs.contains("FooTest"), "FooTest does not exist" );
        Assertions.assertFalse( refs.contains("TestPage"), "TestPage exists" );
        Assertions.assertEquals( refCount+1, refs.size(), "wrong list size" );
    }

    @Test
    public void testReferrerChange()
       throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage]");

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION);

        Assertions.assertEquals( "[FooTest]", data.trim(), "no rename" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers("TestPage");

        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPage2", refs.iterator().next(), "wrong ref" );
    }

    @Test
    public void testReferrerChangeCC()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "TestPage");

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION);

        Assertions.assertEquals( "FooTest", data.trim(), "no rename" );
        Collection< String > refs = m_engine.getReferenceManager().findReferrers("TestPage");

        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPage2", refs.iterator().next(), "wrong ref" );
    }

    @Test
    public void testReferrerChangeAnchor()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage#heading1]");

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION);

        Assertions.assertEquals( "[FooTest#heading1]", data.trim(), "no rename" );
        Collection< String > refs = m_engine.getReferenceManager().findReferrers("TestPage");

        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPage2", refs.iterator().next(), "wrong ref" );
    }

    @Test
    public void testReferrerChangeMultilink()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage] [TestPage] [linktext|TestPage] TestPage [linktext|TestPage] [TestPage#Anchor] [TestPage] TestPage [TestPage]");

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION);

        Assertions.assertEquals( "[FooTest] [FooTest] [linktext|FooTest] FooTest [linktext|FooTest] [FooTest#Anchor] [FooTest] FooTest [FooTest]",
                                 data.trim(), 
                                 "no rename" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers("TestPage");

        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPage2", refs.iterator().next(), "wrong ref" );
    }

    @Test
    public void testReferrerNoWikiName()
        throws Exception
    {
        m_engine.saveText("Test","foo");
        m_engine.saveText("TestPage2", "[Test] [Test#anchor] test Test [test] [link|test] [link|test]");

        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "Test", "TestPage", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION );

        Assertions.assertEquals( "[TestPage] [TestPage#anchor] test Test [TestPage] [link|TestPage] [link|TestPage]", data.trim(), "wrong data" );
    }

    @Test
    public void testAttachmentChange()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage/foo.txt] [linktext|TestPage/bar.jpg]");

        m_engine.addAttachment("TestPage", "foo.txt", "testing".getBytes() );
        m_engine.addAttachment("TestPage", "bar.jpg", "pr0n".getBytes() );
        WikiPage p = m_engine.getPageManager().getPage("TestPage");

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, "TestPage", "FooTest", true);

        String data = m_engine.getPageManager().getPureText("TestPage2", WikiProvider.LATEST_VERSION);

        Assertions.assertEquals( "[FooTest/foo.txt] [linktext|FooTest/bar.jpg]", data.trim(), "no rename" );

        Attachment att = m_engine.getAttachmentManager().getAttachmentInfo("FooTest/foo.txt");
        Assertions.assertNotNull( att, "footext" );

        att = m_engine.getAttachmentManager().getAttachmentInfo("FooTest/bar.jpg");
        Assertions.assertNotNull( att, "barjpg" );

        att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/bar.jpg");
        Assertions.assertNull( att, "testpage/bar.jpg exists" );

        att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/foo.txt");
        Assertions.assertNull( att, "testpage/foo.txt exists" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers("TestPage/bar.jpg");

        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "FooTest/bar.jpg" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPage2", refs.iterator().next(), "wrong ref" );
    }

    @Test
    public void testSamePage() throws Exception
    {
        m_engine.saveText( "TestPage", "[TestPage]");

        rename( "TestPage", "FooTest" );

        WikiPage p = m_engine.getPageManager().getPage( "FooTest" );

        Assertions.assertNotNull( p, "no page" );

        Assertions.assertEquals("[FooTest]", m_engine.getPageManager().getText("FooTest").trim() );
    }

    @Test
    public void testBrokenLink1() throws Exception
    {
        m_engine.saveText( "TestPage", "hubbub");
        m_engine.saveText( "TestPage2", "[TestPage|]" );

        rename( "TestPage", "FooTest" );

        WikiPage p = m_engine.getPageManager().getPage( "FooTest" );

        Assertions.assertNotNull( p, "no page" );

        // Should be no change
        Assertions.assertEquals("[TestPage|]", m_engine.getPageManager().getText("TestPage2").trim() );
    }

    @Test
    public void testBrokenLink2() throws Exception
    {
        m_engine.saveText( "TestPage", "hubbub");
        m_engine.saveText( "TestPage2", "[|TestPage]" );

        WikiPage p;
        rename( "TestPage", "FooTest" );

        p = m_engine.getPageManager().getPage( "FooTest" );

        Assertions.assertNotNull( p, "no page" );

        Assertions.assertEquals("[|FooTest]", m_engine.getPageManager().getText("TestPage2").trim() );
    }

    private void rename( String src, String dst ) throws WikiException
    {
        WikiPage p = m_engine.getPageManager().getPage(src);

        WikiContext context = new WikiContext(m_engine, p);

        m_engine.getPageRenamer().renamePage(context, src, dst, true);
    }

    @Test
    public void testBug25() throws Exception
    {
        String src = "[Cdauth/attach.txt] [link|Cdauth/attach.txt] [cdauth|Cdauth/attach.txt]"+
                     "[CDauth/attach.txt] [link|CDauth/attach.txt] [cdauth|CDauth/attach.txt]"+
                     "[cdauth/attach.txt] [link|cdauth/attach.txt] [cdauth|cdauth/attach.txt]";

        String dst = "[CdauthNew/attach.txt] [link|CdauthNew/attach.txt] [cdauth|CdauthNew/attach.txt]"+
                     "[CDauth/attach.txt] [link|CDauth/attach.txt] [cdauth|CDauth/attach.txt]"+
                     "[CdauthNew/attach.txt] [link|CdauthNew/attach.txt] [cdauth|CdauthNew/attach.txt]";

        m_engine.saveText( "Cdauth", "xxx" );
        m_engine.saveText( "TestPage", src );

        m_engine.addAttachment( "Cdauth", "attach.txt", "Puppua".getBytes() );

        rename( "Cdauth", "CdauthNew" );

        Assertions.assertEquals( dst, m_engine.getPageManager().getText("TestPage").trim() );
    }

    @Test
    public void testBug21() throws Exception
    {
        String src = "[Link to TestPage2|TestPage2]";

        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );

        rename ("TestPage2", "Test");

        Assertions.assertEquals( "[Link to Test|Test]", m_engine.getPageManager().getText( "TestPage" ).trim() );
    }

    @Test
    public void testExtendedLinks() throws Exception
    {
        String src = "[Link to TestPage2|TestPage2|target='_new']";

        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );

        rename ("TestPage2", "Test");

        Assertions.assertEquals( "[Link to Test|Test|target='_new']", m_engine.getPageManager().getText( "TestPage" ).trim() );
    }

    @Test
    public void testBug85_case1() throws Exception
    {
        // renaming a non-existing page
        // This Assertions.fails under 2.5.116, cfr. with http://bugs.jspwiki.org/show_bug.cgi?id=85
        // m_engine.saveText( "TestPage", "blablahblahbla" );
        try
        {
            rename("TestPage123", "Main8887");
            rename("Main8887", "TestPage123");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            Assertions.fail( npe );
        }
        catch( WikiException e )
        {
            // Expected
        }
    }

    @Test
    public void testBug85_case2() throws Exception
    {
        try
        {
            // renaming a non-existing page, but we call m_engine.saveText() before renaming
            // this does not Assertions.fail under 2.5.116
            m_engine.saveText( "TestPage1234", "blablahblahbla" );
            rename("TestPage1234", "Main8887");
            rename("Main8887", "TestPage1234");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            Assertions.fail( npe );
        }
    }

    @Test
    public void testBug85_case3() throws Exception
    {
        try
        {
            // renaming an existing page
            // this does not Assertions.fail under 2.5.116
            // m_engine.saveText( "Main", "blablahblahbla" );
            rename("Main", "Main8887");
            rename("Main8887", "Main");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            Assertions.fail( npe );
        }
        catch( WikiException e )
        {
            // Expected
        }
    }

    @Test
    public void testBug85_case4() throws Exception
    {
        try
        {
            // renaming an existing page, and we call m_engine.saveText() before renaming
            // this does not Assertions.fail under 2.5.116
            m_engine.saveText( "Main", "blablahblahbla" );
            rename("Main", "Main8887");
            rename("Main8887", "Main");
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
            System.out.println("NPE: Bug 85 caught?");
            Assertions.fail( npe );
        }
    }

    @Test
    public void testRenameOfEscapedLinks() throws Exception
    {
        String src = "[[Link to TestPage2|TestPage2|target='_new']";

        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );

        rename ("TestPage2", "Test");

        Assertions.assertEquals( "[[Link to TestPage2|TestPage2|target='_new']", m_engine.getPageManager().getText( "TestPage" ).trim() );
    }

    @Test
    public void testRenameOfEscapedLinks2() throws Exception
    {
        String src = "~[Link to TestPage2|TestPage2|target='_new']";

        m_engine.saveText( "TestPage", src );
        m_engine.saveText( "TestPage2", "foo" );

        rename ("TestPage2", "Test");

        Assertions.assertEquals( "~[Link to TestPage2|TestPage2|target='_new']", m_engine.getPageManager().getText( "TestPage" ).trim() );
    }

    /**
     * Test for a referrer containing blanks
     *
     * @throws Exception
     */
    @Test
    public void testReferrerChangeWithBlanks() throws Exception
    {
        m_engine.saveText( "TestPageReferred", "bla bla bla som content" );
        m_engine.saveText( "TestPageReferring", "[Test Page Referred]" );

        rename( "TestPageReferred", "TestPageReferredNew" );

        String data = m_engine.getPageManager().getPureText( "TestPageReferring", WikiProvider.LATEST_VERSION );
        Assertions.assertEquals( "[Test Page Referred|TestPageReferredNew]", data.trim(), "page not renamed" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers( "TestPageReferred" );
        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "TestPageReferredNew" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "TestPageReferring", refs.iterator().next(), "wrong ref" );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-398 */
    @Test
    public void testReferrerChangeWithBlanks2() throws Exception
    {
        m_engine.saveText( "RenameTest", "[link one] [link two]" );
        m_engine.saveText( "Link one", "Leonard" );
        m_engine.saveText( "Link two", "Cohen" );

        rename( "Link one", "Link uno" );

        String data = m_engine.getPageManager().getPureText( "RenameTest", WikiProvider.LATEST_VERSION );
        Assertions.assertEquals( "[link one|Link uno] [link two]", data.trim(), "page not renamed" );

        Collection< String > refs = m_engine.getReferenceManager().findReferrers( "Link one" );
        Assertions.assertNull( refs, "oldpage" );

        refs = m_engine.getReferenceManager().findReferrers( "Link uno" );
        Assertions.assertEquals( 1, refs.size(), "new size" );
        Assertions.assertEquals( "RenameTest", refs.iterator().next() , "wrong ref");
    }

}
