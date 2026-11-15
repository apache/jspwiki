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

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.wiki.TestEngine.with;

/**
 * Several engines living in the same JVM, e.g. several wikis deployed with a shared classpath, must not see each other's workflows.
 */
class MultiEngineWorkflowTest {

    TestEngine engineA;
    TestEngine engineB;

    @BeforeEach
    void setUp() {
        engineA = TestEngine.build( with( "jspwiki.approver.workflow.saveWikiPage", "Admin" ) );
        engineB = TestEngine.build( with( "jspwiki.approver.workflow.saveWikiPage", "Admin" ) );
    }

    @AfterEach
    void tearDown() {
        engineA.stop();
        engineB.stop();
    }

    @Test
    void testPageSaveApprovalStaysInTheEngineThatStartedIt() throws Exception {
        final WorkflowManager managerA = engineA.getManager( WorkflowManager.class );
        final WorkflowManager managerB = engineB.getManager( WorkflowManager.class );

        Assertions.assertThrows( DecisionRequiredException.class, () -> engineA.saveText( "Main", "saved in A" ) );

        Assertions.assertEquals( 1, managerA.getWorkflows().size() );
        Assertions.assertEquals( 1, managerA.getDecisionQueue().decisions().length );
        Assertions.assertEquals( 0, managerB.getWorkflows().size() );
        Assertions.assertEquals( 0, managerB.getDecisionQueue().decisions().length );
    }

    @Test
    void testWorkflowsOfBothEnginesAreKeptApart() throws Exception {
        final WorkflowManager managerA = engineA.getManager( WorkflowManager.class );
        final WorkflowManager managerB = engineB.getManager( WorkflowManager.class );

        Assertions.assertThrows( DecisionRequiredException.class, () -> engineA.saveText( "Main", "saved in A" ) );
        Assertions.assertThrows( DecisionRequiredException.class, () -> engineB.saveText( "Main", "saved in B" ) );

        Assertions.assertEquals( 1, managerA.getWorkflows().size() );
        Assertions.assertEquals( 1, managerB.getWorkflows().size() );
        Assertions.assertNotEquals( managerA.getWorkflows().iterator().next().getId(), managerB.getWorkflows().iterator().next().getId() );
        Assertions.assertEquals( 1, managerA.getDecisionQueue().decisions().length );
        Assertions.assertEquals( 1, managerB.getDecisionQueue().decisions().length );
    }

    @Test
    void testApprovingInOneEngineDoesNotTouchTheOther() throws Exception {
        final WorkflowManager managerA = engineA.getManager( WorkflowManager.class );
        final WorkflowManager managerB = engineB.getManager( WorkflowManager.class );
        Assertions.assertThrows( DecisionRequiredException.class, () -> engineA.saveText( "Main", "saved in A" ) );
        Assertions.assertThrows( DecisionRequiredException.class, () -> engineB.saveText( "Main", "saved in B" ) );

        final Context context = Wiki.context().create( engineA, Wiki.contents().page( engineA, "Main" ) );
        managerA.getDecisionQueue().decisions()[ 0 ].decide( Outcome.DECISION_APPROVE, context );

        Assertions.assertTrue( engineA.getManager( PageManager.class ).wikiPageExists( "Main" ) );
        Assertions.assertEquals( 0, managerA.getWorkflows().size() );
        Assertions.assertEquals( 1, managerA.getCompletedWorkflows().size() );
        Assertions.assertEquals( 1, managerB.getWorkflows().size() );
        Assertions.assertEquals( 0, managerB.getCompletedWorkflows().size() );
        Assertions.assertEquals( 1, managerB.getDecisionQueue().decisions().length );
    }

}
