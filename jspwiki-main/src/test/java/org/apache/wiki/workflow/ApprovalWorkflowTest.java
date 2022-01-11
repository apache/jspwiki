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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import static org.apache.wiki.TestEngine.with;


public class ApprovalWorkflowTest {

    static TestEngine m_engine = TestEngine.build( with( "jspwiki.approver.workflow.saveWikiPage", "Admin" ),
                                                   with( "jspwiki.approver.workflow.approvalWorkflow", Users.JANNE ) );
    static WorkflowManager m_wm = m_engine.getManager( WorkflowManager.class );
    static DecisionQueue m_dq = m_wm.getDecisionQueue();
    static WorkflowBuilder m_builder = WorkflowBuilder.getBuilder( m_engine );

    @Test
    public void testBuildApprovalWorkflow() throws WikiException {
        final Principal submitter = new WikiPrincipal( "Submitter" );
        final String workflowApproverKey = "workflow.approvalWorkflow";
        final Task prepTask = new TestPrepTask( "task.preSaveWikiPage" );
        final String decisionKey = "decision.saveWikiPage";
        final Fact[] facts = new Fact[3];
        facts[ 0 ] = new Fact( "fact1", 1 );
        facts[ 1 ] = new Fact( "fact2", "A factual String" );
        facts[ 2 ] = new Fact( "fact3", Outcome.DECISION_ACKNOWLEDGE );
        final Task completionTask = new TestPrepTask( "task.saveWikiPage" );
        final String rejectedMessageKey = "notification.saveWikiPage.reject";

        final Workflow w = m_builder.buildApprovalWorkflow( submitter, workflowApproverKey,
                                                            prepTask, decisionKey, facts,
                                                            completionTask, rejectedMessageKey );

        // Check to see if the workflow built correctly
        Assertions.assertFalse( w.isStarted() || w.isCompleted() || w.isAborted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( "workflow.approvalWorkflow", w.getMessageKey() );
        Assertions.assertEquals( Workflow.CREATED, w.getCurrentState() );
        Assertions.assertEquals( new WikiPrincipal("Submitter"), w.getOwner() );
        Assertions.assertEquals( 0, w.getHistory().size() );

        // Our dummy "task complete" attributes should still be null
        Assertions.assertNull( w.getAttribute( "task.preSaveWikiPage") );
        Assertions.assertNull( w.getAttribute( "task.saveWikiPage") );

        // Start the workflow
        w.start( null );

        // Presave complete attribute should be set now, and current step should be Decision
        final Step decision = w.getCurrentStep();
        Assertions.assertTrue( decision instanceof Decision );
        Assertions.assertEquals( 2, w.getHistory().size() );
        Assertions.assertEquals( prepTask, w.getHistory().get( 0 ) );
        Assertions.assertTrue( w.getHistory().get( 1 ) instanceof Decision );
        Assertions.assertNotNull( w.getAttribute( "task.preSaveWikiPage") );
        Assertions.assertEquals( new WikiPrincipal( Users.JANNE ), decision.getActor() );
        Assertions.assertEquals( decisionKey, decision.getMessageKey() );
        final List< Fact > decisionFacts = ((Decision)decision).getFacts();
        Assertions.assertEquals( 3, decisionFacts.size() );
        Assertions.assertEquals( facts[0], decisionFacts.get(0) );
        Assertions.assertEquals( facts[1], decisionFacts.get(1) );
        Assertions.assertEquals( facts[2], decisionFacts.get(2) );

        // Check that our predecessor/successor relationships are ok
        Assertions.assertEquals( decision, prepTask.getSuccessor( Outcome.STEP_COMPLETE ) );
        Assertions.assertNull( prepTask.getSuccessor( Outcome.STEP_ABORT ) );
        Assertions.assertNull( prepTask.getSuccessor( Outcome.STEP_CONTINUE ) );
        Assertions.assertNull( decision.getSuccessor( Outcome.DECISION_ACKNOWLEDGE ) );
        Assertions.assertNull( decision.getSuccessor( Outcome.DECISION_HOLD ) );
        Assertions.assertNull( decision.getSuccessor( Outcome.DECISION_REASSIGN ) );
        Assertions.assertEquals( completionTask, decision.getSuccessor( Outcome.DECISION_APPROVE ) );

        // The "deny" notification should use the right key
        final Step notification = decision.getSuccessor( Outcome.DECISION_DENY );
        Assertions.assertNotNull( notification );
        Assertions.assertEquals( rejectedMessageKey, notification.getMessageKey() );
        Assertions.assertTrue( notification instanceof SimpleNotification );

        // Now, approve the Decision and everything should complete
        ((Decision)decision).decide( Outcome.DECISION_APPROVE, null );
        Assertions.assertTrue( w.isCompleted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( 3, w.getHistory().size() );
        Assertions.assertEquals( completionTask, w.getHistory().get( 2 ) );
        Assertions.assertTrue( completionTask.isCompleted() );
        Assertions.assertEquals( Outcome.STEP_COMPLETE, completionTask.getOutcome() );
    }

    @Test
    public void testBuildApprovalWorkflowDeny() throws WikiException {
        final Principal submitter = new WikiPrincipal( "Submitter" );
        final String workflowApproverKey = "workflow.approvalWorkflow";
        final Task prepTask = new TestPrepTask( "task.preSaveWikiPage" );
        final String decisionKey = "decision.saveWikiPage";
        final Fact[] facts = new Fact[ 3 ];
        facts[ 0 ] = new Fact( "fact1", 1 );
        facts[ 1 ] = new Fact( "fact2", "A factual String" );
        facts[ 2 ] = new Fact( "fact3", Outcome.DECISION_ACKNOWLEDGE );
        final Task completionTask = new TestPrepTask( "task.saveWikiPage" );
        final String rejectedMessageKey = "notification.saveWikiPage.reject";

        final Workflow w = m_builder.buildApprovalWorkflow( submitter, workflowApproverKey,
                                                            prepTask, decisionKey, facts,
                                                            completionTask, rejectedMessageKey );

        // Start the workflow
        w.start( null );

        // Now, deny the Decision and the submitter should see a notification
        Step step = w.getCurrentStep();
        Assertions.assertTrue( step instanceof Decision );
        final Decision decision = (Decision)step;
        decision.decide( Outcome.DECISION_DENY, null );
        Assertions.assertFalse( w.isCompleted() );

        // Check that the notification is ok, then acknowledge it
        step = w.getCurrentStep();
        Assertions.assertTrue( step instanceof SimpleNotification );
        Assertions.assertEquals( rejectedMessageKey, step.getMessageKey() );
        final SimpleNotification notification = (SimpleNotification)step;
        notification.acknowledge( null );

        // Workflow should be complete now
        Assertions.assertTrue( w.isCompleted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( 3, w.getHistory().size() );
        Assertions.assertEquals( notification, w.getHistory().get( 2 ) );
    }

    @Test
    public void testSaveWikiPageWithApproval() throws WikiException {
        // Create a sample test page and try to save it
        final String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        final String text = "This is a test!";

        // Expected exception...
        Assertions.assertThrows( DecisionRequiredException.class, () -> m_engine.saveTextAsJanne( pageName, text ) );

        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        Assertions.assertFalse( m_engine.getManager( PageManager.class ).wikiPageExists( pageName ) );

        // Second, GroupPrincipal Admin should see a Decision in its queue
        Collection< Decision > decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals( 1, decisions.size() );

        // Now, approve the decision and it should go away, and page should appear.
        final Context context = Wiki.context().create( m_engine, Wiki.contents().page( m_engine, pageName ) );
        final Decision decision = decisions.iterator().next();
        decision.decide( Outcome.DECISION_APPROVE, context );
        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( pageName ) );
        decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals( 0, decisions.size() );

        // Delete the page we created
        m_engine.getManager( PageManager.class ).deletePage( pageName );
    }

    @Test
    public void testSaveWikiPageWithRejection() throws WikiException {
        // Create a sample test page and try to save it
        final String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        final String text = "This is a test!";
        try {
            m_engine.saveTextAsJanne( pageName, text );
        } catch( final DecisionRequiredException e ) {
            // Swallow exception, because it is expected...
        }

        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        Assertions.assertFalse( m_engine.getManager( PageManager.class ).wikiPageExists( pageName ) );

        // ...and there should be a Decision in GroupPrincipal Admin's queue
        Collection< Decision > decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals( 1, decisions.size() );

        // Now, DENY the decision and the page should still not exist...
        Decision decision = decisions.iterator().next();
        decision.decide( Outcome.DECISION_DENY, null );
        Assertions.assertFalse( m_engine.getManager( PageManager.class ).wikiPageExists( pageName ) );

        // ...but there should also be a notification decision in Janne's queue
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        Assertions.assertEquals( 1, decisions.size() );
        decision = decisions.iterator().next();
        Assertions.assertEquals( WorkflowManager.WF_WP_SAVE_REJECT_MESSAGE_KEY, decision.getMessageKey() );

        // Once Janne disposes of the notification, his queue should be empty
        decision.decide( Outcome.DECISION_ACKNOWLEDGE, null );
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        Assertions.assertEquals( 0, decisions.size() );
    }

    @Test
    public void testSaveWikiPageWithException() {
        // Add a PageFilter that rejects all save attempts
        final FilterManager fm = m_engine.getManager( FilterManager.class );
        fm.addPageFilter( new AbortFilter(), 0 );

        // Create a sample test page and try to save it
        final String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        final String text = "This is a test!";
        final FilterException fe = Assertions.assertThrows( FilterException.class, () -> m_engine.saveTextAsJanne( pageName, text ) );
        Assertions.assertEquals( "Page save aborted.", fe.getMessage() );
    }

    /**
     * Sample "prep task" that sets an attribute in the workflow indicating that it ran successfully,
     */
    public static class TestPrepTask extends Task {

        private static final long serialVersionUID = 1L;

        public TestPrepTask( final String messageKey ) {
            super( messageKey );
        }

        @Override
        public Outcome execute( final Context context ) {
            getWorkflowContext().put( getMessageKey(), "Completed" );
            setOutcome( Outcome.STEP_COMPLETE );
            return Outcome.STEP_COMPLETE;
        }

    }

    /**
     * Dummy PageFilter that always throws a FilterException during preSave operations.
     */
    public static class AbortFilter extends BasePageFilter {
        @Override
        public String preSave( final Context wikiContext, final String content ) throws FilterException {
            throw new FilterException( "Page save aborted." );
        }
    }

}
