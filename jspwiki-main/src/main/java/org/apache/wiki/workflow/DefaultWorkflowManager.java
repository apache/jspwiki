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

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WorkflowEvent;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * <p>
 * Monitor class that tracks running Workflows. The WorkflowManager also keeps track of the names of
 * users or groups expected to approve particular Workflows.
 * </p>
 */
public class DefaultWorkflowManager implements WorkflowManager {

    private final DecisionQueue m_queue = new DecisionQueue();
    private final Set<Workflow> m_workflows;
    private final Map<String, Principal> m_approvers;
    private final List<Workflow> m_completed;
    private WikiEngine m_engine = null;

    /**
     * Constructs a new WorkflowManager, with an empty workflow cache. New Workflows are automatically assigned unique identifiers,
     * starting with 1.
     */
    public DefaultWorkflowManager() {
        m_next = 1;
        m_workflows = ConcurrentHashMap.newKeySet();
        m_approvers = new ConcurrentHashMap<>();
        m_completed = new CopyOnWriteArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start( final Workflow workflow ) throws WikiException {
        m_workflows.add( workflow );
        workflow.setWorkflowManager( this );
        workflow.setId( nextId() );
        workflow.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set< Workflow > getWorkflows() {
        final Set< Workflow > workflows = ConcurrentHashMap.newKeySet();
        workflows.addAll( m_workflows );
        return workflows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Workflow > getCompletedWorkflows() {
        return new CopyOnWriteArrayList< >( m_completed );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final WikiEngine engine, final Properties props ) {
        m_engine = engine;

        // Identify the workflows requiring approvals
        for( final Object o : props.keySet() ) {
            final String prop = ( String )o;
            if( prop.startsWith( PROPERTY_APPROVER_PREFIX ) ) {
                // For the key, everything after the prefix is the workflow name
                final String key = prop.substring( PROPERTY_APPROVER_PREFIX.length() );
                if( key.length() > 0 ) {
                    // Only use non-null/non-blank approvers
                    final String approver = props.getProperty( prop );
                    if( approver != null && approver.length() > 0 ) {
                        m_approvers.put( key, new UnresolvedPrincipal( approver ) );
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresApproval( final String messageKey ) {
        return  m_approvers.containsKey( messageKey );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getApprover( final String messageKey ) throws WikiException {
        Principal approver = m_approvers.get( messageKey );
        if ( approver == null ) {
            throw new WikiException( "Workflow '" + messageKey + "' does not require approval." );
        }

        // Try to resolve UnresolvedPrincipals
        if ( approver instanceof UnresolvedPrincipal ) {
            final String name = approver.getName();
            approver = m_engine.getAuthorizationManager().resolvePrincipal( name );

            // If still unresolved, throw exception; otherwise, freshen our cache
            if ( approver instanceof UnresolvedPrincipal ) {
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
    protected WikiEngine getEngine() {
        if ( m_engine == null ) {
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
    private synchronized int nextId() {
        final int current = m_next;
        m_next++;
        return current;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Workflow > getOwnerWorkflows( final WikiSession session ) {
        final List< Workflow > workflows = new ArrayList<>();
        if ( session.isAuthenticated() ) {
            final Principal[] sessionPrincipals = session.getPrincipals();
            for( final Workflow w : m_workflows ) {
                final Principal owner = w.getOwner();
                for ( final Principal sessionPrincipal : sessionPrincipals ) {
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
     * Listens for {@link WorkflowEvent} objects emitted by Workflows. In particular, this
     * method listens for {@link WorkflowEvent#CREATED},
     * {@link WorkflowEvent#ABORTED} and {@link WorkflowEvent#COMPLETED}
     * events. If a workflow is created, it is automatically added to the cache. If one is aborted or completed, it 
     * is automatically removed.
     * 
     * @param event the event passed to this listener
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( event instanceof WorkflowEvent ) {
            final Workflow workflow = event.getSrc();
            switch ( event.getType() ) {
                 // Remove from manager
                 case WorkflowEvent.ABORTED   :
                 case WorkflowEvent.COMPLETED : remove( workflow ); break;
                // Add to manager
                case WorkflowEvent.CREATED    : add( workflow ); break;
                default: break;
            }
        }
    }

    /**
     * Protected helper method that adds a newly created Workflow to the cache, and sets its {@code workflowManager} and
     * {@code Id} properties if not set.
     *
     * @param workflow the workflow to add
     */
    protected void add( final Workflow workflow ) {
        if ( workflow.getWorkflowManager() == null ) {
            workflow.setWorkflowManager( this );
        }
        if ( workflow.getId() == Workflow.ID_NOT_SET ) {
            workflow.setId( nextId() );
        }
        m_workflows.add( workflow );
    }

    /**
     * Protected helper method that removes a specified Workflow from the cache, and moves it to the workflow history list. This method
     * defensively checks to see if the workflow has not yet been removed.
     *
     * @param workflow the workflow to remove
     */
    protected void remove( final Workflow workflow ) {
        if( m_workflows.contains( workflow ) ) {
            m_workflows.remove( workflow );
            m_completed.add( workflow );
        }
    }

}
