/*
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

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

public class RPCHandlerTest
{
    TestEngine m_engine;
    RPCHandler m_handler;
    Properties m_props = TestEngine.getTestProperties();

    static final String NAME1 = "Test";

    @BeforeEach
    public void setUp()
        throws Exception
    {
        CacheManager.getInstance().removeAllCaches();
        m_engine = new TestEngine( m_props );

        m_handler = new RPCHandler();
        WikiContext ctx = new WikiContext( m_engine, new WikiPage(m_engine, "Dummy") );
        m_handler.initialize( ctx );
    }

    @AfterEach
    public void tearDown()
    {
        m_engine.deleteTestPage( NAME1 );
        TestEngine.deleteAttachments( NAME1 );
        TestEngine.emptyWorkDir();
    }

    @Test
    public void testNonexistantPage()
    {
        try
        {
            m_handler.getPage( "NoSuchPage" );
            Assertions.fail("No exception for missing page.");
        }
        catch( XmlRpcException e )
        {
            Assertions.assertEquals( RPCHandler.ERR_NOPAGE, e.code, "Wrong error code." );
        }
    }

    @Test
    public void testRecentChanges()
        throws Exception
    {
        Date time = getCalendarTime( Calendar.getInstance().getTime() );
        Vector previousChanges = m_handler.getRecentChanges( time );

        m_engine.saveText( NAME1, "Foo" );
        WikiPage directInfo = m_engine.getPageManager().getPage( NAME1 );
        time = getCalendarTime( directInfo.getLastModified() );
        Vector recentChanges = m_handler.getRecentChanges( time );

        Assertions.assertEquals( 1, recentChanges.size() - previousChanges.size(), "wrong number of changes" );
    }

    @Test
    public void testRecentChangesWithAttachments()
        throws Exception
    {
        Date time = getCalendarTime( Calendar.getInstance().getTime() );
        Vector previousChanges = m_handler.getRecentChanges( time );

        m_engine.saveText( NAME1, "Foo" );
        Attachment att = new Attachment( m_engine, NAME1, "TestAtt.txt" );
        att.setAuthor( "FirstPost" );
        m_engine.getAttachmentManager().storeAttachment( att, m_engine.makeAttachmentFile() );
        WikiPage directInfo = m_engine.getPageManager().getPage( NAME1 );
        time = getCalendarTime( directInfo.getLastModified() );
        Vector recentChanges = m_handler.getRecentChanges( time );

        Assertions.assertEquals( 1, recentChanges.size() - previousChanges.size(), "wrong number of changes" );
    }

    @Test
    public void testPageInfo()
        throws Exception
    {
        m_engine.saveText( NAME1, "Foobar.[{ALLOW view Anonymous}]" );
        WikiPage directInfo = m_engine.getPageManager().getPage( NAME1 );

        Hashtable ht = m_handler.getPageInfo( NAME1 );
        Assertions.assertEquals( (String)ht.get( "name" ), NAME1, "name" );

        Date d = (Date) ht.get( "lastModified" );

        Calendar cal = Calendar.getInstance();
        cal.setTime( d );

        // System.out.println("Real: "+directInfo.getLastModified() );
        // System.out.println("RPC:  "+d );

        // Offset the ZONE offset and DST offset away.  DST only
        // if we're actually in DST.
        cal.add( Calendar.MILLISECOND,
                 (cal.get( Calendar.ZONE_OFFSET )+
                  (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 ) ) );
        // System.out.println("RPC2: "+cal.getTime() );

        Assertions.assertEquals( cal.getTime().getTime(), directInfo.getLastModified().getTime(), "date" );
    }

    /**
     *  Tests if listLinks() works with a single, non-existant local page.
     */
    @Test
    public void testListLinks()
        throws Exception
    {
        String text = "[Foobar]";
        String pageName = NAME1;

        m_engine.saveText( pageName, text );

        Vector links = m_handler.listLinks( pageName );

        Assertions.assertEquals( 1, links.size(), "link count" );

        Hashtable linkinfo = (Hashtable) links.elementAt(0);

        Assertions.assertEquals( "Foobar", linkinfo.get("page"), "name" );
        Assertions.assertEquals( "local",  linkinfo.get("type"), "type" );
        Assertions.assertEquals( "/test/Edit.jsp?page=Foobar", linkinfo.get("href"), "href" );
    }


    @Test
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

        Assertions.assertEquals( 2, links.size(), "link count" );

        Hashtable linkinfo = (Hashtable) links.elementAt(0);

        Assertions.assertEquals( "Foobar", linkinfo.get("page"), "edit name" );
        Assertions.assertEquals( "local",  linkinfo.get("type"), "edit type" );
        Assertions.assertEquals( "/test/Edit.jsp?page=Foobar", linkinfo.get("href"), "edit href" );

        linkinfo = (Hashtable) links.elementAt(1);

        Assertions.assertEquals( NAME1+"/TestAtt.txt", linkinfo.get("page"), "att name" );
        Assertions.assertEquals( "local", linkinfo.get("type"), "att type" );
        Assertions.assertEquals( "/test/attach/"+NAME1+"/TestAtt.txt", linkinfo.get("href"), "att href" );
    }

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
    @Test
    public void testPermissions()
        throws Exception
    {
        String text ="Blaa. [{DENY view Guest}] [{ALLOW view NamedGuest}]";

        m_engine.saveText( NAME1, text );

        try
        {
            Vector links = m_handler.listLinks( NAME1 );
            Assertions.fail("Didn't get an exception in listLinks()");
        }
        catch( XmlRpcException e ) {}

        try
        {
            Hashtable ht = m_handler.getPageInfo( NAME1 );
            Assertions.fail("Didn't get an exception in getPageInfo()");
        }
        catch( XmlRpcException e ) {}
    }
*/

}
