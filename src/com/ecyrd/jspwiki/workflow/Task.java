package com.ecyrd.jspwiki.workflow;

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
 * @author Andrew Jaquith
 * @since 2.5
 */
public abstract class Task extends AbstractStep
{
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
        super.addSuccessor(Outcome.STEP_COMPLETE, null);
        super.addSuccessor(Outcome.STEP_ABORT, null);
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
    public Task(Workflow workflow, String messageKey)
    {
        this( messageKey );
        setWorkflow( workflow );
    }

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
    public final synchronized void setSuccessor(Step step)
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
    public final Step getSuccessor()
    {
        return m_successor;
    }

}
