
package com.ecyrd.jspwiki.xmlrpc;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import org.apache.xmlrpc.*;

import com.ecyrd.jspwiki.providers.*;

public class RPCHandlerTest extends TestCase
{
    WikiEngine m_engine;
    RPCHandler m_handler;
    Properties m_props;

    static final String NAME1 = "Test";

    public RPCHandlerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_props = new Properties();
        m_props.load( TestEngine.findTestProperties() );

        deleteTempFiles();

        m_engine = new TestEngine( m_props );

        m_handler = new RPCHandler();
        m_handler.initialize( m_engine );
    }

    private void deleteTempFiles()
    {
        String files = m_props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        f.delete();
    }

    public void tearDown()
    {
        deleteTempFiles();
    }

    public void testNonexistantPage()
    {
        try
        {
            byte[] res = m_handler.getPage( "NoSuchPage" );
            fail("No exception for missing page.");
        }
        catch( XmlRpcException e ) 
        {
            assertEquals( "Wrong error code.", RPCHandler.ERR_NOPAGE, e.code );
        }
    }

    public void testRecentChanges()
        throws Exception
    {
        String text = "Foo";
        String pageName = NAME1;

        m_engine.saveText( pageName, text );

        WikiPage directInfo = m_engine.getPage( NAME1 );

        Date modDate = directInfo.getLastModified();

        Calendar cal = Calendar.getInstance();
        cal.setTime( modDate );
        cal.add( Calendar.MINUTE, -1 );

        // Go to UTC
        cal.add( Calendar.MILLISECOND, 
                 -(cal.get( Calendar.ZONE_OFFSET )+
                  (cal.getTimeZone().inDaylightTime( modDate ) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        

        Vector v = m_handler.getRecentChanges( cal.getTime() );

        assertEquals( "wrong number of changes", 1, v.size() );
    }

    public void testPageInfo()
        throws Exception
    {
        String text = "Foobar.";
        String pageName = NAME1;

        m_engine.saveText( pageName, text );

        WikiPage directInfo = m_engine.getPage( NAME1 );

        Hashtable ht = m_handler.getPageInfo( NAME1 );

        assertEquals( "name", (String)ht.get( "name" ), NAME1 );
        
        Date d = (Date) ht.get( "lastModified" );

        Calendar cal = Calendar.getInstance();
        cal.setTime( d );

        System.out.println("Real: "+directInfo.getLastModified() );
        System.out.println("RPC:  "+d );

        // Offset the ZONE offset and DST offset away.  DST only
        // if we're actually in DST.
        cal.add( Calendar.MILLISECOND, 
                 (cal.get( Calendar.ZONE_OFFSET )+
                  (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        System.out.println("RPC2: "+cal.getTime() );

        assertEquals( "date", cal.getTime().getTime(), 
                      directInfo.getLastModified().getTime() );
    }

    /**
     *  Tests if listLinks() works with a single, non-existant local page.
     */
    public void testListLinks()
        throws Exception
    {
        String text = "[Foobar]";
        String pageName = NAME1;

        m_engine.saveText( pageName, text );

        Vector links = m_handler.listLinks( pageName );

        assertEquals( "link count", 1, links.size() );

        Hashtable linkinfo = (Hashtable) links.elementAt(0);

        assertEquals( "name", "Foobar", linkinfo.get("page") );
        assertEquals( "type", "local",  linkinfo.get("type") );
        assertEquals( "href", "Edit.jsp?page=Foobar", linkinfo.get("href") );
    }


    public static Test suite()
    {
        return new TestSuite( RPCHandlerTest.class );
    }
}
