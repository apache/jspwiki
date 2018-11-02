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

import java.util.Collection;
import java.util.List;

import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleDecisionTest
{

    Workflow m_workflow;
    Decision m_decision;

    @BeforeEach
    public void setUp() throws Exception
    {

        m_workflow = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        m_decision = new SimpleDecision(m_workflow, "decision.key", new WikiPrincipal("Actor1"));
    }

    @Test
    public void testAddFacts()
    {
        Fact f1 = new Fact("fact1",new Integer(1));
        Fact f2 = new Fact("fact2","A factual String");
        Fact f3 = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);
        m_decision.addFact(f1);
        m_decision.addFact(f2);
        m_decision.addFact(f3);

        // The facts should be available, and returned in order
        List< Fact > facts = m_decision.getFacts();
        Assertions.assertEquals(f1, facts.get(0));
        Assertions.assertEquals(f2, facts.get(1));
        Assertions.assertEquals(f3, facts.get(2));
    }

    @Test
    public void testGetActor()
    {
       Assertions.assertEquals(new WikiPrincipal("Actor1"), m_decision.getActor());
    }

    @Test
    public void testGetDefaultOutcome()
    {
        Assertions.assertEquals(Outcome.DECISION_APPROVE, m_decision.getDefaultOutcome());
    }

    @Test
    public void testIsReassignable()
    {
        Assertions.assertTrue(m_decision.isReassignable());
    }

    @Test
    public void testReassign()
    {
        m_decision.reassign(new WikiPrincipal("Actor2"));
        Assertions.assertEquals(new WikiPrincipal("Actor2"), m_decision.getActor());
    }

    @Test
    public void testSuccessors()
    {
        // If the decision is approved, branch to another decision (d2)
        Step d2 = new SimpleDecision(m_workflow, "decision2.key", new WikiPrincipal("Actor1"));
        m_decision.addSuccessor(Outcome.DECISION_APPROVE, d2);

        // If the decision is denied, branch to another decision (d3)
        Step d3 = new SimpleDecision(m_workflow, "decision3.key", new WikiPrincipal("Actor1"));
        m_decision.addSuccessor(Outcome.DECISION_DENY, d3);

        Assertions.assertEquals(d2, m_decision.getSuccessor(Outcome.DECISION_APPROVE));
        Assertions.assertEquals(d3, m_decision.getSuccessor(Outcome.DECISION_DENY));

        // The other Outcomes should return null when looked up
        Assertions.assertNull(m_decision.getSuccessor(Outcome.DECISION_HOLD));
        Assertions.assertNull(m_decision.getSuccessor(Outcome.DECISION_REASSIGN));
        Assertions.assertNull(m_decision.getSuccessor(Outcome.STEP_ABORT));
    }

    @Test
    public void testErrors()
    {
        m_decision.addError("Error deciding something.");
        m_decision.addError("Error deciding something else.");

        List< String > errors = m_decision.getErrors();
        Assertions.assertEquals(2, errors.size());
        Assertions.assertEquals("Error deciding something.", errors.get(0));
        Assertions.assertEquals("Error deciding something else.", errors.get(1));
    }

    @Test
    public void testAvailableOutcomes()
    {
        Collection< Outcome > outcomes = m_decision.getAvailableOutcomes();
        Assertions.assertTrue(outcomes.contains(Outcome.DECISION_APPROVE));
        Assertions.assertTrue(outcomes.contains(Outcome.DECISION_DENY));
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_HOLD));
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_REASSIGN));
        Assertions.assertFalse(outcomes.contains(Outcome.STEP_ABORT));
        Assertions.assertFalse(outcomes.contains(Outcome.STEP_COMPLETE));
    }

    @Test
    public void testGetEndTime() throws WikiException
    {
        Assertions.assertEquals(Workflow.TIME_NOT_SET, m_decision.getEndTime());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        Assertions.assertTrue((Workflow.TIME_NOT_SET  !=  m_decision.getEndTime()));
    }

    @Test
    public void testGetMessageKey()
    {
        Assertions.assertEquals("decision.key",m_decision.getMessageKey());
    }

    @Test
    public void testGetOutcome() throws WikiException
    {
        Assertions.assertEquals(Outcome.STEP_CONTINUE,m_decision.getOutcome());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        Assertions.assertEquals(Outcome.DECISION_APPROVE, m_decision.getOutcome());
    }

    @Test
    public void testGetStartTime() throws WikiException
    {
        Assertions.assertEquals(Workflow.TIME_NOT_SET, m_decision.getStartTime());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        Assertions.assertTrue((Workflow.TIME_NOT_SET  !=  m_decision.getStartTime()));
    }

    @Test
    public void testGetWorkflow()
    {
        Assertions.assertEquals(m_workflow, m_decision.getWorkflow());
    }

    @Test
    public void testIsCompleted() throws WikiException
    {
        Assertions.assertFalse(m_decision.isCompleted());
        m_decision.start();
        m_decision.decide(Outcome.DECISION_APPROVE);
        Assertions.assertTrue(m_decision.isCompleted());
    }

    @Test
    public void testIsStarted() throws WikiException
    {
        Assertions.assertFalse(m_decision.isStarted());
        m_decision.start();
        Assertions.assertTrue(m_decision.isStarted());
    }

    @Test
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
        Assertions.fail("Decision allowed itself to be started twice!");
    }

}
