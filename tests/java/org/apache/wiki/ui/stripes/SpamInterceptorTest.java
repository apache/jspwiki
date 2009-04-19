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

package org.apache.wiki.ui.stripes;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.TestActionBean;
import org.apache.wiki.filters.SpamFilter;

public class SpamInterceptorTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( SpamInterceptorTest.class );
    }

    private TestEngine m_engine = null;

    Properties props = new Properties();

    public SpamInterceptorTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    public void testInvalidToken() throws Exception
    {
        // Add the trap + token params, but fill in the trap param
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TRAPAA\nTOKENA" );
        trip.addParameter( "TRAPAA", new String[0] );
        trip.addParameter( "TOKENA", new String[0] );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );
        
        // Add the UTF-8 token
        trip.addParameter( SpamFilter.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testInvalidTrap() throws Exception
    {
        // Add the trap + token params, but fill in the trap param
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TRAPAA\nTOKENA" );
        trip.addParameter( "TRAPAA", "BotFilledValue" );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );

        // Add the UTF-8 token
        trip.addParameter( SpamFilter.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testMissingToken() throws Exception
    {
        // Add the trap param (but not the token param)
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TRAPAA" );
        trip.addParameter( "TRAPAA", new String[0] );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );

        // Add the UTF-8 token
        trip.addParameter( SpamFilter.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testMissingTrap() throws Exception
    {
        // Add token param (but not the trap param)
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TOKENA" );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );

        // Add the UTF-8 token
        trip.addParameter( SpamFilter.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testMissingUTF8Check() throws Exception
    {
        // Add the trap + token params
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TRAPAA\nTOKENA" );
        trip.addParameter( "TRAPAA", new String[0] );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testNoTokens() throws Exception
    {
        // Execute the SpamProtect-ed handler with no tokens
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );

        // Add the UTF-8 token
        trip.addParameter( SpamFilter.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

    public void testTokens() throws Exception
    {
        // Add the trap + token params
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        TestEngine.addSpamProtectParams( trip );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...and that we passed the token check
        assertEquals( null, trip.getDestination() );
    }

    public void testWrongOrder() throws Exception
    {
        // Add token param (but not the trap param)
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TOKENA\nTRAPAA" );
        trip.addParameter( "TRAPAA", new String[0] );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( SpamFilter.REQ_SPAM_PARAM, paramValue );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( "/Wiki.action?view=&page=SessionExpired", trip.getDestination() );
    }

}
