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

import java.util.Locale;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OutcomeTest
{

    @Test
    public void testGetKey()
    {
        Assertions.assertEquals("outcome.decision.approve", Outcome.DECISION_APPROVE.getMessageKey());
        Assertions.assertEquals("outcome.decision.hold", Outcome.DECISION_HOLD.getMessageKey());
        Assertions.assertEquals("outcome.decision.deny", Outcome.DECISION_DENY.getMessageKey());
        Assertions.assertEquals("outcome.decision.reassign", Outcome.DECISION_REASSIGN.getMessageKey());
    }

    @Test
    public void testHashCode()
    {
        Assertions.assertEquals("outcome.decision.approve".hashCode(), Outcome.DECISION_APPROVE.hashCode());
        Assertions.assertEquals("outcome.decision.hold".hashCode()*2, Outcome.DECISION_HOLD.hashCode());
        Assertions.assertEquals("outcome.decision.deny".hashCode(), Outcome.DECISION_DENY.hashCode());
        Assertions.assertEquals("outcome.decision.reassign".hashCode()*2, Outcome.DECISION_REASSIGN.hashCode());
    }

    @Test
    public void testEquals()
    {
        Assertions.assertEquals(Outcome.DECISION_APPROVE, Outcome.DECISION_APPROVE);
        Assertions.assertNotSame(Outcome.DECISION_APPROVE, Outcome.DECISION_REASSIGN);
    }

    @Test
    public void testMessage() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        WikiEngine engine = new TestEngine(props);
        InternationalizationManager i18n = engine.getInternationalizationManager();
        String core = "templates.default";
        Locale rootLocale = Locale.ROOT;
        Outcome o;

        o = Outcome.DECISION_APPROVE;
        Assertions.assertEquals("Approve", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_DENY;
        Assertions.assertEquals("Deny", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_HOLD;
        Assertions.assertEquals("Hold", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_REASSIGN;
        Assertions.assertEquals("Reassign", i18n.get(core, rootLocale, o.getMessageKey()));
    }

    @Test
    public void testIsCompletion()
    {
        Assertions.assertTrue(Outcome.DECISION_ACKNOWLEDGE.isCompletion());
        Assertions.assertTrue(Outcome.DECISION_APPROVE.isCompletion());
        Assertions.assertTrue(Outcome.DECISION_DENY.isCompletion());
        Assertions.assertFalse(Outcome.DECISION_HOLD.isCompletion());
        Assertions.assertFalse(Outcome.DECISION_REASSIGN.isCompletion());
        Assertions.assertTrue(Outcome.STEP_ABORT.isCompletion());
        Assertions.assertTrue(Outcome.STEP_COMPLETE.isCompletion());
        Assertions.assertFalse(Outcome.STEP_CONTINUE.isCompletion());
    }

    @Test
    public void testForName()
    {
        try
        {
            Assertions.assertEquals(Outcome.DECISION_ACKNOWLEDGE, Outcome.forName("outcome.decision.acknowledge"));
            Assertions.assertEquals(Outcome.DECISION_APPROVE, Outcome.forName("outcome.decision.approve"));
            Assertions.assertEquals(Outcome.DECISION_DENY, Outcome.forName("outcome.decision.deny"));
            Assertions.assertEquals(Outcome.DECISION_HOLD, Outcome.forName("outcome.decision.hold"));
            Assertions.assertEquals(Outcome.DECISION_REASSIGN, Outcome.forName("outcome.decision.reassign"));
            Assertions.assertEquals(Outcome.STEP_ABORT, Outcome.forName("outcome.step.abort"));
            Assertions.assertEquals(Outcome.STEP_COMPLETE, Outcome.forName("outcome.step.complete"));
            Assertions.assertEquals(Outcome.STEP_CONTINUE, Outcome.forName("outcome.step.continue"));
        }
        catch (NoSuchOutcomeException e)
        {
            // We should never get here
            Assertions.fail("Could not look up an Outcome...");
        }

        // Look for a non-existent one
        try
        {
            Outcome.forName("outcome.decision.nonexistent");
        }
        catch (NoSuchOutcomeException e)
        {
            return;
        }
        // We should never get here
        Assertions.fail("Could not look up an Outcome...");
    }

}
