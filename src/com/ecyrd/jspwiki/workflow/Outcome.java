package com.ecyrd.jspwiki.workflow;

/**
 * Resolution of a workflow Step, such as "approve," "deny," "hold," "task
 * error," or other potential resolutions.
 *
 * @author Andrew Jaquith
 * @since 2.5
 */
public final class Outcome
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

    private static final Outcome[] c_outcomes = new Outcome[] { STEP_COMPLETE, STEP_ABORT, STEP_CONTINUE, DECISION_ACKNOWLEDGE,
                                                               DECISION_APPROVE, DECISION_DENY, DECISION_HOLD, DECISION_REASSIGN };

    private final String m_key;

    private final boolean m_completion;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param key
     *            message key for the Outcome
     * @param completion
     *            whether this Outcome should be interpreted as the logical
     *            completion of a Step.
     */
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

    /**
     * The hashcode of an Outcome is identical to the hashcode of its message
     * key, multiplied by 2 if it is a "completion" Outcome.
     */
    public int hashCode()
    {
        return m_key.hashCode() * (m_completion ? 1 : 2);
    }

    /**
     * Two Outcome objects are
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Outcome))
        {
            return false;
        }
        return m_key.equals(((Outcome) obj).getMessageKey());
    }

    /**
     * Returns a named Outcome. If an Outcome matching the supplied key is not
     * found, this method throws a {@link NoSuchOutcomeException}.
     *
     * @param key
     *            the name of the outcome
     * @return the Outcome
     * @throws NoSuchOutcomeException
     *             if an Outcome matching the key isn't found.
     */
    public static Outcome forName(String key) throws NoSuchOutcomeException
    {
        if (key != null)
        {
            for (int i = 0; i < c_outcomes.length; i++)
            {
                if (c_outcomes[i].m_key.equals(key))
                {
                    return c_outcomes[i];
                }
            }
        }
        throw new NoSuchOutcomeException("Outcome " + key + " not found.");
    }

    public String toString()
    {
        return "[Outcome:" + m_key + "]";
    }

}
