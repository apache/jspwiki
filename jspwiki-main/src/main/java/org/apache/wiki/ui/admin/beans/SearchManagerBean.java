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
package org.apache.wiki.ui.admin.beans;

import java.util.Collection;

import javax.management.NotCompliantMBeanException;

import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.ui.admin.SimpleAdminBean;
import org.apache.wiki.ui.progress.ProgressItem;

/**
 *  The SearchManagerBean is a simple AdminBean interface
 *  to the SearchManager.  It currently can be used to force
 *  a reload of all of the pages.
 *
 *  @since 2.6
 */
public class SearchManagerBean extends SimpleAdminBean
{
    private static final String PROGRESS_ID = "searchmanagerbean.reindexer";

    private static final String[] METHODS = { "reload" };

    // private static Logger log = Logger.getLogger( SearchManagerBean.class );

    private WikiBackgroundThread m_updater;

    public SearchManagerBean(WikiEngine engine) throws NotCompliantMBeanException
    {
        super();
        initialize(engine);
    }

    public String[] getAttributeNames()
    {
        return new String[0];
    }

    public String[] getMethodNames()
    {
        return METHODS;
    }

    public String getTitle()
    {
        return "Search manager";
    }

    /**
     *  Starts a background thread which goes through all the pages and adds them
     *  to the reindex queue.
     *  <p>
     *  This method prevents itself from being called twice.
     */
    public synchronized void reload()
    {
        if( m_updater == null )
        {
            m_updater = new WikiBackgroundThread(m_engine, 0) {
                int m_count;
                int m_max;

                public void startupTask() throws Exception
                {
                    super.startupTask();

                    setName("Reindexer started");
                }

                public void backgroundTask() throws Exception
                {
                    Collection<WikiPage> allPages = m_engine.getPageManager().getAllPages();

                    SearchManager mgr = m_engine.getSearchManager();
                    m_max = allPages.size();

                    ProgressItem pi = new ProgressItem() {
                        public int getProgress()
                        {
                            return 100 * m_count / m_max;
                        }
                    };

                    m_engine.getProgressManager().startProgress( pi, PROGRESS_ID );

                    for( WikiPage page : allPages )
                    {
                        mgr.reindexPage(page);
                        m_count++;
                    }

                    m_engine.getProgressManager().stopProgress( PROGRESS_ID );
                    shutdown();
                    m_updater = null;
                }

            };

            m_updater.start();
        }
    }

    public int getType()
    {
        return CORE;
    }

    public String doGet(WikiContext context)
    {
        if( m_updater != null )
        {
            return "Update already in progress ("+
                   context.getEngine().getProgressManager().getProgress(PROGRESS_ID)+
                   "%)";
        }

        return "<input type='submit' id='searchmanagerbean-reload' name='searchmanagerbean-reload' value='Force index reload'/>"+
               "<div class='description'>Forces JSPWiki search engine to reindex all pages.  Use this if you think some pages are not being found even if they should.</div>";
    }

    public String doPost(WikiContext context)
    {
        String val = context.getHttpParameter("searchmanagerbean-reload");

        if( val != null )
        {
            reload();

            context.getWikiSession().addMessage( "Started reload of all indexed pages..." );

            return "";
        }

        return doGet(context);
    }

}
