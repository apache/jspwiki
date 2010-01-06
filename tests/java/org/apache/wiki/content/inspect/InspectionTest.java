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
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.inspect.Finding.Result;

/**
 */
public class InspectionTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite( InspectionTest.class );
    }

    Properties m_props = new Properties();

    TestEngine m_engine;

    public InspectionTest( String s )
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

    /**
     * Dummy listener class that can be configured to interrupt an Inspection.
     */
    static class TestInspectionListener implements InspectionListener
    {
        private final boolean m_interrupt;

        private boolean m_executed = false;

        public TestInspectionListener( boolean interrupt )
        {
            m_interrupt = interrupt;
        }

        public void changedScore( Inspection inspection, Finding score ) throws InspectionInterruptedException
        {
            m_executed = true;
            if( m_interrupt )
            {
                throw new InspectionInterruptedException( this, "Interrupted by TestInspectionListener." );
            }
        }

        public boolean isExecuted()
        {
            return m_executed;
        }
    }

    /**
     * Dummy inspector class that returns a predictable Result.
     */
    static class TestInspector implements Inspector
    {
        private final Result m_result;

        private final Topic m_topic;

        public TestInspector( Topic topic, Result result )
        {
            m_topic = topic;
            m_result = result;
        }

        public void initialize( InspectionPlan config )
        {
        }

        public Finding[] inspect( Inspection inspection, Change change )
        {
            return new Finding[] { new Finding( m_topic, m_result, m_result.toString() ) };
        }

        public Scope getScope()
        {
            return Scope.FIELD;
        }
    }

    public void testInspectOneTopic() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        assertNotNull( plan.getInspectors() );
        assertEquals( 0, plan.getInspectors().length );
        Inspector pass = new TestInspector( Topic.SPAM, Result.PASSED );
        Inspector fail = new TestInspector( Topic.SPAM, Result.FAILED );
        Inspector noEffect = new TestInspector( Topic.SPAM, Result.NO_EFFECT );
        plan.addInspector( pass, 4 );
        plan.addInspector( fail, 2 );
        plan.addInspector( noEffect, 1 );

        // Run the inspection
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        Inspection inspection = new Inspection( context, plan );
        inspection.inspect( Change.getChange( "page", "Sample text" ) );

        // Result should be 2: +4 -2 +0
        assertEquals( 2.0f, inspection.getScore( Topic.SPAM ) );
    }

    public void testInspectTwoTopics() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        assertNotNull( plan.getInspectors() );
        assertEquals( 0, plan.getInspectors().length );
        Inspector pass = new TestInspector( Topic.SPAM, Result.PASSED );
        Inspector fail = new TestInspector( Topic.SPAM, Result.FAILED );
        Inspector noEffect = new TestInspector( Topic.SPAM, Result.NO_EFFECT );
        Inspector passTopic2 = new TestInspector( new Topic( "Topic2" ), Result.PASSED );
        plan.addInspector( pass, 4 );
        plan.addInspector( fail, 2 );
        plan.addInspector( noEffect, 1 );
        plan.addInspector( passTopic2, 8 );

        // Run the inspection
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        Inspection inspection = new Inspection( context, plan );
        inspection.inspect( Change.getChange( "page", "Sample text" ) );

        // Result should be 2: +4 -2 +0
        assertEquals( 2.0f, inspection.getScore( Topic.SPAM ) );

        // Result should be 8 for other topic
        assertEquals( 8.0f, inspection.getScore( new Topic( "Topic2" ) ) );
    }

    public void testGetFindings() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        assertNotNull( plan.getInspectors() );
        assertEquals( 0, plan.getInspectors().length );
        Inspector pass = new TestInspector( Topic.SPAM, Result.PASSED );
        Inspector fail = new TestInspector( Topic.SPAM, Result.FAILED );
        Inspector noEffect = new TestInspector( Topic.SPAM, Result.NO_EFFECT );
        Inspector passTopic2 = new TestInspector( new Topic( "Topic2" ), Result.PASSED );
        plan.addInspector( pass, 4 );
        plan.addInspector( fail, 2 );
        plan.addInspector( noEffect, 1 );
        plan.addInspector( passTopic2, 8 );

        // Run the inspection
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        Inspection inspection = new Inspection( context, plan );
        inspection.inspect( Change.getChange( "page", "Sample text" ) );

        // Verify that the findings were added in order
        Finding[] findings = inspection.getFindings( Topic.SPAM );
        assertEquals( 3, findings.length );
        assertEquals( new Finding( Topic.SPAM, Result.PASSED, Result.PASSED.toString() ), findings[0] );
        assertEquals( new Finding( Topic.SPAM, Result.FAILED, Result.FAILED.toString() ), findings[1] );
        assertEquals( new Finding( Topic.SPAM, Result.NO_EFFECT, Result.NO_EFFECT.toString() ), findings[2] );

        findings = inspection.getFindings( new Topic( "Topic2" ) );
        assertEquals( 1, findings.length );
        assertEquals( new Finding( new Topic( "Topic2" ), Result.PASSED, Result.PASSED.toString() ), findings[0] );

        // No findings at all for bogus topic
        findings = inspection.getFindings( new Topic( "NoFindingsTopic" ) );
        assertEquals( 0, findings.length );
    }

    public void testAddListener() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        Inspector pass = new TestInspector( Topic.SPAM, Result.PASSED );
        Inspector pass2 = new TestInspector( Topic.SPAM, Result.PASSED );
        plan.addInspector( pass, 2 );
        plan.addInspector( pass2, 1 );

        // Configure the inspection with 2 listeners
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        Inspection inspection = new Inspection( context, plan );
        TestInspectionListener listener = new TestInspectionListener( false );
        TestInspectionListener listener2 = new TestInspectionListener( false );
        inspection.addListener( Topic.SPAM, listener );
        inspection.addListener( new Topic( "Topic2" ), listener2 );

        // Run the inspection
        inspection.inspect( Change.getChange( "page", "Sample text" ) );

        // Verify that Spam listener fired, but Topic2 listener did not
        assertTrue( listener.isExecuted() );
        assertFalse( listener2.isExecuted() );

        // Verify the listener did not interrupt execution
        assertEquals( 3.0f, inspection.getScore( Topic.SPAM ) );
    }

    public void testAddInterruptingListener() throws Exception
    {
        InspectionPlan plan = new InspectionPlan( m_props );
        Inspector pass = new TestInspector( Topic.SPAM, Result.PASSED );
        Inspector pass2 = new TestInspector( Topic.SPAM, Result.PASSED );
        plan.addInspector( pass, 2 );
        plan.addInspector( pass2, 1 );

        // Configure the inspection with 2 listeners that interrupt
        WikiPage page = m_engine.getFrontPage( ContentManager.DEFAULT_SPACE );
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        Inspection inspection = new Inspection( context, plan );
        TestInspectionListener listener = new TestInspectionListener( true );
        TestInspectionListener listener2 = new TestInspectionListener( true );
        inspection.addListener( Topic.SPAM, listener );
        inspection.addListener( new Topic( "Topic2" ), listener2 );

        // Run the inspection
        inspection.inspect( Change.getChange( "page", "Sample text" ) );

        // Verify that Spam listener fired, but Topic2 listener did not
        assertTrue( listener.isExecuted() );
        assertFalse( listener2.isExecuted() );

        // Verify the Spam listener interrupted execution
        assertEquals( 2f, inspection.getScore( Topic.SPAM ) );
    }
}
