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
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.auth.Users;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class ApprovalWorkflowTest {

    WorkflowBuilder m_builder;
    TestEngine m_engine;
    WorkflowManager m_wm;
    DecisionQueue m_dq;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        // Explicitly turn on Admin approvals for page saves and our sample approval workflow
        props.put("jspwiki.approver.workflow.saveWikiPage", "Admin");
        props.put( "jspwiki.approver.workflow.approvalWorkflow", Users.JANNE );

        // Start the wiki engine
        m_engine = new TestEngine(props);
        m_wm = m_engine.getWorkflowManager();
        m_dq = m_wm.getDecisionQueue();
        m_builder = WorkflowBuilder.getBuilder( m_engine );
    }


    @Test
    public void testBuildApprovalWorkflow() throws WikiException
    {

        Principal submitter = new WikiPrincipal( "Submitter" );
        String workflowApproverKey = "workflow.approvalWorkflow";
        Task prepTask = new TestPrepTask( "task.preSaveWikiPage" );
        String decisionKey = "decision.saveWikiPage";
        Fact[] facts = new Fact[3];
        facts[0] = new Fact("fact1",new Integer(1));
        facts[1] = new Fact("fact2","A factual String");
        facts[2] = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);
        Task completionTask = new TestPrepTask( "task.saveWikiPage" );
        String rejectedMessageKey = "notification.saveWikiPage.reject";

        Workflow w = m_builder.buildApprovalWorkflow(submitter, workflowApproverKey,
                                                   prepTask, decisionKey, facts,
                                                   completionTask, rejectedMessageKey);
        w.setWorkflowManager( m_engine.getWorkflowManager() );

        // Check to see if the workflow built correctly
        Assertions.assertFalse( w.isStarted() || w.isCompleted() || w.isAborted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( "workflow.approvalWorkflow", w.getMessageKey() );
        Assertions.assertEquals( Workflow.CREATED, w.getCurrentState() );
        Assertions.assertEquals( new WikiPrincipal("Submitter"), w.getOwner() );
        Assertions.assertEquals( m_engine.getWorkflowManager(), w.getWorkflowManager() );
        Assertions.assertEquals( 0, w.getHistory().size() );

        // Our dummy "task complete" attributes should still be null
        Assertions.assertNull( w.getAttribute( "task.preSaveWikiPage") );
        Assertions.assertNull( w.getAttribute( "task.saveWikiPage") );

        // Start the workflow
        w.start();

        // Presave complete attribute should be set now, and current step should be Decision
        Step decision = w.getCurrentStep();
        Assertions.assertTrue( decision instanceof Decision );
        Assertions.assertEquals( 2, w.getHistory().size() );
        Assertions.assertEquals( prepTask, w.getHistory().get( 0 ) );
        Assertions.assertTrue( w.getHistory().get( 1 ) instanceof Decision );
        Assertions.assertNotNull( w.getAttribute( "task.preSaveWikiPage") );
        Assertions.assertEquals( new WikiPrincipal( Users.JANNE ), decision.getActor() );
        Assertions.assertEquals( decisionKey, decision.getMessageKey() );
        List< Fact > decisionFacts = ((Decision)decision).getFacts();
        Assertions.assertEquals( 3, decisionFacts.size() );
        Assertions.assertEquals( facts[0], decisionFacts.get(0) );
        Assertions.assertEquals( facts[1], decisionFacts.get(1) );
        Assertions.assertEquals( facts[2], decisionFacts.get(2) );

        // Check that our predecessor/successor relationships are ok
        Assertions.assertEquals( decision, prepTask.getSuccessor( Outcome.STEP_COMPLETE ) );
        Assertions.assertEquals( null, prepTask.getSuccessor( Outcome.STEP_ABORT ) );
        Assertions.assertEquals( null, prepTask.getSuccessor( Outcome.STEP_CONTINUE ) );
        Assertions.assertEquals( null, decision.getSuccessor( Outcome.DECISION_ACKNOWLEDGE ) );
        Assertions.assertEquals( null, decision.getSuccessor( Outcome.DECISION_HOLD ) );
        Assertions.assertEquals( null, decision.getSuccessor( Outcome.DECISION_REASSIGN ) );
        Assertions.assertEquals( completionTask, decision.getSuccessor( Outcome.DECISION_APPROVE ) );

        // The "deny" notification should use the right key
        Step notification = decision.getSuccessor( Outcome.DECISION_DENY );
        Assertions.assertNotNull( notification );
        Assertions.assertEquals( rejectedMessageKey, notification.getMessageKey() );
        Assertions.assertTrue( notification instanceof SimpleNotification );

        // Now, approve the Decision and everything should complete
        ((Decision)decision).decide( Outcome.DECISION_APPROVE );
        Assertions.assertTrue( w.isCompleted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( 3, w.getHistory().size() );
        Assertions.assertEquals( completionTask, w.getHistory().get( 2 ) );
        Assertions.assertTrue( completionTask.isCompleted() );
        Assertions.assertEquals( Outcome.STEP_COMPLETE, completionTask.getOutcome() );
    }

    @Test
    public void testBuildApprovalWorkflowDeny() throws WikiException
    {
        Principal submitter = new WikiPrincipal( "Submitter" );
        String workflowApproverKey = "workflow.approvalWorkflow";
        Task prepTask = new TestPrepTask( "task.preSaveWikiPage" );
        String decisionKey = "decision.saveWikiPage";
        Fact[] facts = new Fact[3];
        facts[0] = new Fact("fact1",new Integer(1));
        facts[1] = new Fact("fact2","A factual String");
        facts[2] = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);
        Task completionTask = new TestPrepTask( "task.saveWikiPage" );
        String rejectedMessageKey = "notification.saveWikiPage.reject";

        Workflow w = m_builder.buildApprovalWorkflow(submitter, workflowApproverKey,
                                                   prepTask, decisionKey, facts,
                                                   completionTask, rejectedMessageKey);
        w.setWorkflowManager( m_engine.getWorkflowManager() );

        // Start the workflow
        w.start();

        // Now, deny the Decision and the submitter should see a notification
        Step step = w.getCurrentStep();
        Assertions.assertTrue( step instanceof Decision );
        Decision decision = (Decision)step;
        decision.decide( Outcome.DECISION_DENY );
        Assertions.assertFalse( w.isCompleted() );

        // Check that the notification is ok, then acknowledge it
        step = w.getCurrentStep();
        Assertions.assertTrue( step instanceof SimpleNotification );
        Assertions.assertEquals( rejectedMessageKey, step.getMessageKey() );
        SimpleNotification notification = (SimpleNotification)step;
        notification.acknowledge();

        // Workflow should be complete now
        Assertions.assertTrue( w.isCompleted() );
        Assertions.assertNull( w.getCurrentStep() );
        Assertions.assertEquals( 3, w.getHistory().size() );
        Assertions.assertEquals( notification, w.getHistory().get( 2 ) );
    }

    @Test
    public void testSaveWikiPageWithApproval() throws WikiException
    {
        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        try
        {
            m_engine.saveTextAsJanne(pageName, text);
        }
        catch ( DecisionRequiredException e )
        {
            // Swallow exception, because it is expected...
        }

        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        Assertions.assertFalse( m_engine.getPageManager().wikiPageExists(pageName));

        // Second, GroupPrincipal Admin should see a Decision in its queue
        Collection< Decision > decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals(1, decisions.size());

        // Now, approve the decision and it should go away, and page should appear.
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_APPROVE);
        Assertions.assertTrue( m_engine.getPageManager().wikiPageExists(pageName));
        decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals(0, decisions.size());

        // Delete the page we created
        m_engine.getPageManager().deletePage( pageName );
    }

    @Test
    public void testSaveWikiPageWithRejection() throws WikiException
    {
        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        try
        {
            m_engine.saveTextAsJanne(pageName, text);
        }
        catch ( DecisionRequiredException e )
        {
            // Swallow exception, because it is expected...
        }

        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        Assertions.assertFalse( m_engine.getPageManager().wikiPageExists(pageName));

        // ...and there should be a Decision in GroupPrincipal Admin's queue
        Collection< Decision > decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        Assertions.assertEquals(1, decisions.size());

        // Now, DENY the decision and the page should still not exist...
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_DENY);
        Assertions.assertFalse( m_engine.getPageManager().wikiPageExists(pageName) );

        // ...but there should also be a notification decision in Janne's queue
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        Assertions.assertEquals(1, decisions.size());
        decision = (Decision)decisions.iterator().next();
        Assertions.assertEquals(WorkflowManager.WF_WP_SAVE_REJECT_MESSAGE_KEY, decision.getMessageKey());

        // Once Janne disposes of the notification, his queue should be empty
        decision.decide(Outcome.DECISION_ACKNOWLEDGE);
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        Assertions.assertEquals(0, decisions.size());
    }

    @Test
    public void testSaveWikiPageWithException() throws WikiException
    {
        // Add a PageFilter that rejects all save attempts
        FilterManager fm = m_engine.getFilterManager();
        fm.addPageFilter( new AbortFilter(), 0 );

        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        try
        {
            m_engine.saveTextAsJanne(pageName, text);
        }
        catch ( WikiException e )
        {
            Assertions.assertTrue( e instanceof FilterException );
            Assertions.assertEquals( "Page save aborted.", e.getMessage() );
            return;
        }
        Assertions.fail( "Page save should have thrown a FilterException, but didn't." );
    }

    /**
     * Sample "prep task" that sets an attribute in the workflow indicating
     * that it ran successfully,
     */
    public static class TestPrepTask extends Task
    {
        private static final long serialVersionUID = 1L;

        public TestPrepTask( String messageKey )
        {
            super( messageKey );
        }

        public Outcome execute() throws WikiException
        {
            getWorkflow().setAttribute( getMessageKey(), "Completed" );
            setOutcome( Outcome.STEP_COMPLETE );
            return Outcome.STEP_COMPLETE;
        }

    }

    /**
     * Dummy PageFilter that always throws a FilterException during preSave operations.
     */
    public static class AbortFilter extends BasicPageFilter
    {
        public String preSave(WikiContext wikiContext, String content) throws FilterException
        {
            throw new FilterException( "Page save aborted." );
        }
    }

}
