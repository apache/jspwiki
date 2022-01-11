/*
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
package org.apache.wiki.workflow;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;


public class WorkflowTest {

    TestEngine engine = TestEngine.build();

    // listen to workflow events
    WorkflowManager workflowsEventListener = new DefaultWorkflowManager();

    Workflow w;

    Task initTask;

    Decision decision;

    Task finishTask;

    String ATTR = "TestAttribute";

    @BeforeEach
    public void setUp() throws Exception {
        workflowsEventListener.initialize( engine, TestEngine.getTestProperties() );

        // Create workflow; owner is test user
        w = new Workflow( "workflow.myworkflow", new WikiPrincipal( "Owner1" ) );

        // Create custom initialization task
        initTask = new TaskTest.NormalTask( w );

        // Create finish task
        finishTask = new TaskTest.NormalTask( w );

        // Create an intermetidate decision step
        final Principal actor = new GroupPrincipal( "Admin" );
        decision = new SimpleDecision( w.getId(), w.getAttributes(), "decision.AdminDecision", actor );

        // Hook the steps together
        initTask.addSuccessor( Outcome.STEP_COMPLETE, decision );
        decision.addSuccessor( Outcome.DECISION_APPROVE, finishTask );

        // Stash page name as message attribute
        w.addMessageArgument( "MyPage" );

        // Set workflow's first step
        w.setFirstStep( initTask );
    }

    @Test
    public void testWorkflow() {
        // Make sure everything is set to their proper default values
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertNull( w.getCurrentActor() );
        Assertions.assertEquals( 0, w.getHistory().size() );
        Assertions.assertEquals( new WikiPrincipal( "Owner1" ), w.getOwner() );
        Assertions.assertEquals( Workflow.CREATED, w.getCurrentState() );
        Assertions.assertEquals( Step.TIME_NOT_SET, w.getStartTime() );
        Assertions.assertEquals( Step.TIME_NOT_SET, w.getEndTime() );
    }

    @Test
    public void testGetSetAttribute() {
        Assertions.assertNull( w.getAttribute( ATTR ) );
        w.setAttribute( ATTR, "Test String" );
        Assertions.assertNotNull( w.getAttribute( ATTR ) );
        Assertions.assertEquals( "Test String", w.getAttribute( ATTR ) );
    }

    @Test
    public void testGetMessageArgs() throws WikiException {
        Object[] args;

        // Before start, arg1=Owner1, arg2=- (no current actor), arg3=MyPage
        args = w.getMessageArguments();
        Assertions.assertEquals( "Owner1", args[ 0 ] );
        Assertions.assertEquals( "-", args[ 1 ] );
        Assertions.assertEquals( "MyPage", args[ 2 ] );

        // After start (at Decision), arg1=Owner1, arg2=Admin, arg3=MyPage
        w.start( null );
        args = w.getMessageArguments();
        Assertions.assertEquals( "Owner1", args[ 0 ] );
        Assertions.assertEquals( "Admin", args[ 1 ] );
        Assertions.assertEquals( "MyPage", args[ 2 ] );

        // After end, arg1=Owner1, arg2=-, arg3=MyPage
        decision.decide( Outcome.DECISION_APPROVE, null );
        args = w.getMessageArguments();
        Assertions.assertEquals( "Owner1", args[ 0 ] );
        Assertions.assertEquals( "-", args[ 1 ] );
        Assertions.assertEquals( "MyPage", args[ 2 ] );
    }

    @Test
    public void testGetMessageArgObjects() {
        // Try passing some valid object types: Date, Number
        w.addMessageArgument( new Date() );
        w.addMessageArgument( 1 );
        w.addMessageArgument( 2d );
        w.addMessageArgument( new BigDecimal( "3.14" ) );

        // Try passing an invalid one: e.g., a Workflow (it should Assertions.fail)
        try {
            w.addMessageArgument( w );
        } catch( final IllegalArgumentException e ) {
            // Swallow
            return;
        }
        // We should never get here
        Assertions.fail( "Illegal argument passed..." );
    }

    @Test
    public void testGetMessageKey() {
        Assertions.assertEquals( "workflow.myworkflow", w.getMessageKey() );
    }

    @Test
    public void testGetOwner() {
        Assertions.assertEquals( new WikiPrincipal( "Owner1" ), w.getOwner() );
    }

    @Test
    public void testStart() throws WikiException {
        Assertions.assertFalse( w.isStarted() );
        w.start( null );
        Assertions.assertTrue( w.isStarted() );
    }

    @Test
    public void testWaitstate() throws WikiException {
        w.start( null );

        // Default workflow should have hit the Decision step and put itself
        // into WAITING
        Assertions.assertEquals( Workflow.WAITING, w.getCurrentState() );
    }

    @Test
    public void testRestart() throws WikiException {
        w.start( null );

        // Default workflow should have hit the Decision step and put itself
        // into WAITING
        Assertions.assertEquals( Workflow.WAITING, w.getCurrentState() );
        w.restart( null );
        Assertions.assertEquals( Workflow.WAITING, w.getCurrentState() );
    }

    @Test
    public void testAbortBeforeStart() throws WikiException {
        // Workflow hasn't been started yet
        Assertions.assertFalse( w.isAborted() );
        w.abort( null );
        Assertions.assertTrue( w.isAborted() );

        // Try to start anyway
        try {
            w.start( null );
        } catch( final IllegalStateException e ) {
            // Swallow
            return;
        }
        // We should never get here
        Assertions.fail( "Workflow allowed itself to be started even though it was aborted!" );
    }

    @Test
    public void testAbortDuringWait() throws WikiException {
        // Start workflow, then abort while in WAITING state
        Assertions.assertFalse( w.isAborted() );
        w.start( null );
        w.abort( null );
        Assertions.assertTrue( w.isAborted() );

        // Try to restart anyway
        try {
            w.restart( null );
        } catch( final IllegalStateException e ) {
            // Swallow
            return;
        }
        // We should never get here
        Assertions.fail( "Workflow allowed itself to be re-started even though it was aborted!" );
    }

    @Test
    public void testAbortAfterCompletion() throws WikiException {
        // Start workflow, then abort after completion
        Assertions.assertFalse( w.isAborted() );
        w.start( null );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );

        // Try to abort anyway
        try {
            w.abort( null );
            Assertions.assertTrue( w.isAborted() );
        } catch( final IllegalStateException e ) {
            // Swallow
            return;
        }
        // We should never get here
        Assertions.fail( "Workflow allowed itself to be aborted even though it was completed!" );
    }

    @Test
    public void testCurrentState() throws WikiException {
        Assertions.assertEquals( Workflow.CREATED, w.getCurrentState() );
        w.start( null );
        Assertions.assertEquals( Workflow.WAITING, w.getCurrentState() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertEquals( Workflow.COMPLETED, w.getCurrentState() );
    }

    @Test
    public void testCurrentStep() throws WikiException {
        Assertions.assertNull( w.getCurrentStep() );
        w.start( null );

        // Workflow stops at the decision step
        Assertions.assertEquals( decision, w.getCurrentStep() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );

        // After we decide, it blows through step 3 and leaves us with a null step (done)
        Assertions.assertNull( w.getCurrentStep() );
    }

    @Test
    public void testPreviousStep() throws WikiException {
        // If not started, no previous steps available for anything
        Assertions.assertNull( w.getPreviousStep() );
        Assertions.assertNull( w.previousStep( initTask ) );
        Assertions.assertNull( w.previousStep( decision ) );
        Assertions.assertNull( w.previousStep( finishTask ) );

        // Once we start, initTask and decisions' predecessors are known, but finish task is indeterminate
        w.start( null );
        Assertions.assertNull( w.previousStep( initTask ) );
        Assertions.assertEquals( initTask, w.previousStep( decision ) );
        Assertions.assertNull( w.previousStep( finishTask ) );

        // Once we decide, the finish task returns the correct predecessor
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertNull( w.previousStep( initTask ) );
        Assertions.assertEquals( initTask, w.previousStep( decision ) );
        Assertions.assertEquals( decision, w.previousStep( finishTask ) );
    }

    @Test
    public void testCurrentActor() throws WikiException {
        // Before starting, actor should be null
        Assertions.assertNull( w.getCurrentActor() );

        // After starting, actor should be GroupPrincipal Admin
        w.start( null );
        Assertions.assertEquals( new GroupPrincipal( "Admin" ), w.getCurrentActor() );

        // After decision, actor should be null again
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertNull( w.getCurrentActor() );
    }

    @Test
    public void testHistory() throws WikiException {
        Assertions.assertEquals( 0, w.getHistory().size() );
        w.start( null );
        Assertions.assertEquals( 2, w.getHistory().size() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertEquals( 3, w.getHistory().size() );
    }

    @Test
    public void testGetStartTime() throws WikiException {
        // Start time should be not be set until we start the workflow
        Assertions.assertEquals( Step.TIME_NOT_SET, w.getStartTime() );
        w.start( null );
        Assertions.assertNotSame( Step.TIME_NOT_SET, w.getStartTime() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertNotSame( Step.TIME_NOT_SET, w.getStartTime() );
    }

    @Test
    public void testGetEndTime() throws WikiException {
        // End time should be not set until we finish all 3 steps
        Assertions.assertEquals( Step.TIME_NOT_SET, w.getEndTime() );
        w.start( null );
        Assertions.assertEquals( Step.TIME_NOT_SET, w.getEndTime() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertNotSame( Step.TIME_NOT_SET, w.getEndTime() );
    }

    @Test
    public void testIsCompleted() throws WikiException {
        // Workflow isn't completed until we finish all 3 steps
        Assertions.assertFalse( w.isCompleted() );
        w.start( null );
        Assertions.assertFalse( w.isCompleted() );
        final Decision d = ( Decision )w.getCurrentStep();
        d.decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertTrue( w.isCompleted() );
    }

    @Test
    public void testIsStarted() throws WikiException {
        Assertions.assertFalse( w.isStarted() );
        w.start( null );
        Assertions.assertTrue( w.isStarted() );
    }

    @Test
    public void testStartTwice() throws WikiException {
        w.start( null );
        Assertions.assertThrows( IllegalStateException.class, () -> w.start( null ) );
    }

    @Test
    public void testSetId() {
        w.setId( 1001 );
        Assertions.assertEquals( 1001, w.getId() );
    }

}
