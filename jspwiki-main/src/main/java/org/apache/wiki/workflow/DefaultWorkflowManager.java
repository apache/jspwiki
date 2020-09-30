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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.acl.UnresolvedPrincipal;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventEmitter;
import org.apache.wiki.event.WorkflowEvent;

import java.io.*;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * <p>
 * Monitor class that tracks running Workflows. The WorkflowManager also keeps track of the names of
 * users or groups expected to approve particular Workflows.
 * </p>
 */
public class DefaultWorkflowManager implements WorkflowManager {

    private static final Logger LOG = Logger.getLogger( DefaultWorkflowManager.class );
    static final String SERIALIZATION_FILE = "wkflmgr.ser";

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 6L;

    DecisionQueue m_queue = new DecisionQueue();
    Set< Workflow > m_workflows;
    final Map< String, Principal > m_approvers;
    List< Workflow > m_completed;
    private Engine m_engine = null;

    /**
     * Constructs a new WorkflowManager, with an empty workflow cache.
     */
    public DefaultWorkflowManager() {
        m_workflows = ConcurrentHashMap.newKeySet();
        m_approvers = new ConcurrentHashMap<>();
        m_completed = new CopyOnWriteArrayList<>();
        WikiEventEmitter.attach( this );
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
     *
     * Any properties that begin with {@link #PROPERTY_APPROVER_PREFIX} will be assumed to be Decisions that require approval. For a given
     * property key, everything after the prefix denotes the Decision's message key. The property value indicates the Principal (Role,
     * GroupPrincipal, WikiPrincipal) that must approve the Decision. For example, if the property key/value pair is
     * {@code jspwiki.approver.workflow.saveWikiPage=Admin}, the Decision's message key is <code>workflow.saveWikiPage</code>. The Principal
     * <code>Admin</code> will be resolved via {@link org.apache.wiki.auth.AuthorizationManager#resolvePrincipal(String)}.
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) {
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

        unserializeFromDisk( new File( m_engine.getWorkDir(), SERIALIZATION_FILE ) );
    }

    /**
     *  Reads the serialized data from the disk back to memory.
     *
     * @return the date when the data was last written on disk or {@code 0} if there has been problems reading from disk.
     */
    @SuppressWarnings( "unchecked" )
    synchronized long unserializeFromDisk( final File f ) {
        long saved = 0L;
        final StopWatch sw = new StopWatch();
        sw.start();
        try( final ObjectInputStream in = new ObjectInputStream( new BufferedInputStream( new FileInputStream( f ) ) ) ) {
            final long ver = in.readLong();
            if( ver != serialVersionUID ) {
                LOG.warn( "File format has changed; Unable to recover workflows and decision queue from disk." );
            } else {
                saved        = in.readLong();
                m_workflows  = ( Set< Workflow > )in.readObject();
                m_queue      = ( DecisionQueue )in.readObject();
                m_completed  = ( List< Workflow > )in.readObject();
                LOG.debug( "Read serialized data successfully in " + sw );
            }
        } catch( final IOException | ClassNotFoundException e ) {
            LOG.error( "unable to recover from disk workflows and decision queue: " + e.getMessage(), e );
        }
        sw.stop();

        return saved;
    }

    /**
     *  Serializes workflows and decisionqueue to disk.  The format is private, don't touch it.
     */
    synchronized void serializeToDisk( final File f ) {
        try( final ObjectOutputStream out = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( f ) ) ) ) {
            final StopWatch sw = new StopWatch();
            sw.start();

            out.writeLong( serialVersionUID );
            out.writeLong( System.currentTimeMillis() ); // Timestamp
            out.writeObject( m_workflows );
            out.writeObject( m_queue );
            out.writeObject( m_completed );

            sw.stop();

            LOG.debug( "serialization done - took " + sw );
        } catch( final IOException ioe ) {
            LOG.error( "Unable to serialize!", ioe );
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
            approver = m_engine.getManager( AuthorizationManager.class ).resolvePrincipal( name );

            // If still unresolved, throw exception; otherwise, freshen our cache
            if ( approver instanceof UnresolvedPrincipal ) {
                throw new WikiException( "Workflow approver '" + name + "' cannot not be resolved." );
            }

            m_approvers.put( messageKey, approver );
        }
        return approver;
    }

    /**
     * Protected helper method that returns the associated Engine
     *
     * @return the wiki engine
     */
    protected Engine getEngine() {
        if ( m_engine == null ) {
            throw new IllegalStateException( "Engine cannot be null; please initialize WorkflowManager first." );
        }
        return m_engine;
    }

    /**
     * Returns the DecisionQueue associated with this WorkflowManager
     *
     * @return the decision queue
     */
    @Override
    public DecisionQueue getDecisionQueue() {
        return m_queue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List< Workflow > getOwnerWorkflows( final Session session ) {
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
     * Listens for {@link WorkflowEvent} objects emitted by Workflows. In particular, this method listens for {@link WorkflowEvent#CREATED},
     * {@link WorkflowEvent#ABORTED}, {@link WorkflowEvent#COMPLETED} and {@link WorkflowEvent#DQ_REMOVAL} events. If a workflow is created,
     * it is automatically added to the cache. If one is aborted or completed, it is automatically removed. If a removal from decision queue
     * is issued, the current step from workflow, which is assumed to be a {@link Decision}, is removed from the {@link DecisionQueue}.
     * 
     * @param event the event passed to this listener
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( event instanceof WorkflowEvent ) {
            if( event.getSrc() instanceof Workflow ) {
                final Workflow workflow = event.getSrc();
                switch( event.getType() ) {
                // Remove from manager
                case WorkflowEvent.ABORTED   :
                case WorkflowEvent.COMPLETED : remove( workflow ); break;
                // Add to manager
                case WorkflowEvent.CREATED   : add( workflow ); break;
                default: break;
                }
            } else if( event.getSrc() instanceof Decision ) {
                final Decision decision = event.getSrc();
                switch( event.getType() ) {
                // Add to DecisionQueue
                case WorkflowEvent.DQ_ADDITION : addToDecisionQueue( decision ); break;
                // Remove from DecisionQueue
                case WorkflowEvent.DQ_REMOVAL  : removeFromDecisionQueue( decision, event.getArg( 0, Context.class ) ); break;
                default: break;
                }
            }
            serializeToDisk( new File( m_engine.getWorkDir(), SERIALIZATION_FILE ) );
        }
    }

    /**
     * Protected helper method that adds a newly created Workflow to the cache, and sets its {@code workflowManager} and
     * {@code Id} properties if not set.
     *
     * @param workflow the workflow to add
     */
    protected void add( final Workflow workflow ) {
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

    protected void removeFromDecisionQueue( final Decision decision, final Context context ) {
        // If current workflow is waiting for input, restart it and remove Decision from DecisionQueue
        final int workflowId = decision.getWorkflowId();
        final Optional< Workflow > optw = m_workflows.stream().filter( w -> w.getId() == workflowId ).findAny();
        if( optw.isPresent() ) {
            final Workflow w = optw.get();
            if( w.getCurrentState() == Workflow.WAITING && decision.equals( w.getCurrentStep() ) ) {
                getDecisionQueue().remove( decision );
                // Restart workflow
                try {
                    w.restart( context );
                } catch( final WikiException e ) {
                    LOG.error( "restarting workflow #" + w.getId() + " caused " + e.getMessage(), e );
                }
            }
        }
    }

    protected void addToDecisionQueue( final Decision decision ) {
        getDecisionQueue().add( decision );
    }

}
