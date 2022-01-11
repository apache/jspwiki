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

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.WorkflowManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;

import static org.apache.wiki.TestEngine.with;

public class SaveWikiPageTaskTest {

    @Test
    public void testSaveWikiPageTask() throws Exception {
        final TestEngine engine = TestEngine.build( with( "jspwiki.lucene.initialdelay", "0" ),
                                                    with( "jspwiki.lucene.indexdelay", "1" ) );
        final String pageName = "TestSaveWikiPageTestPage";
        final Page page = Wiki.contents().page( engine, pageName );
        engine.saveText( pageName, "initial text on first revision" );
        final Context context = Wiki.context().create( engine, engine.newHttpRequest(), page );
        final SaveWikiPageTask task = new SaveWikiPageTask();
        task.setWorkflow( 1, new HashMap<>() );
        task.getWorkflowContext().put( WorkflowManager.WF_WP_SAVE_FACT_PROPOSED_TEXT, "correct horse battery staple" );
        final Collection< SearchResult > res = new ArrayList<>();

        Assertions.assertEquals( Outcome.STEP_COMPLETE, task.execute( context ) );
        Awaitility.await( "ensure page gets indexed" ).until( findsResultsFor( context, res, "horse" ) );
        Assertions.assertEquals( 1, res.size(), "no pages found" );
        Assertions.assertEquals( pageName, res.iterator().next().getPage().getName(), "page" );
    }

    @Test
    public void testSaveWikiPageTaskWithVersioningFileProvider() throws Exception {
        final TestEngine engine = TestEngine.build( with( "jspwiki.pageProvider", "VersioningFileProvider" ),
                                                    with( "jspwiki.lucene.initialdelay", "0" ),
                                                    with( "jspwiki.lucene.indexdelay", "1" ) );
        final String pageName = "TestSaveWikiPageTestPageWithVersioningFileProvider";
        final Page page = Wiki.contents().page( engine, pageName );
        engine.saveText( pageName, "initial text on first revision" );
        final Context context = Wiki.context().create( engine, engine.newHttpRequest(), page );
        final SaveWikiPageTask task = new SaveWikiPageTask();
        task.setWorkflow( 1, new HashMap<>() );
        task.getWorkflowContext().put( WorkflowManager.WF_WP_SAVE_FACT_PROPOSED_TEXT,
                                       "It is based on ties of history, culture, language, ethnicity, kinship and geography" );
        final Collection< SearchResult > res = new ArrayList<>();

        Assertions.assertEquals( Outcome.STEP_COMPLETE, task.execute( context ) );
        Awaitility.await( "ensure latest version of page gets indexed" ).until( findsResultsFor( context, res, "kinship" ) );
        Assertions.assertEquals( 1, res.size(), "no pages found" );
        Assertions.assertEquals( pageName, res.iterator().next().getPage().getName(), "page" );
    }

    Callable< Boolean > findsResultsFor( final Context context, final Collection< SearchResult > res, final String text ) {
        return () -> {
            final Collection< SearchResult > search = context.getEngine().getManager( SearchManager.class ).findPages( text, context );
            if( search != null && search.size() > 0 ) {
                res.addAll( search );
                return true;
            }
            return false;
        };
    }

}
