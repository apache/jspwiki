package com.ecyrd.jspwiki.workflow;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;

import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class SimpleDecisionTest extends TestCase
{

    Workflow w;
    Decision d;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        w = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        d = new SimpleDecision(w, "decision.key", new WikiPrincipal("Actor1"));
    }

    public void testGetActor()
    {
       assertEquals(new WikiPrincipal("Actor1"), d.getActor());
    }

    public void testGetDefaultOutcome()
    {
        assertEquals(Outcome.DECISION_APPROVE, d.getDefaultOutcome());
    }

    public void testIsReassignable()
    {
        assertTrue(d.isReassignable());
    }

    public void testReassign()
    {
        d.reassign(new WikiPrincipal("Actor2"));
        assertEquals(new WikiPrincipal("Actor2"), d.getActor());
    }

    public void testSuccessors()
    {
        // If the decision is approved, branch to another decision (d2)
        Step d2 = new SimpleDecision(w, "decision2.key", new WikiPrincipal("Actor1"));
        d.addSuccessor(Outcome.DECISION_APPROVE, d2);
        
        // If the decision is denied, branch to another decision (d3)
        Step d3 = new SimpleDecision(w, "decision3.key", new WikiPrincipal("Actor1"));
        d.addSuccessor(Outcome.DECISION_DENY, d3);
        
        assertEquals(d2, d.successor(Outcome.DECISION_APPROVE));
        assertEquals(d3, d.successor(Outcome.DECISION_DENY));
        
        // The other Outcomes should return null when looked up
        assertNull(d.successor(Outcome.DECISION_HOLD));
        assertNull(d.successor(Outcome.DECISION_REASSIGN));
        assertNull(d.successor(Outcome.STEP_ABORT));
    }

    public void testErrors()
    {
        d.addError("Error deciding something.");
        d.addError("Error deciding something else.");
        
        assertEquals(2, d.errors().length);
        assertEquals("Error deciding something.", d.errors()[0]);
        assertEquals("Error deciding something else.", d.errors()[1]);
    }

    public void testAvailableOutcomes()
    {
        Outcome[] outcomes = d.availableOutcomes();
        assertTrue(ArrayUtils.contains(outcomes,Outcome.DECISION_APPROVE));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.DECISION_DENY));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.DECISION_HOLD));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.DECISION_REASSIGN));
        assertTrue(ArrayUtils.contains(outcomes,Outcome.STEP_ABORT));
        assertFalse(ArrayUtils.contains(outcomes,Outcome.STEP_COMPLETE));
    }

    public void testGetEndTime()
    {
        assertEquals(Workflow.TIME_NOT_SET, d.getEndTime());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertTrue((Workflow.TIME_NOT_SET  !=  d.getEndTime()));
    }

    public void testGetMessageKey()
    {
        assertEquals("decision.key",d.getMessageKey());
    }

    public void testGetOutcome()
    {
        assertEquals(Outcome.STEP_CONTINUE,d.getOutcome());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(Outcome.DECISION_APPROVE, d.getOutcome());
    }

    public void testGetStartTime()
    {
        assertEquals(Workflow.TIME_NOT_SET, d.getStartTime());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertTrue((Workflow.TIME_NOT_SET  !=  d.getStartTime()));
    }

    public void testGetWorkflow()
    {
        assertEquals(w, d.getWorkflow());
    }

    public void testIsCompleted()
    {
        assertFalse(d.isCompleted());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertTrue(d.isCompleted());
    }

    public void testIsStarted()
    {
        assertFalse(d.isStarted());
        d.start();
        assertTrue(d.isStarted());
    }
    
    public void testStartTwice() {
        d.start();
        try {
            d.start();
        }
        catch (IllegalStateException e) {
            // Swallow
            return;
        }
        // We should never get here
        fail("Decision allowed itself to be started twice!");
    }

}
