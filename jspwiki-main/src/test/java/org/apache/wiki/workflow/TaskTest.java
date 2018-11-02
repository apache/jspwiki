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

public class TaskTest
{

    Workflow m_workflow;
    Task m_task;

    /** Sample Task that completes normally. */
    public static class NormalTask extends Task
    {
        private static final long serialVersionUID = 1L;

        public NormalTask(Workflow workflow)
        {
            super(workflow, "task.normal");
        }
        public Outcome execute() throws WikiException
        {
            return Outcome.STEP_COMPLETE;
        }

    }

    /** Sample Task that encounters an error during processing. */
    public static class ErrorTask extends Task
    {
        private static final long serialVersionUID = 1L;

        public ErrorTask(Workflow workflow)
        {
            super(workflow, "task.error");
        }
        public Outcome execute() throws WikiException
        {
            addError("Found an error.");
            addError("Found a second one!");
            return Outcome.STEP_ABORT;
        }

    }

    @BeforeEach
    public void setUp() throws Exception
    {

        m_workflow = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        m_task = new NormalTask(m_workflow);
    }

    @Test
    public void testGetActor()
    {
       Assertions.assertNotSame(new WikiPrincipal("Actor1"), m_task.getActor());
       Assertions.assertEquals(SystemPrincipal.SYSTEM_USER, m_task.getActor());
    }

    @Test
    public void testSuccessors()
    {
        // If task finishes normally, branch to a decision (d1)
        Step d1 = new SimpleDecision(m_workflow, "decision1.key", new WikiPrincipal("Actor1"));
        m_task.addSuccessor(Outcome.STEP_COMPLETE, d1);

        // If the task aborts, branch to an alternate decision (d2)
        Step d2 = new SimpleDecision(m_workflow, "decision2.key", new WikiPrincipal("Actor2"));
        m_task.addSuccessor(Outcome.STEP_ABORT, d2);

        Assertions.assertEquals(d1, m_task.getSuccessor(Outcome.STEP_COMPLETE));
        Assertions.assertEquals(d2, m_task.getSuccessor(Outcome.STEP_ABORT));

        // The other Outcomes should return null when looked up
        Assertions.assertNull(m_task.getSuccessor(Outcome.DECISION_APPROVE));
        Assertions.assertNull(m_task.getSuccessor(Outcome.DECISION_DENY));
        Assertions.assertNull(m_task.getSuccessor(Outcome.DECISION_HOLD));
        Assertions.assertNull(m_task.getSuccessor(Outcome.DECISION_REASSIGN));
        Assertions.assertNull(m_task.getSuccessor(Outcome.STEP_CONTINUE));
    }

    @Test
    public void testErrors()
    {
        m_task.addError("Error deciding something.");
        m_task.addError("Error deciding something else.");

        List< String > errors = m_task.getErrors();
        Assertions.assertEquals(2, errors.size());
        Assertions.assertEquals("Error deciding something.", errors.get(0));
        Assertions.assertEquals("Error deciding something else.", errors.get(1));
    }

    @Test
    public void testAvailableOutcomes()
    {
        Collection< Outcome > outcomes = m_task.getAvailableOutcomes();
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_APPROVE));
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_DENY));
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_HOLD));
        Assertions.assertFalse(outcomes.contains(Outcome.DECISION_REASSIGN));
        Assertions.assertTrue(outcomes.contains(Outcome.STEP_ABORT));
        Assertions.assertTrue(outcomes.contains(Outcome.STEP_COMPLETE));
    }

    @Test
    public void testGetEndTime() throws WikiException
    {
        Assertions.assertEquals(Workflow.TIME_NOT_SET, m_task.getEndTime());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assertions.assertTrue((Workflow.TIME_NOT_SET  !=  m_task.getEndTime()));
    }

    @Test
    public void testGetMessageKey()
    {
        Assertions.assertEquals("task.normal",m_task.getMessageKey());
    }

    @Test
    public void testGetOutcome() throws WikiException
    {
        Assertions.assertEquals(Outcome.STEP_CONTINUE,m_task.getOutcome());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assertions.assertEquals(Outcome.STEP_COMPLETE, m_task.getOutcome());

        // Test the "error task"
        m_task = new ErrorTask(m_workflow);
        Assertions.assertEquals(Outcome.STEP_CONTINUE,m_task.getOutcome());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assertions.assertEquals(Outcome.STEP_ABORT, m_task.getOutcome());
    }

    @Test
    public void testGetStartTime() throws WikiException
    {
        Assertions.assertEquals(Workflow.TIME_NOT_SET, m_task.getStartTime());
        m_task.start();
        m_task.execute();
        Assertions.assertTrue((Workflow.TIME_NOT_SET  !=  m_task.getStartTime()));
    }

    @Test
    public void testGetWorkflow()
    {
        Assertions.assertEquals(m_workflow, m_task.getWorkflow());
    }

    @Test
    public void testIsCompleted() throws WikiException
    {
        Assertions.assertFalse(m_task.isCompleted());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assertions.assertTrue(m_task.isCompleted());
    }

    @Test
    public void testIsStarted() throws WikiException
    {
        Assertions.assertFalse(m_task.isStarted());
        m_task.start();
        Assertions.assertTrue(m_task.isStarted());
    }

    @Test
    public void testStartTwice() throws WikiException
    {
        m_task.start();
        try
        {
            m_task.start();
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
