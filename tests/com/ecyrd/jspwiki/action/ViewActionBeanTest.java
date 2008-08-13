package com.ecyrd.jspwiki.action;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiPage;

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
    
    public void testActionBean() throws Exception {
        // Save page Main
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page);
        
        // Set the 'page' request parameter to 'Main'...
        MockRoundtrip trip = m_engine.guestTrip( "/Wiki.jsp");
        trip.setParameter("page", "Test");
        trip.execute("view");

        // ...we should automatically see Test bound to the ActionBean (nice!)
        ViewActionBean bean = trip.getActionBean(ViewActionBean.class);
        assertEquals( page, bean.getPage() );
        
        // ...and the destination should be Wiki.jsp (aka /View.action)
        assertEquals("/Wiki.jsp", trip.getDestination() );
    }
    
    public void testActionBeanNoParameter() throws Exception {
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
        
        // ...and the destination should be Wiki.jsp (aka /View.action)
        assertEquals("/Wiki.jsp", trip.getDestination() );
    }
    
    public static Test suite()
    {
        return new TestSuite( ViewActionBeanTest.class );
    }
}
