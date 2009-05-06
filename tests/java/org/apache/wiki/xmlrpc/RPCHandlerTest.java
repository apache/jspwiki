/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */

package org.apache.wiki.xmlrpc;


import junit.framework.*;
import java.util.*;

import org.apache.wiki.*;
import org.apache.wiki.api.WikiPage;import org.apache.wiki.xmlrpc.RPCHandler;
import org.apache.xmlrpc.*;

public class RPCHandlerTest extends TestCase
{
    TestEngine m_engine;
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

        m_engine = new TestEngine( m_props );

        m_handler = new RPCHandler();
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( m_engine.createPage( "Dummy" ) );
        m_handler.initialize( context );
        m_engine.getContentManager().release();
    }

    public void tearDown() throws Exception
    {
        TestEngine.deleteTestPage( NAME1 );
        TestEngine.deleteAttachments( NAME1 );
        TestEngine.emptyWorkDir();
        m_engine.shutdown();
    }

    public void testNonexistentPage()
    {
        try
        {
            m_handler.getPage( "NoSuchPage" );
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
        m_engine.emptyRepository();
        Date time = getCalendarTime( Calendar.getInstance().getTime() );
        Vector<Hashtable<String,Object>> previousChanges = m_handler.getRecentChanges( time );
        assertEquals( 0, previousChanges.size() );

        m_engine.saveText( NAME1, "Foo" );
        WikiPage directInfo = m_engine.getPage( NAME1 );
        time = getCalendarTime( directInfo.getLastModified() );
        Vector<Hashtable<String,Object>> recentChanges = m_handler.getRecentChanges( time );

        assertEquals( "wrong number of changes", 1, recentChanges.size() - previousChanges.size() );
    }
/*
    public void testRecentChangesWithAttachments()
        throws Exception
    {
        Date time = getCalendarTime( Calendar.getInstance().getTime() );
        Vector previousChanges = m_handler.getRecentChanges( time );

        m_engine.saveText( NAME1, "Foo" );
        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );
        WikiPage directInfo = m_engine.getPage( NAME1 );
        time = getCalendarTime( directInfo.getLastModified() );
        Vector recentChanges = m_handler.getRecentChanges( time );

        assertEquals( "wrong number of changes", 1, recentChanges.size() - previousChanges.size() );
    }
*/
    public void testPageInfo()
        throws Exception
    {
        m_engine.saveText( NAME1, "Foobar.[{ALLOW view Anonymous}]" );
        WikiPage directInfo = m_engine.getPage( NAME1 );

        Hashtable<String,Object> ht = m_handler.getPageInfo( NAME1 );
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

        Vector<Hashtable<String,String>> links = m_handler.listLinks( pageName );

        assertEquals( "link count", 1, links.size() );

        Hashtable<String,String> linkinfo = links.elementAt(0);

        assertEquals( "name", "Foobar", linkinfo.get("page") );
        assertEquals( "type", "local",  linkinfo.get("type") );
        assertEquals( "href", "http://localhost/Edit.jsp?page=Foobar", linkinfo.get("href") );
    }

/*
    public void testListLinksWithAttachments()
        throws Exception
    {
        String text = "[Foobar] [Test/TestAtt.txt]";
        String pageName = NAME1;

        m_engine.saveText( pageName, text );

        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );

        // Test.

        Vector links = m_handler.listLinks( pageName );

        assertEquals( "link count", 2, links.size() );

        Hashtable linkinfo = (Hashtable) links.elementAt(0);

        assertEquals( "edit name", "Foobar", linkinfo.get("page") );
        assertEquals( "edit type", "local",  linkinfo.get("type") );
        assertEquals( "edit href", "http://localhost/Edit.jsp?page=Foobar", linkinfo.get("href") );

        linkinfo = (Hashtable) links.elementAt(1);

        assertEquals( "att name", NAME1+"/TestAtt.txt", linkinfo.get("page") );
        assertEquals( "att type", "local", linkinfo.get("type") );
        assertEquals( "att href", "http://localhost/attach/"+NAME1+"/TestAtt.txt", linkinfo.get("href") );
    }
*/
    private Date getCalendarTime( Date modifiedDate )
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( modifiedDate );
        cal.add( Calendar.HOUR, -1 );

        // Go to UTC
        // Offset the ZONE offset and DST offset away.  DST only
        // if we're actually in DST.
        cal.add( Calendar.MILLISECOND,
                 -(cal.get( Calendar.ZONE_OFFSET )+
                  (cal.getTimeZone().inDaylightTime( modifiedDate ) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );

        return cal.getTime();
    }

    /*
     * TODO: ENABLE
    public void testPermissions()
        throws Exception
    {
        String text ="Blaa. [{DENY view Guest}] [{ALLOW view NamedGuest}]";

        m_engine.saveText( NAME1, text );

        try
        {
            Vector links = m_handler.listLinks( NAME1 );
            fail("Didn't get an exception in listLinks()");
        }
        catch( XmlRpcException e ) {}

        try
        {
            Hashtable ht = m_handler.getPageInfo( NAME1 );
            fail("Didn't get an exception in getPageInfo()");
        }
        catch( XmlRpcException e ) {}
    }
*/

    public static Test suite()
    {
        return new TestSuite( RPCHandlerTest.class );
    }
}
