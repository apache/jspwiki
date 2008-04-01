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
package com.ecyrd.jspwiki.util;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.event.WikiEngineEvent;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;

/**
 * Abstract Thread subclass that operates in the background;
 * when it detects the {@link WikiEngineEvent#SHUTDOWN} event,
 * it terminates itself. Subclasses of this method need only
 * implement the method {@link #backgroundTask()}, instead of
 * the normal {@link Thread#run()}, and provide a constructor that
 * passes the WikiEngine and sleep interval. This class is
 * thread-safe.
 * @author Andrew Jaquith
 */
public abstract class WikiBackgroundThread extends Thread implements WikiEventListener
{
    private volatile boolean m_killMe = false;
    private final WikiEngine m_engine;
    private final int m_interval;
    private static final long POLLING_INTERVAL = 1000L;
    
    /**
     * Constructs a new instance of this background thread with 
     * a specified sleep interval, and adds the new instance to the 
     * wiki engine's event listeners.
     * @param engine the wiki engine
     * @param sleepInterval the interval between invocations of
     * the thread's {@link Thread#run()} method, in seconds
     */
    public WikiBackgroundThread( WikiEngine engine, int sleepInterval )
    {
        super();
        m_engine = engine;
        m_interval = sleepInterval;
        engine.addWikiEventListener( this );
        setDaemon( false );
        
    }
    
    /**
     * Listens for {@link com.ecyrd.jspwiki.event.WikiEngineEvent#SHUTDOWN}
     * and, if detected, marks the thread for death.
     * @see com.ecyrd.jspwiki.event.WikiEventListener#actionPerformed(com.ecyrd.jspwiki.event.WikiEvent)
     */
    public final void actionPerformed( WikiEvent event )
    {
        if ( event instanceof WikiEngineEvent )
        {
            if ( ((WikiEngineEvent)event).getType() == WikiEngineEvent.SHUTDOWN )
            {
                System.out.println( "Detected wiki engine shutdown: killing " + getName() + "." );
                m_killMe = true;
            }
        }
    }
    
    /**
     * Abstract method that performs the actual work for this
     * background thread; subclasses must implement this method.
     */
    public abstract void backgroundTask() throws Exception;
    
    /**
     * Returns the WikiEngine that created this background thread.
     * @return the wiki engine
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }
    
    /**
     *  Requests the shutdown of this background thread.  Note that the shutdown
     *  is not immediate.
     *  
     *  @since 2.4.92
     *
     */
    public void shutdown()
    {
        m_killMe = true;
    }
    
    /**
     * Runs the background thread's {@link #backgroundTask()} method
     * at the interval specified at construction.
     * The thread will initially pause for a full sleep interval
     * before starting, after which it will execute 
     * {@link #startupTask()}. This method will cleanly 
     * terminates the thread if the it has previously 
     * been marked for death, before which it will execute
     * {@link #shutdownTask()}. If any of the three methods
     * return an exception, it will be re-thrown as a
     * {@link com.ecyrd.jspwiki.InternalWikiException}.
     * @see java.lang.Thread#run()
     */
    public final void run() 
    {
        try 
        {
            // Perform the initial startup task
            final String name = getName();
            System.out.println( "Starting up background thread: " + name + ".");
            startupTask();
            
            // Perform the background task; check every
            // second for thread death
            while( !m_killMe )
            {
                // Perform the background task
                // log.debug( "Running background task: " + name + "." );
                backgroundTask();
                
                // Sleep for the interval we're supposed do, but
                // wake up every second to see if thread should die
                boolean interrupted = false;
                try
                {
                    for( int i = 0; i < m_interval; i++ )
                    {
                        Thread.sleep( POLLING_INTERVAL );
                        if( m_killMe )
                        {
                            interrupted = true;
                            System.out.println( "Interrupted background thread: " + name + "." );
                            break;
                        }
                    }
                    if( interrupted )
                    {
                        break;
                    }
                }
                catch( Throwable t ) 
                {
                    System.err.println( "Background thread error: (stack trace follows)" );
                    t.printStackTrace();
                }
            }
            
            // Perform the shutdown task
            shutdownTask();
        }
        catch( Throwable t )
        {
            System.err.println( "Background thread error: (stack trace follows)" );
            t.printStackTrace();
            throw new InternalWikiException( t.getMessage() );
        }
    }
    
    /**
     * Executes a task after shutdown signal was detected.
     * By default, this method does nothing; override it 
     * to implement custom functionality.
     */
    public void shutdownTask() throws Exception
    {
    }
    
    /**
     * Executes a task just after the thread's {@link Thread#run()}
     * method starts, but before the {@link #backgroundTask()}
     * task executes. By default, this method does nothing; 
     * override it to implement custom functionality.
     */
    public void startupTask() throws Exception
    {
    }
    
}
