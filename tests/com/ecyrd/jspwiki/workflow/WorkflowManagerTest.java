package com.ecyrd.jspwiki.workflow;

import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class WorkflowManagerTest extends TestCase
{
    protected Workflow w;
    protected WorkflowManager wm;
    protected WikiEngine m_engine;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());
        m_engine = new TestEngine(props);
        wm = m_engine.getWorkflowManager();
        // Create a workflow with 3 steps, with a Decision in the middle
        w = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        w.setWorkflowManager(m_engine.getWorkflowManager());
        Step startTask = new TaskTest.NormalTask(w);
        Step endTask = new TaskTest.NormalTask(w);
        Decision decision = new SimpleDecision(w, "decision.editWikiApproval", new WikiPrincipal("Actor1"));
        startTask.addSuccessor(Outcome.STEP_COMPLETE, decision);
        decision.addSuccessor(Outcome.DECISION_APPROVE, endTask);
        w.setFirstStep(startTask);
        
        // Add a message argument to the workflow with the page name
        w.addMessageArgument("MyPage");
    }

    public void testStart() throws WikiException
    {
        // Once we start the workflow, it should show that it's started
        // and the WM should have assigned it an ID
        assertEquals(Workflow.ID_NOT_SET, w.getId());
        assertFalse(w.isStarted());
        wm.start(w);
        assertFalse(Workflow.ID_NOT_SET == w.getId());
        assertTrue(w.isStarted());
    }

    public void testWorkflows() throws WikiException
    {
        // There should be no workflows in the cache, and none in completed list
        assertEquals(0, wm.workflows().size());
        assertEquals(0, wm.completedWorkflows().size());
        
        // After starting, there should be 1 in the cache, with ID=1
        wm.start(w);
        assertEquals(1, wm.workflows().size());
        assertEquals(0, wm.completedWorkflows().size());
        Workflow workflow = (Workflow)wm.workflows().iterator().next();
        assertEquals(w, workflow);
        assertEquals(1, workflow.getId());
        
        // After forcing a decision on step 2, the workflow should complete and vanish from the cache
        Decision d = (Decision)w.currentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(0, wm.workflows().size());
        assertEquals(1, wm.completedWorkflows().size());
    }
    
    public void testRequiresApproval()
    {
        // Test properties says we need approvals for workflow.saveWikiPage & workflow.foo
        assertFalse(wm.requiresApproval("workflow.saveWikiPage"));
        assertTrue(wm.requiresApproval("workflow.foo"));
        assertTrue(wm.requiresApproval("workflow.bar"));
    }

    public void testGetApprover() throws WikiException
    {
        // Test properties says workflow.saveWikiPage approver is GP Admin; workflow.foo is 'janne'
        assertEquals(new WikiPrincipal("janne", WikiPrincipal.LOGIN_NAME), wm.getApprover("workflow.foo"));
        assertEquals(new GroupPrincipal("JSPWiki","Admin"), wm.getApprover("workflow.bar"));
        
        // 'saveWikiPage' workflow doesn't require approval, so we will need to catch an Exception
        try 
        {
            assertEquals(new GroupPrincipal("JSPWiki","Admin"), wm.getApprover("workflow.saveWikiPage"));
        }
        catch (WikiException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Workflow.bar doesn't need approval!");
    }

    public void testGetMessage()
    {
        // Current step is the decision step
        w.start();
        
        // Make sure we get the right message key for the Step
        Step s = w.currentStep();
        assertEquals("decision.editWikiApproval", s.getMessageKey());
        
        // Now, let's see if it creates the correct message...
        String message = wm.getMessage(w.currentStep(), Locale.ENGLISH);
        assertEquals("Edit page MyPage by user Owner1 (required approver: Actor1)", message);
    }

}
