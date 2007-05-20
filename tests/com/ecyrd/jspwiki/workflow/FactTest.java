package com.ecyrd.jspwiki.workflow;

import junit.framework.TestCase;

public class FactTest extends TestCase
{

    public void testCreate()
    {
        Fact f1 = new Fact("fact1",new Integer(1));
        Fact f2 = new Fact("fact2","A factual String");
        Fact f3 = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);

        assertEquals("fact1", f1.getMessageKey());
        assertEquals("fact2", f2.getMessageKey());
        assertEquals("fact3", f3.getMessageKey());

        assertEquals(new Integer(1), f1.getValue());
        assertEquals("A factual String", f2.getValue());
        assertEquals(Outcome.DECISION_ACKNOWLEDGE, f3.getValue());
    }

}
