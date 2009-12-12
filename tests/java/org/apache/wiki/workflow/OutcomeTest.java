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
package org.apache.wiki.workflow;

import java.util.Locale;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.workflow.NoSuchOutcomeException;
import org.apache.wiki.workflow.Outcome;

import junit.framework.TestCase;


public class OutcomeTest extends TestCase
{

    public void testGetKey()
    {
        assertEquals("decision.approve", Outcome.DECISION_APPROVE.getMessageKey());
        assertEquals("decision.hold", Outcome.DECISION_HOLD.getMessageKey());
        assertEquals("decision.deny", Outcome.DECISION_DENY.getMessageKey());
        assertEquals("decision.reassign", Outcome.DECISION_REASSIGN.getMessageKey());
    }

    public void testHashCode()
    {
        assertEquals("decision.approve".hashCode(), Outcome.DECISION_APPROVE.hashCode());
        assertEquals("decision.hold".hashCode()*2, Outcome.DECISION_HOLD.hashCode());
        assertEquals("decision.deny".hashCode(), Outcome.DECISION_DENY.hashCode());
        assertEquals("decision.reassign".hashCode()*2, Outcome.DECISION_REASSIGN.hashCode());
    }

    public void testEquals()
    {
        assertEquals(Outcome.DECISION_APPROVE, Outcome.DECISION_APPROVE);
        assertNotSame(Outcome.DECISION_APPROVE, Outcome.DECISION_REASSIGN);
    }

    public void testMessage() throws Exception
    {
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());
        WikiEngine engine = new TestEngine(props);
        InternationalizationManager i18n = engine.getInternationalizationManager();
        String core = "CoreResources";
        Locale english = Locale.ENGLISH;
        Outcome o;
        String key;

        o = Outcome.DECISION_APPROVE;
        key = "Outcome." + o.getMessageKey();
        assertEquals("Approve", i18n.get( core, english, key ) );

        o = Outcome.DECISION_DENY;
        key = "Outcome." + o.getMessageKey();
        assertEquals("Deny", i18n.get( core, english, key ) );

        o = Outcome.DECISION_HOLD;
        key = "Outcome." + o.getMessageKey();
        assertEquals("Hold", i18n.get( core, english, key ) );

        o = Outcome.DECISION_REASSIGN;
        key = "Outcome." + o.getMessageKey();
        assertEquals("Reassign", i18n.get( core, english, key ) );

        engine.shutdown();
    }

    public void testIsCompletion()
    {
        assertTrue(Outcome.DECISION_ACKNOWLEDGE.isCompletion());
        assertTrue(Outcome.DECISION_APPROVE.isCompletion());
        assertTrue(Outcome.DECISION_DENY.isCompletion());
        assertFalse(Outcome.DECISION_HOLD.isCompletion());
        assertFalse(Outcome.DECISION_REASSIGN.isCompletion());
        assertTrue(Outcome.STEP_ABORT.isCompletion());
        assertTrue(Outcome.STEP_COMPLETE.isCompletion());
        assertFalse(Outcome.STEP_CONTINUE.isCompletion());
    }

    public void testForName()
    {
        try
        {
            assertEquals(Outcome.DECISION_ACKNOWLEDGE, Outcome.forName("decision.acknowledge"));
            assertEquals(Outcome.DECISION_APPROVE, Outcome.forName("decision.approve"));
            assertEquals(Outcome.DECISION_DENY, Outcome.forName("decision.deny"));
            assertEquals(Outcome.DECISION_HOLD, Outcome.forName("decision.hold"));
            assertEquals(Outcome.DECISION_REASSIGN, Outcome.forName("decision.reassign"));
            assertEquals(Outcome.STEP_ABORT, Outcome.forName("step.abort"));
            assertEquals(Outcome.STEP_COMPLETE, Outcome.forName("step.complete"));
            assertEquals(Outcome.STEP_CONTINUE, Outcome.forName("step.continue"));
        }
        catch (NoSuchOutcomeException e)
        {
            // We should never get here
            fail("Could not look up an Outcome...");
        }

        // Look for a non-existent one
        try
        {
            Outcome.forName("decision.nonexistent");
        }
        catch (NoSuchOutcomeException e)
        {
            return;
        }
        // We should never get here
        fail("Could not look up an Outcome...");
    }

}
