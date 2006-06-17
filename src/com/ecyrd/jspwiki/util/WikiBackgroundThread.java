package com.ecyrd.jspwiki.util;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.event.WikiEngineEvent;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;

/**
 * Abstract Theat class that operates in the background, and listens
 * for the {@link WikiEngineEvent#SHUTDOWN} event to determine
 * whether it still needs to run. Subclasses of this method need only
 * implement the method {@link #backgroundTask()} (instead of
 * the normal {@link Thread#run()}, and provide a constructor that
 * passes the WikiEngine and sleep interval.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-06-17 23:19:40 $
 */
public abstract class WikiBackgroundThread extends Thread implements WikiEventListener
{
    private volatile boolean m_killMe = false;
    private final int m_interval;
    private static final long POLLING_INTERVAL = 1000L;
    
    /**
     * Constructs a new instance of this background thread with 
     * a specified sleep interval, and adds the thread to the 
     * wiki engine's event listeners.
     * @param engine the wiki engine
     * @param sleepInterval the interval between invocations of
     * the thread's {@link Thread#run()} method, in seconds
     */
    public WikiBackgroundThread( WikiEngine engine, int sleepInterval )
    {
        super();
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
                Logger log = Logger.getLogger( WikiBackgroundThread.class );
                log.info( "Detected wiki engine shutdown: killing " + getName() + "." );
                m_killMe = true;
            }
        }
    }
    
    /**
     * Abstract method that performs the actual work for this
     * background thread;
     *
     */
    public abstract void backgroundTask() throws Exception;
    
    /**
     * Runs the background thread's {@link #backgroundTask()} method
     * at the interval specified by {@link #getSleepInterval}.
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
            final Logger log = Logger.getLogger( WikiBackgroundThread.class );
            final String name = getName();
            log.info( "Starting up background thread: " + name + ".");
            startupTask();
            
            // Perform the background task; check every
            // second for thread death
            while( !m_killMe )
            {
                // Perform the background task
                log.debug( "Running background task: " + name + "." );
                backgroundTask();
                
                // Sleep for the interval we're supposed do, but
                // wake up every second to see if thread should die
                boolean interrupted = false;
                try
                {
                    for ( int i = 0; i < m_interval; i++ )
                    {
                        Thread.sleep( POLLING_INTERVAL );
                        if ( m_killMe )
                        {
                            interrupted = true;
                            log.debug( "Interrupted background thread: " + name + "." );
                            break;
                        }
                    }
                    if ( interrupted )
                    {
                        break;
                    }
                }
                catch( Throwable t ) {
                    log.error( "Background thread error", t);
                }
            }
            
            // Perform the shutdown task
            shutdownTask();
        }
        catch ( Exception e )
        {
            throw new InternalWikiException( e.getMessage() );
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
