package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.ecyrd.jspwiki.WikiException;

/**
 * Keeps a queue of pending Decisions that need to be acted on by named Principals.
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public class DecisionQueue
{

    private LinkedList m_queue = new LinkedList();

    public DecisionQueue()
    {
    }

    protected synchronized void add(Decision decision)
    {
        m_queue.addLast(decision);
    }

    protected synchronized void remove(Decision decision)
    {
        m_queue.remove(decision);
    }

    /**
     * Returns all pending decisions in the queue, in order of submission. If no
     * decisions are pending, this method returns a zero-length array.
     * 
     * @return the pending decisions
     */
    public Decision[] decisions()
    {
        return (Decision[]) m_queue.toArray(new Decision[m_queue.size()]);
    }

    /**
     * Returns the pending decisions for a supplied owner, in order of
     * submission. If no decisions are pending, this method returns a
     * zero-length array.
     * 
     * @param principal
     *            the principal for whom decisions have been queued.
     * @return the pending decisions for this Principal
     */
    public Decision[] actorDecisions(Principal principal)
    {
        ArrayList decisions = new ArrayList();
        for (Iterator it = m_queue.iterator(); it.hasNext();)
        {
            Decision decision = (Decision) it.next();
            if (principal.equals(decision.getActor()))
            {
                decisions.add(decision);
            }
        }
        return (Decision[]) decisions.toArray(new Decision[decisions.size()]);
    }

    /**
     * Attempts to complete a Decision by calling
     * {@link Decision#decide(Outcome)}. If the decision completes
     * successfully, this method also removes the completed decision from the
     * queue.
     * 
     * @param decision
     * @param outcome
     * @throws WikiException
     */
    public void decide(Decision decision, Outcome outcome) throws WikiException
    {
        decision.decide(outcome);
        if (decision.isCompleted())
        {
            remove(decision);
        }

        // TODO: We should fire an event indicating the Outcome, and whether the
        // Decision completed successfully
    }

    /**
     * Reassigns the owner of the Decision to a new owner. Under the covers,
     * this method calls {@link Decision#reassign(Principal)}.
     * 
     * @param decision
     *            the Decision to reassign
     * @param owner
     *            the new owner
     * @throws WikiException
     */
    public synchronized void reassign(Decision decision, Principal owner) throws WikiException
    {
        if (decision.isReassignable())
        {
            decision.reassign(owner);

            // TODO: We should fire an event indicating the reassignment
            return;
        }
        throw new IllegalStateException("Reassignments not allowed for this decision.");
    }
}
