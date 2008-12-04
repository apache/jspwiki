package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.jspwiki.api.FilterException;
import org.apache.jspwiki.api.WikiException;

import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.filters.BasicPageFilter;

public class ApprovalWorkflowTest extends TestCase
{
    WorkflowBuilder m_builder;
    TestEngine m_engine;
    WorkflowManager m_wm;
    DecisionQueue m_dq;


    protected void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());

        // Explicitly turn on Admin approvals for page saves and our sample approval workflow
        props.put("jspwiki.approver.workflow.saveWikiPage", "Admin");
        props.put( "jspwiki.approver.workflow.approvalWorkflow", Users.JANNE );

        // Start the wiki engine
        m_engine = new TestEngine(props);
        m_wm = m_engine.getWorkflowManager();
        m_dq = m_wm.getDecisionQueue();
        m_builder = WorkflowBuilder.getBuilder( m_engine );
    }


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
        assertFalse( w.isStarted() || w.isCompleted() || w.isAborted() );
        assertNull( w.getCurrentStep() );
        assertEquals( "workflow.approvalWorkflow", w.getMessageKey() );
        assertEquals( Workflow.CREATED, w.getCurrentState() );
        assertEquals( new WikiPrincipal("Submitter"), w.getOwner() );
        assertEquals( m_engine.getWorkflowManager(), w.getWorkflowManager() );
        assertEquals( 0, w.getHistory().size() );

        // Our dummy "task complete" attributes should still be null
        assertNull( w.getAttribute( "task.preSaveWikiPage") );
        assertNull( w.getAttribute( "task.saveWikiPage") );

        // Start the workflow
        w.start();

        // Presave complete attribute should be set now, and current step should be Decision
        Step decision = w.getCurrentStep();
        assertTrue( decision instanceof Decision );
        assertEquals( 2, w.getHistory().size() );
        assertEquals( prepTask, w.getHistory().get( 0 ) );
        assertTrue( w.getHistory().get( 1 ) instanceof Decision );
        assertNotNull( w.getAttribute( "task.preSaveWikiPage") );
        assertEquals( new WikiPrincipal( Users.JANNE ), decision.getActor() );
        assertEquals( decisionKey, decision.getMessageKey() );
        List decisionFacts = ((Decision)decision).getFacts();
        assertEquals( 3, decisionFacts.size() );
        assertEquals( facts[0], decisionFacts.get(0) );
        assertEquals( facts[1], decisionFacts.get(1) );
        assertEquals( facts[2], decisionFacts.get(2) );

        // Check that our predecessor/successor relationships are ok
        assertEquals( decision, prepTask.getSuccessor( Outcome.STEP_COMPLETE ) );
        assertEquals( null, prepTask.getSuccessor( Outcome.STEP_ABORT ) );
        assertEquals( null, prepTask.getSuccessor( Outcome.STEP_CONTINUE ) );
        assertEquals( null, decision.getSuccessor( Outcome.DECISION_ACKNOWLEDGE ) );
        assertEquals( null, decision.getSuccessor( Outcome.DECISION_HOLD ) );
        assertEquals( null, decision.getSuccessor( Outcome.DECISION_REASSIGN ) );
        assertEquals( completionTask, decision.getSuccessor( Outcome.DECISION_APPROVE ) );

        // The "deny" notification should use the right key
        Step notification = decision.getSuccessor( Outcome.DECISION_DENY );
        assertNotNull( notification );
        assertEquals( rejectedMessageKey, notification.getMessageKey() );
        assertTrue( notification instanceof SimpleNotification );

        // Now, approve the Decision and everything should complete
        ((Decision)decision).decide( Outcome.DECISION_APPROVE );
        assertTrue( w.isCompleted() );
        assertNull( w.getCurrentStep() );
        assertEquals( 3, w.getHistory().size() );
        assertEquals( completionTask, w.getHistory().get( 2 ) );
        assertTrue( completionTask.isCompleted() );
        assertEquals( Outcome.STEP_COMPLETE, completionTask.getOutcome() );
    }

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
        assertTrue( step instanceof Decision );
        Decision decision = (Decision)step;
        decision.decide( Outcome.DECISION_DENY );
        assertFalse( w.isCompleted() );

        // Check that the notification is ok, then acknowledge it
        step = w.getCurrentStep();
        assertTrue( step instanceof SimpleNotification );
        assertEquals( rejectedMessageKey, step.getMessageKey() );
        SimpleNotification notification = (SimpleNotification)step;
        notification.acknowledge();

        // Workflow should be complete now
        assertTrue( w.isCompleted() );
        assertNull( w.getCurrentStep() );
        assertEquals( 3, w.getHistory().size() );
        assertEquals( notification, w.getHistory().get( 2 ) );
    }

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
        assertFalse( m_engine.pageExists(pageName));

        // Second, GroupPrincipal Admin should see a Decision in its queue
        Collection decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        assertEquals(1, decisions.size());

        // Now, approve the decision and it should go away, and page should appear.
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_APPROVE);
        assertTrue( m_engine.pageExists(pageName));
        decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        assertEquals(0, decisions.size());

        // Delete the page we created
        m_engine.deletePage( pageName );
    }

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
        assertFalse( m_engine.pageExists(pageName));

        // ...and there should be a Decision in GroupPrincipal Admin's queue
        Collection decisions = m_dq.getActorDecisions( m_engine.adminSession() );
        assertEquals(1, decisions.size());

        // Now, DENY the decision and the page should still not exist...
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_DENY);
        assertFalse( m_engine.pageExists(pageName) );

        // ...but there should also be a notification decision in Janne's queue
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        assertEquals(1, decisions.size());
        decision = (Decision)decisions.iterator().next();
        assertEquals(PageManager.SAVE_REJECT_MESSAGE_KEY, decision.getMessageKey());

        // Once Janne disposes of the notification, his queue should be empty
        decision.decide(Outcome.DECISION_ACKNOWLEDGE);
        decisions = m_dq.getActorDecisions( m_engine.janneSession() );
        assertEquals(0, decisions.size());
    }

    public void testSaveWikiPageWithException() throws WikiException
    {
        // Add a PageFilter that rejects all save attempts
        m_engine.getFilterManager().addPageFilter( new AbortFilter(), 0 );

        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        try
        {
            m_engine.saveTextAsJanne(pageName, text);
        }
        catch ( WikiException e )
        {
            assertTrue( e instanceof FilterException );
            assertEquals( "Page save aborted.", e.getMessage() );
            return;
        }
        fail( "Page save should have thrown a FilterException, but didn't." );
    }

    /**
     * Sample "prep task" that sets an attribute in the workflow indicating
     * that it ran successfully,
     * @author Andrew Jaquith
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
     * @author Andrew Jaquith
     */
    public static class AbortFilter extends BasicPageFilter
    {
        public String preSave(WikiContext wikiContext, String content) throws FilterException
        {
            throw new FilterException( "Page save aborted." );
        }
    }

}
