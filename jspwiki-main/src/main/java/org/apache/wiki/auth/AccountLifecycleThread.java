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
package org.apache.wiki.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.api.core.Engine;

import java.util.Date;


/**
 * Applies {@link AccountLifecycle} to the user accounts of a wiki, once when the wiki has started and then at an interval. It ends
 * with the wiki.
 *
 * @since 3.0.1
 */
final class AccountLifecycleThread extends WikiBackgroundThread {

    private static final Logger LOG = LogManager.getLogger( AccountLifecycleThread.class );

    /** How long to wait for the wiki to be ready, in seconds. */
    private static final int STARTUP_WAIT = 120;

    private final AccountLifecycle lifecycle;

    /**
     * Creates the thread, which is not started.
     *
     * @param engine the wiki engine
     * @param lifecycle what to do to the accounts
     * @param intervalSeconds the time between two passes, in seconds
     */
    AccountLifecycleThread( final Engine engine, final AccountLifecycle lifecycle, final int intervalSeconds ) {
        super( engine, intervalSeconds );
        this.lifecycle = lifecycle;
        setName( "JSPWiki Account Lifecycle" );
    }

    /** The thread is started while the wiki is, and the managers it needs may not be there yet. */
    @Override
    public void startupTask() throws InterruptedException {
        for( int i = 0; i < STARTUP_WAIT * 2 && !getEngine().isConfigured(); i++ ) {
            Thread.sleep( 500L );
        }
    }

    @Override
    public void backgroundTask() {
        try {
            lifecycle.apply( new Date() );
        } catch( final RuntimeException e ) {
            // Whatever it was, the next pass tries again
            LOG.error( "The pass over the user accounts failed: {}", e.getMessage(), e );
        }
    }

}
