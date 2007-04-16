package com.ecyrd.jspwiki.workflow;

import java.security.Principal;

import com.ecyrd.jspwiki.WikiException;

/**
 * Decision subclass used for notifications that includes only one available Outcome:
 * {@link Outcome#DECISION_ACKNOWLEDGE}. The Decision is not reassignable, and
 * the default Outcome is {@link Outcome#DECISION_ACKNOWLEDGE}.
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public final class SimpleNotification extends Decision
{

    /**
     * Constructs a new SimpleNotification object with a supplied message key,
     * associated Workflow, and named actor who must acknowledge the message.
     * The notification is placed in the Principal's list of queued Decisions.
     * Because the only available Outcome is
     * {@link Outcome#DECISION_ACKNOWLEDGE}, the actor can only acknowledge the
     * message.
     * 
     * @param workflow
     *            the Workflow to associate this notification with
     * @param messageKey
     *            the message key
     * @param actor
     *            the Principal who will acknowledge the message
     */
    public SimpleNotification(Workflow workflow, String messageKey, Principal actor)
    {
        super(workflow, messageKey, actor, Outcome.DECISION_ACKNOWLEDGE);
    }
    
    /**
     * Convenience method that simply calls {@link #decide(Outcome)} 
     * with the value {@link Outcome#DECISION_ACKNOWLEDGE}.
     */
    public void acknowledge() throws WikiException
    {
        this.decide( Outcome.DECISION_ACKNOWLEDGE );
    }

    /**
     * Notifications cannot be re-assigned, so this method always returns
     * <code>false</code>.
     */
    public final boolean isReassignable()
    {
        return false;
    }

}
