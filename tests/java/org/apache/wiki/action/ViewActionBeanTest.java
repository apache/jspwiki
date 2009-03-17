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
    
    
    public void testView() throws Exception {
        // Save page Main
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page);
        
        // Set the 'page' request parameter to 'Main'...
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.action");
        trip.setParameter("page", "Test");
        trip.execute("view");

        // ...we should automatically see Test bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        assertEquals( page, bean.getPage() );
        
        // ...and the destination should be Wiki.jsp (aka display JSP)
        assertEquals("/Wiki.jsp", trip.getDestination() );
    }
    
    public void testViewNoParameter() throws Exception {
        // Save page Main
        m_engine.saveText("Main", "This is the main page.");
        WikiPage page = m_engine.getPage("Main");
        assertNotNull("Did not save page Main!", page);
        
        // Execute the request without specifying a page
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.action");
        trip.execute("view");

        // ...we should automatically see Main bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        page = m_engine.getPage("Main");
        assertEquals( page, bean.getPage() );
        
        // ...and the destination should be Wiki.jsp (aka display JSP)
        assertEquals("/Wiki.jsp", trip.getDestination() );
    }
    
    public void testSpecialPage() throws Exception {
        // Make sure no special page FindPage actually exists
        m_engine.deletePage( "FindPage" );
        
        // Execute the request with a 'special page' reference
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.action");
        trip.addParameter( "page","FindPage" );
        trip.execute("view");

        // ...we should get a null for the 'page' property
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        assertEquals( null, bean.getPage() );
        
        // ...and the destination should be Search.jsp
        assertEquals("/Search.jsp", trip.getDestination() );
    }

    public static Test suite()
    {
        return new TestSuite( ViewActionBeanTest.class );
    }
}
