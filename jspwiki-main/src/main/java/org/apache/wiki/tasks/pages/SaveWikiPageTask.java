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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.tasks.TasksManager;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.Task;
import org.apache.wiki.workflow.WorkflowManager;


/**
 * Handles the actual page save and post-save actions. 
 */
public class SaveWikiPageTask extends Task {

    private static final long serialVersionUID = 3190559953484411420L;

    /**
     * Creates the Task.
     */
    public SaveWikiPageTask() {
        super( TasksManager.WIKIPAGE_SAVE_TASK_MESSAGE_KEY );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Outcome execute() throws WikiException {
        // Retrieve attributes
        final WikiContext context = ( WikiContext ) getWorkflow().getAttribute( WorkflowManager.WF_WP_SAVE_ATTR_PRESAVE_WIKI_CONTEXT );
        final String proposedText = (String) getWorkflow().getAttribute( WorkflowManager.WF_WP_SAVE_FACT_PROPOSED_TEXT );

        final WikiEngine engine = context.getEngine();
        final WikiPage page = context.getPage();

        // Let the rest of the engine handle actual saving.
        engine.getPageManager().putPageText( page, proposedText );

        // Refresh the context for post save filtering.
        engine.getPageManager().getPage( page.getName() );
        engine.getRenderingManager().textToHTML( context, proposedText );
        engine.getFilterManager().doPostSaveFiltering( context, proposedText );

        return Outcome.STEP_COMPLETE;
    }

}
