
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.providers.*;

public class WikiEngineTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine m_engine;

    public WikiEngineTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        m_engine = new TestEngine(props);
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
            WikiEngine test = new TestEngine( props );

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

        assertEquals( "<I>Foobar.</I>\n",
                       data );
    }

    public void testEncodeNameLatin1()
    {
        String name = "abcåäö";

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

        Object[] result = m_engine.scanWikiLinks( src ).toArray();
        
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

        assertEquals("JSPWiki Page", m_engine.beautifyTitle( src ) );
    }

    /**
     *  Acronyms should be treated wisely.
     */
    public void testBeautifyTitleAcronym2()
    {
        String src = "DELETEME";

        assertEquals("DELETEME", m_engine.beautifyTitle( src ) );
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


    public void testLatestGet()
        throws Exception
    {
        props.setProperty( "jspwiki.pageProvider", 
                           "com.ecyrd.jspwiki.providers.VerySimpleProvider" );

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

        String p = engine.getHTML( "test", -1 );

        CachingProvider cp = (CachingProvider)engine.getPageManager().getProvider();
        VerySimpleProvider vsp = (VerySimpleProvider) cp.getRealProvider();

        assertEquals( "wrong page", "test", vsp.m_latestReq );
        assertEquals( "wrong version", -1, vsp.m_latestVers );
    }

    public static Test suite()
    {
        return new TestSuite( WikiEngineTest.class );
    }


}
