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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() throws Exception
    {

        m_workflow = new Workflow("workflow.key", new WikiPrincipal("Owner1"));
        m_task = new NormalTask(m_workflow);
    }

    @Test
    public void testGetActor()
    {
       Assert.assertNotSame(new WikiPrincipal("Actor1"), m_task.getActor());
       Assert.assertEquals(SystemPrincipal.SYSTEM_USER, m_task.getActor());
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

        Assert.assertEquals(d1, m_task.getSuccessor(Outcome.STEP_COMPLETE));
        Assert.assertEquals(d2, m_task.getSuccessor(Outcome.STEP_ABORT));

        // The other Outcomes should return null when looked up
        Assert.assertNull(m_task.getSuccessor(Outcome.DECISION_APPROVE));
        Assert.assertNull(m_task.getSuccessor(Outcome.DECISION_DENY));
        Assert.assertNull(m_task.getSuccessor(Outcome.DECISION_HOLD));
        Assert.assertNull(m_task.getSuccessor(Outcome.DECISION_REASSIGN));
        Assert.assertNull(m_task.getSuccessor(Outcome.STEP_CONTINUE));
    }

    @Test
    public void testErrors()
    {
        m_task.addError("Error deciding something.");
        m_task.addError("Error deciding something else.");

        List errors = m_task.getErrors();
        Assert.assertEquals(2, errors.size());
        Assert.assertEquals("Error deciding something.", errors.get(0));
        Assert.assertEquals("Error deciding something else.", errors.get(1));
    }

    @Test
    public void testAvailableOutcomes()
    {
        Collection outcomes = m_task.getAvailableOutcomes();
        Assert.assertFalse(outcomes.contains(Outcome.DECISION_APPROVE));
        Assert.assertFalse(outcomes.contains(Outcome.DECISION_DENY));
        Assert.assertFalse(outcomes.contains(Outcome.DECISION_HOLD));
        Assert.assertFalse(outcomes.contains(Outcome.DECISION_REASSIGN));
        Assert.assertTrue(outcomes.contains(Outcome.STEP_ABORT));
        Assert.assertTrue(outcomes.contains(Outcome.STEP_COMPLETE));
    }

    @Test
    public void testGetEndTime() throws WikiException
    {
        Assert.assertEquals(Workflow.TIME_NOT_SET, m_task.getEndTime());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assert.assertTrue((Workflow.TIME_NOT_SET  !=  m_task.getEndTime()));
    }

    @Test
    public void testGetMessageKey()
    {
        Assert.assertEquals("task.normal",m_task.getMessageKey());
    }

    @Test
    public void testGetOutcome() throws WikiException
    {
        Assert.assertEquals(Outcome.STEP_CONTINUE,m_task.getOutcome());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assert.assertEquals(Outcome.STEP_COMPLETE, m_task.getOutcome());

        // Test the "error task"
        m_task = new ErrorTask(m_workflow);
        Assert.assertEquals(Outcome.STEP_CONTINUE,m_task.getOutcome());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assert.assertEquals(Outcome.STEP_ABORT, m_task.getOutcome());
    }

    @Test
    public void testGetStartTime() throws WikiException
    {
        Assert.assertEquals(Workflow.TIME_NOT_SET, m_task.getStartTime());
        m_task.start();
        m_task.execute();
        Assert.assertTrue((Workflow.TIME_NOT_SET  !=  m_task.getStartTime()));
    }

    @Test
    public void testGetWorkflow()
    {
        Assert.assertEquals(m_workflow, m_task.getWorkflow());
    }

    @Test
    public void testIsCompleted() throws WikiException
    {
        Assert.assertFalse(m_task.isCompleted());
        m_task.start();
        m_task.setOutcome(m_task.execute());
        Assert.assertTrue(m_task.isCompleted());
    }

    @Test
    public void testIsStarted() throws WikiException
    {
        Assert.assertFalse(m_task.isStarted());
        m_task.start();
        Assert.assertTrue(m_task.isStarted());
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
        Assert.fail("Decision allowed itself to be started twice!");
    }

}
