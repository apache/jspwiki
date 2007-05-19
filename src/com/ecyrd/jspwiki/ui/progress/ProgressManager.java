/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.ui.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.json.JSONRPCManager;

/**
 *  Manages progressing items.  In general this class is used whenever JSPWiki
 *  is doing something which may require a long time.  In addition, this manager
 *  provides a JSON interface for finding remotely what the progress is.  The
 *  JSON object name is JSON_PROGRESSTRACKER = "{@value JSON_PROGRESSTRACKER}".
 *
 *  @author Janne Jalkanen
 *  @since  2.6
 */
// FIXME: Gotta synchronize
public class ProgressManager
{
    private Map m_progressingTasks = new HashMap();

    /**
     *  The name of the progress tracker JSON object.  The current value is "{@value}",
     */
    public static final String JSON_PROGRESSTRACKER = "progressTracker";

    private static Logger log = Logger.getLogger( ProgressManager.class );

    public ProgressManager()
    {
        JSONRPCManager.registerGlobalObject( JSON_PROGRESSTRACKER, new JSONTracker() );
    }

    /**
     *  You can use this to get an unique process identifier.
     *  @return
     */
    public String getNewProgressIdentifier()
    {
        // FIXME: Not very good UUID
        return Long.toString( new Random().nextLong() );
    }

    /**
     *  Call this method to get your ProgressItem into the ProgressManager queue.
     *  The ProgressItem will be moved to state STARTED.
     *
     *  @param pi
     *  @param id
     */
    public void startProgress( ProgressItem pi, String id )
    {
        log.info("Adding "+id+" to progress queue");
        m_progressingTasks.put( id, pi );
        pi.setState( ProgressItem.STARTED );
    }

    /**
     *  Call this method to remove your ProgressItem from the queue (after which
     *  getProgress() will no longer find it.  The ProgressItem will be moved to state
     *  STOPPED.
     *
     *  @param id
     */
    public void stopProgress( String id )
    {
        log.info("Removed "+id+" from progress queue");
        ProgressItem pi = (ProgressItem) m_progressingTasks.remove( id );
        if( pi != null ) pi.setState( ProgressItem.STOPPED );
    }

    /**
     *  Get the progress in percents.
     *
     *  @param id
     *  @return
     *  @throws IllegalArgumentException If no such progress item exists.
     */
    public int getProgress( String id )
    {
        ProgressItem pi = (ProgressItem)m_progressingTasks.get( id );

        if( pi != null )
        {
            return pi.getProgress();
        }

        throw new IllegalArgumentException("No such id was found");
    }

    /**
     *  Provides access to a progress indicator, assuming you know the ID.
     *
     *  @author Janne Jalkanen
     */
    private class JSONTracker implements RPCCallable
    {
        /**
         *  Returns upload progress in percents so far.
         *  @param uploadId
         *  @return
         */
        public int getProgress( String progressId )
        {
            return getProgress( progressId );
        }
    }
}