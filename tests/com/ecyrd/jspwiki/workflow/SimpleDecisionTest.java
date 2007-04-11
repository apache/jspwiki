package com.ecyrd.jspwiki.workflow;

import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.WikiException;
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
    
    public void testAddFacts() {
        Fact f1 = new Fact("fact1",new Integer(1));
        Fact f2 = new Fact("fact2","A factual String");
        Fact f3 = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);
        d.addFact(f1);
        d.addFact(f2);
        d.addFact(f3);

        // The facts should be available, and returned in order
        List facts = d.getFacts();
        assertEquals(f1, facts.get(0));
        assertEquals(f2, facts.get(1));
        assertEquals(f3, facts.get(2));
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
        
        assertEquals(d2, d.getSuccessor(Outcome.DECISION_APPROVE));
        assertEquals(d3, d.getSuccessor(Outcome.DECISION_DENY));
        
        // The other Outcomes should return null when looked up
        assertNull(d.getSuccessor(Outcome.DECISION_HOLD));
        assertNull(d.getSuccessor(Outcome.DECISION_REASSIGN));
        assertNull(d.getSuccessor(Outcome.STEP_ABORT));
    }

    public void testErrors()
    {
        d.addError("Error deciding something.");
        d.addError("Error deciding something else.");
        
        List errors = d.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Error deciding something.", errors.get(0));
        assertEquals("Error deciding something else.", errors.get(1));
    }

    public void testAvailableOutcomes()
    {
        Collection outcomes = d.getAvailableOutcomes();
        assertTrue(outcomes.contains(Outcome.DECISION_APPROVE));
        assertTrue(outcomes.contains(Outcome.DECISION_DENY));
        assertFalse(outcomes.contains(Outcome.DECISION_HOLD));
        assertFalse(outcomes.contains(Outcome.DECISION_REASSIGN));
        assertFalse(outcomes.contains(Outcome.STEP_ABORT));
        assertFalse(outcomes.contains(Outcome.STEP_COMPLETE));
    }

    public void testGetEndTime() throws WikiException
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

    public void testGetOutcome() throws WikiException
    {
        assertEquals(Outcome.STEP_CONTINUE,d.getOutcome());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertEquals(Outcome.DECISION_APPROVE, d.getOutcome());
    }

    public void testGetStartTime() throws WikiException
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

    public void testIsCompleted() throws WikiException
    {
        assertFalse(d.isCompleted());
        d.start();
        d.decide(Outcome.DECISION_APPROVE);
        assertTrue(d.isCompleted());
    }

    public void testIsStarted() throws WikiException
    {
        assertFalse(d.isStarted());
        d.start();
        assertTrue(d.isStarted());
    }
    
    public void testStartTwice() throws WikiException
    {
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
