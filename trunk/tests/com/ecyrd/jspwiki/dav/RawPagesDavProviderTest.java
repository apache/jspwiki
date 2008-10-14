package com.ecyrd.jspwiki.dav;

import java.util.Properties;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RawPagesDavProviderTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    RawPagesDavProvider m_provider;
    
    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        engine = new TestEngine(props);

        m_provider = new RawPagesDavProvider(engine);
    }

    protected void tearDown() throws Exception
    {
        TestEngine.deleteTestPage("TestPage");
    }

    public void testGetPageURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
        
        DavItem di = m_provider.getItem( new DavPath("t/TestPage.txt") );
        
        assertNotNull( "No di", di );
        assertEquals("URL", "http://localhost/dav/raw/t/TestPage.txt", di.getHref() );
    }

    public void testDirURL()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");
    
        DavItem di = m_provider.getItem( new DavPath("") );
    
        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/dav/raw/", di.getHref() );
    }

    public void testDirURL2()
        throws Exception
    {
        engine.saveText("TestPage", "foobar");

        DavItem di = m_provider.getItem( new DavPath("t/") );

        assertNotNull( "No di", di );
        assertTrue( "DI is of wrong type", di instanceof DirectoryItem );
        assertEquals("URL", "http://localhost/dav/raw/t/", di.getHref() );
    }

    public static Test suite()
    {
        return new TestSuite( RawPagesDavProviderTest.class );
    }


}
