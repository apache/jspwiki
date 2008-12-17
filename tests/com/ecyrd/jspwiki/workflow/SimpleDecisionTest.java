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
package com.ecyrd.jspwiki.workflow;

import java.util.Collection;
import java.util.List;

import org.apache.jspwiki.api.WikiException;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class SimpleDecisionTest extends TestCase
{

    Workflow m_workflow;
    Decision m_decision;

    protected void setUp() throws Exception
    {
        super.setUp();
        m_workflow = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        m_decision = new SimpleDecision(m_workflow, "decision.key", new WikiPrincipal("Actor1"));
    }

    public void testAddFacts()
    {
        Fact f1 = new Fact("fact1",new Integer(1));
        Fact f2 = new Fact("fact2","A factual String");
        Fact f3 = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);
        m_decision.addFact(f1);
        m_decision.addFact(f2);
        m_decision.addFact(f3);

        // The facts should be available, and returned in order
        List facts = m_decision.getFacts();
        assertEquals(f1, facts.get(0));
        assertEquals(f2, facts.get(1));
        assertEquals(f3, facts.get(2));
    }

    public void testGetActor()
    {
       assertEquals(new WikiPrincipal("Actor1"), m_decision.getActor());
    }

    public void testGetDefaultOutcome()
    {
        assertEquals(Outcome.DECISION_APPROVE, m_decision.getDefaultOutcome());
    }

    public void testIsReassignable()
    {
        assertTrue(m_decision.isReassignable());
    }

    public void testReassign()
    {
        m_decision.reassign(new WikiPrincipal("Actor2"));
        assertEquals(new WikiPrincipal("Actor2"), m_decision.getActor());
    }

    public void testSuccessors()
    {
        // If the decision is approved, branch to another decision (d2)
        Step d2 = new SimpleDecision(m_workflow, "decision2.key", new WikiPrincipal("Actor1"));
        m_decision.addSuccessor(Outcome.DECISION_APPROVE, d2);

        // If the decision is denied, branch to another decision (d3)
        Step d3 = new SimpleDecision(m_workflow, "decision3.key", new WikiPrincipal("Actor1"));
        m_decision.addSuccessor(Outcome.DECISION_DENY, d3);

        assertEquals(d2, m_decision.getSuccessor(Outcome.DECISION_APPROVE));
        assertEquals(d3, m_decision.getSuccessor(Outcome.DECISION_DENY));

        // The other Outcomes should return null when looked up
        assertNull(m_decision.getSuccessor(Outcome.DECISION_HOLD));
        assertNull(m_decision.getSuccessor(Outcome.DECISION_REASSIGN));
        assertNull(m_decision.getSuccessor(Outcome.STEP_ABORT));
    }

    public void testErrors()
    {
        m_decision.addError("Error deciding something.");
        m_decision.addError("Error deciding something else.");

        List errors = m_decision.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Error deciding something.", errors.get(0));
        assertEquals("Error deciding something else.", errors.get(1));
    }

    public void testAvailableOutcomes()
    {
        Collection outcomes = m_decision.getAvailableOutcomes();
        assertTrue(outcomes.contains(Outcome.DECISION_APPROVE));
        assertTrue(outcomes.contains(Outcome.DECISION_DENY));
        assertFalse(outcomes.contains(Outcome.DECISION_HOLD));
        assertFalse(outcomes.contains(Outcome.DECISION_REASSIGN));
        assertFalse(outcomes.contains(Outcome.STEP_ABORT));
        assertFalse(outcomes.contains(Outcome.STEP_COMPLETE));
    }

    public void testGetEndTime() throws WikiException
    {
        assertEquals(Workflow.TIME_NOT_SET, m_decision.getEndTime());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        assertTrue((Workflow.TIME_NOT_SET  !=  m_decision.getEndTime()));
    }

    public void testGetMessageKey()
    {
        assertEquals("decision.key",m_decision.getMessageKey());
    }

    public void testGetOutcome() throws WikiException
    {
        assertEquals(Outcome.STEP_CONTINUE,m_decision.getOutcome());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        assertEquals(Outcome.DECISION_APPROVE, m_decision.getOutcome());
    }

    public void testGetStartTime() throws WikiException
    {
        assertEquals(Workflow.TIME_NOT_SET, m_decision.getStartTime());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        assertTrue((Workflow.TIME_NOT_SET  !=  m_decision.getStartTime()));
    }

    public void testGetWorkflow()
    {
        assertEquals(m_workflow, m_decision.getWorkflow());
    }

    public void testIsCompleted() throws WikiException
    {
        assertFalse(m_decision.isCompleted());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        assertTrue(m_decision.isCompleted());
    }

    public void testIsStarted() throws WikiException
    {
        assertFalse(m_decision.isStarted());
        m_decision.start();
        assertTrue(m_decision.isStarted());
    }

    public void testStartTwice() throws WikiException
    {
        m_decision.start();
        try
        {
            m_decision.start();
        }
        catch (IllegalStateException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        fail("Decision allowed itself to be started twice!");
    }

}
