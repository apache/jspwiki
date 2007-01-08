package com.ecyrd.jspwiki.workflow;

import java.security.Principal;

/**
 * Decision subclass that includes two available Outcomes:
 * {@link Outcome#DECISION_APPROVE} or {@link Outcome#DECISION_DENY}.
 * The Decision is reassignable, and the default Outcome is 
 * {@link Outcome#DECISION_APPROVE}.
 * 
 * @author Andrew Jaquith
 */
public class SimpleDecision extends Decision
{

    public SimpleDecision(Workflow workflow, String messageKey, Principal actor)
    {
        super(workflow, messageKey, actor, Outcome.DECISION_APPROVE);

        // Add the other default outcomes
        super.addSuccessor(Outcome.DECISION_DENY, null);
    }

}
