
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

public class WikiEngineTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine engine;

    public WikiEngineTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        engine = new TestEngine2(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        f.delete();
    }

    public void testNonExistantPage()
        throws Exception
    {
        String pagename = "Test1";

        assertEquals( "Page already exists",
                      false,
                      engine.pageExists( pagename ) );
    }

    public void testPutPage()
    {
        String text = "Foobar.";
        String name = NAME1;

        engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      engine.pageExists( name ) );

        assertEquals( "wrong content",
                      text,
                      engine.getText( name ) );
    }

    public void testEncodeNameLatin1()
    {
        String name = "abcåäö";

        assertEquals( "abc%E5%E4%F6",
                      engine.encodeName(name) );
    }

    public void testEncodeNameUTF8()
        throws Exception
    {
        String name = "\u0041\u2262\u0391\u002E";

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );

        WikiEngine engine = new TestEngine2( props );

        assertEquals( "A%E2%89%A2%CE%91.",
                      engine.encodeName(name) );
    }

    public static Test suite()
    {
        return new TestSuite( WikiEngineTest.class );
    }
}
