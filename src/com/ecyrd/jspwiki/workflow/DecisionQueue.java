/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

    private LinkedList m_queue = new LinkedList();

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
        return (Decision[]) m_queue.toArray(new Decision[m_queue.size()]);
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
    public Collection getActorDecisions(WikiSession session)
    {
        ArrayList decisions = new ArrayList();
        if (session.isAuthenticated())
        {
            Principal[] principals = session.getPrincipals();
            Principal[] rolePrincipals = session.getRoles();
            for (Iterator it = m_queue.iterator(); it.hasNext();)
            {
                Decision decision = (Decision) it.next();

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
