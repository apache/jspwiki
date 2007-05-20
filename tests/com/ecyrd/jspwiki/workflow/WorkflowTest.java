package com.ecyrd.jspwiki.workflow;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class WorkflowTest extends TestCase
{
    Workflow w;

    Task initTask;

    Decision decision;

    Task finishTask;

    String ATTR = "TestAttribute";

    protected void setUp() throws Exception
    {
        super.setUp();

        // Create workflow; owner is test user
        w = new Workflow("workflow.myworkflow", new WikiPrincipal("Owner1"));

        // Create custom initialization task
        initTask = new TaskTest.NormalTask(w);

        // Create finish task
        finishTask = new TaskTest.NormalTask(w);

        // Create an intermetidate decision step
        Principal actor = new GroupPrincipal("Admin");
        decision = new SimpleDecision(w, "decision.AdminDecision", actor);

        // Hook the steps together
        initTask.addSuccessor(Outcome.STEP_COMPLETE, decision);
        decision.addSuccessor(Outcome.DECISION_APPROVE, finishTask);

        // Stash page name as message attribute
        w.addMessageArgument("MyPage");

        // Set workflow's first step
        w.setFirstStep(initTask);
    }

    public void testWorkflow()
    {
        // Make sure everything is set to their proper default values
        assertNull(w.getCurrentStep());
        assertNull(w.getCurrentActor());
        assertEquals(0, w.getHistory().size());
        assertEquals(Workflow.ID_NOT_SET, w.getId());
        assertNull(w.getWorkflowManager());
        assertEquals(new WikiPrincipal("Owner1"), w.getOwner());
        assertEquals(Workflow.CREATED, w.getCurrentState());
        assertEquals(Workflow.TIME_NOT_SET, w.getStartTime());
        assertEquals(Workflow.TIME_NOT_SET, w.getEndTime());
    }

    public void testSetWorkflowManager()
    {
        assertNull(w.getWorkflowManager());
        WorkflowManager m = new WorkflowManager();
        w.setWorkflowManager(m);
        assertEquals(m, w.getWorkflowManager());
    }

    public void testGetSetAttribute()
    {
        assertNull(w.getAttribute(ATTR));
        w.setAttribute(ATTR, "Test String");
        assertNotNull(w.getAttribute(ATTR));
        assertEquals("Test String", w.getAttribute(ATTR));
    }

    public void testGetMessageArgs() throws WikiException
    {
        Object[] args;

        // Before start, arg1=Owner1, arg2=- (no current actor), arg3=MyPage
        args = w.getMessageArguments();
        assertEquals("Owner1", args[0]);
        assertEquals("-",      args[1]);
        assertEquals("MyPage", args[2]);

        // After start (at Decision), arg1=Owner1, arg2=Admin, arg3=MyPage
        w.start();
        args = w.getMessageArguments();
        assertEquals("Owner1", args[0]);
        assertEquals("Admin", args[1]);
        assertEquals("MyPage", args[2]);

        // After end, arg1=Owner1, arg2=-, arg3=MyPage
        decision.decide(Outcome.DECISION_APPROVE);
        args = w.getMessageArguments();
        assertEquals("Owner1", args[0]);
        assertEquals("-", args[1]);
        assertEquals("MyPage", args[2]);
    }

    public void testGetMessageArgObjects()
    {
        // Try passing some valid object types: Date, Number
        w.addMessageArgument(new Date());
        w.addMessageArgument(new Integer(1));
        w.addMessageArgument(new Double(2));
        w.addMessageArgument(new BigDecimal(3.14));

        // Try passing an invalid one: e.g., a Workflow (it should fail)
        try
        {
            w.addMessageArgument(w);
        }
        catch (IllegalArgumentException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Illegal argument passed...");
    }

    public void testGetMessageKey()
    {
        assertEquals("workflow.myworkflow", w.getMessageKey());
    }

    public void testGetOwner()
    {
        assertEquals(new WikiPrincipal("Owner1"), w.getOwner());
    }

    public void testStart() throws WikiException
    {
        assertFalse(w.isStarted());
        w.start();
        assertTrue(w.isStarted());
    }

    public void testWaitstate() throws WikiException
    {
        w.start();

        // Default workflow should have hit the Decision step and put itself
        // into WAITING
        assertEquals(Workflow.WAITING, w.getCurrentState());
    }

    public void testRestart() throws WikiException
    {
        w.start();

        // Default workflow should have hit the Decision step and put itself
        // into WAITING
        assertEquals(Workflow.WAITING, w.getCurrentState());
        w.restart();
        assertEquals(Workflow.WAITING, w.getCurrentState());
    }

    public void testAbortBeforeStart() throws WikiException
    {
        // Workflow hasn't been started yet
        assertFalse(w.isAborted());
        w.abort();
        assertTrue(w.isAborted());

        // Try to start anyway
        try
        {
            w.start();
        }
        catch (IllegalStateException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Workflow allowed itself to be started even though it was aborted!");
    }

    public void testAbortDuringWait() throws WikiException
    {
        // Start workflow, then abort while in WAITING state
        assertFalse(w.isAborted());
        w.start();
        w.abort();
        assertTrue(w.isAborted());

        // Try to restart anyway
        try
        {
            w.restart();
        }
        catch (IllegalStateException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Workflow allowed itself to be re-started even though it was aborted!");
    }

    public void testAbortAfterCompletion() throws WikiException
    {
        // Start workflow, then abort after completion
        assertFalse(w.isAborted());
        w.start();
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);

        // Try to abort anyway
        try
        {
            w.abort();
            assertTrue(w.isAborted());
        }
        catch (IllegalStateException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Workflow allowed itself to be aborted even though it was completed!");
    }

    public void testCurrentState() throws WikiException
    {
        assertEquals(Workflow.CREATED, w.getCurrentState());
        w.start();
        assertEquals(Workflow.WAITING, w.getCurrentState());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(Workflow.COMPLETED, w.getCurrentState());
    }

    public void testCurrentStep() throws WikiException
    {
        assertNull(w.getCurrentStep());
        w.start();

        // Workflow stops at the decision step
        assertEquals(decision, w.getCurrentStep());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);

        // After we decide, it blows through step 3 and leaves us with a null
        // step (done)
        assertNull(w.getCurrentStep());
    }

    public void testPreviousStep() throws WikiException
    {
        // If not started, no previous steps available for anything
        assertNull(w.getPreviousStep());
        assertEquals(null, w.previousStep(initTask));
        assertEquals(null, w.previousStep(decision));
        assertEquals(null, w.previousStep(finishTask));

        // Once we start, initTask and decisions' predecessors are known, but
        // finish task is indeterminate
        w.start();
        assertEquals(null, w.previousStep(initTask));
        assertEquals(initTask, w.previousStep(decision));
        assertEquals(null, w.previousStep(finishTask));

        // Once we decide, the finish task returns the correct predecessor
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(null, w.previousStep(initTask));
        assertEquals(initTask, w.previousStep(decision));
        assertEquals(decision, w.previousStep(finishTask));
    }

    public void testCurrentActor() throws WikiException
    {
        // Before starting, actor should be null
        assertNull(w.getCurrentActor());

        // After starting, actor should be GroupPrincipal Admin
        w.start();
        assertEquals(new GroupPrincipal("Admin"), w.getCurrentActor());

        // After decision, actor should be null again
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertNull(w.getCurrentActor());
    }

    public void testHistory() throws WikiException
    {
        assertEquals(0, w.getHistory().size());
        w.start();
        assertEquals(2, w.getHistory().size());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(3, w.getHistory().size());
    }

    public void testGetStartTime() throws WikiException
    {
        // Start time should be not be set until we start the workflow
        assertEquals(Workflow.TIME_NOT_SET, w.getStartTime());
        w.start();
        assertFalse(Workflow.TIME_NOT_SET == w.getStartTime());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertFalse(Workflow.TIME_NOT_SET == w.getStartTime());
    }

    public void testGetEndTime() throws WikiException
    {
        // End time should be not set until we finish all 3 steps
        assertEquals(Workflow.TIME_NOT_SET, w.getEndTime());
        w.start();
        assertEquals(Workflow.TIME_NOT_SET, w.getEndTime());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertFalse(Workflow.TIME_NOT_SET == w.getEndTime());
    }

    public void testIsCompleted() throws WikiException
    {
        // Workflow isn't completed until we finish all 3 steps
        assertFalse(w.isCompleted());
        w.start();
        assertFalse(w.isCompleted());
        Decision d = (Decision) w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        assertTrue(w.isCompleted());
    }

    public void testIsStarted() throws WikiException
    {
        assertFalse(w.isStarted());
        w.start();
        assertTrue(w.isStarted());
    }

    public void testStartTwice() throws WikiException
    {
        w.start();
        try
        {
            w.start();
        }
        catch (IllegalStateException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Workflow allowed itself to be started twice!");
    }

    public void testSetId()
    {
        assertEquals(Workflow.ID_NOT_SET, w.getId());
        w.setId(1001);
        assertEquals(1001, w.getId());
    }

}
