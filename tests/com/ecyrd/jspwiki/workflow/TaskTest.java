package com.ecyrd.jspwiki.workflow;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;

import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class TaskTest extends TestCase
{

    Workflow w;
    Task t;
    
    /** Sample Task that completes normally. */
    public static class NormalTask extends Task {
        public NormalTask(Workflow workflow) {
            super(workflow, "task.normal");
        }
        public Outcome execute() throws WikiException
        {
            return Outcome.STEP_COMPLETE;
        }
        
    }
    
    /** Sample Task that encounters an error during processing. */
    public static class ErrorTask extends Task {
        public ErrorTask(Workflow workflow) {
            super(workflow, "task.error");
        }
        public Outcome execute() throws WikiException
        {
            addError("Found an error.");
            addError("Found a second one!");
            return Outcome.STEP_ABORT;
        }
        
    }
    
    protected void setUp() throws Exception
    {
        super.setUp();
        w = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        t = new NormalTask(w);
    }

    public void testGetActor()
    {
       assertNotSame(new WikiPrincipal("Actor1"), t.getActor());
       assertEquals(SystemPrincipal.SYSTEM_USER, t.getActor());
    }

    public void testSuccessors()
    {
        // If task finishes normally, branch to a decision (d1)
        Step d1 = new SimpleDecision(w, "decision1.key", new WikiPrincipal("Actor1"));
        t.addSuccessor(Outcome.STEP_COMPLETE, d1);
        
        // If the task aborts, branch to an alternate decision (d2)
        Step d2 = new SimpleDecision(w, "decision2.key", new WikiPrincipal("Actor2"));
        t.addSuccessor(Outcome.STEP_ABORT, d2);
        
        assertEquals(d1, t.successor(Outcome.STEP_COMPLETE));
        assertEquals(d2, t.successor(Outcome.STEP_ABORT));
        
        // The other Outcomes should return null when looked up
        assertNull(t.successor(Outcome.DECISION_APPROVE));
        assertNull(t.successor(Outcome.DECISION_DENY));
        assertNull(t.successor(Outcome.DECISION_HOLD));
        assertNull(t.successor(Outcome.DECISION_REASSIGN));
        assertNull(t.successor(Outcome.STEP_CONTINUE));
    }

    public void testErrors()
    {
        t.addError("Error deciding something.");
        t.addError("Error deciding something else.");
        
        assertEquals(2, t.errors().length);
        assertEquals("Error deciding something.", t.errors()[0]);
        assertEquals("Error deciding something else.", t.errors()[1]);
    }

    public void testAvailableOutcomes()
    {
        Outcome[] outcomes = t.availableOutcomes();
        assertFalse(ArrayUtils.contains(outcomes,Outcome.DECISION_APPROVE));
        assertFalse(ArrayUtils.contains(outcomes,Outcome.DECISION_DENY));
        assertFalse(ArrayUtils.contains(outcomes,Outcome.DECISION_HOLD));
        assertFalse(ArrayUtils.contains(outcomes,Outcome.DECISION_REASSIGN));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.STEP_ABORT));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.STEP_COMPLETE));
    }

    public void testGetEndTime() throws WikiException
    {
        assertEquals(Workflow.TIME_NOT_SET, t.getEndTime());
        t.start();
        t.setOutcome(t.execute());
        assertTrue((Workflow.TIME_NOT_SET  !=  t.getEndTime()));
    }

    public void testGetMessageKey()
    {
        assertEquals("task.normal",t.getMessageKey());
    }

    public void testGetOutcome() throws WikiException
    {
        assertEquals(Outcome.STEP_CONTINUE,t.getOutcome());
        t.start();
        t.setOutcome(t.execute());
        assertEquals(Outcome.STEP_COMPLETE, t.getOutcome());
        
        // Test the "error task"
        t = new ErrorTask(w);
        assertEquals(Outcome.STEP_CONTINUE,t.getOutcome());
        t.start();
        t.setOutcome(t.execute());
        assertEquals(Outcome.STEP_ABORT, t.getOutcome());
    }

    public void testGetStartTime() throws WikiException
    {
        assertEquals(Workflow.TIME_NOT_SET, t.getStartTime());
        t.start();
        t.execute();
        assertTrue((Workflow.TIME_NOT_SET  !=  t.getStartTime()));
    }

    public void testGetWorkflow()
    {
        assertEquals(w, t.getWorkflow());
    }

    public void testIsCompleted() throws WikiException
    {
        assertFalse(t.isCompleted());
        t.start();
        t.setOutcome(t.execute());
        assertTrue(t.isCompleted());
    }

    public void testIsStarted()
    {
        assertFalse(t.isStarted());
        t.start();
        assertTrue(t.isStarted());
    }
    
    public void testStartTwice() {
        t.start();
        try {
            t.start();
        }
        catch (IllegalStateException e) {
            // Swallow
            return;
        }
        // We should never get here
        fail("Decision allowed itself to be started twice!");
    }

}
