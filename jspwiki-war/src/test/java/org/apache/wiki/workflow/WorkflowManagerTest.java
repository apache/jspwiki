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

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkflowManagerTest
{
    protected Workflow w;
    protected WorkflowManager wm;
    protected WikiEngine m_engine;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
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

    @Test
    public void testStart() throws WikiException
    {
        // Once we start the workflow, it should show that it's started
        // and the WM should have assigned it an ID
        Assertions.assertEquals(Workflow.ID_NOT_SET, w.getId());
        Assertions.assertFalse(w.isStarted());
        wm.start(w);
        Assertions.assertFalse(Workflow.ID_NOT_SET == w.getId());
        Assertions.assertTrue(w.isStarted());
    }

    @Test
    public void testWorkflows() throws WikiException
    {
        // There should be no workflows in the cache, and none in completed list
        Assertions.assertEquals(0, wm.getWorkflows().size());
        Assertions.assertEquals(0, wm.getCompletedWorkflows().size());

        // After starting, there should be 1 in the cache, with ID=1
        wm.start(w);
        Assertions.assertEquals(1, wm.getWorkflows().size());
        Assertions.assertEquals(0, wm.getCompletedWorkflows().size());
        Workflow workflow = (Workflow)wm.getWorkflows().iterator().next();
        Assertions.assertEquals(w, workflow);
        Assertions.assertEquals(1, workflow.getId());

        // After forcing a decision on step 2, the workflow should complete and vanish from the cache
        Decision d = (Decision)w.getCurrentStep();
        d.decide(Outcome.DECISION_APPROVE);
        Assertions.assertEquals(0, wm.getWorkflows().size());
        Assertions.assertEquals(1, wm.getCompletedWorkflows().size());
    }

    @Test
    public void testRequiresApproval()
    {
        // Test properties says we need approvals for workflow.saveWikiPage & workflow.foo
        Assertions.assertFalse(wm.requiresApproval("workflow.saveWikiPage"));
        Assertions.assertTrue(wm.requiresApproval("workflow.foo"));
        Assertions.assertTrue(wm.requiresApproval("workflow.bar"));
    }

    @Test
    public void testGetApprover() throws WikiException
    {
        // Test properties says workflow.saveWikiPage approver is GP Admin; workflow.foo is 'janne'
        Assertions.assertEquals(new WikiPrincipal("janne", WikiPrincipal.LOGIN_NAME), wm.getApprover("workflow.foo"));
        Assertions.assertEquals(new GroupPrincipal("Admin"), wm.getApprover("workflow.bar"));

        // 'saveWikiPage' workflow doesn't require approval, so we will need to catch an Exception
        try
        {
            Assertions.assertEquals(new GroupPrincipal("Admin"), wm.getApprover("workflow.saveWikiPage"));
        }
        catch (WikiException e)
        {
            // Swallow
            return;
        }
        // We should never get here
        Assertions.fail("Workflow.bar doesn't need approval!");
    }

}
