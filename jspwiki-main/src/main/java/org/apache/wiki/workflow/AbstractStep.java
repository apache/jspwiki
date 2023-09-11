/* 
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
package org.apache.wiki.workflow;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.WikiException;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract superclass that provides a complete implementation of most Step methods; subclasses need only implement {@link #execute(Context)} and
 * {@link #getActor()}.
 *
 * @since 2.5
 */
public abstract class AbstractStep implements Step {

    private static final long serialVersionUID = 8635678679349653768L;

    /** Timestamp of when the step started. */
    private Date m_start;

    /** Timestamp of when the step ended. */
    private Date m_end;

    private final String m_key;

    private boolean m_completed;

    private final Map< Outcome, Step > m_successors;

    private int workflowId;

    /** attribute map. */
    private Map< String, Serializable > workflowContext;

    private Outcome m_outcome;

    private final List<String> m_errors;

    private boolean m_started;

    /**
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Protected constructor that creates a new Step with a specified message key. After construction, the method
     * {@link #setWorkflow(int, Map)} should be called.
     *
     * @param messageKey the Step's message key, such as {@code decision.editPageApproval}. By convention, the message prefix should
     *                   be a lower-case version of the Step's type, plus a period (<em>e.g.</em>, {@code task.} and {@code decision.}).
     */
    protected AbstractStep( final String messageKey ) {
        m_started = false;
        m_start = Step.TIME_NOT_SET;
        m_completed = false;
        m_end = Step.TIME_NOT_SET;
        m_errors = new ArrayList<>();
        m_outcome = Outcome.STEP_CONTINUE;
        m_key = messageKey;
        m_successors = new LinkedHashMap<>();
    }

    /**
     * Constructs a new Step belonging to a specified Workflow and having a specified message key.
     *
     * @param workflowId the parent workflow id to set
     * @param workflowContext the parent workflow context to set
     * @param messageKey the Step's message key, such as {@code decision.editPageApproval}. By convention, the message prefix should
     *                   be a lower-case version of the Step's type, plus a period (<em>e.g.</em>, {@code task.} and {@code decision.}).
     */
    public AbstractStep( final int workflowId, final Map< String, Serializable > workflowContext, final String messageKey ) {
        this( messageKey );
        setWorkflow( workflowId, workflowContext );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addSuccessor(final Outcome outcome, final Step step ) {
        m_successors.put( outcome, step );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection< Outcome > getAvailableOutcomes() {
        final Set< Outcome > outcomes = m_successors.keySet();
        return Collections.unmodifiableCollection( outcomes );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List< String > getErrors() {
        return Collections.unmodifiableList( m_errors );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Outcome execute(Context ctx ) throws WikiException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Principal getActor();

    /**
     * {@inheritDoc}
     */
    @Override
    public final Date getEndTime() {
        return m_end;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getMessageKey() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Outcome getOutcome() {
        lock.lock();
        try {
            return m_outcome;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Date getStartTime() {
        return m_start;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isCompleted() {
        return m_completed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isStarted() {
        return m_started;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOutcome(final Outcome outcome ) {
        lock.lock();
        try {
            // Is this an allowed Outcome?
            if( !m_successors.containsKey( outcome ) ) {
                if( !Outcome.STEP_CONTINUE.equals( outcome ) && !Outcome.STEP_ABORT.equals( outcome ) ) {
                    throw new IllegalArgumentException( "Outcome " + outcome.getMessageKey() + " is not supported for this Step." );
                }
            }

            // Is this a "completion" outcome?
            if( outcome.isCompletion() ) {
                if( m_completed ) {
                    throw new IllegalStateException( "Step has already been marked complete; cannot set again." );
                }
                m_completed = true;
                m_end = new Date( System.currentTimeMillis() );
            }
            m_outcome = outcome;
        } finally {
            lock.unlock();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void start() throws WikiException {
        lock.lock();
        try {
            if( m_started ) {
                throw new IllegalStateException( "Step already started." );
            }
            m_started = true;
            m_start = new Date( System.currentTimeMillis() );
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Step getSuccessor(final Outcome outcome ) {
        return m_successors.get( outcome );
    }

    // --------------------------Helper methods--------------------------

    /**
     * method that sets the parent Workflow id and context post-construction.
     *
     * @param workflowId the parent workflow id to set
     * @param workflowContext the parent workflow context to set
     */
    @Override
    public final void setWorkflow(final int workflowId, final Map< String, Serializable > workflowContext ) {
        lock.lock();
        try {
            this.workflowId = workflowId;
            this.workflowContext = workflowContext;
        } finally {
            lock.unlock();
        }
    }

    public int getWorkflowId() {
        return workflowId;
    }

    public Map< String, Serializable > getWorkflowContext() {
        return workflowContext;
    }

    /**
     * Protected helper method that adds a String representing an error message to the Step's cached errors list.
     *
     * @param message the error message
     */
    protected final void addError( final String message ) {
        lock.lock();
        try {
            m_errors.add( message );
        } finally {
            lock.unlock();
        }
    }

}
