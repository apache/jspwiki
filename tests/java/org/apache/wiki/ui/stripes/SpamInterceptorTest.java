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

import java.util.Map;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.TestActionBean;
import org.apache.wiki.content.inspect.BotTrapInspector;
import org.apache.wiki.content.inspect.Challenge;
import org.apache.wiki.tags.SpamProtectTag;

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

    public void testGetBeanProperties() throws Exception
    {
        TestActionBean bean = new TestActionBean();
        bean.setAcl( "ACL" );
        bean.setText( "Sample text" );
        Map<String, Object> map = SpamInterceptor.getBeanProperties( bean, new String[] { "text", "acl", "nonExistentProperty" } );
        assertEquals( 2, map.size() );
        Object value = map.get( "text" );
        assertEquals( "Sample text", value );
        value = map.get( "acl" );
        assertEquals( "ACL", value );
    }

    public void testInvalidToken() throws Exception
    {
        // Add the trap + token params, but fill in the trap param
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        trip.addParameter( "TOKENA", new String[0] );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, CryptoUtil.encrypt( "TOKENA" ) );
        
        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertNull( bean.getPage() );

        // ...but that we failed the token check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testInvalidTrap() throws Exception
    {
        // Add the trap + token params, but fill in the trap param
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, "BotFilledThisIn" );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, CryptoUtil.encrypt( "TOKENA" ) );

        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the trap check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testMissingToken() throws Exception
    {
        // Add the trap param (but not the token param)
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, CryptoUtil.encrypt( "" ) );

        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testMissingTrap() throws Exception
    {
        // Add token param (but not the trap param)
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, CryptoUtil.encrypt( "TOKENA" ) );

        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...and that we passed the inspection
        assertEquals( 0, trip.getValidationErrors().size() );
        assertFalse( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testMissingUTF8Check() throws Exception
    {
        // Add the trap + token params
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        String paramValue = CryptoUtil.encrypt( "TRAPAA\nTOKENA" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, paramValue );

        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the UTF-8 check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testNoChallenge() throws Exception
    {
        // Add the trap + token params
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        trip.addParameter( BotTrapInspector.REQ_TRAP_PARAM, new String[0] );
        trip.addParameter( "TOKENA", trip.getRequest().getSession().getId() );
        trip.addParameter( BotTrapInspector.REQ_SPAM_PARAM, CryptoUtil.encrypt( "TOKENA" ) );

        // Omit the challenge param

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the challenge check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testNoToken() throws Exception
    {
        // Execute the SpamProtect-ed handler with no token
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );

        // Add the challenge param
        String challengeParam = CryptoUtil.encrypt( Challenge.Request.CAPTCHA_ON_DEMAND.name() );
        trip.addParameter( SpamInterceptor.CHALLENGE_REQUEST_PARAM, challengeParam );

        // Add the UTF-8 token
        trip.addParameter( BotTrapInspector.REQ_ENCODING_CHECK, "\u3041" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...but that we failed the token check
        assertEquals( 1, trip.getValidationErrors().size() );
        assertTrue( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }

    public void testToken() throws Exception
    {
        // Add the trap + token + challenge params
        MockRoundtrip trip = m_engine.guestTrip( "/Test.action" );
        TestEngine.addSpamProtectParams( trip );
        trip.addParameter( "text", "test value" );

        // Verify that we got the ActionBean...
        trip.execute( "test" );
        TestActionBean bean = trip.getActionBean( TestActionBean.class );
        assertEquals( null, bean.getPage() );

        // ...and that we passed the inspection
        assertEquals( 0, trip.getValidationErrors().size() );
        assertFalse( SpamProtectTag.isSpamDetected( bean.getContext() ) );
        assertEquals( null, trip.getDestination() );
    }
}
