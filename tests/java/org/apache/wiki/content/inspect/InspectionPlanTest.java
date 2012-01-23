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

import org.apache.wiki.TestEngine;
import org.apache.wiki.content.inspect.Finding.Result;

/**
 */
public class InspectionPlanTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( InspectionPlanTest.class );
    }

    Properties m_props = new Properties();

    TestEngine m_engine;

    public InspectionPlanTest( String s )
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

    public void testAddInspectors() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        assertNotNull( plan.getInspectors() );
        assertEquals( 0, plan.getInspectors().length );
        Inspector pass = new InspectionTest.TestInspector( Topic.SPAM, Result.PASSED );
        Inspector fail = new InspectionTest.TestInspector( Topic.SPAM, Result.FAILED );
        Inspector noEffect = new InspectionTest.TestInspector( Topic.SPAM, Result.NO_EFFECT );
        plan.addInspector( pass, 4 );
        plan.addInspector( fail, 2 );
        plan.addInspector( noEffect, 1 );
        
        // Make sure the Inspectors are returned in the correct order
        Inspector[] inspectors = plan.getInspectors();
        assertEquals( pass, inspectors[0] );
        assertEquals( fail, inspectors[1] );
        assertEquals( noEffect, inspectors[2] );
    }
    
    public void testGetWeight() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        assertNotNull( plan.getInspectors() );
        assertEquals( 0, plan.getInspectors().length );
        Inspector pass = new InspectionTest.TestInspector( Topic.SPAM, Result.PASSED );
        Inspector fail = new InspectionTest.TestInspector( Topic.SPAM, Result.FAILED );
        Inspector noEffect = new InspectionTest.TestInspector( Topic.SPAM, Result.NO_EFFECT );
        plan.addInspector( pass, 4 );
        plan.addInspector( fail, 2 );
        plan.addInspector( noEffect, 1 );
        
        // Make sure each Inspector's weight was stored correctly
        assertEquals( 4.0f, plan.getWeight( pass ) );
        assertEquals( 2.0f, plan.getWeight( fail ) );
        assertEquals( 1.0f, plan.getWeight( noEffect ) );
    }

    public void testGetProperties()
    {
        int nProps = m_props.size();
        InspectionPlan ctx = new InspectionPlan( m_props );
        assertNotNull( ctx.getProperties() );
        assertEquals( nProps, ctx.getProperties().size() );
        assertEquals( m_props, ctx.getProperties() );
    }
}
