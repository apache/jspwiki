
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.providers.*;
import com.ecyrd.jspwiki.attachment.*;

public class WikiEngineTest extends TestCase
{
    public static final String NAME1 = "Test1";
    public static final long PAGEPROVIDER_RESCAN_PERIOD = 2000L;

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
	// We'll need a shorter-than-default consistency check for
	// the page-changed checks. This will cause additional load
	// to the file system, though.
	props.setProperty( CachingProvider.PROP_CACHECHECKINTERVAL, 
			   Long.toString(PAGEPROVIDER_RESCAN_PERIOD) );

        //
        //  We must make sure that the reference manager cache is cleaned before.
        //
        String workDir = props.getProperty( "jspwiki.workDir" );

        if( workDir != null )
        {
            File refmgrfile = new File( workDir, "refmgr.ser" );
            if( refmgrfile.exists() ) refmgrfile.delete();
        }

        m_engine = new TestEngine(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        if( files != null )
        {
            File f = new File( files );

            m_engine.deleteAll( f );
        }

    }
    
    public void testNonExistantDirectory()
        throws Exception
    {
        String tmpdir = System.getProperties().getProperty("java.io.tmpdir");
        String dirname = "non-existant-directory";

        String newdir = tmpdir + File.separator + dirname;

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           newdir );

        WikiEngine test = new TestEngine( props );

        File f = new File( newdir );

        assertTrue( "didn't create it", f.exists() );
        assertTrue( "isn't a dir", f.isDirectory() );

        f.delete();
    }

    public void testNonExistantDirProperty()
        throws Exception
    {
        props.remove( FileSystemProvider.PROP_PAGEDIR );

        try
        {
            WikiEngine test = new TestEngine( props );

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
        WikiPage page = new WikiPage("Test1");

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

    public void testEncodeNameUTF8()
        throws Exception
    {
        String name = "\u0041\u2262\u0391\u002E";

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );

        WikiEngine engine = new TestEngine( props );

        assertEquals( "A%E2%89%A2%CE%91.",
                      engine.encodeName(name) );
    }

    public void testReadLinks()
        throws Exception
    {
        String src="Foobar. [Foobar].  Frobozz.  [This is a link].";

        Object[] result = m_engine.scanWikiLinks( new WikiPage("Test"), src ).toArray();
        
        assertEquals( "item 0", result[0], "Foobar" );
        assertEquals( "item 1", result[1], "ThisIsALink" );
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

    public void testLatestGet()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", 
                           "com.ecyrd.jspwiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        WikiPage p = engine.getPage( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        assertEquals( "wrong page", "test", vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
    }

    public void testLatestGet2()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", 
                           "com.ecyrd.jspwiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getText( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        assertEquals( "wrong page", "test", vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
    }

    public void testLatestGet3()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", 
                           "com.ecyrd.jspwiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "false" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getHTML( "test", -1 );

        VerySimpleProvider vsp = (VerySimpleProvider) engine.getPageManager().getProvider();

        assertEquals( "wrong page", "test", vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
    }

    public void testLatestGet4()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", 
                           "com.ecyrd.jspwiki.providers.VerySimpleProvider" );
        props.setProperty( "jspwiki.usePageCache", "true" );

        WikiEngine engine = new TestEngine( props );

        String p = engine.getHTML( VerySimpleProvider.PAGENAME, -1 );

        CachingProvider cp = (CachingProvider)engine.getPageManager().getProvider();
        VerySimpleProvider vsp = (VerySimpleProvider) cp.getRealProvider();

        assertEquals( "wrong page", VerySimpleProvider.PAGENAME, vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
    }

    /**
     *  Checks, if ReferenceManager is informed of new attachments.
     */
    public void testAttachmentRefs()
        throws Exception
    {
        ReferenceManager refMgr = m_engine.getReferenceManager();
        AttachmentManager attMgr = m_engine.getAttachmentManager();
        
        m_engine.saveText( NAME1, "fooBar");

        Attachment att = new Attachment( NAME1, "TestAtt.txt" );
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
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            m_engine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
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
            
        Attachment att = new Attachment( NAME1, "TestAtt.txt" );
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
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            m_engine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
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

        Attachment att = new Attachment( NAME1, "TestAtt.txt" );
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
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            m_engine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
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

        Attachment att = new Attachment( NAME1, "TestAtt.txt" );
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
            String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
            m_engine.deleteAll( new File( files, NAME1+BasicAttachmentProvider.DIR_EXTENSION ) );
            new File( files, "TestPage2"+FileSystemProvider.FILE_EXT ).delete();
        }
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

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        assertTrue( "No file!", saved.exists() );

        FileWriter out = new FileWriter( saved );
        FileUtil.copyContents( new StringReader("Puppaa"), out );
        out.close();

	// Wait for the caching provider to notice a refresh.
        Thread.sleep( 2L*PAGEPROVIDER_RESCAN_PERIOD );

	// Trim - engine.saveText() may append a newline.
        String text = m_engine.getText( NAME1 ).trim();
        assertEquals( "wrong contents", "Puppaa", text );
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

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        assertTrue( "No file!", saved.exists() );

        FileWriter out = new FileWriter( saved );
        FileUtil.copyContents( new StringReader("[Puppaa]"), out );
        out.close();

        Thread.sleep( 5000L ); // Wait five seconds for CachingProvider to wake up.

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

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        assertTrue( "No file!", saved.exists() );

        saved.delete();

        assertFalse( "File not deleted!", saved.exists() );

        Thread.sleep( 5000L ); // Wait five seconds for CachingProvider to catch up.

        WikiPage p = m_engine.getPage( NAME1 );

        assertNull( "Got page!", p );

        String text = m_engine.getText( NAME1 );

        assertEquals( "wrong contents", "", text );

        c = refMgr.findUncreated();
        assertEquals( "NEW: uncreated count", 0, c.size() );
    }
    /*
    public void testDeletePage()
        throws Exception
    {
        m_engine.saveText( NAME1, "Test" );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        File saved = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        assertTrue( "Didn't create it!", saved.exists() );

        WikiPage page = m_engine.getPage( NAME1, WikiProvider.LATEST_VERSION );

        m_engine.deletePage( page );

        assertFalse( "Page has not been removed!", saved.exists() );
    }
    */
}
