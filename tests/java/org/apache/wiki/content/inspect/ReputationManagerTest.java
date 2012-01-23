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

import javax.servlet.http.HttpServletRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.content.inspect.ReputationManager.Host;

/**
 */
public class ReputationManagerTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( ReputationManagerTest.class );
    }

    Properties m_props = new Properties();

    TestEngine m_engine;

    public ReputationManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );
        m_props.setProperty( InspectionPlan.PROP_BANTIME, "1" );
        m_engine = new TestEngine( m_props );
    }

    public void tearDown() throws Exception
    {
        m_engine.shutdown();
    }

    public void testAddModifiers() throws Exception
    {
        HttpServletRequest req = new MockHttpServletRequest( "/JSPWiki", "/servlet" );
        InspectionPlan plan = new InspectionPlan( m_props );
        ReputationManager reputationManager = plan.getReputationManager();
        Change change = Change.getChange( "page", "changed text" );
        reputationManager.addModifier( req, change );
        assertEquals( 1, reputationManager.getModifiers().length );
        Host host = reputationManager.getModifiers()[0];
        assertEquals( "127.0.0.1", host.getAddress() );
        assertNotNull( host.getAddedTime() );
        assertNotNull( host.getReleaseTime() );
        assertNotSame( host.getAddedTime(), host.getReleaseTime() );
        assertEquals( "changed text", host.getChange() );

        req = new MockHttpServletRequest( "/JSPWiki", "/servlet" );
        change = Change.getChange( "page", "more changed text" );
        reputationManager.addModifier( req, change );
        assertEquals( 2, reputationManager.getModifiers().length );
    }

    public void testBanHost() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        ReputationManager reputationManager = plan.getReputationManager();
        assertNotNull( reputationManager );
        assertEquals( 1, reputationManager.getBanTime() );
        HttpServletRequest req = new MockHttpServletRequest( "/JSPWiki", "/servlet" );
        assertEquals( ReputationManager.NOT_BANNED, reputationManager.getRemainingBan( req.getRemoteAddr() ) );
        reputationManager.banHost( req );

        // IP address should be banned for at 1 minute
        assertNotSame( ReputationManager.NOT_BANNED, reputationManager.getRemainingBan( req.getRemoteAddr() ) );
    }

    public void testBanHostInstantExpiry() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.setProperty( InspectionPlan.PROP_BANTIME, "0" );
        InspectionPlan plan = new InspectionPlan( props );
        ReputationManager reputationManager = plan.getReputationManager();
        assertEquals( 0, reputationManager.getBanTime() );
        assertNotNull( reputationManager );
        HttpServletRequest req = new MockHttpServletRequest( "/JSPWiki", "/servlet" );
        assertEquals( ReputationManager.NOT_BANNED, reputationManager.getRemainingBan( req.getRemoteAddr() ) );
        reputationManager.banHost( req );

        // Because the ban expires immediately, IP address should disappear from
        // the list
        assertEquals( ReputationManager.NOT_BANNED, reputationManager.getRemainingBan( req.getRemoteAddr() ) );
    }
}
