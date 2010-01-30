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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.util.CryptoUtil;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.WikiPage;

public class EditActionBeanTest extends TestCase
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
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    public void testEditNoParameters() throws Exception
    {
        // Try editing without specifying a page
        MockRoundtrip trip = m_engine.guestTrip( "/Edit.action" );
        String startTime = String.valueOf( System.currentTimeMillis() );
        trip.addParameter( "startTime", CryptoUtil.encrypt( startTime ) );
        trip.addParameter( "wikiText", "This is the edited text" );
        TestEngine.addSpamProtectParams( trip );
        trip.execute( "save" );

        // ...we should NOT see any page bound to the ActionBean
        EditActionBean bean = trip.getActionBean( EditActionBean.class );
        assertNull( bean.getPage() );

        // ...and the "page" and "append" params should be flagged as invalid
        ValidationErrors errors = trip.getValidationErrors();
        assertEquals( 2, errors.size() );
        assertTrue( errors.hasFieldErrors() );
        assertTrue( errors.containsKey( "page" ) );
        assertTrue( errors.containsKey( "append" ) );

        // ...and the destination should be the original display JSP (for
        // displaying errors)
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    }

    public void testEditSpecialPage() throws Exception
    {
        // Make sure no special page FindPage actually exists
        m_engine.deletePage( "FindPage" );

        // Try editing the 'special page'
        MockRoundtrip trip = m_engine.guestTrip( "/Edit.action" );
        String startTime = String.valueOf( System.currentTimeMillis() );
        trip.addParameter( "page", "FindPage" );
        trip.addParameter( "startTime", CryptoUtil.encrypt( startTime ) );
        trip.addParameter( "wikiText", "This is the edited text" );
        trip.addParameter( "append", CryptoUtil.encrypt( "false" ) );
        TestEngine.addSpamProtectParams( trip );
        trip.execute( "save" );

        // ...we should NOT see any page bound to the ActionBean
        EditActionBean bean = trip.getActionBean( EditActionBean.class );
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

    public void testEditExistingPage() throws Exception
    {

        String pageName = "EditActionBeanTest" + System.currentTimeMillis();

        m_engine.saveText( pageName, "This is a test." );
        WikiPage page = m_engine.getPage( pageName );
        assertNotNull( "Did not save page " + pageName + "!", page );

        // Set up the marked-up page
        MockRoundtrip trip = m_engine.guestTrip( "/Edit.action" );
        trip.setParameter( "page", pageName );
        String startTime = String.valueOf( System.currentTimeMillis() );
        trip.addParameter( "startTime", CryptoUtil.encrypt( startTime ) );
        trip.addParameter( "wikiText", "This is the edited text" );
        trip.addParameter( "append", CryptoUtil.encrypt( "false" ) );
        TestEngine.addSpamProtectParams( trip );
        trip.execute( "save" );

        // ...we should automatically see the test page bound to the ActionBean
        EditActionBean bean = trip.getActionBean( EditActionBean.class );
        ValidationErrors errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( page, bean.getPage() );
        assertEquals( "This is the edited text\r\n", page.getContentAsString() );

        // ...and the destination should be the Wiki action bean
        assertEquals( "/Wiki.jsp?view=&page=" + pageName, trip.getDestination() );
        
        // Save the page one more time!
        trip = m_engine.guestTrip( "/Edit.action" );
        trip.setParameter( "page", pageName );
        startTime = String.valueOf( System.currentTimeMillis() );
        trip.addParameter( "startTime", CryptoUtil.encrypt( startTime ) );
        trip.addParameter( "wikiText", "This is the third revision." );
        trip.addParameter( "append", CryptoUtil.encrypt( "false" ) );
        TestEngine.addSpamProtectParams( trip );
        trip.execute( "save" );
        
        // ...and the text should again be the same as our revision
        bean = trip.getActionBean( EditActionBean.class );
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( page, bean.getPage() );
        assertEquals( "This is the third revision.\r\n", page.getContentAsString() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }

    public void testEditNewPage() throws Exception
    {

        String pageName = "EditActionBeanTest" + System.currentTimeMillis();
        assertFalse( m_engine.pageExists( pageName ) );

        // Set up the marked-up page
        MockRoundtrip trip = m_engine.guestTrip( "/Edit.action" );
        trip.setParameter( "page", pageName );
        String startTime = String.valueOf( System.currentTimeMillis() );
        trip.addParameter( "startTime", CryptoUtil.encrypt( startTime ) );
        trip.addParameter( "wikiText", "This is the edited text" );
        trip.addParameter( "append", CryptoUtil.encrypt( "false" ) );
        TestEngine.addSpamProtectParams( trip );
        trip.execute( "save" );

        // ...we should automatically see the test page bound to the ActionBean
        EditActionBean bean = trip.getActionBean( EditActionBean.class );
        ValidationErrors errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertNotNull( bean.getPage() );
        assertEquals( pageName, bean.getPage().getName() );

        // ...and the destination should be the Wiki action bean
        assertEquals( "/Wiki.jsp?view=&page=" + pageName, trip.getDestination() );

        // Delete the test page
        m_engine.deletePage( pageName );
    }

    public static Test suite()
    {
        return new TestSuite( EditActionBeanTest.class );
    }
}
