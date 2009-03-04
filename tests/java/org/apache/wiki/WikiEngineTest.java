/*
    JSPWiki - a JSP-based WikiWiki clone.

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

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.wiki.*;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.*;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.providers.*;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;


public class WikiEngineTest extends TestCase
{
    public static final String NAME1 = "Test1";
    public static final long PAGEPROVIDER_RESCAN_PERIOD = 2;

    Properties props = new Properties();

    TestEngine m_engine;


    public WikiEngineTest( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        return new TestSuite( WikiEngineTest.class );
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.main(new String[] { WikiEngineTest.class.getName() } );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );

        TestEngine.emptyWorkDir();
        
        m_engine = new TestEngine(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        if( files != null )
        {
            File f = new File( files );

            TestEngine.deleteAll( f );
        }

        TestEngine.emptyWorkDir();
        
        m_engine.shutdown();
    }
    
    public void testNonExistantDirectory()
        throws Exception
    {
        String tmpdir = System.getProperties().getProperty("java.io.tmpdir");
        String dirname = "non-existant-directory";

        String newdir = tmpdir + File.separator + dirname;

        props.setProperty( AbstractFileProvider.PROP_PAGEDIR, 
                           newdir );

        m_engine.shutdown();
        m_engine = new TestEngine( props );

        File f = new File( newdir );

        assertTrue( "didn't create it", f.exists() );
        assertTrue( "isn't a dir", f.isDirectory() );

        f.delete();
        
    }

    public void testNonExistantDirProperty() throws Exception
    {
        props.remove( AbstractFileProvider.PROP_PAGEDIR );
        try
        {
            m_engine.shutdown();
            m_engine = new TestEngine( props );

            fail( "Wiki did not warn about missing property." );
        }
        catch( WikiException e )
        {
            // This is okay.
        }
    }

    /**
     *  Check that calling pageExists( String ) works.
     */
    public void testNonExistantPage()
        throws Exception
    {
        String pagename = "Test1";

        assertEquals( "Page already exists",
                      false,
                      m_engine.pageExists( pagename ) );
    }

    /**
     *  Check that calling pageExists( WikiPage ) works.
     */
    public void testNonExistantPage2()
        throws Exception
    {
        WikiPage page = m_engine.createPage( WikiName.valueOf( "Test1" ) );

        assertEquals( "Page already exists",
                      false,
                      m_engine.pageExists( page ) );
    }

    public void testFinalPageName()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );
        m_engine.saveText( "Foobars", "2" );

        assertEquals( "plural mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobars" ) );

        assertEquals( "singular mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobar" ) );
    }

    public void testFinalPageNameSingular()
        throws Exception
    {
        m_engine.saveText( "Foobar", "1" );

        assertEquals( "plural mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobars" ) );
        assertEquals( "singular mistake", "Foobar",
                      m_engine.getFinalPageName( "Foobar" ) );
    }

    public void testFinalPageNamePlural()
        throws Exception
    {
        m_engine.saveText( "Foobars", "1" );

        assertEquals( "plural mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobars" ) );
        assertEquals( "singular mistake", "Foobars",
                      m_engine.getFinalPageName( "Foobar" ) );
    }
    
    public void testPutPage()
        throws Exception
    {
        String text = "Foobar.\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        assertEquals( "wrong content",
                      text,
                      m_engine.getText( name ) );
    }

    public void testPutPageEntities()
        throws Exception
    {
        String text = "Foobar. &quot;\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        assertEquals( "wrong content",
                      "Foobar. &amp;quot;\r\n",
                      m_engine.getText( name ) );
    }

    /**
     *  Cgeck that basic " is changed.
     */
    public void testPutPageEntities2()
        throws Exception
    {
        String text = "Foobar. \"\r\n";
        String name = NAME1;

        m_engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );

        assertEquals( "wrong content",
                      "Foobar. &quot;\r\n",
                      m_engine.getText( name ) );
    }

    public void testGetHTML()
        throws Exception
    {
        String text = "''Foobar.''";
        String name = NAME1;

        m_engine.saveText( name, text );

        String data = m_engine.getHTML( name );

        assertEquals( "<i>Foobar.</i>\n",
                       data );
    }

    public void testEncodeNameLatin1()
    {
        String name = "abc\u00e5\u00e4\u00f6";

        assertEquals( "abc%E5%E4%F6",
                      m_engine.encodeName(name) );
    }

    public void testEncodeNameUTF8() throws Exception
    {
        String name = "\u0041\u2262\u0391\u002E";

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );

        m_engine.shutdown();
        m_engine = new TestEngine( props );

        assertEquals( "A%E2%89%A2%CE%91.", m_engine.encodeName( name ) );
    }

    public void testReadLinks()
        throws Exception
    {
        String src="Foobar. [Foobar].  Frobozz.  [This is a link].";

        Object[] result = m_engine.scanWikiLinks( m_engine.createPage( WikiName.valueOf( "Test" ) ), src ).toArray();
        
        assertEquals( "item 0", "Foobar", result[0] );
        assertEquals( "item 1", "This is a link", result[1] );
    }

    public void testBeautifyTitle()
    {
        String src = "WikiNameThingy";

        assertEquals("Wiki Name Thingy", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    public void testBeautifyTitleAcronym()
    {
        String src = "JSPWikiPage";

        assertEquals("JSP Wiki Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    public void testBeautifyTitleAcronym2()
    {
        String src = "DELETEME";

        assertEquals("DELETEME", m_engine.beautifyTitle( src ) );
    }

    public void testBeautifyTitleAcronym3()
    {
        String src = "JSPWikiFAQ";

        assertEquals("JSP Wiki FAQ", m_engine.beautifyTitle( src ) );
    }

    public void testBeautifyTitleNumbers()
    {
        String src = "TestPage12";

        assertEquals("Test Page 12", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too.
     */
    public void testBeautifyTitleArticle()
    {
        String src = "ThisIsAPage";

        assertEquals("This Is A Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  English articles too, pathological case...
     */
    /*
    public void testBeautifyTitleArticle2()
    {
        String src = "ThisIsAJSPWikiPage";

        assertEquals("This Is A JSP Wiki Page", m_engine.beautifyTitle( src ) );
    }
    */

    /*
    public void testLatestGet() throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        m_engine.shutdown();
        m_engine = new TestEngine( props );

        WikiPage p = m_engine.getPage( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) m_engine.getPageManager().getProvider();

        assertEquals( "wrong page", "test", vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
        assertNotNull( "null", p );
    }

    public void testLatestGet2() throws Exception
    {
            props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
            props.setProperty( "jspwiki.usePageCache", "false" );

            m_engine.shutdown();
            m_engine = new TestEngine( props );

            String p = m_engine.getText( "test", -1 );

            VerySimpleProvider vsp = (VerySimpleProvider) m_engine.getPageManager().getProvider();

            assertEquals( "wrong page", "test", vsp.m_latestReq );
            assertEquals( "wrong version", -1, vsp.m_latestVers );
            assertNotNull( "null", p );
        }

    public void testLatestGet3() throws Exception
    {
            props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
            props.setProperty( "jspwiki.usePageCache", "false" );

            m_engine.shutdown();
            m_engine = new TestEngine( props );

            String p = m_engine.getHTML( "test", -1 );

            VerySimpleProvider vsp = (VerySimpleProvider) m_engine.getPageManager().getProvider();

            assertEquals( "wrong page", "test", vsp.m_latestReq );
            assertEquals( "wrong version", 5, vsp.m_latestVers );
            assertNotNull( "null", p );
        }

    public void testLatestGet4() throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "true" );

        m_engine.shutdown();
        m_engine = new TestEngine( props );

        String p = m_engine.getHTML( VerySimpleProvider.PAGENAME, -1 );

        CachingProvider cp = (CachingProvider) m_engine.getPageManager().getProvider();
        VerySimpleProvider vsp = (VerySimpleProvider) cp.getRealProvider();

        assertEquals( "wrong page", VerySimpleProvider.PAGENAME, vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
        assertNotNull( "null", p );
    }
*/
    
    /**
     *  Checks, if ReferenceManager is informed of new attachments.
     */
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
            assertTrue("attachment exists: "+c,            
                       c==null || c.size()==0 );
    
            c = refMgr.findUnreferenced();
            assertEquals( "unreferenced count", 2, c.size() );
            Iterator i = c.iterator();
            String first = (String) i.next();
            String second = (String) i.next();
            assertTrue( "unreferenced",            
                        (first.equals( NAME1 ) && second.equals( NAME1+"/TestAtt.txt"))
                        || (first.equals( NAME1+"/TestAtt.txt" ) && second.equals( NAME1 )) );
        }
        finally
        { 
            // do cleanup
            String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
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

    public void testAttachmentRefs2()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();
        
        m_engine.saveText( NAME1, "[TestAtt.txt]");

        // check a few pre-conditions
        
        Collection c = refMgr.findReferrers( "TestAtt.txt" );
        assertTrue( "normal, unexisting page", 
                    c!=null && ((String)c.iterator().next()).equals( NAME1 ) );
        
        c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
        assertTrue( "no attachment", c==null || c.size()==0 );
        
        c = refMgr.findUncreated();
        assertTrue( "unknown attachment", 
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
            assertTrue( "attachment exists: ",            
                        c==null || c.size()==0 );
    
            c = refMgr.findReferrers( "TestAtt.txt" );
            assertTrue( "no normal page", c==null || c.size()==0 );
    
            c = refMgr.findReferrers( NAME1+"/TestAtt.txt" );
            assertTrue( "attachment exists now", c!=null && ((String)c.iterator().next()).equals( NAME1 ) );

            c = refMgr.findUnreferenced();
            assertTrue( "unreferenced",            
                        c.size()==1 && ((String)c.iterator().next()).equals( NAME1 ));
        }
        finally
        { 
            // do cleanup
            String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
        }
    }

    /** 
     *  Checks, if ReferenceManager is informed if a link to an attachment is added.
     */
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
            assertTrue( "attachment exists",            
                        c==null || c.size()==0 );
    
            c = refMgr.findUnreferenced();
            assertEquals( "unreferenced count", c.size(), 1 );
            assertTrue( "unreferenced",            
                        ((String)c.iterator().next()).equals( NAME1 ) );
        }
        finally
        { 
            // do cleanup
            String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
        }
    }
    
    /** 
     *  Checks, if ReferenceManager is informed if a third page references an attachment.
     */
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
            assertTrue( "attachment exists",            
                        c==null || c.size()==0 );
    
            c = refMgr.findUnreferenced();
            assertEquals( "unreferenced count", c.size(), 1 );
            assertTrue( "unreferenced",            
                        ((String)c.iterator().next()).equals( NAME1 ) );
        }
        finally
        { 
            // do cleanup
            String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
            TestEngine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
            new File( files, "TestPage2.txt" ).delete();
        }
    }    


    

    public void testDeletePage()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+".txt" );

        assertTrue( "Didn't create it!", saved.exists() );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        assertFalse( "Page has not been removed!", saved.exists() );
    }


    public void testDeletePageAndAttachments()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );
        
        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+".txt" );

        String atts = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );
        
        assertTrue( "Didn't create it!", saved.exists() );

        assertTrue( "Attachment dir does not exist", attfile.exists() );
        
        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page.getName() );

        assertFalse( "Page has not been removed!", saved.exists() );
        assertFalse( "Attachment has not been removed", attfile.exists() );
    }

    public void testDeletePageAndAttachments2()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );
        
        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+".txt" );

        String atts = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );
        File attfile = new File( atts, NAME1+"-att/TestAtt.txt-dir" );
        
        assertTrue( "Didn't create it!", saved.exists() );

        assertTrue( "Attachment dir does not exist", attfile.exists() );
        
        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        assertNotNull( "page", page );

        att = m_engine.getAttachmentManager().getAttachmentInfo(NAME1+"/TestAtt.txt");
        
        m_engine.deletePage(att.getName());
        
        m_engine.deletePage( NAME1 );
        
        assertNull( "Page not removed", m_engine.getPage(NAME1) );
        assertNull( "Att not removed", m_engine.getPage(NAME1+"/TestAtt.txt") );
        
        Collection refs = m_engine.getReferenceManager().findReferrers(NAME1);
        
        assertNull( "referrers", refs );
    }
    
    public void testDeleteVersion() throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );

        m_engine.shutdown();
        m_engine = new TestEngine( props );
        m_engine.saveText( NAME1, "Test1" );
        m_engine.saveText( NAME1, "Test2" );
        m_engine.saveText( NAME1, "Test3" );

        WikiPage page = m_engine.getPage( NAME1, 3 );

        m_engine.deleteVersion( page );

        assertNull( "got page", m_engine.getPage( NAME1, 3 ) );

        String content = m_engine.getText( NAME1, WikiProvider.LATEST_VERSION );

        assertEquals( "content", "Test2", content.trim() );
    }

    public void testDeleteVersion2() throws Exception
    {
            props.setProperty( "jspwiki.pageProvider", "VersioningFileProvider" );

            m_engine.shutdown();
            m_engine = new TestEngine( props );
            m_engine.saveText( NAME1, "Test1" );
            m_engine.saveText( NAME1, "Test2" );
            m_engine.saveText( NAME1, "Test3" );

            WikiPage page = m_engine.getPage( NAME1, 1 );

            m_engine.deleteVersion( page );

            assertNull( "got page", m_engine.getPage( NAME1, 1 ) );

            String content = m_engine.getText( NAME1, WikiProvider.LATEST_VERSION );

            assertEquals( "content", "Test3", content.trim() );

            assertEquals( "content1", "", m_engine.getText( NAME1, 1 ).trim() );
        }
    
    
    /**
     *  Assumes that CachingProvider is in use.
     */
    public void testExternalModificationRefs()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();

        m_engine.saveText( NAME1, "[Foobar]" );
        m_engine.getText( NAME1 ); // Ensure that page is cached.

        Collection c = refMgr.findUncreated();
        assertTrue( "Non-existent reference not detected by ReferenceManager",
            Util.collectionContains( c, "Foobar" ));

        Thread.sleep( 2000L ); // Wait two seconds for filesystem granularity

        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+".txt" );

        assertTrue( "No file!", saved.exists() );

        FileWriter out = new FileWriter( saved );
        FileUtil.copyContents( new StringReader("[Puppaa]"), out );
        out.close();

        Thread.sleep( 2000L*PAGEPROVIDER_RESCAN_PERIOD ); // Wait five seconds for CachingProvider to wake up.

        String text = m_engine.getText( NAME1 );

        assertEquals( "wrong contents", "[Puppaa]", text );

        c = refMgr.findUncreated();

        assertTrue( "Non-existent reference after external page change " +
                    "not detected by ReferenceManager",
                    Util.collectionContains( c, "Puppaa" ));
    }


    /**
     *  Assumes that CachingProvider is in use.
     */
    public void testExternalModificationRefsDeleted()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();

        m_engine.saveText( NAME1, "[Foobar]" );
        m_engine.getText( NAME1 ); // Ensure that page is cached.

        Collection c = refMgr.findUncreated();
        assertEquals( "uncreated count", 1, c.size() );
        assertEquals( "wrong referenced page", "Foobar", (String)c.iterator().next() );

        Thread.sleep( 2000L ); // Wait two seconds for filesystem granularity

        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+".txt" );

        assertTrue( "No file!", saved.exists() );

        saved.delete();

        assertFalse( "File not deleted!", saved.exists() );

        Thread.sleep( 2000L*PAGEPROVIDER_RESCAN_PERIOD ); // Wait five seconds for CachingProvider to catch up.

        WikiPage p = m_engine.getPage( NAME1 );

        assertNull( "Got page!", p );

        String text = m_engine.getText( NAME1 );

        assertEquals( "wrong contents", "", text );

        c = refMgr.findUncreated();
        assertEquals( "NEW: uncreated count", 0, c.size() );
    }

    /**
     *  Assumes that CachingProvider is in use.
     */
    public void testExternalModification()
        throws Exception
    {
        m_engine.saveText( NAME1, "Foobar" );

        m_engine.getText( NAME1 ); // Ensure that page is cached.

        Thread.sleep( 2000L ); // Wait two seconds for filesystem granularity

        String files = props.getProperty( AbstractFileProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+".txt" );

        assertTrue( "No file!", saved.exists() );

        FileWriter out = new FileWriter( saved );
        FileUtil.copyContents( new StringReader("Puppaa"), out );
        out.close();

        // Wait for the caching provider to notice a refresh.
        Thread.sleep( 2000L*PAGEPROVIDER_RESCAN_PERIOD );

        // Trim - engine.saveText() may append a newline.
        String text = m_engine.getText( NAME1 ).trim();
        assertEquals( "wrong contents", "Puppaa", text );
    }

    /**
     * Tests BugReadingOfVariableNotWorkingForOlderVersions
     * 
     * @throws Exception
     */
    public void testOldVersionVars() throws Exception
    {
            Properties pr = new Properties();
            pr.load( TestEngine.findTestProperties( "/jspwiki_vers.properties" ) );

            pr.setProperty( PageManager.PROP_USECACHE, "true" );

            m_engine.shutdown();
            m_engine = new TestEngine( pr );

            m_engine.saveText( NAME1, "[{SET foo=bar}]" );

            m_engine.saveText( NAME1, "[{SET foo=notbar}]" );

            WikiPage v1 = m_engine.getPage( NAME1, 1 );

            WikiPage v2 = m_engine.getPage( NAME1, 2 );

            assertEquals( "V1", "bar", v1.getAttribute( "foo" ) );

            // FIXME: The following must run as well
            assertEquals( "V2", "notbar", v2.getAttribute( "foo" ) );

            m_engine.deletePage( NAME1 );
        }
    
    public void testSpacedNames1()
        throws Exception
    {
        m_engine.saveText("This is a test", "puppaa");
        
        assertEquals( "normal", "puppaa", m_engine.getText("This is a test").trim() );
        assertEquals( "lowercase", "puppaa", m_engine.getText("this is a test").trim() );
        assertEquals( "randomcase", "puppaa", m_engine.getText("ThiS Is a teSt").trim() );
    }


    public void testParsedVariables() throws Exception
    {
        m_engine.saveText( "TestPage", "[{SET foo=bar}][{SamplePlugin text='{$foo}'}]");
        
        String res = m_engine.getHTML( "TestPage" );
        
        assertEquals( "bar\n", res );
    }
    
    /**
     * Tests BugReferenceToRenamedPageNotCleared
     * 
     * @throws Exception
     */
    public void testRename() throws Exception
    {
        m_engine.saveText( "RenameBugTestPage", "Mary had a little generic object" );
        m_engine.saveText( "OldNameTestPage", "Linked to RenameBugTestPage" );
       
        Collection pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );
        assertEquals( "has one", "OldNameTestPage", pages.iterator().next() );
        
        WikiContext ctx = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage("OldNameTestPage") );
        
        m_engine.renamePage( ctx, "OldNameTestPage", "NewNameTestPage", true );
            
        assertFalse( "did not vanish", m_engine.pageExists( "OldNameTestPage") );
        assertTrue( "did not appear", m_engine.pageExists( "NewNameTestPage") );
        
        pages = m_engine.getReferenceManager().findReferrers( "RenameBugTestPage" );
        
        assertEquals( "wrong # of referrers", 1, pages.size() );
        
        assertEquals( "has wrong referrer", "NewNameTestPage", pages.iterator().next() );        
    }
    
    public void testChangeNoteOldVersion2() throws Exception
    {
        WikiPage p = m_engine.createPage( WikiName.valueOf( NAME1 ) );
    
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( p );

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
    
        assertEquals( null, p3.getAttribute(WikiPage.CHANGENOTE) );
    }
    
    public void testCreatePage() throws Exception 
    {
        String text = "Foobar.\r\n";
        String name = "mrmyxpltz";
        
        assertEquals( "page should not exist right now",
                      false,
                      m_engine.pageExists( name ) );

        m_engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );
    }
    
    public void testCreateEmptyPage() throws Exception 
    {
        String text = "";
        String name = "mrmxyzptlk";
        
        assertEquals( "page should not exist right now",
                      false,
                      m_engine.pageExists( name ) );

        m_engine.saveText( name, text );

        assertEquals( "page should not exist right now neither",
                      false,
                      m_engine.pageExists( name ) );
    }
    
    public void testSaveExistingPageWithEmptyContent() throws Exception 
    {
        String text = "Foobar.\r\n";
        String name = NAME1;
        
        m_engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      m_engine.pageExists( name ) );
        
        // saveText uses normalizePostData to assure it conforms to certain rules
        assertEquals( "wrong content",
                      TextUtil.normalizePostData( text ),
                      m_engine.getText( name ) );
        
        m_engine.saveText( name, "" );
        
        assertEquals( "wrong content",
                      TextUtil.normalizePostData( "" ), 
                      m_engine.getText( name ) );
        
    }

}
