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
import java.util.*;

import com.ecyrd.jspwiki.WikiException;

/**
 * Abstact superclass that provides a complete implementation of most
 * Step methods; subclasses need only implement {@link #execute()} and
 * {@link #getActor()}.
 *
 * @author Andrew Jaquith
 * @since 2.5
 */
public abstract class AbstractStep implements Step
{

    /** Timestamp of when the step started. */
    private Date m_start;

    /** Timestamp of when the step ended. */
    private Date m_end;

    private final String m_key;

    private boolean m_completed;

    private final Map<Outcome,Step> m_successors;

    private Workflow m_workflow;

    private Outcome m_outcome;

    private final List<String> m_errors;

    private boolean m_started;

    /**
     * Protected constructor that creates a new Step with a specified message key.
     * After construction, the protected method {@link #setWorkflow(Workflow)} should be
     * called.
     *
     * @param messageKey
     *            the Step's message key, such as
     *            <code>decision.editPageApproval</code>. By convention, the
     *            message prefix should be a lower-case version of the Step's
     *            type, plus a period (<em>e.g.</em>, <code>task.</code>
     *            and <code>decision.</code>).
     */
    protected AbstractStep( String messageKey )
    {
        m_started = false;
        m_start = Workflow.TIME_NOT_SET;
        m_completed = false;
        m_end = Workflow.TIME_NOT_SET;
        m_errors = new ArrayList<String>();
        m_outcome = Outcome.STEP_CONTINUE;
        m_key = messageKey;
        m_successors = new LinkedHashMap<Outcome,Step>();
    }

    /**
     * Constructs a new Step belonging to a specified Workflow and having a
     * specified message key.
     *
     * @param workflow
     *            the workflow the Step belongs to
     * @param messageKey
     *            the Step's message key, such as
     *            <code>decision.editPageApproval</code>. By convention, the
     *            message prefix should be a lower-case version of the Step's
     *            type, plus a period (<em>e.g.</em>, <code>task.</code>
     *            and <code>decision.</code>).
     */
    public AbstractStep( Workflow workflow, String messageKey )
    {
        this( messageKey );
        setWorkflow( workflow );
    }

    /**
     * {@inheritDoc}
     */
    public final void addSuccessor(Outcome outcome, Step step)
    {
        m_successors.put( outcome, step );
    }

    /**
     * {@inheritDoc}
     */
    public final Collection<Outcome> getAvailableOutcomes()
    {
        return Collections.unmodifiableCollection( m_successors.keySet() );
    }

    /**
     * {@inheritDoc}
     */
    public final List<String> getErrors()
    {
        return Collections.unmodifiableList( m_errors );
    }

    /**
     * {@inheritDoc}
     */
    public abstract Outcome execute() throws WikiException;

    /**
     * {@inheritDoc}
     */
    public abstract Principal getActor();

    /**
     * {@inheritDoc}
     */
    public final Date getEndTime()
    {
        return m_end;
    }

    /**
     * {@inheritDoc}
     */
    public final Object[] getMessageArguments()
    {
        if ( m_workflow == null )
        {
            return new Object[0];
        }
        return m_workflow.getMessageArguments();
    }

    /**
     * {@inheritDoc}
     */
    public final String getMessageKey()
    {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Outcome getOutcome()
    {
        return m_outcome;
    }

    /**
     * {@inheritDoc}
     */
    public Principal getOwner()
    {
        if ( m_workflow == null )
        {
            return null;
        }
        return m_workflow.getOwner();
    }

    /**
     * {@inheritDoc}
     */
    public final Date getStartTime()
    {
        return m_start;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized Workflow getWorkflow()
    {
        return m_workflow;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isCompleted()
    {
        return m_completed;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isStarted()
    {
        return m_started;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void setOutcome(Outcome outcome)
    {
        // Is this an allowed Outcome?
        if ( !m_successors.containsKey( outcome ) )
        {
            if ( !Outcome.STEP_CONTINUE.equals( outcome ) &&
                 !Outcome.STEP_ABORT.equals( outcome ) )
            {
                 throw new IllegalArgumentException( "Outcome " + outcome.getMessageKey() + " is not supported for this Step." );
            }
        }

        // Is this a "completion" outcome?
        if ( outcome.isCompletion() )
        {
            if ( m_completed )
            {
                throw new IllegalStateException( "Step has already been marked complete; cannot set again." );
            }
            m_completed = true;
            m_end = new Date( System.currentTimeMillis() );
        }
        m_outcome = outcome;
    }

    /**
     * {@inheritDoc}
     */
    public final synchronized void start() throws WikiException
    {
        if ( m_started )
        {
            throw new IllegalStateException( "Step already started." );
        }
        m_started = true;
        m_start = new Date( System.currentTimeMillis() );
    }

    /**
     * {@inheritDoc}
     */
    public final Step getSuccessor( Outcome outcome )
    {
        return m_successors.get( outcome );
    }

    // --------------------------Helper methods--------------------------

    /**
     * Protected method that sets the parent Workflow post-construction.
     * @param workflow the parent workflow to set
     */
    protected final synchronized void setWorkflow( Workflow workflow )
    {
        m_workflow = workflow;
    }

    /**
     * Protected helper method that adds a String representing an error message
     * to the Step's cached errors list.
     *
     * @param message
     *            the error message
     */
    protected final synchronized void addError( String message )
    {
        m_errors.add( message );
    }

}
