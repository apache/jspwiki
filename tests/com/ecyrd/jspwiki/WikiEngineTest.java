
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.providers.*;

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

    public void testNonExistantDirectory()
        throws Exception
    {
        String tmpdir = System.getProperties().getProperty("java.tmpdir");
        String dirname = "non-existant-directory";

        String newdir = tmpdir + File.separator + dirname;

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           newdir );

        try
        {
            WikiEngine test = new TestEngine2( props );

            fail( "Wiki did not warn about wrong property." );
        }
        catch( WikiException e )
        {
            // This is okay.
        }
    }

    public void testNonExistantDirProperty()
        throws Exception
    {
        props.remove( FileSystemProvider.PROP_PAGEDIR );

        try
        {
            WikiEngine test = new TestEngine2( props );

            fail( "Wiki did not warn about missing property." );
        }
        catch( WikiException e )
        {
            // This is okay.
        }
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
        String text = "Foobar.\r\n";
        String name = NAME1;

        engine.saveText( name, text );

        assertEquals( "page does not exist",
                      true,
                      engine.pageExists( name ) );

        assertEquals( "wrong content",
                      text,
                      engine.getText( name ) );
    }

    public void testGetHTML()
    {
        String text = "''Foobar.''";
        String name = NAME1;

        engine.saveText( name, text );

        String data = engine.getHTML( name );

        assertEquals( "<I>Foobar.</I>\n",
                       data );
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

    public void testReadLinks()
        throws Exception
    {
        String src="Foobar. [Foobar].  Frobozz.  [This is a link].";

        Object[] result = engine.scanWikiLinks( src ).toArray();
        
        assertEquals( "item 0", result[0], "Foobar" );
        assertEquals( "item 1", result[1], "ThisIsALink" );
    }

    /**
     *  Tries to find an existing class.
     */
    public void testFindClass()
        throws Exception
    {
        Class foo = WikiEngine.findWikiClass( "WikiPage", "com.ecyrd.jspwiki" );

        assertEquals( foo.getName(), "com.ecyrd.jspwiki.WikiPage" );
    }

    /**
     *  Non-existant classes should throw ClassNotFoundEx.
     */
    public void testFindClassNoClass()
        throws Exception
    {
        try
        {
            Class foo = WikiEngine.findWikiClass( "MubbleBubble", "com.ecyrd.jspwiki" );
            fail("Found class");
        }
        catch( ClassNotFoundException e )
        {
            // Expected
        }
    }

    public static Test suite()
    {
        return new TestSuite( WikiEngineTest.class );
    }
}
