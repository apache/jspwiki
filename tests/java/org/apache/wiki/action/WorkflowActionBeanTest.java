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

import java.security.Principal;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.workflow.*;

public class WorkflowActionBeanTest extends TestCase
{
    TestEngine m_engine;

    Decision m_decision;

    Workflow m_workflow;

    public void setUp() throws Exception
    {
        super.setUp();

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

        WikiSession session = m_engine.janneSession();
        m_workflow = new Workflow( "workflow.key", new WikiPrincipal( "Administrator", WikiPrincipal.FULL_NAME ) );
        m_workflow.setWorkflowManager( m_engine.getWorkflowManager() );
        m_decision = new SimpleDecision( m_workflow, "decision.key", session.getUserPrincipal() );
        m_workflow.setFirstStep( m_decision );
        m_engine.getWorkflowManager().start( m_workflow );
    }

    /**
     * Verifies that a workflow owner who has created a workflow can see it when
     * the "view" event executes.
     * 
     * @throws Exception
     */
    public void testOwnerView() throws Exception
    {
        // Start with the Admin user, who owns a workflow
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, WorkflowActionBean.class );

        // View the workflows
        trip.execute( "view" );

        // Verify we are directed to the template page
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/templates/default/Workflow.jsp", trip.getDestination() );

        // Verify that Admin owns 1 Workflow
        List<Workflow> workflows = bean.getWorkflows();
        assertNotNull( workflows );
        assertEquals( 1, workflows.size() );
        assertEquals( m_workflow, workflows.get( 0 ) );
        Principal userPrincipal = bean.getContext().getWikiSession().getUserPrincipal();
        assertEquals( userPrincipal, m_workflow.getOwner() );
        assertNotSame( userPrincipal, m_workflow.getCurrentActor() );

        // Verify that Admin has no Decisions in his queue
        List<Decision> decisions = bean.getDecisions();
        assertNotNull( decisions );
        assertEquals( 0, decisions.size() );
    }

    public void testAbortNoParams() throws Exception
    {
        // Start with the Admin user, who owns a workflow
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, WorkflowActionBean.class );

        // Attempt to abort the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.execute( "abort" );

        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors validationErrors = bean.getContext().getValidationErrors();
        assertEquals( 1, validationErrors.size() );
        assertTrue( validationErrors.hasFieldErrors() );
        List<ValidationError> errors = validationErrors.get( "id" );
        assertEquals( 1, errors.size() );
    }

    /**
     * Ensures that a user cannot abort someone elses's workflow
     * 
     * @throws Exception
     */
    public void testAbortWrongUser() throws Exception
    {
        // Start with Janne, who is NOT a workflow owner
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, WorkflowActionBean.class );

        // Attempt to abort the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.addParameter( "id", String.valueOf( m_workflow.getId() ) );
        trip.execute( "abort" );

        // The event should execute without error, but nothing should actually
        // happen...
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        assertEquals( 0, bean.getContext().getValidationErrors().size() );
        assertEquals( "/Workflow.jsp?view=", trip.getDestination() );

        // Verify that the workflow is still running and was NOT aborted
        WorkflowManager mgr = m_engine.getWorkflowManager();
        List<Workflow> workflows = mgr.getCompletedWorkflows();
        assertEquals( 0, workflows.size() );
        assertNotNull( null, m_workflow.getCurrentStep() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
    }

    public void testDecideNoParams() throws Exception
    {
        // Start with Janne, who has a pending decision
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, WorkflowActionBean.class );

        // Try to complete the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.execute( "decide" );

        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors validationErrors = bean.getContext().getValidationErrors();
        assertEquals( 2, validationErrors.size() );
        assertTrue( validationErrors.hasFieldErrors() );
        List<ValidationError> errors = validationErrors.get( "id" );
        assertEquals( 1, errors.size() );
        errors = validationErrors.get( "outcome" );
        assertEquals( 1, errors.size() );
    }

    /**
     * Ensures that a user cannot make a decision on a workflow when they aren't
     * the current Actor.
     * 
     * @throws Exception
     */
    public void testDecideWrongUser() throws Exception
    {
        // Start with Admin, who does NOT have a pending decision
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, WorkflowActionBean.class );

        // Try to complete the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.addParameter( "id", String.valueOf( m_decision.getId() ) );
        trip.addParameter( "outcome", Outcome.DECISION_APPROVE.getMessageKey() );
        trip.execute( "decide" );

        // The event should execute without error, but nothing should actually
        // happen...
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        assertEquals( 0, bean.getContext().getValidationErrors().size() );
        assertEquals( "/Workflow.jsp?view=", trip.getDestination() );

        // Verify that the workflow is still running and was NOT completed
        WorkflowManager mgr = m_engine.getWorkflowManager();
        List<Workflow> workflows = mgr.getCompletedWorkflows();
        assertEquals( 0, workflows.size() );
        assertNotNull( null, m_workflow.getCurrentStep() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
    }

    public void testAbort() throws Exception
    {
        // Start with the Admin user, who owns a workflow
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, WorkflowActionBean.class );

        // Attempt to abort the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.addParameter( "id", String.valueOf( m_workflow.getId() ) );
        trip.execute( "abort" );

        // Verify we are directed to the view page
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Workflow.jsp?view=", trip.getDestination() );

        // Verify that Admin has no more current workflows
        List<Workflow> workflows = bean.getWorkflows();
        assertNotNull( workflows );
        assertEquals( 0, workflows.size() );

        // Verify that the workflow was successfully aborted
        WorkflowManager mgr = m_engine.getWorkflowManager();
        workflows = mgr.getCompletedWorkflows();
        assertEquals( 1, workflows.size() );
        assertEquals( null, m_workflow.getCurrentStep() );
        assertEquals( m_workflow, workflows.get( 0 ) );
        assertTrue( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
    }

    public void testDecide() throws Exception
    {
        // Start with Janne, who has a pending decision
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, WorkflowActionBean.class );

        // Try to complete the workflow
        assertTrue( m_workflow.isStarted() );
        assertFalse( m_workflow.isAborted() );
        assertFalse( m_workflow.isCompleted() );
        trip.addParameter( "id", String.valueOf( m_decision.getId() ) );
        trip.addParameter( "outcome", Outcome.DECISION_APPROVE.getMessageKey() );
        trip.execute( "decide" );

        // Verify we are directed to the view page
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Workflow.jsp?view=", trip.getDestination() );

        // Verify that Janne has no more current decisions
        List<Decision> decisions = bean.getDecisions();
        assertNotNull( decisions );
        assertEquals( 0, decisions.size() );

        // Verify that the workflow was successfully approved
        WorkflowManager mgr = m_engine.getWorkflowManager();
        List<Workflow> workflows = mgr.getCompletedWorkflows();
        assertEquals( 1, workflows.size() );
        assertEquals( null, m_workflow.getCurrentStep() );
        assertEquals( m_workflow, workflows.get( 0 ) );
        assertFalse( m_workflow.isAborted() );
        assertTrue( m_workflow.isCompleted() );
    }

    /**
     * Verifies that a user who has pending decisions in his queue can see them
     * when the "view" event executes.
     * 
     * @throws Exception
     */
    public void testActorView() throws Exception
    {
        // Start with Janne, who has a pending decision
        MockRoundtrip trip = m_engine.authenticatedTrip( Users.JANNE, Users.JANNE_PASS, WorkflowActionBean.class );

        // View the workflows
        trip.execute( "view" );

        // Verify we are directed to the template page
        WorkflowActionBean bean = trip.getActionBean( WorkflowActionBean.class );
        ValidationErrors errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/templates/default/Workflow.jsp", trip.getDestination() );

        // Verify that Janne does not own any Workflows
        List<Workflow> workflows = bean.getWorkflows();
        assertNotNull( workflows );
        assertEquals( 0, workflows.size() );

        // Verify that Janne has one Decision in his queue
        List<Decision> decisions = bean.getDecisions();
        assertNotNull( decisions );
        assertEquals( 1, decisions.size() );
        assertEquals( m_decision, decisions.get( 0 ) );
        Principal userPrincipal = bean.getContext().getWikiSession().getUserPrincipal();
        assertEquals( userPrincipal, m_decision.getActor() );
        assertNotSame( userPrincipal, m_decision.getOwner() );
        assertNotSame( userPrincipal, m_decision.getWorkflow().getOwner() );
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    public static Test suite()
    {
        return new TestSuite( WorkflowActionBeanTest.class );
    }
}
