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

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Collection;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;

public class DecisionQueueTest
{

    TestEngine m_engine;

    DecisionQueue m_queue;

    Workflow w;

    Decision d1;

    Decision d2;

    Decision d3;

    WikiSession janneSession;

    WikiSession adminSession;

    @BeforeEach
    public void setUp() throws Exception
    {

        Properties props = TestEngine.getTestProperties();
        m_engine = new TestEngine(props);
        m_queue = m_engine.getWorkflowManager().getDecisionQueue();
        adminSession = m_engine.adminSession();
        janneSession = m_engine.janneSession();
        w = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        w.setWorkflowManager(m_engine.getWorkflowManager());
        d1 = new SimpleDecision(w, "decision1.key", new GroupPrincipal("Admin"));
        d2 = new SimpleDecision(w, "decision2.key", new WikiPrincipal("Owner2"));
        d3 = new SimpleDecision(w, "decision3.key", janneSession.getUserPrincipal());
        m_queue.add(d1);
        m_queue.add(d2);
        m_queue.add(d3);
    }

    @Test
    public void testAdd()
    {
        Decision[] decisions = m_queue.decisions();
        Assertions.assertEquals(d1, decisions[0]);
        Assertions.assertEquals(d2, decisions[1]);
        Assertions.assertEquals(d3, decisions[2]);
    }

    @Test
    public void testRemove()
    {
        Decision[] decisions = m_queue.decisions();
        Assertions.assertEquals(3, decisions.length);

        m_queue.remove(d2);
        decisions = m_queue.decisions();
        Assertions.assertEquals(2, decisions.length);
        Assertions.assertEquals(d1, decisions[0]);
        Assertions.assertEquals(d3, decisions[1]);

        m_queue.remove(d1);
        decisions = m_queue.decisions();
        Assertions.assertEquals(1, decisions.length);
        Assertions.assertEquals(d3, decisions[0]);

        m_queue.remove(d3);
        decisions = m_queue.decisions();
        Assertions.assertEquals(0, decisions.length);
    }

    @Test
    public void testComplete() throws WikiException
    {
        Assertions.assertEquals(3, m_queue.decisions().length);

        // Execute the competion for decision 1 (approve/deny)
        m_queue.decide(d1, Outcome.DECISION_APPROVE);

        // Decision should be marked completed, and removed from queue
        Assertions.assertTrue(d1.isCompleted());
        Assertions.assertEquals(2, m_queue.decisions().length);

        // Execute the competion for decision 2 (approve/deny/hold)
        m_queue.decide(d2, Outcome.DECISION_DENY);

        // Decision should be marked completed, and removed from queue
        Assertions.assertTrue(d2.isCompleted());
        Assertions.assertEquals(1, m_queue.decisions().length);
    }

    @Test
    public void testReassign() throws WikiException
    {
        // Janne owns 1 decision (d3)
        Assertions.assertEquals(janneSession.getUserPrincipal(), d3.getActor());
        Assertions.assertEquals(1, m_queue.getActorDecisions(janneSession).size());

        // Reassign the decision
        m_queue.reassign(d3, new WikiPrincipal("NewOwner"));

        // d3 should have a different owner now, and it won't show up in
        // Janne's decision list
        Assertions.assertEquals(new WikiPrincipal("NewOwner"), d3.getActor());
        Assertions.assertEquals(0, m_queue.getActorDecisions(janneSession).size());
    }

    @Test
    public void testDecisions()
    {
        Decision[] decisions = m_queue.decisions();
        Assertions.assertEquals(3, decisions.length);
        Assertions.assertEquals(d1, decisions[0]);
        Assertions.assertEquals(d2, decisions[1]);
        Assertions.assertEquals(d3, decisions[2]);
    }

    @Test
    public void testActorDecisions()
    {
        Collection< Decision > decisions = m_queue.getActorDecisions(adminSession);
        Assertions.assertEquals(1, decisions.size());

        decisions = m_queue.getActorDecisions(janneSession);
        Assertions.assertEquals(1, decisions.size());
    }

    @Test
    public void testDecisionWorkflow() throws WikiException
    {
        Principal janne = janneSession.getUserPrincipal();

        // Clean out the queue first
        m_queue.remove(d1);
        m_queue.remove(d2);
        m_queue.remove(d3);

        // Create a workflow with 3 steps, with a Decision for Janne in the middle
        w = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        w.setWorkflowManager(m_engine.getWorkflowManager());
        Step startTask = new TaskTest.NormalTask(w);
        Step endTask = new TaskTest.NormalTask(w);
        Decision decision = new SimpleDecision(w, "decision.Actor1Decision", janne);
        startTask.addSuccessor(Outcome.STEP_COMPLETE, decision);
        decision.addSuccessor(Outcome.DECISION_APPROVE, endTask);
        w.setFirstStep(startTask);

        // Start the workflow, and verify that the Decision is the current Step
        w.start();
        Assertions.assertEquals(decision, w.getCurrentStep());

        // Verify that it's also in Janne's DecisionQueue
        Collection< Decision > decisions = m_queue.getActorDecisions(janneSession);
        Assertions.assertEquals(1, decisions.size());
        Decision d = (Decision)decisions.iterator().next();
        Assertions.assertEquals(decision, d);

        // Make Decision, and verify that it's gone from the queue
        m_queue.decide(decision, Outcome.DECISION_APPROVE);
        decisions = m_queue.getActorDecisions(janneSession);
        Assertions.assertEquals(0, decisions.size());
    }

}
