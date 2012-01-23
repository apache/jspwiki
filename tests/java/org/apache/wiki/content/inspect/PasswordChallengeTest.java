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

package org.apache.wiki.content.inspect;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.ViewActionBean;
import org.apache.wiki.auth.Users;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 */
public class PasswordChallengeTest extends TestCase
{
    private TestEngine m_engine;

    public static Test suite()
    {
        return new TestSuite( PasswordChallengeTest.class );
    }

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

    public void testNotAuthenticated() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        trip.addParameter( "j_password", Users.JANNE_PASS );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        WikiActionBeanContext context = bean.getContext();
        PasswordChallenge challenge = new PasswordChallenge();
        
        // No j_password parameter: should be false
        assertFalse( challenge.check( context ) );
    }
    
    public void testNoPasswordParameter() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, ViewActionBean.class );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        WikiActionBeanContext context = bean.getContext();
        PasswordChallenge challenge = new PasswordChallenge();
        
        // No j_password parameter: should be false
        assertFalse( challenge.check( context ) );
    }
    
    public void testPassword() throws Exception
    {
        // Start with an authenticated user
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, ViewActionBean.class );
        trip.addParameter( "j_password", Users.JANNE_PASS );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        WikiActionBeanContext context = bean.getContext();
        PasswordChallenge challenge = new PasswordChallenge();
        
        // No j_password parameter: should be false
        assertTrue( challenge.check( context ) );
    }
}
