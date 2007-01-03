package com.ecyrd.jspwiki.workflow;

/**
 * Resolution of a workflow Step, such as "approve," "deny," "hold," "task
 * error," or other potential resolutions.
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public final class Outcome implements Cloneable
{

    /** Complete workflow step (without errors) */
    public static final Outcome STEP_COMPLETE = new Outcome("outcome.step.complete", true);

    /** Terminate workflow step (without errors) */
    public static final Outcome STEP_ABORT = new Outcome("outcome.step.abort", true);

    /** Continue workflow step (without errors) */
    public static final Outcome STEP_CONTINUE = new Outcome("outcome.step.continue", false);

    public static final Outcome DECISION_ACKNOWLEDGE = new Outcome("outcome.decision.acknowledge", true);

    public static final Outcome DECISION_APPROVE = new Outcome("outcome.decision.approve", true);

    public static final Outcome DECISION_DENY = new Outcome("outcome.decision.deny", true);

    public static final Outcome DECISION_HOLD = new Outcome("outcome.decision.hold", false);

    public static final Outcome DECISION_REASSIGN = new Outcome("outcome.decision.reassign", false);

    private final String m_key;

    private final boolean m_completion;

    private Outcome(String key, boolean completion)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        m_key = key;
        m_completion = completion;
    }

    /**
     * Returns <code>true</code> if this Outcome represents a completion
     * condition for a Step.
     * 
     * @return the result
     */
    public boolean isCompletion()
    {
        return m_completion;
    }

    /**
     * The i18n key for this outcome, which is prefixed by <code>outcome.</code>.
     * If calling classes wish to return a locale-specific name for this task
     * (such as "approve this request"), they can use this method to obtain the
     * correct key suffix.
     * 
     * @return the i18n key for this outcome
     */
    public String getMessageKey()
    {
        return m_key;
    }

    public Object clone()
    {
        return new Outcome(m_key, m_completion);
    }

    public int hashCode()
    {
        return m_key.hashCode() * (m_completion ? 1 : 2);
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Outcome))
        {
            return false;
        }
        return m_key.equals(((Outcome) obj).getMessageKey());
    }

}
