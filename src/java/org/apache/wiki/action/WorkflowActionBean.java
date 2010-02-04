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

package org.apache.wiki.action;

import java.util.Collection;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.workflow.Decision;
import org.apache.wiki.workflow.DecisionQueue;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.Workflow;

/**
 * Displays and processes workflow events.
 */
@UrlBinding( "/Workflow.jsp" )
public class WorkflowActionBean extends AbstractActionBean
{
    private Outcome m_outcome = null;

    private int m_id;

    /**
     * Aborts a particular workflow.
     * 
     * @return always returns a {@link RedirectResolution} to the
     *         {@link #view()} handler, so that other decisions can be made if
     *         desired.
     */
    @HandlesEvent( "abort" )
    public Resolution abort()
    {
        WikiEngine engine = getContext().getEngine();
        WikiSession wikiSession = getContext().getWikiSession();
        Collection<Workflow> workflows = engine.getWorkflowManager().getOwnerWorkflows( wikiSession );
        for( Workflow workflow : workflows )
        {
            if( workflow.getId() == m_id )
            {
                workflow.abort();
                break;
            }
        }
        return new RedirectResolution( WorkflowActionBean.class, "view" );
    }

    /**
     * Takes a decision for a particular workflow.
     * 
     * @return always returns a {@link RedirectResolution} to the
     *         {@link #view()} handler, so that other decisions can be made if
     *         desired.
     */
    @HandlesEvent( "decide" )
    public Resolution decide() throws WikiException
    {
        WikiEngine engine = getContext().getEngine();
        WikiSession wikiSession = getContext().getWikiSession();
        DecisionQueue dq = engine.getWorkflowManager().getDecisionQueue();

        // Find our actor's decision
        Collection<Decision> decisions = dq.getActorDecisions( wikiSession );
        for( Decision decision : decisions )
        {
            if( decision.getId() == m_id )
            {
                // Cool, we found it. Now make the decision.
                dq.decide( decision, m_outcome );
                break;
            }
        }
        return new RedirectResolution( WorkflowActionBean.class, "view" );
    }

    /**
     * Returns the collection of Decisions for the current user. This is a
     * read-only property.
     * 
     * @return the collection fo decisions
     */
    public List<Decision> getDecisions()
    {
        WikiEngine engine = getContext().getEngine();
        WikiSession wikiSession = getContext().getWikiSession();
        DecisionQueue dq = engine.getWorkflowManager().getDecisionQueue();
        return dq.getActorDecisions( wikiSession );
    }

    /**
     * Returns the workflow or decision ID that is being acted on.
     * 
     * @return the ID
     */
    public int getId()
    {
        return m_id;
    }

    /**
     * Return the Outcome of the decision taken in the {@link #decide()} action.
     * 
     * @return the outcome
     */
    public Outcome getOutcome()
    {
        return m_outcome;
    }

    /**
     * Returns the collection of Workflows for the current user. This is a
     * read-only property.
     * 
     * @return the collection of workflows
     */
    public List<Workflow> getWorkflows()
    {
        WikiEngine engine = getContext().getEngine();
        WikiSession wikiSession = getContext().getWikiSession();
        return engine.getWorkflowManager().getOwnerWorkflows( wikiSession );
    }

    /**
     * Sets the workflow or decision ID that is being acted on.
     * 
     * @param id the ID number of the workflow to work on.
     */
    @Validate( required = true, on = { "abort", "decide" } )
    public void setId( int id )
    {
        m_id = id;
    }

    /**
     * Sets the Outcome for the decision taken in the {@link #decide()} action.
     * 
     * @param outcome the outcome to set
     */
    @Validate( required = true, on = "decide" )
    public void setOutcome( Outcome outcome )
    {
        m_outcome = outcome;
    }

    /**
     * Event that the user to the preview display JSP.
     * 
     * @return always returns a forward resolution to the template JSP
     * {@code /Workflow.jsp}.
     */
    @DefaultHandler
    @HandlesEvent( "view" )
    @WikiRequestContext( "workflow" )
    public Resolution view()
    {
        return new ForwardResolution( "/templates/default/Workflow.jsp" );
    }
}
