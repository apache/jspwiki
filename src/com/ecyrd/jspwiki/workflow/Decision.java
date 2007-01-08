package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ecyrd.jspwiki.WikiException;

/**
 * <p>
 * AbstractStep subclass that asks an actor Principal to choose an Outcome on
 * behalf of an owner (also a Principal). When a Decision completes, its
 * {@link #isCompleted()} method returns <code>true</code>. It also tells its
 * parent WorkflowManager to remove it from the list of pending tasks by calling
 * {@link DecisionQueue#remove(Decision)}.
 * </p>
 * <p>
 * To enable actors to choose an appropriate Outcome, Decisions can store
 * arbitrary key-value pairs called "facts." These facts can be presented by the
 * user interface to show details the actor needs to know about. Facts are added
 * by calling classes to the Decision, in order of expected presentation, by the
 * {@link } method. They can be retrieved, in order, via {@link }.
 * </p>
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public abstract class Decision extends AbstractStep
{
    private Principal m_actor;

    private int m_id;

    private final Outcome m_defaultOutcome;

    private final List m_facts;

    public Decision(Workflow workflow, String messageKey, Principal actor, Outcome defaultOutcome)
    {
        super(workflow, messageKey);
        m_actor = actor;
        m_defaultOutcome = defaultOutcome;
        m_facts = new ArrayList();
        addSuccessor(defaultOutcome, null);
    }

    /**
     * Appends a Fact to the list of Facts associated with this Decision.
     * 
     * @param fact
     *            the new fact to add
     */
    public final void addFact(Fact fact)
    {
        m_facts.add(fact);
    }

    /**
     * Sets this Decision's outcome, but only if the parent Workflow's state is
     * {@link Workflow#WAITING} and this Decision is its currently active Step.
     * All other invocations of this method will return an
     * IllegalStateException.
     * 
     * @param outcome
     *            the Outcome of the Decision
     * @throws IllegalStateException
     *             if invoked when this Decision is not the parent Workflow's
     *             currently active Step
     * @throws IllegalArgumentException
     *             if the Outcome is not one of the Outcomes returned by
     *             {@link #getAvailableOutcomes()}
     */
    public void decide(Outcome outcome)
    {
        super.setOutcome(outcome);

        // If current workflow is waiting for input, restart it and remove
        // Decision from DecisionQueue
        Workflow w = getWorkflow();
        if (w.getCurrentState() == Workflow.WAITING && this.equals(w.getCurrentStep()))
        {
            WorkflowManager wm = w.getWorkflowManager();
            if (wm != null)
            {
                wm.getDecisionQueue().remove(this);
            }
            // Restart workflow
            w.restart();
        }
    }

    /**
     * Default implementation that always returns {@link Outcome#STEP_CONTINUE}
     * if the current Outcome isn't a completion (which will be true if the
     * {@link #decide(Outcome)} method hasn't been executed yet. This method
     * will also add the Decision to the associated DecisionQueue.
     */
    public Outcome execute() throws WikiException
    {
        if (getOutcome().isCompletion())
        {
            return getOutcome();
        }

        // Put decision in the DecisionQueue
        WorkflowManager wm = getWorkflow().getWorkflowManager();
        if (wm != null)
        {
            wm.getDecisionQueue().add(this);
        }

        // Indicate we are waiting for user input
        return Outcome.STEP_CONTINUE;
    }

    public final Principal getActor()
    {
        return m_actor;
    }

    /**
     * Returns the default or suggested outcome, which must be one of those
     * returned by {@link #getAvailableOutcomes()}. This method is guaranteed
     * to return a non-<code>null</code> Outcome.
     * 
     * @return the default outcome.
     */
    public Outcome getDefaultOutcome()
    {
        return m_defaultOutcome;
    }

    /**
     * Returns the Facts associated with this Decision, in the order in which
     * they were added.
     * 
     * @return the list of Facts
     */
    public final List getFacts()
    {
        return Collections.unmodifiableList(m_facts);
    }

    /**
     * Returns the unique identifier for this Decision. Normally, this ID is
     * programmatically assigned when the Decision is added to the
     * DecisionQueue.
     * 
     * @return the identifier
     */
    public final int getId()
    {
        return m_id;
    }

    /**
     * Returns <code>true</code> if the Decision can be reassigned to another
     * actor. This implementation always returns <code>true</code>.
     * 
     * @return the result
     */
    public boolean isReassignable()
    {
        return true;
    }

    /**
     * Reassigns the Decision to a new actor (that is, provide an outcome).
     * 
     * @param actor
     *            the actor to reassign the Decision to
     * @throws IllegalArgumentException
     *             if the Decision cannot be reassigned
     */
    public final synchronized void reassign(Principal actor)
    {
        if (isReassignable())
        {
            m_actor = actor;
        }
        else
        {
            throw new IllegalArgumentException("Decision cannot be reassigned.");
        }
    }

    /**
     * Sets the unique identfier for this Decision.
     * 
     * @param id
     *            the identifier
     */
    public final void setId(int id)
    {
        m_id = id;
    }
}
