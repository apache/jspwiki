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
import org.junit.Assert;
import org.junit.Test;

public class OutcomeTest
{

    @Test
    public void testGetKey()
    {
        Assert.assertEquals("outcome.decision.approve", Outcome.DECISION_APPROVE.getMessageKey());
        Assert.assertEquals("outcome.decision.hold", Outcome.DECISION_HOLD.getMessageKey());
        Assert.assertEquals("outcome.decision.deny", Outcome.DECISION_DENY.getMessageKey());
        Assert.assertEquals("outcome.decision.reassign", Outcome.DECISION_REASSIGN.getMessageKey());
    }

    @Test
    public void testHashCode()
    {
        Assert.assertEquals("outcome.decision.approve".hashCode(), Outcome.DECISION_APPROVE.hashCode());
        Assert.assertEquals("outcome.decision.hold".hashCode()*2, Outcome.DECISION_HOLD.hashCode());
        Assert.assertEquals("outcome.decision.deny".hashCode(), Outcome.DECISION_DENY.hashCode());
        Assert.assertEquals("outcome.decision.reassign".hashCode()*2, Outcome.DECISION_REASSIGN.hashCode());
    }

    @Test
    public void testEquals()
    {
        Assert.assertEquals(Outcome.DECISION_APPROVE, Outcome.DECISION_APPROVE);
        Assert.assertNotSame(Outcome.DECISION_APPROVE, Outcome.DECISION_REASSIGN);
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
        Assert.assertEquals("Approve", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_DENY;
        Assert.assertEquals("Deny", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_HOLD;
        Assert.assertEquals("Hold", i18n.get(core, rootLocale, o.getMessageKey()));

        o = Outcome.DECISION_REASSIGN;
        Assert.assertEquals("Reassign", i18n.get(core, rootLocale, o.getMessageKey()));
    }

    @Test
    public void testIsCompletion()
    {
        Assert.assertTrue(Outcome.DECISION_ACKNOWLEDGE.isCompletion());
        Assert.assertTrue(Outcome.DECISION_APPROVE.isCompletion());
        Assert.assertTrue(Outcome.DECISION_DENY.isCompletion());
        Assert.assertFalse(Outcome.DECISION_HOLD.isCompletion());
        Assert.assertFalse(Outcome.DECISION_REASSIGN.isCompletion());
        Assert.assertTrue(Outcome.STEP_ABORT.isCompletion());
        Assert.assertTrue(Outcome.STEP_COMPLETE.isCompletion());
        Assert.assertFalse(Outcome.STEP_CONTINUE.isCompletion());
    }

    @Test
    public void testForName()
    {
        try
        {
            Assert.assertEquals(Outcome.DECISION_ACKNOWLEDGE, Outcome.forName("outcome.decision.acknowledge"));
            Assert.assertEquals(Outcome.DECISION_APPROVE, Outcome.forName("outcome.decision.approve"));
            Assert.assertEquals(Outcome.DECISION_DENY, Outcome.forName("outcome.decision.deny"));
            Assert.assertEquals(Outcome.DECISION_HOLD, Outcome.forName("outcome.decision.hold"));
            Assert.assertEquals(Outcome.DECISION_REASSIGN, Outcome.forName("outcome.decision.reassign"));
            Assert.assertEquals(Outcome.STEP_ABORT, Outcome.forName("outcome.step.abort"));
            Assert.assertEquals(Outcome.STEP_COMPLETE, Outcome.forName("outcome.step.complete"));
            Assert.assertEquals(Outcome.STEP_CONTINUE, Outcome.forName("outcome.step.continue"));
        }
        catch (NoSuchOutcomeException e)
        {
            // We should never get here
            Assert.fail("Could not look up an Outcome...");
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
        Assert.fail("Could not look up an Outcome...");
    }

}
