
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.*;

public class BasicAttachmentProviderTest extends TestCase
{
    public static final String NAME1 = "TestPage";
    public static final String NAME2 = "TestPageToo";

    Properties props = new Properties();

    TestEngine m_engine;

    BasicAttachmentProvider m_provider;

    /**
     *  This is the sound of my head hitting the keyboard.
     */
    private static final String c_fileContents = "gy th tgyhgthygyth tgyfgftrfgvtgfgtr";

    public BasicAttachmentProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        m_engine  = new TestEngine(props);

        m_provider = new BasicAttachmentProvider();
        m_provider.initialize( props );

        m_engine.saveText( NAME1, "Foobar" );
        m_engine.saveText( NAME2, "Foobar2" );
    }

    private File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );
        
        FileUtil.copyContents( new StringReader( c_fileContents ), out );

        out.close();
        
        return tmpFile;
    }

    public void tearDown()
    {
        m_engine.deletePage( NAME1 );
        m_engine.deletePage( NAME2 );

        String tmpfiles = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

        File f = new File( tmpfiles, NAME1+BasicAttachmentProvider.DIR_EXTENSION );

        m_engine.deleteAll( f );

        f = new File( tmpfiles, NAME2+BasicAttachmentProvider.DIR_EXTENSION );

        m_engine.deleteAll( f );
    }

    public void testExtension()
    {
        String s = "test.png";

        assertEquals( m_provider.getFileExtension(s), "png" );
    }

    public void testExtension2()
    {
        String s = ".foo";

        assertEquals( "foo", m_provider.getFileExtension(s) );
    }

    public void testExtension3()
    {
        String s = "test.png.3";

        assertEquals( "3", m_provider.getFileExtension(s) );
    }

    public void testExtension4()
    {
        String s = "testpng";

        assertEquals( "", m_provider.getFileExtension(s) );
    }


    public void testExtension5()
    {
        String s = "test.";

        assertEquals( "", m_provider.getFileExtension(s) );
    }

    public void testListAll()
        throws Exception
    {
        File in = makeAttachmentFile();

        Attachment att = new Attachment( NAME1, "test1.txt" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );

        Thread.sleep( 2000L ); // So that we get a bit of granularity.

        Attachment att2 = new Attachment( NAME2, "test2.txt" );

        m_provider.putAttachmentData( att2, new FileInputStream(in) );
        
        List res = m_provider.listAllChanged( new Date(0L) );

        assertEquals( "list size", 2, res.size() );

        Attachment a2 = (Attachment) res.get(0);  // Most recently changed
        Attachment a1 = (Attachment) res.get(1);  // Least recently changed

        assertEquals( "a1 name", att.getName(), a1.getName() );
        assertEquals( "a2 name", att2.getName(), a2.getName() );
    }

    public static Test suite()
    {
        return new TestSuite( BasicAttachmentProviderTest.class );
    }


}
