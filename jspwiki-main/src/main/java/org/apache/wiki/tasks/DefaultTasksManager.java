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
package org.apache.wiki.tasks;

import java.util.Locale;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.tasks.auth.SaveUserProfileTask;
import org.apache.wiki.tasks.pages.PreSaveWikiPageTask;
import org.apache.wiki.tasks.pages.SaveWikiPageTask;
import org.apache.wiki.workflow.Step;


/**
 * Default implementation for {@link TasksManager}.
 */
public class DefaultTasksManager implements TasksManager {

    /**
     * {@inheritDoc}
     */
    @Override
    public Step buildPreSaveWikiPageTask( WikiContext context, String proposedText ) {
        return new PreSaveWikiPageTask( context, proposedText );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Step buildSaveWikiPageTask() {
        return new SaveWikiPageTask();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Step buildSaveUserProfileTask( WikiEngine engine, Locale loc ) {
        return new SaveUserProfileTask( engine, loc );
    }
    
}
