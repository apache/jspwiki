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

import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.ui.admin.SimpleAdminBean;
import org.apache.wiki.ui.progress.ProgressItem;
import org.apache.wiki.ui.progress.ProgressManager;

import javax.management.NotCompliantMBeanException;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The SearchManagerBean is a simple AdminBean interface to the SearchManager.  It currently can be used to force a reload of all of the pages.
 *
 *  @since 2.6
 */
public class SearchManagerBean extends SimpleAdminBean {

    private static final String PROGRESS_ID = "searchmanagerbean.reindexer";
    private static final String[] METHODS = { "reload" };

    // private static Logger log = LogManager.getLogger( SearchManagerBean.class );

    private WikiBackgroundThread m_updater;

    /**
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private final ReentrantLock lock;

    public SearchManagerBean( final Engine engine ) throws NotCompliantMBeanException {
        super();
        initialize( engine );
        this.lock = new ReentrantLock();
    }

    @Override
    public String[] getAttributeNames()
    {
        return new String[0];
    }

    @Override
    public String[] getMethodNames()
    {
        return METHODS;
    }

    @Override
    public String getTitle()
    {
        return "Search manager";
    }

    /**
     *  Starts a background thread which goes through all the pages and adds them to the reindex queue.
     *  <p>
     *  This method prevents itself from being called twice.
     */
    public void reload() {
        lock.lock();
        try {
            if( m_updater == null ) {
                m_updater = new WikiBackgroundThread( m_engine, 0 ) {

                    int m_count;
                    int m_max;

                    @Override
                    public void startupTask() throws Exception {
                        super.startupTask();

                        setName( "Reindexer started" );
                    }

                    @Override
                    public void backgroundTask() throws Exception {
                        final Collection< Page > allPages = m_engine.getManager( PageManager.class ).getAllPages();

                        final SearchManager mgr = m_engine.getManager( SearchManager.class );
                        m_max = allPages.size();

                        final ProgressItem pi = new ProgressItem() {

                            @Override
                            public int getProgress() {
                                return 100 * m_count / m_max;
                            }
                        };
                        m_engine.getManager( ProgressManager.class ).startProgress( pi, PROGRESS_ID );

                        for( final Page page : allPages ) {
                            mgr.reindexPage( page );
                            m_count++;
                        }

                        m_engine.getManager( ProgressManager.class ).stopProgress( PROGRESS_ID );
                        shutdown();
                        m_updater = null;
                    }

                };

                m_updater.start();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getType() {
        return CORE;
    }

    @Override
    public String doGet( final Context context ) {
        if( m_updater != null ) {
            return "Update already in progress ("+ context.getEngine().getManager( ProgressManager.class ).getProgress(PROGRESS_ID)+ "%)";
        }

        return "<input type='submit' id='searchmanagerbean-reload' name='searchmanagerbean-reload' value='Force index reload'/>"+
               "<div class='description'>Forces JSPWiki search engine to reindex all pages.  Use this if you think some pages are not being found even if they should.</div>";
    }

    @Override
    public String doPost( final Context context ) {
        final String val = context.getHttpParameter( "searchmanagerbean-reload" );
        if( val != null ) {
            reload();
            context.getWikiSession().addMessage( "Started reload of all indexed pages..." );
            return "";
        }

        return doGet( context );
    }

}
