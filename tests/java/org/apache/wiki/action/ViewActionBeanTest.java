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
package org.apache.wiki.action;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.ViewActionBean;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;


public class ViewActionBeanTest extends TestCase
{
    TestEngine m_engine;
    
    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try 
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not set up TestEngine: " + e.getMessage());
        }
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }
    
    public void testNonExistentPage() throws Exception {
        // Save test page page
        String pageName = "NonExistent" + System.currentTimeMillis();
        assertFalse( m_engine.pageExists( pageName ) );
        
        // Set the 'page' request parameter to test page name...
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp");
        trip.setParameter("page", pageName );
        trip.execute("view");

        // ...we should automatically see test page bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        assertNotNull( bean.getPage() );
        assertEquals( pageName, bean.getPage().getName() );
        
        // ...and the destination should be the Wiki template JSP
        assertEquals("/templates/default/Wiki.jsp?tab=view", trip.getDestination() );
    }
    
    public void testNoParameter() throws Exception {
        // Save page Main
        m_engine.saveText("Main", "This is the main page.");
        WikiPage page = m_engine.getPage("Main");
        assertNotNull("Did not save page Main!", page);
        
        // Execute the request without specifying a page
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp");
        trip.execute("view");

        // ...we should automatically see Main bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        page = m_engine.getPage("Main");
        assertEquals( page, bean.getPage() );
        
        // ...and the destination should be the Wiki template JSP
        assertEquals("/templates/default/Wiki.jsp?tab=view", trip.getDestination() );
    }
    
    public void testSpecialPage() throws Exception {
        // Make sure no special page FindPage actually exists
        m_engine.deletePage( "FindPage" );
        
        // Execute the request with a 'special page' reference
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp");
        trip.addParameter( "page","FindPage" );
        trip.execute("view");

        // ...we should get a dummy page for the 'page' property
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        assertNotNull( bean.getPage() );
        assertEquals( m_engine.getFrontPage( ContentManager.DEFAULT_SPACE ), bean.getPage() );
        
        // ...and the destination should be Search.jsp
        assertEquals("/Search.jsp", trip.getDestination() );
    }

    
    public void testView() throws Exception {
        String pageName = "ViewActionBeanTest";

        // Save test page
        m_engine.saveText( pageName, "This is a test." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull("Did not save page " + pageName + "!", page);
        
        // Set the 'page' request parameter to 'ViewActionBeanTest'...
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp" );
        trip.setParameter( "page", pageName );
        trip.execute( "view" );

        // ...we should automatically see Test bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertEquals( page, bean.getPage() );
        
        // ...and the destination should be the Wiki template JSP
        assertEquals( "/templates/default/Wiki.jsp?tab=view", trip.getDestination() );
    }

    public void testViewVersion() throws Exception {
        String pageName = "ViewActionBeanTest";
        m_engine.deletePage( pageName );

        // Save test page
        m_engine.saveText( pageName, "This is the first version." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull("Did not save page " + pageName + "!", page);
        
        // Go get the first version
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp" );
        trip.setParameter( "page", pageName );
        trip.execute( "view" );
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertEquals( page, bean.getPage() );
        String pageText = page.getContentAsString();
        assertEquals( "This is the first version.\r\n", pageText );
        
        // Save a second version
        m_engine.saveText( pageName, "This is the second version." );
        WikiPage pageV1 = m_engine.getPage( pageName, 1 );
        WikiPage pageV2 = m_engine.getPage( pageName, 2 );
        
        // Go get the first version again
        trip = m_engine.guestTrip( "/Wiki.jsp" );
        trip.setParameter( "page", pageName );
        trip.setParameter( "version", "1" );
        trip.execute( "view" );
        bean = trip.getActionBean( ViewActionBean.class );
        assertEquals( pageV1, bean.getPage() );
        pageText = pageV1.getContentAsString();
        assertEquals( "This is the first version.\r\n", pageText );
        
        // Go get the second version
        trip = m_engine.guestTrip( "/Wiki.jsp" );
        trip.setParameter( "page", pageName );
        trip.setParameter( "version", "2" );
        trip.execute( "view" );
        bean = trip.getActionBean( ViewActionBean.class );
        assertEquals( pageV2, bean.getPage() );
        pageText = pageV2.getContentAsString();
        assertEquals( "This is the second version.\r\n", pageText );
        
        // Go get the "latest" version
        trip = m_engine.guestTrip( "/Wiki.jsp" );
        trip.setParameter( "page", pageName );
        trip.execute( "view" );
        bean = trip.getActionBean( ViewActionBean.class );
        assertEquals( pageV2, bean.getPage() );
        pageText = pageV2.getContentAsString();
        assertEquals( "This is the second version.\r\n", pageText );
        
        m_engine.deletePage( pageName );
    }
    
    public static Test suite()
    {
        return new TestSuite( ViewActionBeanTest.class );
    }
}
