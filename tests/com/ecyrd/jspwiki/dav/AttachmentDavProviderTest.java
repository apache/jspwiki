package com.ecyrd.jspwiki.dav;

import java.util.Properties;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AttachmentDavProviderTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    AttachmentDavProvider m_provider;
    
    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine(props);

        m_provider = new AttachmentDavProvider(engine);
    }

    protected void tearDown() throws Exception
    {
        engine.deleteAttachments( "TestPage" );
        TestEngine.deleteTestPage("TestPage");
    }

    public void testGetPageURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
        Attachment att = new Attachment(engine,"TestPage","deceit of the tribbles.txt");
        
        engine.getAttachmentManager().storeAttachment( att, engine.makeAttachmentFile() );
        
        DavItem di = m_provider.getItem( new DavPath("TestPage/deceit of the tribbles.txt") );
        
        assertNotNull( "No di", di );
        assertEquals("URL", "http://localhost/attach/TestPage/deceit+of+the+tribbles.txt", 
                     di.getHref() );
    }

    public void testDirURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
    
        DavItem di = m_provider.getItem( new DavPath("") );
    
        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/attach/", di.getHref() );
    }

    public void testDirURL2()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");

        DavItem di = m_provider.getItem( new DavPath("TestPage/") );

        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/attach/TestPage/", di.getHref() );
    }

    public static Test suite()
    {
        return new TestSuite( RawPagesDavProviderTest.class );
    }


}
