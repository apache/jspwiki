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

import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WorkflowEvent;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Sequence of {@link Step} objects linked together. Workflows are always
 * initialized with a message key that denotes the name of the Workflow, and a
 * Principal that represents its owner.
 * </p>
 * <h2>Workflow lifecycle</h2>
 * A Workflow's state (obtained by {@link #getCurrentState()}) will be one of the
 * following:
 * </p>
 * <ul>
 * <li><strong>{@link #CREATED}</strong>: after the Workflow has been
 * instantiated, but before it has been started using the {@link #start()}
 * method.</li>
 * <li><strong>{@link #RUNNING}</strong>: after the Workflow has been started
 * using the {@link #start()} method, but before it has finished processing all
 * Steps. Note that a Workflow can only be started once; attempting to start it
 * again results in an IllegalStateException. Callers can place the Workflow
 * into the WAITING state by calling {@link #waitstate()}.</li>
 * <li><strong>{@link #WAITING}</strong>: when the Workflow has temporarily
 * paused, for example because of a pending Decision. Once the responsible actor
 * decides what to do, the caller can change the Workflow back to the RUNNING
 * state by calling the {@link #restart()} method (this is done automatically by
 * the Decision class, for instance, when the {@link Decision#decide(Outcome)}
 * method is invoked)</li>
 * <li><strong>{@link #COMPLETED}</strong>: after the Workflow has finished
 * processing all Steps, without errors.</li>
 * <li><strong>{@link #ABORTED}</strong>: if a Step has elected to abort the
 * Workflow.</li>
 * </ul>
 * <h2>Steps and processing algorithm</h2>
 * <p>
 * Workflow Step objects can be of type {@link Decision}, {@link Task} or other
 * Step subclasses. Decisions require user input, while Tasks do not. See the
 * {@link Step} class for more details.
 * </p>
 * <p>
 * After instantiating a new Workflow (but before telling it to {@link #start()}),
 * calling classes should specify the first Step by executing the
 * {@link #setFirstStep(Step)} method. Additional Steps can be chained by
 * invoking the first step's {@link Step#addSuccessor(Outcome, Step)} method.
 * </p>
 * <p>
 * When a Workflow's <code>start</code> method is invoked, the Workflow
 * retrieves the first Step and processes it. This Step, and subsequent ones,
 * are processed as follows:
 * </p>
 * <ul>
 * <li>The Step's {@link Step#start()} method executes, which sets the start
 * time.</li>
 * <li>The Step's {@link Step#execute()} method is called to begin processing,
 * which will return an Outcome to indicate completion, continuation or errors:</li>
 * <ul>
 * <li>{@link Outcome#STEP_COMPLETE} indicates that the execution method ran
 * without errors, and that the Step should be considered "completed."</li>
 * <li>{@link Outcome#STEP_CONTINUE} indicates that the execution method ran
 * without errors, but that the Step is not "complete" and should be put into
 * the WAITING state.</li>
 * <li>{@link Outcome#STEP_ABORT} indicates that the execution method
 * encountered errors, and should abort the Step <em>and</em> the Workflow as
 * a whole. When this happens, the Workflow will set the current Step's Outcome
 * to {@link Outcome#STEP_ABORT} and invoke the Workflow's {@link #abort()}
 * method. The Step's processing errors, if any, can be retrieved by
 * {@link Step#getErrors()}.</li>
 * </ul>
 * <li>The Outcome of the <code>execute</code> method also affects what
 * happens next. Depending on the result (and assuming the Step did not abort),
 * the Workflow will either move on to the next Step or put the Workflow into
 * the {@link Workflow#WAITING} state:</li>
 * <ul>
 * <li>If the Outcome denoted "completion" (<em>i.e.</em>, its
 * {@link Step#isCompleted()} method returns <code>true</code>) then the Step
 * is considered complete; the Workflow looks up the next Step by calling the
 * current Step's {@link Step#getSuccessor(Outcome)} method. If
 * <code>successor()</code> returns a non-<code>null</code> Step, the
 * return value is marked as the current Step and added to the Workflow's Step
 * history. If <code>successor()</code> returns <code>null</code>, then the
 * Workflow has no more Steps and it enters the {@link #COMPLETED} state.</li>
 * <li>If the Outcome did not denote "completion" (<em>i.e.</em>, its
 * {@link Step#isCompleted()} method returns <code>false</code>), then the
 * Step still has further work to do. The Workflow enters the {@link #WAITING}
 * state and stops further processing until a caller restarts it.</li>
 * </ul>
 * </ul>
 * </p>
 * <p>
 * The currently executing Step can be obtained by {@link #getCurrentStep()}. The
 * actor for the current Step is returned by {@link #getCurrentActor()}.
 * </p>
 * <p>
 * To provide flexibility for specific implementations, the Workflow class
 * provides two additional features that enable Workflow participants (<em>i.e.</em>,
 * Workflow subclasses and Step/Task/Decision subclasses) to share context and
 * state information. These two features are <em>named attributes</em> and
 * <em>message arguments</em>:
 * </p>
 * <ul>
 * <li><strong>Named attributes</strong> are simple key-value pairs that
 * Workflow participants can get or set. Keys are Strings; values can be any
 * Object. Named attributes are set with {@link #setAttribute(String, Object)}
 * and retrieved with {@link #getAttribute(String)}.</li>
 * <li><strong>Message arguments</strong> are used in combination with
 * JSPWiki's {@link org.apache.wiki.i18n.InternationalizationManager} to
 * create language-independent user interface messages. The message argument
 * array is retrieved via {@link #getMessageArguments()}; the first two array
 * elements will always be these: a String representing work flow owner's name,
 * and a String representing the current actor's name. Workflow participants
 * can add to this array by invoking {@link #addMessageArgument(Serializable)}.</li>
 * </ul>
 * <h2>Example</h2>
 * <p>
 * Workflow Steps can be very powerful when linked together. JSPWiki provides
 * two abstract subclasses classes that you can use to build your own Workflows:
 * Tasks and Decisions. As noted, Tasks are Steps that execute without user
 * intervention, while Decisions require actors (<em>aka</em> Principals) to
 * take action. Decisions and Tasks can be mixed freely to produce some highly
 * elaborate branching structures.
 * </p>
 * <p>
 * Here is a simple case. For example, suppose you would like to create a
 * Workflow that (a) executes a initialization Task, (b) pauses to obtain an
 * approval Decision from a user in the Admin group, and if approved, (c)
 * executes a "finish" Task. Here's sample code that illustrates how to do it:
 * </p>
 *
 * <pre>
 *    // Create workflow; owner is current user
 * 1  Workflow workflow = new Workflow(&quot;workflow.myworkflow&quot;, context.getCurrentUser());
 *
 *    // Create custom initialization task
 * 2  Step initTask = new InitTask(this);
 *
 *    // Create finish task
 * 3  Step finishTask = new FinishTask(this);
 *
 *    // Create an intermediate decision step
 * 4  Principal actor = new GroupPrincipal(&quot;Admin&quot;);
 * 5  Step decision = new SimpleDecision(this, &quot;decision.AdminDecision&quot;, actor);
 *
 *    // Hook the steps together
 * 6  initTask.addSuccessor(Outcome.STEP_COMPLETE, decision);
 * 7  decision.addSuccessor(Outcome.DECISION_APPROVE, finishTask);
 *
 *    // Set workflow's first step
 * 8  workflow.setFirstStep(initTask);
 * </pre>
 *
 * <p>
 * Some comments on the source code:
 * </p>
 * <ul>
 * <li>Line 1 instantiates the workflow with a sample message key and
 * designated owner Principal, in this case the current wiki user</li>
 * <li>Lines 2 and 3 instantiate the custom Task subclasses, which contain the
 * business logic</li>
 * <li>Line 4 creates the relevant GroupPrincipal for the <code>Admin</code>
 * group, who will be the actor in the Decision step</li>
 * <li>Line 5 creates the Decision step, passing the Workflow, sample message
 * key, and actor in the constructor</li>
 * <li>Line 6 specifies that if the InitTask's Outcome signifies "normal
 * completion" (STEP_COMPLETE), the SimpleDecision step should be invoked next</li>
 * <li>Line 7 specifies that if the actor (anyone possessing the
 * <code>Admin</code> GroupPrincipal) selects DECISION_APPROVE, the FinishTask
 * step should be invoked</li>
 * <li>Line 8 adds the InitTask (and all of its successor Steps, nicely wired
 * together) to the workflow</li>
 * </ul>
 *
 */
public class Workflow implements Serializable
{
    private static final long serialVersionUID = 5228149040690660032L;

    /** Time value: the start or end time has not been set. */
    public static final Date TIME_NOT_SET = new Date( 0 );

    /** ID value: the workflow ID has not been set. */
    public static final int ID_NOT_SET = 0;

    /** State value: Workflow completed all Steps without errors. */
    public static final int COMPLETED = 50;

    /** State value: Workflow aborted before completion. */
    public static final int ABORTED = 40;

    /**
     * State value: Workflow paused, typically because a Step returned an
     * Outcome that doesn't signify "completion."
     */
    public static final int WAITING = 30;

    /** State value: Workflow started, and is running. */
    public static final int RUNNING = -1;

    /** State value: Workflow instantiated, but not started. */
    public static final int CREATED = -2;

    /** Lazily-initialized attribute map. */
    private Map<String, Object> m_attributes;

    /** The initial Step for this Workflow. */
    private Step m_firstStep;

    /** Flag indicating whether the Workflow has started yet. */
    private boolean m_started;

    private final LinkedList<Step> m_history;

    private int m_id;

    private final String m_key;

    private final Principal m_owner;

    private final List<Serializable> m_messageArgs;

    private int m_state;

    private Step m_currentStep;

    private WorkflowManager m_manager;

    /**
     * Constructs a new Workflow object with a supplied message key, owner
     * Principal, and undefined unique identifier {@link #ID_NOT_SET}. Once
     * instantiated the Workflow is considered to be in the {@link #CREATED}
     * state; a caller must explicitly invoke the {@link #start()} method to
     * begin processing.
     *
     * @param messageKey
     *            the message key used to construct a localized workflow name,
     *            such as <code>workflow.saveWikiPage</code>
     * @param owner
     *            the Principal who owns the Workflow. Typically, this is the
     *            user who created and submitted it
     */
    public Workflow(String messageKey, Principal owner)
    {
        super();
        m_attributes = null;
        m_currentStep = null;
        m_history = new LinkedList<Step>();
        m_id = ID_NOT_SET;
        m_key = messageKey;
        m_manager = null;
        m_messageArgs = new ArrayList<Serializable>();
        m_owner = owner;
        m_started = false;
        m_state = CREATED;
    }

    /**
     * Aborts the Workflow by setting the current Step's Outcome to
     * {@link Outcome#STEP_ABORT}, and the Workflow's overall state to
     * {@link #ABORTED}. It also appends the aborted Step into the workflow
     * history, and sets the current step to <code>null</code>. If the Step
     * is a Decision, it is removed from the DecisionQueue. This method
     * can be called at any point in the lifecycle prior to completion, but it
     * cannot be called twice. It finishes by calling the {@link #cleanup()}
     * method to flush retained objects. If the Workflow had been previously
     * aborted, this method throws an IllegalStateException.
     */
    public final synchronized void abort()
    {
        // Check corner cases: previous abort or completion
        if ( m_state == ABORTED )
        {
            throw new IllegalStateException( "The workflow has already been aborted." );
        }
        if ( m_state == COMPLETED )
        {
            throw new IllegalStateException( "The workflow has already completed." );
        }

        if ( m_currentStep != null )
        {
            if ( m_manager != null && m_currentStep instanceof Decision )
            {
                Decision d = (Decision)m_currentStep;
                m_manager.getDecisionQueue().remove( d );
            }
            m_currentStep.setOutcome( Outcome.STEP_ABORT );
            m_history.addLast( m_currentStep );
        }
        m_state = ABORTED;
        fireEvent( WorkflowEvent.ABORTED );
        cleanup();
    }

    /**
     * Appends a message argument object to the array returned by
     * {@link #getMessageArguments()}. The object <em>must</em> be an type
     * used by the {@link java.text.MessageFormat}: String, Date, or Number
     * (BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short).
     * If the object is not of type String, Number or Date, this method throws
     * an IllegalArgumentException.
     * @param obj the object to add
     */
    public final void addMessageArgument( Serializable obj )
    {
        if ( obj instanceof String || obj instanceof Date || obj instanceof Number )
        {
            m_messageArgs.add( obj );
            return;
        }
        throw new IllegalArgumentException( "Message arguments must be of type String, Date or Number." );
    }

    /**
     * Returns the actor Principal responsible for the current Step. If there is
     * no current Step, this method returns <code>null</code>.
     *
     * @return the current actor
     */
    public final synchronized Principal getCurrentActor()
    {
        if ( m_currentStep == null )
        {
            return null;
        }
        return m_currentStep.getActor();
    }

    /**
     * Returns the workflow state: {@link #CREATED}, {@link #RUNNING},
     * {@link #WAITING}, {@link #COMPLETED} or {@link #ABORTED}.
     *
     * @return the workflow state
     */
    public final int getCurrentState()
    {
        return m_state;
    }

    /**
     * Returns the current Step, or <code>null</code> if the workflow has not
     * started or already completed.
     *
     * @return the current step
     */
    public final Step getCurrentStep()
    {
        return m_currentStep;
    }

    /**
     * Retrieves a named Object associated with this Workflow. If the Workflow
     * has completed or aborted, this method always returns <code>null</code>.
     *
     * @param attr the name of the attribute
     * @return the value
     */
    public final synchronized Object getAttribute( String attr )
    {
        if ( m_attributes == null )
        {
            return null;
        }
        return m_attributes.get( attr );
    }

    /**
     * The end time for this Workflow, expressed as a system time number. This
     * value is equal to the end-time value returned by the final Step's
     * {@link Step#getEndTime()} method, if the workflow has completed.
     * Otherwise, this method returns {@link #TIME_NOT_SET}.
     *
     * @return the end time
     */
    public final Date getEndTime()
    {
        if ( isCompleted() )
        {
            Step last = m_history.getLast();
            if ( last != null )
            {
                return last.getEndTime();
            }
        }
        return TIME_NOT_SET;
    }

    /**
     * Returns the unique identifier for this Workflow. If not set, this method
     * returns ID_NOT_SET ({@value #ID_NOT_SET}).
     *
     * @return the unique identifier
     */
    public final synchronized int getId()
    {
        return m_id;
    }

    /**
     * <p>
     * Returns an array of message arguments, used by
     * {@link java.text.MessageFormat} to create localized messages. The first
     * two array elements will always be these:
     * </p>
     * <ul>
     * <li>String representing the name of the workflow owner (<em>i.e.</em>,{@link #getOwner()})</li>
     * <li>String representing the name of the current actor (<em>i.e.</em>,{@link #getCurrentActor()}).
     * If the current step is <code>null</code> because the workflow hasn't started or has already
     * finished, the value of this argument will be a dash character (<code>-</code>)</li>
     * </ul>
     * <p>
     * Workflow and Step subclasses are free to append items to this collection
     * with {@link #addMessageArgument(Serializable)}.
     * </p>
     *
     * @return the array of message arguments
     */
    public final Serializable[] getMessageArguments()
    {
        List<Serializable> args = new ArrayList<Serializable>();
        args.add( m_owner.getName() );
        Principal actor = getCurrentActor();
        args.add( actor == null ? "-" : actor.getName() );
        args.addAll( m_messageArgs );
        return args.toArray( new Serializable[args.size()] );
    }

    /**
     * Returns an i18n message key for the name of this workflow; for example,
     * <code>workflow.saveWikiPage</code>.
     *
     * @return the name
     */
    public final String getMessageKey()
    {
        return m_key;
    }

    /**
     * The owner Principal on whose behalf this Workflow is being executed; that
     * is, the user who created the workflow.
     *
     * @return the name of the Principal who owns this workflow
     */
    public final Principal getOwner()
    {
        return m_owner;
    }

    /**
     * The start time for this Workflow, expressed as a system time number. This
     * value is equal to the start-time value returned by the first Step's
     * {@link Step#getStartTime()} method, if the workflow has started already.
     * Otherwise, this method returns {@link #TIME_NOT_SET}.
     *
     * @return the start time
     */
    public final Date getStartTime()
    {
        return isStarted() ? m_firstStep.getStartTime() : TIME_NOT_SET;
    }

    /**
     * Returns the WorkflowManager that contains this Workflow.
     *
     * @return the workflow manager
     */
    public final synchronized WorkflowManager getWorkflowManager()
    {
        return m_manager;
    }

    /**
     * Returns a Step history for this Workflow as a List, chronologically, from the
     * first Step to the currently executing one. The first step is the first
     * item in the array. If the Workflow has not started, this method returns a
     * zero-length array.
     *
     * @return an array of Steps representing those that have executed, or are
     *         currently executing
     */
    public final List< Step > getHistory()
    {
        return Collections.unmodifiableList( m_history );
    }

    /**
     * Returns <code>true</code> if the workflow had been previously aborted.
     *
     * @return the result
     */
    public final boolean isAborted()
    {
        return m_state == ABORTED;
    }

    /**
     * Determines whether this Workflow is completed; that is, if it has no
     * additional Steps to perform. If the last Step in the workflow is
     * finished, this method will return <code>true</code>.
     *
     * @return <code>true</code> if the workflow has been started but has no
     *         more steps to perform; <code>false</code> if not.
     */
    public final synchronized boolean isCompleted()
    {
        // If current step is null, then we're done
        return m_started && m_state == COMPLETED;
    }

    /**
     * Determines whether this Workflow has started; that is, its
     * {@link #start()} method has been executed.
     *
     * @return <code>true</code> if the workflow has been started;
     *         <code>false</code> if not.
     */
    public final boolean isStarted()
    {
        return m_started;
    }

    /**
     * Convenience method that returns the predecessor of the current Step. This
     * method simply examines the Workflow history and returns the
     * second-to-last Step.
     *
     * @return the predecessor, or <code>null</code> if the first Step is
     *         currently executing
     */
    public final Step getPreviousStep()
    {
        return previousStep( m_currentStep );
    }

    /**
     * Restarts the Workflow from the {@link #WAITING} state and puts it into
     * the {@link #RUNNING} state again. If the Workflow had not previously been
     * paused, this method throws an IllegalStateException. If any of the
     * Steps in this Workflow throw a WikiException, the Workflow will abort
     * and propagate the exception to callers.
     * @throws WikiException if the current task's {@link Task#execute()} method
     * throws an exception
     */
    public final synchronized void restart() throws WikiException
    {
        if ( m_state != WAITING )
        {
            throw new IllegalStateException( "Workflow is not paused; cannot restart." );
        }
        m_state = RUNNING;
        fireEvent( WorkflowEvent.RUNNING );

        // Process current step
        try
        {
            processCurrentStep();
        }
        catch ( WikiException e )
        {
            abort();
            throw e;
        }
    }

    /**
     * Temporarily associates an object with this Workflow, as a named attribute, for the
     * duration of workflow execution. The passed object can be anything required by
     * an executing Step, although it <em>should</em> be serializable. Note that when the workflow
     * completes or aborts, all attributes will be cleared.
     *
     * @param attr
     *            the attribute name
     * @param obj
     *            the value
     */
    public final synchronized void setAttribute(String attr, Object obj )
    {
        if ( m_attributes == null )
        {
            m_attributes = new HashMap<String, Object>();
        }
        m_attributes.put( attr, obj );
    }

    /**
     * Sets the first Step for this Workflow, which will be executed immediately
     * after the {@link #start()} method executes. Note than the Step is not
     * marked as the "current" step or added to the Workflow history until the
     * {@link #start()} method is called.
     *
     * @param step
     *            the first step for the workflow
     */
    public final synchronized void setFirstStep(Step step)
    {
        m_firstStep = step;
    }

    /**
     * Sets the unique identifier for this Workflow.
     *
     * @param id
     *            the unique identifier
     */
    public final synchronized void setId( int id )
    {
        this.m_id = id;
    }

    /**
     * Sets the WorkflowManager that contains this Workflow.
     *
     * @param manager
     *            the workflow manager
     */
    public final synchronized void setWorkflowManager( WorkflowManager manager )
    {
        m_manager = manager;
        addWikiEventListener( manager );
    }

    /**
     * Starts the Workflow and sets the state to {@link #RUNNING}. If the
     * Workflow has already been started (or previously aborted), this method
     * returns an {@linkplain IllegalStateException}. If any of the
     * Steps in this Workflow throw a WikiException, the Workflow will abort
     * and propagate the exception to callers.
     * @throws WikiException if the current Step's {@link Step#start()}
     * method throws an exception of any kind
     */
    public final synchronized void start() throws WikiException
    {
        if ( m_state == ABORTED )
        {
            throw new IllegalStateException( "Workflow cannot be started; it has already been aborted." );
        }
        if ( m_started )
        {
            throw new IllegalStateException( "Workflow has already started." );
        }
        m_started = true;
        m_state = RUNNING;
        fireEvent( WorkflowEvent.RUNNING );

        // Mark the first step as the current one & add to history
        m_currentStep = m_firstStep;
        m_history.add( m_currentStep );

        // Process current step
        try
        {
            processCurrentStep();
        }
        catch ( WikiException e )
        {
            abort();
            throw e;
        }
    }

    /**
     * Sets the Workflow in the {@link #WAITING} state. If the Workflow is not
     * running or has already been paused, this method throws an
     * IllegalStateException. Once paused, the Workflow can be un-paused by
     * executing the {@link #restart()} method.
     */
    public final synchronized void waitstate()
    {
        if ( m_state != RUNNING )
        {
            throw new IllegalStateException( "Workflow is not running; cannot pause." );
        }
        m_state = WAITING;
        fireEvent( WorkflowEvent.WAITING );
    }

    /**
     * Clears the attribute map and sets the current step field to
     * <code>null</code>.
     */
    protected void cleanup()
    {
        m_currentStep = null;
        m_attributes = null;
    }

    /**
     * Protected helper method that changes the Workflow's state to
     * {@link #COMPLETED} and sets the current Step to <code>null</code>. It
     * calls the {@link #cleanup()} method to flush retained objects.
     * This method will no-op if it has previously been called.
     */
    protected final synchronized void complete()
    {
        if ( !isCompleted() )
        {
            m_state = COMPLETED;
            fireEvent( WorkflowEvent.COMPLETED );
            cleanup();
        }
    }

    /**
     * Protected method that returns the predecessor for a supplied Step.
     *
     * @param step
     *            the Step for which the predecessor is requested
     * @return its predecessor, or <code>null</code> if the first Step was
     *         supplied.
     */
    protected final Step previousStep(Step step)
    {
        int index = m_history.indexOf( step );
        return index < 1 ? null : m_history.get( index - 1 );
    }

    /**
     * Protected method that processes the current Step by calling
     * {@link Step#execute()}. If the <code>execute</code> throws an
     * exception, this method will propagate the exception immediately
     * to callers without aborting.
     * @throws WikiException if the current Step's {@link Step#start()}
     * method throws an exception of any kind
     */
    protected final void processCurrentStep() throws WikiException
    {
        while ( m_currentStep != null )
        {

            // Start and execute the current step
            if ( !m_currentStep.isStarted() )
            {
                m_currentStep.start();
            }
            try
            {
                Outcome result = m_currentStep.execute();
                if ( Outcome.STEP_ABORT.equals( result ) )
                {
                    abort();
                    break;
                }

                if ( !m_currentStep.isCompleted() )
                {
                    m_currentStep.setOutcome( result );
                }
            }
            catch ( WikiException e )
            {
                throw e;
            }

            // Get the execution Outcome; if not complete, pause workflow and
            // exit
            Outcome outcome = m_currentStep.getOutcome();
            if ( !outcome.isCompletion() )
            {
                waitstate();
                break;
            }

            // Get the next Step; if null, we're done
            Step nextStep = m_currentStep.getSuccessor( outcome );
            if ( nextStep == null )
            {
                complete();
                break;
            }

            // Add the next step to Workflow history, and mark as current
            m_history.add( nextStep );
            m_currentStep = nextStep;
        }

    }

    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance. This is a convenience
     * method.
     *
     * @param listener
     *            the event listener
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance. This is a
     * convenience method.
     *
     * @param listener
     *            the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     * Fires a WorkflowEvent of the provided type to all registered listeners.
     *
     * @see org.apache.wiki.event.WorkflowEvent
     * @param type
     *            the event type to be fired
     */
    protected final void fireEvent( int type )
    {
        if ( WikiEventManager.isListening( this ) )
        {
            WikiEventManager.fireEvent( this, new WorkflowEvent( this, type ) );
        }
    }

}
