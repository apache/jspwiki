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

import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.engine.RenderApi;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.cache.CachingManager;
import org.apache.wiki.content.PageRenamer;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public class WikiEngineTest {

    public static final String NAME1 = "Test1";

    Properties props = TestEngine.getTestProperties();
    TestEngine m_engine;

    @BeforeEach
    public void setUp() {
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        m_engine = TestEngine.build( props );
    }

    @AfterEach
    public void tearDown() {
        final String files = m_engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );

        if( files != null ) {
            final File f = new File( files );
            TestEngine.deleteAll( f );
        }

        TestEngine.emptyWorkDir();
        m_engine.shutdown();
    }

    @Test
    public void testNonExistentDirectory() throws Exception {
        final String newdir = "." + File.separator + "target" + File.separator + "non-existent-directory";

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, newdir );
        m_engine = new TestEngine( props );

        final File f = new File( m_engine.getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR ) );
        Assertions.assertTrue( f.exists(), "didn't create it" );
        Assertions.assertTrue( f.isDirectory(), "isn't a dir" );

        f.delete();
    }

    @Test
    public void testFinalPageName() throws Exception {
        m_engine.saveText( "Foobar", "1" );
        m_engine.saveText( "Foobars", "2" );

        Assertions.assertEquals( "Foobars",m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testFinalPageNameSingular() throws Exception {
        m_engine.saveText( "Foobar", "1" );

        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobar", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testFinalPageNamePlural() throws Exception {
        m_engine.saveText( "Foobars", "1" );

        Assertions.assertEquals( "Foobars", m_engine.getFinalPageName( "Foobars" ), "plural mistake" );
        Assertions.assertEquals( "Foobars", m_engine.getFinalPageName( "Foobar" ), "singular mistake" );
    }

    @Test
    public void testEncodeNameLatin1() {
        final String name = "abc\u00e5\u00e4\u00f6";
        Assertions.assertEquals( "abc%E5%E4%F6", m_engine.encodeName(name) );
    }

    @Test
    public void testEncodeNameUTF8() throws Exception {
        final String name = "\u0041\u2262\u0391\u002E";
        props.setProperty( WikiEngine.PROP_ENCODING, StandardCharsets.UTF_8.name() );
        final WikiEngine engine = new TestEngine( props );

        Assertions.assertEquals( "A%E2%89%A2%CE%91.", engine.encodeName(name) );
    }

    /**
     *  Checks, if ReferenceManager is informed of new attachments.
     */
    @Test
    public void testAttachmentRefs() throws Exception {
        final ReferenceManager refMgr = m_engine.getManager( ReferenceManager.class );
        final AttachmentManager attMgr = m_engine.getManager( AttachmentManager.class );
        m_engine.saveText( NAME1, "fooBar");

        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

        // and check post-conditions
        Collection< String > c = refMgr.findUncreated();
        Assertions.assertTrue( c==null || c.size()==0, "attachment exists: " + c );

        c = refMgr.findUnreferenced();
        Assertions.assertEquals( 2, c.size(), "unreferenced count" );
        final Iterator< String > i = c.iterator();
        final String first = i.next();
        final String second = i.next();
        Assertions.assertTrue(  ( first.equals( NAME1 ) && second.equals( NAME1 + "/TestAtt.txt" ) )
                             || ( first.equals( NAME1 + "/TestAtt.txt" ) && second.equals( NAME1 ) ), "unreferenced" );
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
    public void testAttachmentRefs2() throws Exception {
        final ReferenceManager refMgr = m_engine.getManager( ReferenceManager.class );
        final AttachmentManager attMgr = m_engine.getManager( AttachmentManager.class );

        m_engine.saveText( NAME1, "[TestAtt.txt]");

        // check a few pre-conditions

        Collection< String > c = refMgr.findReferrers( "TestAtt.txt" );
        Assertions.assertTrue( c!=null && c.iterator().next().equals( NAME1 ), "normal, unexisting page" );

        c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
        Assertions.assertTrue( c==null || c.size()==0, "no attachment" );

        c = refMgr.findUncreated();
        Assertions.assertTrue( c!=null && c.size()==1 && c.iterator().next().equals( "TestAtt.txt" ), "unknown attachment" );

        // now we create the attachment

        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

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

    /**
     *  Checks, if ReferenceManager is informed if a link to an attachment is added.
     */
    @Test
    public void testAttachmentRefs3() throws Exception {
        final ReferenceManager refMgr = m_engine.getManager( ReferenceManager.class );
        final AttachmentManager attMgr = m_engine.getManager( AttachmentManager.class );

        m_engine.saveText( NAME1, "fooBar");

        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );

        m_engine.saveText( NAME1, " ["+NAME1+"/TestAtt.txt] ");

        // and check post-conditions
        Collection< String > c = refMgr.findUncreated();
        Assertions.assertTrue( c==null || c.size()==0, "attachment exists" );

        c = refMgr.findUnreferenced();
        Assertions.assertEquals( c.size(), 1, "unreferenced count" );
        Assertions.assertEquals( NAME1, c.iterator().next(), "unreferenced" );
    }

    /**
     *  Checks, if ReferenceManager is informed if a third page references an attachment.
     */
    @Test
    public void testAttachmentRefs4() throws Exception {
        final ReferenceManager refMgr = m_engine.getManager( ReferenceManager.class );
        final AttachmentManager attMgr = m_engine.getManager( AttachmentManager.class );

        m_engine.saveText( NAME1, "[TestPage2]");

        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        attMgr.storeAttachment( att, m_engine.makeAttachmentFile() );
        m_engine.saveText( "TestPage2", "["+NAME1+"/TestAtt.txt]");

        // and check post-conditions
        Collection< String > c = refMgr.findUncreated();
        Assertions.assertTrue( c==null || c.size()==0, "attachment exists" );

        c = refMgr.findUnreferenced();
        Assertions.assertEquals( c.size(), 1, "unreferenced count" );
        Assertions.assertEquals( NAME1, c.iterator().next(), "unreferenced" );
    }

    /**
     *  Tests BugReadingOfVariableNotWorkingForOlderVersions
     */
    @Test
    public void testOldVersionVars() throws Exception {
        final Properties props = TestEngine.getTestProperties("/jspwiki-vers-custom.properties");
        props.setProperty( CachingManager.PROP_CACHE_ENABLE, "true" );
        final TestEngine engine = new TestEngine( props );
        engine.saveText( NAME1, "[{SET foo=bar}]" );
        engine.saveText( NAME1, "[{SET foo=notbar}]");

        final Page v1 = engine.getManager( PageManager.class ).getPage( NAME1, 1 );
        final Page v2 = engine.getManager( PageManager.class ).getPage( NAME1, 2 );

        Assertions.assertEquals( "bar", v1.getAttribute("foo"), "V1" );
        Assertions.assertEquals( "notbar", v2.getAttribute("foo"), "V2" );

        engine.getManager( PageManager.class ).deletePage( NAME1 );
    }

    @Test
    public void testSpacedNames1() throws Exception {
        m_engine.saveText("This is a test", "puppaa");
        Assertions.assertEquals( "puppaa", m_engine.getManager( PageManager.class ).getText("This is a test").trim(), "normal" );
    }

    @Test
    public void testParsedVariables() throws Exception {
        m_engine.saveText( "TestPage", "[{SET foo=bar}][{SamplePlugin text='{$foo}'}]");
        final String res = m_engine.getManager( RenderingManager.class ).getHTML( "TestPage" );

        Assertions.assertEquals( "bar\n", res );
    }

    /**
     * Tests BugReferenceToRenamedPageNotCleared
     */
    @Test
    public void testRename() throws Exception {
        m_engine.saveText( "RenameBugTestPage", "Mary had a little generic object" );
        m_engine.saveText( "OldNameTestPage", "Linked to RenameBugTestPage" );

        Collection< String > pages = m_engine.getManager( ReferenceManager.class ).findReferrers( "RenameBugTestPage" );
        Assertions.assertEquals( "OldNameTestPage", pages.iterator().next(), "has one" );

        final WikiContext ctx = new WikiContext( m_engine, m_engine.getManager( PageManager.class ).getPage("OldNameTestPage") );
        m_engine.getManager( PageRenamer.class ).renamePage( ctx, "OldNameTestPage", "NewNameTestPage", true );

        Assertions.assertFalse( m_engine.getManager( PageManager.class ).wikiPageExists( "OldNameTestPage"), "did not vanish" );
        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( "NewNameTestPage"), "did not appear" );

        pages = m_engine.getManager( ReferenceManager.class ).findReferrers( "RenameBugTestPage" );
        Assertions.assertEquals( 1, pages.size(),  "wrong # of referrers" );
        Assertions.assertEquals( "NewNameTestPage", pages.iterator().next(), "has wrong referrer" );
    }

    @Test
    public void testChangeNoteOldVersion2() throws Exception {
        final Page p = Wiki.contents().page( m_engine, NAME1 );
        final Context context = Wiki.context().create( m_engine,p );
        context.getPage().setAttribute( Page.CHANGENOTE, "Test change" );
        m_engine.getManager( PageManager.class ).saveText( context, "test" );

        for( int i = 0; i < 5; i++ ) {
            final Page p2 = m_engine.getManager( PageManager.class ).getPage( NAME1 ).clone();
            p2.removeAttribute( Page.CHANGENOTE );
            context.setPage( p2 );
            m_engine.getManager( PageManager.class ).saveText( context, "test" + i );
        }

        final Page p3 = m_engine.getManager( PageManager.class ).getPage( NAME1, -1 );
        Assertions.assertNull( p3.getAttribute( Page.CHANGENOTE ) );
    }

    @Test
    public void testGetManagers() {
        Assertions.assertNull( m_engine.getManager( String.class ) );
        Assertions.assertNotNull( m_engine.getManager( RenderApi.class ) );
        Assertions.assertNotNull( m_engine.getManager( PageManager.class ) );
        Assertions.assertNotNull( m_engine.getManager( ModuleManager.class ) );

        Assertions.assertEquals( 0, m_engine.getManagers( String.class ).size() );
        Assertions.assertEquals( 1, m_engine.getManagers( RenderApi.class ).size() );
        Assertions.assertEquals( 1, m_engine.getManagers( PageManager.class ).size() );
        Assertions.assertEquals( 4, m_engine.getManagers( ModuleManager.class ).size() );
    }

}
