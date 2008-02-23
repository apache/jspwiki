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
import java.util.Collection;
import java.util.LinkedList;

import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;

/**
 * Keeps a queue of pending Decisions that need to be acted on by named
 * Principals.
 *
 * @author Andrew Jaquith
 * @since 2.5
 */
public class DecisionQueue
{

    private LinkedList<Decision> m_queue = new LinkedList<Decision>();

    private volatile int m_next;

    /**
     * Constructs a new DecisionQueue.
     */
    public DecisionQueue()
    {
        m_next = 1000;
    }

    /**
     * Adds a Decision to the DecisionQueue; also sets the Decision's unique
     * identifier.
     *
     * @param decision
     *            the Decision to add
     */
    protected synchronized void add(Decision decision)
    {
        m_queue.addLast(decision);
        decision.setId(nextId());
    }

    /**
     * Protected method that returns all pending Decisions in the queue, in
     * order of submission. If no Decisions are pending, this method returns a
     * zero-length array.
     *
     * @return the pending decisions TODO: explore whether this method could be
     *         made protected
     */
    protected Decision[] decisions()
    {
        return m_queue.toArray(new Decision[m_queue.size()]);
    }

    /**
     * Protected method that removes a Decision from the queue.
     * @param decision the decision to remove
     */
    protected synchronized void remove(Decision decision)
    {
        m_queue.remove(decision);
    }

    /**
     * Returns a Collection representing the current Decisions that pertain to a
     * users's WikiSession. The Decisions are obtained by iterating through the
     * WikiSession's Principals and selecting those Decisions whose
     * {@link Decision#getActor()} value match. If the wiki session is not
     * authenticated, this method returns an empty Collection.
     *
     * @param session
     *            the wiki session
     * @return the collection of Decisions, which may be empty
     */
    public Collection<Decision> getActorDecisions(WikiSession session)
    {
        ArrayList<Decision> decisions = new ArrayList<Decision>();
        if (session.isAuthenticated())
        {
            Principal[] principals = session.getPrincipals();
            Principal[] rolePrincipals = session.getRoles();
            for ( Decision decision : m_queue )
            {
                // Iterate through the Principal set
                for (int i = 0; i < principals.length; i++)
                {
                    if (principals[i].equals(decision.getActor()))
                    {
                        decisions.add(decision);
                    }
                }
                // Iterate through the Role set
                for (int i = 0; i < rolePrincipals.length; i++)
                {
                    if (rolePrincipals[i].equals(decision.getActor()))
                    {
                        decisions.add(decision);
                    }
                }
            }
        }
        return decisions;
    }

    /**
     * Attempts to complete a Decision by calling
     * {@link Decision#decide(Outcome)}. This will cause the Step immediately
     * following the Decision (if any) to start. If the decision completes
     * successfully, this method also removes the completed decision from the
     * queue.
     *
     * @param decision the Decision for which the Outcome will be supplied
     * @param outcome the Outcome of the Decision
     * @throws WikiException if the succeeding Step cannot start
     * for any reason
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
     * @param decision the Decision to reassign
     * @param owner the new owner
     * @throws WikiException never
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

    /**
     * Returns the next available unique identifier, which is subsequently
     * incremented.
     *
     * @return the id
     */
    private synchronized int nextId()
    {
        int current = m_next;
        m_next++;
        return current;
    }

}
