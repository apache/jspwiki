
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

// FIXME: Should this thingy go directly to the VersioningFileProvider,
//        or should it rely on the WikiEngine API?

public class VersioningFileProviderTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine engine;

    public VersioningFileProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki_vers.properties") );

        engine = new TestEngine2(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        // Remove file
        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );
        f.delete();

        f = new File( files, "OLD" );

        TestEngine.deleteAll(f);
    }

    public void testMillionChanges()
    {
        String text = "";
        String name = NAME1;
        int    maxver = 1000; // Save 1000 versions.

        for( int i = 0; i < maxver; i++ )
        {
            text = text + ".";
            engine.saveText( name, text );
        }

        WikiPage pageinfo = engine.getPage( NAME1 );

        assertEquals( "wrong version", maxver, pageinfo.getVersion() );
        
        // +2 comes from \r\n.
        assertEquals( "wrong text", maxver+2, engine.getText(NAME1).length() );
    }

    public void testCheckin()
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        String res = engine.getText(NAME1);
       
        assertEquals( text, res );
    }

    public void testPageInfo()
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage res = engine.getPage(NAME1);
       
        assertEquals( 1, res.getVersion() );
    }

    public void testGetOldVersion()
    {
        String text = "diddo\r\n";
        String text2 = "barbar\r\n";
        String text3 = "Barney\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        WikiPage res = engine.getPage(NAME1);

        assertEquals("wrong version", 3, res.getVersion() );

        assertEquals("ver1", text, engine.getText( NAME1, 1 ) );
        assertEquals("ver2", text2, engine.getText( NAME1, 2 ) );
        assertEquals("ver3", text3, engine.getText( NAME1, 3 ) );
    }

    public void testNonexistantPage()
    {
        assertNull( engine.getPage("fjewifjeiw") );
    }

    public void testVersionHistory()
    {
        String text = "diddo\r\n";
        String text2 = "barbar\r\n";
        String text3 = "Barney\r\n";

        engine.saveText( NAME1, text );
        engine.saveText( NAME1, text2 );
        engine.saveText( NAME1, text3 );

        Collection history = engine.getVersionHistory(NAME1);

        assertEquals( "size", 3, history.size() );
    }

    public static Test suite()
    {
        return new TestSuite( VersioningFileProviderTest.class );
    }
}
