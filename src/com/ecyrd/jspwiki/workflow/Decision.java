/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ecyrd.jspwiki.WikiException;

/**
 * <p>
 * AbstractStep subclass that asks an actor Principal to choose an Outcome on
 * behalf of an owner (also a Principal). The actor "makes the decision" by
 * calling the {@link #decide(Outcome)} method. When this method is called,
 * it will set the Decision's Outcome to the one supplied. If the parent
 * Workflow is in the {@link Workflow#WAITING} state, it will be re-started.
 * Any checked WikiExceptions thrown by the workflow after re-start will be
 * re-thrown to callers.
 * </p>
 * <p>
 * When a Decision completes, its
 * {@link #isCompleted()} method returns <code>true</code>. It also tells its
 * parent WorkflowManager to remove it from the list of pending tasks by calling
 * {@link DecisionQueue#remove(Decision)}.
 * </p>
 * <p>
 * To enable actors to choose an appropriate Outcome, Decisions can store
 * arbitrary key-value pairs called "facts." These facts can be presented by the
 * user interface to show details the actor needs to know about. Facts are added
 * by calling classes to the Decision, in order of expected presentation, by the
 * {@link #addFact(Fact)} method. They can be retrieved, in order, via {@link #getFacts()}.
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
     * <p>Sets this Decision's outcome, and restarts the parent Workflow if
     * it is in the {@link Workflow#WAITING} state and this Decision is
     * its currently active Step. Any checked WikiExceptions thrown by
     * the workflow after re-start will be re-thrown to callers.</p>
     * <p>This method cannot be invoked if the Decision is not the
     * current Workflow step; all other invocations will throw
     * an IllegalStateException.</p>
     *
     * @param outcome
     *            the Outcome of the Decision
     * @throws IllegalStateException
     *             if invoked when this Decision is not the parent Workflow's
     *             currently active Step
     * @throws IllegalArgumentException
     *             if the Outcome is not one of the Outcomes returned by
     *             {@link #getAvailableOutcomes()}
     * @throws WikiException
     *             if the act of restarting the Workflow throws an exception
     */
    public void decide(Outcome outcome) throws WikiException
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
