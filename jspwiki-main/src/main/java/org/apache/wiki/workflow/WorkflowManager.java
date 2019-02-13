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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WorkflowEvent;


/**
 * <p>
 * Monitor class that tracks running Workflows. The WorkflowManager also keeps track of the names of
 * users or groups expected to approve particular Workflows.
 * </p>
 */
public class WorkflowManager implements WikiEventListener {

    /** The workflow attribute which stores the wikiContext. */
    public static final String WF_WP_SAVE_ATTR_PRESAVE_WIKI_CONTEXT = "wikiContext";
    /** The name of the key from jspwiki.properties which defines who shall approve the workflow of storing a wikipage.  Value is <tt>{@value}</tt> */
    public static final String WF_WP_SAVE_APPROVER = "workflow.saveWikiPage";
    /** The message key for storing the Decision text for saving a page.  Value is {@value}. */
    public static final String WF_WP_SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    /** The message key for rejecting the decision to save the page.  Value is {@value}. */
    public static final String WF_WP_SAVE_REJECT_MESSAGE_KEY = "notification.saveWikiPage.reject";
    /** Fact name for storing the page name.  Value is {@value}. */
    public static final String WF_WP_SAVE_FACT_PAGE_NAME = "fact.pageName";
    /** Fact name for storing a diff text. Value is {@value}. */
    public static final String WF_WP_SAVE_FACT_DIFF_TEXT = "fact.diffText";
    /** Fact name for storing the current text.  Value is {@value}. */
    public static final String WF_WP_SAVE_FACT_CURRENT_TEXT = "fact.currentText";
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    public static final String WF_WP_SAVE_FACT_PROPOSED_TEXT = "fact.proposedText";
    /** Fact name for storing whether the user is authenticated or not.  Value is {@value}. */
    public static final String WF_WP_SAVE_FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    /** The workflow attribute which stores the user profile. */
    public static final String WF_UP_CREATE_SAVE_ATTR_SAVED_PROFILE = "userProfile";
    /** The name of the key from jspwiki.properties which defines who shall approve the workflow of creating a user profile.  Value is <tt>{@value}</tt> */
    public static final String WF_UP_CREATE_SAVE_APPROVER = "workflow.createUserProfile";
    /** The message key for storing the Decision text for saving a user profile.  Value is {@value}. */
    public static final String WF_UP_CREATE_SAVE_DECISION_MESSAGE_KEY = "decision.createUserProfile";
    /** Fact name for storing a the submitter name. Value is {@value}. */
    public static final String WF_UP_CREATE_SAVE_FACT_SUBMITTER = "fact.submitter";
    /** Fact name for storing the preferences' login name. Value is {@value}. */
    public static final String WF_UP_CREATE_SAVE_FACT_PREFS_LOGIN_NAME = "prefs.loginname";
    /** Fact name for storing the preferences' full name. Value is {@value}. */
    public static final String WF_UP_CREATE_SAVE_FACT_PREFS_FULL_NAME = "prefs.fullname";
    /** Fact name for storing the preferences' email. Value is {@value}. */
    public static final String WF_UP_CREATE_SAVE_FACT_PREFS_EMAIL = "prefs.email";

    private final DecisionQueue m_queue = new DecisionQueue();

    private final Set<Workflow> m_workflows;

    private final Map<String, Principal> m_approvers;

    private final List<Workflow> m_completed;

    /** The prefix to use for looking up <code>jspwiki.properties</code> approval roles. */
    protected static final String PROPERTY_APPROVER_PREFIX = "jspwiki.approver.";

    /**
     * Constructs a new WorkflowManager, with an empty workflow cache. New
     * Workflows are automatically assigned unique identifiers, starting with 1.
     */
    public WorkflowManager()
    {
        m_next = 1;
        m_workflows = ConcurrentHashMap.newKeySet();
        m_approvers = new ConcurrentHashMap<>();
        m_completed = new CopyOnWriteArrayList<>();
    }

    /**
     * Adds a new workflow to the set of workflows and starts it. The new
     * workflow is automatically assigned a unique ID. If another workflow with
     * the same ID already exists, this method throws a WikIException.
     * @param workflow the workflow to start
     * @throws WikiException if a workflow the automatically assigned
     * ID already exist; this should not happen normally
     */
    public void start( Workflow workflow ) throws WikiException
    {
        m_workflows.add( workflow );
        workflow.setWorkflowManager( this );
        workflow.setId( nextId() );
        workflow.start();
    }

    /**
     * Returns a collection of the currently active workflows.
     *
     * @return the current workflows
     */
    public Collection< Workflow > getWorkflows() {
        Set< Workflow > workflows = ConcurrentHashMap.newKeySet();
        workflows.addAll( m_workflows );
        return workflows;
    }

    /**
     * Returns a collection of finished workflows; that is, those that have aborted or completed.
     * @return the finished workflows
     */
    public List< Workflow > getCompletedWorkflows() {
        return new CopyOnWriteArrayList< >( m_completed );
    }

    private WikiEngine m_engine = null;

    /**
     * Initializes the WorkflowManager using a specfied WikiEngine and
     * properties. Any properties that begin with
     * {@link #PROPERTY_APPROVER_PREFIX} will be assumed to be
     * Decisions that require approval. For a given property key, everything
     * after the prefix denotes the Decision's message key. The property
     * value indicates the Principal (Role, GroupPrincipal, WikiPrincipal) that
     * must approve the Decision. For example, if the property key/value pair
     * is <code>jspwiki.approver.workflow.saveWikiPage=Admin</code>,
     * the Decision's message key is <code>workflow.saveWikiPage</code>.
     * The Principal <code>Admin</code> will be resolved via
     * {@link org.apache.wiki.auth.AuthorizationManager#resolvePrincipal(String)}.
     * @param engine the wiki engine to associate with this WorkflowManager
     * @param props the wiki engine's properties
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;

        // Identify the workflows requiring approvals
        for ( Iterator<?> it = props.keySet().iterator(); it.hasNext(); )
        {
            String prop = (String) it.next();
            if ( prop.startsWith( PROPERTY_APPROVER_PREFIX ) )
            {

                // For the key, everything after the prefix is the workflow name
                String key = prop.substring( PROPERTY_APPROVER_PREFIX.length() );
                if ( key != null && key.length() > 0 )
                {

                    // Only use non-null/non-blank approvers
                    String approver = props.getProperty( prop );
                    if ( approver != null && approver.length() > 0 )
                    {
                        m_approvers.put( key, new UnresolvedPrincipal( approver ) );
                    }
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if a workflow matching a particular key
     * contains an approval step.
     *
     * @param messageKey
     *            the name of the workflow; corresponds to the value returned by
     *            {@link Workflow#getMessageKey()}.
     * @return the result
     */
    public boolean requiresApproval( String messageKey )
    {
        return  m_approvers.containsKey( messageKey );
    }

    /**
     * Looks up and resolves the actor who approves a Decision for a particular
     * Workflow, based on the Workflow's message key. If not found, or if
     * Principal is Unresolved, throws WikiException. This particular
     * implementation always returns the GroupPrincipal <code>Admin</code>
     *
     * @param messageKey the Decision's message key
     * @return the actor who approves Decisions
     * @throws WikiException if the message key was not found, or the
     * Principal value corresponding to the key could not be resolved
     */
    public Principal getApprover( String messageKey ) throws WikiException
    {
        Principal approver = m_approvers.get( messageKey );
        if ( approver == null )
        {
            throw new WikiException( "Workflow '" + messageKey + "' does not require approval." );
        }

        // Try to resolve UnresolvedPrincipals
        if ( approver instanceof UnresolvedPrincipal )
        {
            String name = approver.getName();
            approver = m_engine.getAuthorizationManager().resolvePrincipal( name );

            // If still unresolved, throw exception; otherwise, freshen our
            // cache
            if ( approver instanceof UnresolvedPrincipal )
            {
                throw new WikiException( "Workflow approver '" + name + "' cannot not be resolved." );
            }

            m_approvers.put( messageKey, approver );
        }
        return approver;
    }

    /**
     * Protected helper method that returns the associated WikiEngine
     *
     * @return the wiki engine
     */
    protected WikiEngine getEngine()
    {
        if ( m_engine == null )
        {
            throw new IllegalStateException( "WikiEngine cannot be null; please initialize WorkflowManager first." );
        }
        return m_engine;
    }

    /**
     * Returns the DecisionQueue associated with this WorkflowManager
     *
     * @return the decision queue
     */
    public DecisionQueue getDecisionQueue()
    {
        return m_queue;
    }

    private volatile int m_next;

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

    /**
     * Returns the current workflows a wiki session owns. These are workflows whose
     * {@link Workflow#getOwner()} method returns a Principal also possessed by the
     * wiki session (see {@link org.apache.wiki.WikiSession#getPrincipals()}). If the
     * wiki session is not authenticated, this method returns an empty Collection.
     * @param session the wiki session
     * @return the collection workflows the wiki session owns, which may be empty
     */
    public Collection< Workflow > getOwnerWorkflows( WikiSession session ) {
        List<Workflow> workflows = new ArrayList<>();
        if ( session.isAuthenticated() ) {
            Principal[] sessionPrincipals = session.getPrincipals();
            for ( Workflow w : m_workflows ) {
                Principal owner = w.getOwner();
                for ( Principal sessionPrincipal : sessionPrincipals ) {
                    if ( sessionPrincipal.equals( owner ) ) {
                        workflows.add( w );
                        break;
                    }
                }
            }
        }
        return workflows;
    }

    /**
     * Listens for {@link org.apache.wiki.event.WorkflowEvent} objects emitted by Workflows. In particular, this 
     * method listens for {@link org.apache.wiki.event.WorkflowEvent#CREATED}, 
     * {@link org.apache.wiki.event.WorkflowEvent#ABORTED} and {@link org.apache.wiki.event.WorkflowEvent#COMPLETED} 
     * events. If a workflow is created, it is automatically added to the cache. If one is aborted or completed, it 
     * is automatically removed.
     * 
     * @param event the event passed to this listener
     */
    @Override
    public void actionPerformed(WikiEvent event) {
        if (event instanceof WorkflowEvent) {
            Workflow workflow = event.getSrc();
            switch ( event.getType() ) {
                case WorkflowEvent.ABORTED:
                    // Remove from manager
                    remove( workflow );
                    break;
                case WorkflowEvent.COMPLETED:
                    // Remove from manager
                    remove( workflow );
                    break;
                case WorkflowEvent.CREATED:
                    // Add to manager
                    add( workflow );
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Protected helper method that adds a newly created Workflow to the cache, and sets its 
     * <code>workflowManager</code> and <code>Id</code> properties if not set.
     *
     * @param workflow the workflow to add
     */
    protected void add( Workflow workflow ) {
        if ( workflow.getWorkflowManager() == null ) {
            workflow.setWorkflowManager( this );
        }
        if ( workflow.getId() == Workflow.ID_NOT_SET ) {
            workflow.setId( nextId() );
        }
        m_workflows.add( workflow );
    }

    /**
     * Protected helper method that removes a specified Workflow from the cache,
     * and moves it to the workflow history list. This method defensively
     * checks to see if the workflow has not yet been removed.
     *
     * @param workflow the workflow to remove
     */
    protected void remove(Workflow workflow) {
        if ( m_workflows.contains( workflow ) ) {
            m_workflows.remove( workflow );
            m_completed.add( workflow );
        }
    }

}
