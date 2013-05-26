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

import java.security.Principal;

/**
 * AbstractStep subclass that executes instructions, uninterrupted, and results
 * in an Outcome. Concrete classes only need to implement {@link Task#execute()}.
 * When the execution step completes, <code>execute</code> must return
 * {@link Outcome#STEP_COMPLETE}, {@link Outcome#STEP_CONTINUE} or
 * {@link Outcome#STEP_ABORT}. Subclasses can add any errors by calling the
 * helper method {@link AbstractStep#addError(String)}. The execute method should
 * <em>generally</em> capture and add errors to the error list instead of
 * throwing a WikiException.
 * <p>
 *
 * @since 2.5
 */
public abstract class Task extends AbstractStep
{
    private static final long serialVersionUID = 4630293957752430807L;
    
    private Step m_successor = null;

    /**
     * Protected constructor that creates a new Task with a specified message key.
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
    public Task( String messageKey )
    {
        super( messageKey );
        super.addSuccessor( Outcome.STEP_COMPLETE, null );
        super.addSuccessor( Outcome.STEP_ABORT, null );
    }

    /**
     * Constructs a new instance of a Task, with an associated Workflow and
     * message key.
     *
     * @param workflow
     *            the associated workflow
     * @param messageKey
     *            the i18n message key
     */
    public Task( Workflow workflow, String messageKey )
    {
        this( messageKey );
        setWorkflow( workflow );
    }

    /**
     * Returns {@link SystemPrincipal#SYSTEM_USER}.
     * @return the system principal
     */
    public final Principal getActor()
    {
        return SystemPrincipal.SYSTEM_USER;
    }

    /**
     * Sets the successor Step to this one, which will be triggered if the Task
     * completes successfully (that is, {@link Step#getOutcome()} returns
     * {@link Outcome#STEP_COMPLETE}. This method is really a convenient
     * shortcut for {@link Step#addSuccessor(Outcome, Step)}, where the first
     * parameter is {@link Outcome#STEP_COMPLETE}.
     *
     * @param step
     *            the successor
     */
    public final synchronized void setSuccessor( Step step )
    {
        m_successor = step;
    }

    /**
     * Identifies the next Step after this Task finishes successfully. This
     * method will always return the value set in method
     * {@link #setSuccessor(Step)}, regardless of the current completion state.
     *
     * @return the next step
     */
    public final synchronized Step getSuccessor()
    {
        return m_successor;
    }

}
