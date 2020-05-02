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
package org.apache.wiki.tasks.pages;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.tasks.TasksManager;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.Task;
import org.apache.wiki.workflow.WorkflowManager;

import java.security.Principal;


/**
 * Handles the page pre-save actions. If the proposed page text is the same as the current version, 
 * the {@link #execute()} method returns {@link org.apache.wiki.workflow.Outcome#STEP_ABORT}. Any
 * WikiExceptions thrown by page filters will be re-thrown, and the workflow will abort.
 */
public class PreSaveWikiPageTask extends Task {

    private static final long serialVersionUID = 6304715570092804615L;
    private final Context m_context;
    private final String m_proposedText;

    /**
     * Creates the task.
     *
     * @param context The WikiContext
     * @param proposedText The text that was just saved.
     */
    public PreSaveWikiPageTask( final Context context, final String proposedText ) {
        super( TasksManager.WIKIPAGE_PRESAVE_TASK_MESSAGE_KEY );
        m_context = context;
        m_proposedText = proposedText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Outcome execute() throws WikiException {
        // Get the wiki page
        final Page page = m_context.getPage();

        // Figure out who the author was. Prefer the author set programmatically; otherwise get from the current logged in user
        if( page.getAuthor() == null ) {
            final Principal wup = m_context.getCurrentUser();
            if( wup != null ) {
                page.setAuthor( wup.getName() );
            }
        }

        // Run the pre-save filters. If any exceptions, add error to list, abort, and redirect
        final String saveText = m_context.getEngine().getManager( FilterManager.class ).doPreSaveFiltering(m_context, m_proposedText);

        // Stash the wiki context, old and new text as workflow attributes
        getWorkflowContext().put( WorkflowManager.WF_WP_SAVE_ATTR_PRESAVE_WIKI_CONTEXT, m_context );
        getWorkflowContext().put( WorkflowManager.WF_WP_SAVE_FACT_PROPOSED_TEXT, saveText );
        return Outcome.STEP_COMPLETE;
    }

}
