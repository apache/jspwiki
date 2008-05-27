
package com.ecyrd.jspwiki.providers;

import junit.framework.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.StringReader;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

public class CachingProviderTest extends TestCase
{
    public CachingProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        TestEngine.emptyWorkDir();

        Properties props2 = new Properties();

        props2.load( TestEngine.findTestProperties() );
        PropertyConfigurator.configure(props2);
    }

    public void tearDown()
    {
        TestEngine.emptyWorkDir();
        TestEngine.deleteTestPage("Testi");
    }

    /**
     *  Checks that at startup we call the provider once, and once only.
     */
    public void testInitialization()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        props.setProperty( "jspwiki.usePageCache", "true" );
        props.setProperty( "jspwiki.pageProvider", "com.ecyrd.jspwiki.providers.CounterProvider" );
        props.setProperty( "jspwiki.cachingProvider.capacity", "100" );

        TestEngine engine = new TestEngine( props );

        CounterProvider p = (CounterProvider)((CachingProvider)engine.getPageManager().getProvider()).getRealProvider();

        assertEquals("init", 1, p.m_initCalls);
        assertEquals("getAllPages", 1, p.m_getAllPagesCalls);
        assertEquals("pageExists", 0, p.m_pageExistsCalls);
        assertEquals("getPage", 2, p.m_getPageCalls); // These two are for non-existant pages (with and without s)
        assertEquals("getPageText", 4, p.m_getPageTextCalls);

        engine.getPage( "Foo" );

        assertEquals("pageExists2", 0, p.m_pageExistsCalls);
        assertEquals("getPage2", 2, p.m_getPageCalls);
    }

    public void testSneakyAdd()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        props.setProperty( "jspwiki.cachingProvider.cacheCheckInterval", "2" );
        
        TestEngine engine = new TestEngine( props );
        
        String dir = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        
        File f = new File( dir, "Testi.txt" );
        String content = "[fuufaa]";
        
        PrintWriter out = new PrintWriter( new FileWriter(f) );
        FileUtil.copyContents( new StringReader(content), out );
        out.close();
        
        Thread.sleep( 4000L ); // Make sure we wait long enough
        
        WikiPage p = engine.getPage( "Testi" );
        assertNotNull( "page did not exist?", p );
        
        String text = engine.getText( "Testi");
        assertEquals("text", "[fuufaa]", text );
        
        // TODO: ReferenceManager check as well
    }
    
    public static Test suite()
    {
        return new TestSuite( CachingProviderTest.class );
    }

}
