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

import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiPage;

public class DiffActionBeanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( DiffActionBeanTest.class );
    }

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
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    public void testDiffProvider() throws Exception
    {
        // Try diffing without specifying a page
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.execute( "diff" );
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        assertEquals( "ContextualDiffProvider", bean.getDiffProvider() );
    }
    
    public void testHistory() throws Exception
    {
        String pageName = "DiffActionBeanTest" + System.currentTimeMillis();

        // Save 3 versions
        m_engine.saveText( pageName, "This is version one." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );
        m_engine.saveText( pageName, "This is version two!" );
        m_engine.saveText( pageName, "This is version three..." );

        // Diff with no versions
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.setParameter( "page", pageName );
        trip.execute( "diff" );
        
        // The page history should be set
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        List<WikiPage> history = bean.getHistory();
        assertNotNull( history );
        assertEquals( 3, history.size() );
        
        // Verify that the history was retrieved in the right order
        assertEquals( 1, history.get( 0 ).getVersion() );
        assertEquals( 2, history.get( 1 ).getVersion() );
        assertEquals( 3, history.get( 2 ).getVersion() );
        assertEquals( "This is version one.\r\n", history.get( 0 ).getContentAsString() );
        assertEquals( "This is version two!\r\n", history.get( 1 ).getContentAsString() );
        assertEquals( "This is version three...\r\n", history.get( 2 ).getContentAsString() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }

    public void testNoParameters() throws Exception
    {
        // Try diffing without specifying a page
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.execute( "diff" );

        // ...we should NOT see any page bound to the ActionBean
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        assertNull( bean.getPage() );

        // ...and the "page" param should be flagged as invalid
        ValidationErrors errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.hasFieldErrors() );
        assertTrue( errors.containsKey( "page" ) );

        // ...and the destination should be the original display JSP (for
        // displaying errors)
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    }
    
    public void testNoVersionParameters() throws Exception
    {
        String pageName = "DiffActionBeanTest" + System.currentTimeMillis();

        // Save 3 versions
        m_engine.saveText( pageName, "This is version one." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );
        m_engine.saveText( pageName, "This is version two!" );
        m_engine.saveText( pageName, "This is version three..." );

        // Diff without specifying R1 or R2
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.setParameter( "page", pageName );
        trip.execute( "diff" );
        
        // The default versions should be set
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        assertEquals( WikiProvider.LATEST_VERSION, bean.getR1() );
        assertEquals( 2, bean.getR2() );

        // ...and the destination should be Diff.jsp (aka display JSP)
        assertEquals( "/Diff.jsp", trip.getDestination() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }
    
    public void testR1andR2() throws Exception
    {

        String pageName = "DiffActionBeanTest" + System.currentTimeMillis();

        // Save 3 versions
        m_engine.saveText( pageName, "This is version one." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );
        m_engine.saveText( pageName, "This is version two!" );
        m_engine.saveText( pageName, "This is version three..." );

        // Diff with R1 and R2
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.setParameter( "page", pageName );
        trip.setParameter( "r1", "1" );
        trip.setParameter( "r2", "2" );
        trip.execute( "diff" );

        // ...we should automatically see the test page bound to the ActionBean
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        ValidationErrors errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( page, bean.getPage() );
        assertEquals( "This is version three...\r\n", page.getContentAsString() );
        
        // .. and the correct versions should be set
        assertEquals( 1, bean.getR1() );
        assertEquals( 2, bean.getR2() );

        // ...and the destination should be Diff.jsp (aka display JSP)
        assertEquals( "/Diff.jsp", trip.getDestination() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }
    
    public void testR1Only() throws Exception
    {
        String pageName = "DiffActionBeanTest" + System.currentTimeMillis();

        // Save 3 versions
        m_engine.saveText( pageName, "This is version one." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );
        m_engine.saveText( pageName, "This is version two!" );
        m_engine.saveText( pageName, "This is version three..." );

        // Diff with R1 but not R2
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.setParameter( "page", pageName );
        trip.setParameter( "r1", "1" );
        trip.execute( "diff" );
        
        // The default R2 should be set
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        assertEquals( 1, bean.getR1() );
        assertEquals( 2, bean.getR2() );

        // ...and the destination should be Diff.jsp (aka display JSP)
        assertEquals( "/Diff.jsp", trip.getDestination() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }

    public void testR2Only() throws Exception
    {
        String pageName = "DiffActionBeanTest" + System.currentTimeMillis();

        // Save 3 versions
        m_engine.saveText( pageName, "This is version one." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );
        m_engine.saveText( pageName, "This is version two!" );
        m_engine.saveText( pageName, "This is version three..." );

        // Diff with R2 but not R1
        MockRoundtrip trip = m_engine.guestTrip( "/Diff.action" );
        trip.setParameter( "page", pageName );
        trip.setParameter( "r2", "1" );
        trip.execute( "diff" );
        
        // The default R2 should be set
        DiffActionBean bean = trip.getActionBean( DiffActionBean.class );
        assertEquals( WikiProvider.LATEST_VERSION, bean.getR1() );
        assertEquals( 1, bean.getR2() );

        // ...and the destination should be Diff.jsp (aka display JSP)
        assertEquals( "/Diff.jsp", trip.getDestination() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }
}
