package com.ecyrd.jspwiki;

import java.util.Collection;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.attachment.Attachment;

public class PageRenamerTest extends TestCase
{
    TestEngine m_engine;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        
        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);  
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        TestEngine.deleteTestPage("TestPage");
        TestEngine.deleteTestPage("TestPage2");
        TestEngine.deleteTestPage("FooTest");
        TestEngine.deleteTestPage("Test");
        
        TestEngine.emptyWorkDir();
    }

    public void testSimpleRename()
        throws Exception
    {
        // Count the numberof existing references
        int refCount = m_engine.getReferenceManager().findCreated().size();
        
        m_engine.saveText("TestPage", "the big lazy dog thing" );
        
        WikiPage p = m_engine.getPage("TestPage");
        
        WikiContext context = new WikiContext(m_engine, p);
        
        m_engine.renamePage(context, "TestPage", "FooTest", false);
        
        WikiPage newpage = m_engine.getPage("FooTest");
        
        assertNotNull( "no new page", newpage );
        assertNull( "old page not gone", m_engine.getPage("TestPage") );
        
        // Refmgr
        
        Collection refs = m_engine.getReferenceManager().findCreated();
        
        assertEquals( "wrong list size", refCount+1, refs.size() );
        assertTrue( refs.contains("FooTest") );
    }
    
    public void testReferrerChange()
       throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage]");
        
        WikiPage p = m_engine.getPage("TestPage");
        
        WikiContext context = new WikiContext(m_engine, p);
        
        m_engine.renamePage(context, "TestPage", "FooTest", true);
        
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION);
        
        assertEquals( "no rename", "[FooTest]", data.trim() );
        
        Collection refs = m_engine.getReferenceManager().findReferrers("TestPage");
        
        assertNull( "oldpage", refs );
        
        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", "TestPage2", (String)refs.iterator().next() );
    }

    public void testReferrerChangeCC()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "TestPage");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = new WikiContext(m_engine, p);
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION);
     
        assertEquals( "no rename", "FooTest", data.trim() );
        Collection refs = m_engine.getReferenceManager().findReferrers("TestPage");
        
        assertNull( "oldpage", refs );
        
        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", "TestPage2", (String)refs.iterator().next() );
    }
    
    public void testReferrerChangeAnchor()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage#heading1]");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = new WikiContext(m_engine, p);
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION);
     
        assertEquals( "no rename", "[FooTest#heading1]", data.trim() );
        Collection refs = m_engine.getReferenceManager().findReferrers("TestPage");
        
        assertNull( "oldpage", refs );
        
        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", "TestPage2", (String)refs.iterator().next() );
    }
    
    public void testReferrerChangeMultilink()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage] [TestPage] [linktext|TestPage] TestPage [linktext|TestPage] [TestPage#Anchor] [TestPage] TestPage [TestPage]");
     
        WikiPage p = m_engine.getPage("TestPage");
     
        WikiContext context = new WikiContext(m_engine, p);
     
        m_engine.renamePage(context, "TestPage", "FooTest", true);
     
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION);
     
        assertEquals( "no rename", 
                      "[FooTest] [FooTest] [linktext|FooTest] FooTest [linktext|FooTest] [FooTest#Anchor] [FooTest] FooTest [FooTest]", 
                      data.trim() );

        Collection refs = m_engine.getReferenceManager().findReferrers("TestPage");
        
        assertNull( "oldpage", refs );
        
        refs = m_engine.getReferenceManager().findReferrers( "FooTest" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", "TestPage2", (String)refs.iterator().next() );
    }
    
    public void testReferrerNoWikiName()
        throws Exception
    {
        m_engine.saveText("Test","foo");
        m_engine.saveText("TestPage2", "[Test] [Test#anchor] test Test [test] [link|test] [link|test]");
        
        WikiPage p = m_engine.getPage("TestPage");
        
        WikiContext context = new WikiContext(m_engine, p);
     
        m_engine.renamePage(context, "Test", "TestPage", true);
        
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION );
        
        assertEquals( "wrong data", "[TestPage] [TestPage#anchor] test Test [TestPage] [link|TestPage] [link|TestPage]", data.trim() );
    }

    public void testAttachmentChange()
        throws Exception
    {
        m_engine.saveText("TestPage", "foofoo" );
        m_engine.saveText("TestPage2", "[TestPage/foo.txt] [linktext|TestPage/bar.jpg]");
 
        m_engine.addAttachment("TestPage", "foo.txt", "testing".getBytes() );
        m_engine.addAttachment("TestPage", "bar.jpg", "pr0n".getBytes() );
        WikiPage p = m_engine.getPage("TestPage");
 
        WikiContext context = new WikiContext(m_engine, p);
 
        m_engine.renamePage(context, "TestPage", "FooTest", true);
 
        String data = m_engine.getPureText("TestPage2", WikiProvider.LATEST_VERSION);
 
        assertEquals( "no rename", 
                      "[FooTest/foo.txt] [linktext|FooTest/bar.jpg]", 
                      data.trim() );

        Attachment att = m_engine.getAttachmentManager().getAttachmentInfo("FooTest/foo.txt");
        assertNotNull("footext",att);
        
        att = m_engine.getAttachmentManager().getAttachmentInfo("FooTest/bar.jpg");
        assertNotNull("barjpg",att);
        
        att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/bar.jpg");
        assertNull("testpage/bar.jpg exists",att);
        
        att = m_engine.getAttachmentManager().getAttachmentInfo("TestPage/foo.txt");
        assertNull("testpage/foo.txt exists",att);
        
        Collection refs = m_engine.getReferenceManager().findReferrers("TestPage/bar.jpg");
    
        assertNull( "oldpage", refs );
    
        refs = m_engine.getReferenceManager().findReferrers( "FooTest/bar.jpg" );
        assertEquals( "new size", 1, refs.size() );
        assertEquals( "wrong ref", "TestPage2", (String)refs.iterator().next() );
    }

    public static Test suite()
    {
        return new TestSuite( PageRenamerTest.class );
    }

}
