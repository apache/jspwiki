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
package org.apache.wiki.ui.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.apache.wiki.rpc.RPCCallable;
import org.apache.wiki.rpc.json.JSONRPCManager;

/**
 *  Manages progressing items.  In general this class is used whenever JSPWiki
 *  is doing something which may require a long time.  In addition, this manager
 *  provides a JSON interface for finding remotely what the progress is.  The
 *  JSON object name is JSON_PROGRESSTRACKER = "{@value #JSON_PROGRESSTRACKER}".
 *
 *  @since  2.6
 */
// FIXME: Needs synchronization, I think
public class ProgressManager
{
    private Map<String,ProgressItem> m_progressingTasks = new HashMap<String,ProgressItem>();

    /**
     *  The name of the progress tracker JSON object.  The current value is "{@value}",
     */
    public static final String JSON_PROGRESSTRACKER = "progressTracker";

    private static Logger log = Logger.getLogger( ProgressManager.class );

    /**
     *  Creates a new ProgressManager.
     */
    public ProgressManager()
    {
        JSONRPCManager.registerGlobalObject( JSON_PROGRESSTRACKER, new JSONTracker() );
    }

    /**
     *  You can use this to get an unique process identifier.
     *  @return A new random value
     */
    public String getNewProgressIdentifier()
    {
        return UUID.randomUUID().toString();
    }

    /**
     *  Call this method to get your ProgressItem into the ProgressManager queue.
     *  The ProgressItem will be moved to state STARTED.
     *
     *  @param pi ProgressItem to start
     *  @param id The progress identifier
     */
    public void startProgress( ProgressItem pi, String id )
    {
        log.debug("Adding "+id+" to progress queue");
        m_progressingTasks.put( id, pi );
        pi.setState( ProgressItem.STARTED );
    }

    /**
     *  Call this method to remove your ProgressItem from the queue (after which
     *  getProgress() will no longer find it.  The ProgressItem will be moved to state
     *  STOPPED.
     *
     *  @param id The progress identifier
     */
    public void stopProgress( String id )
    {
        log.debug("Removed "+id+" from progress queue");
        ProgressItem pi = m_progressingTasks.remove( id );
        if( pi != null ) pi.setState( ProgressItem.STOPPED );
    }

    /**
     *  Get the progress in percents.
     *
     *  @param id The progress identifier.
     *  @return a value between 0 to 100 indicating the progress
     *  @throws IllegalArgumentException If no such progress item exists.
     */
    public int getProgress( String id )
        throws IllegalArgumentException
    {
        ProgressItem pi = m_progressingTasks.get( id );

        if( pi != null )
        {
            return pi.getProgress();
        }

        throw new IllegalArgumentException("No such id was found");
    }

    /**
     *  Provides access to a progress indicator, assuming you know the ID.
     *  Progress of zero (0) means that the progress has just started, and a progress of
     *  100 means that it is complete.
     */
    public class JSONTracker implements RPCCallable
    {
        /**
         *  Returns upload progress in percents so far.
         *  @param progressId The string representation of the progress ID that you want to know the
         *                    progress of.
         *  @return a value between 0 to 100 indicating the progress
         */
        public int getProgress( String progressId )
        {
            return ProgressManager.this.getProgress( progressId );
        }
    }
}
