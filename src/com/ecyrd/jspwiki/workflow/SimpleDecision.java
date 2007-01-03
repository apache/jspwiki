package com.ecyrd.jspwiki.workflow;

import java.security.Principal;

/**
 * Decision subclass that includes five available Outcomes:
 * {@link Outcome#DECISION_APPROVE}, {@link Outcome#DECISION_DENY},
 * {@link Outcome#DECISION_HOLD}, {@link Outcome#DECISION_REASSIGN} and
 * {@link Outcome#STEP_ABORT}. The Decision is reassignable, and the default
 * Outcome is {@link Outcome#DECISION_APPROVE}.
 * 
 * @author Andrew Jaquith
 */
public class SimpleDecision extends Decision
{

    public SimpleDecision(Workflow workflow, String messageKey, Principal actor)
    {
        super(workflow, messageKey, actor, Outcome.DECISION_APPROVE);

        // Add the four other default outcomes
        super.addSuccessor(Outcome.DECISION_DENY, null);
        super.addSuccessor(Outcome.DECISION_HOLD, null);
        super.addSuccessor(Outcome.DECISION_REASSIGN, null);
        super.addSuccessor(Outcome.STEP_ABORT, null);
    }

}
