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
package org.apache.wiki.util;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.Date;

import javax.management.*;
import javax.management.timer.Timer;
import javax.management.timer.TimerNotification;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.Release;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.event.WikiEngineEvent;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * Abstract background class that responds to JMX timer events for a particular
 * WikiEngine; when it detects the {@link WikiEngineEvent#SHUTDOWN} event, it
 * removes itself from the timer's listener list. Subclasses of this method need
 * only implement the method {@link #backgroundTask()}, and provide a
 * constructor that passes the WikiEngine and sleep interval to this superclass
 * constructor. This class is <em>not</em> thread-safe.
 */
public abstract class WikiBackgroundThread implements WikiEventListener
{
    public static class TimerListener implements NotificationListener
    {
        private final int m_timerId;

        private final WikiBackgroundThread m_task;

        TimerListener( WikiBackgroundThread task, int timerId )
        {
            m_timerId = timerId;
            m_task = task;
        }

        public void handleNotification( Notification notification, Object handback )
        {
            if( ((TimerNotification) notification).getNotificationID() == m_timerId )
            {
                m_task.doBackgroundTask();
            }
        }
    }

    /**
     * Registers and starts the wiki-wide JMX timer.
     * @param engine the WikiEngine
     */
    public static final ObjectName registerTimer( WikiEngine engine )
    {
        try
        {
            ObjectName timer = getTimer( engine );
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if( !server.isRegistered( timer ) )
            {
                server.createMBean( "javax.management.timer.Timer", timer );
                server.invoke( timer, "start", new Object[] {}, new String[] {} );
            }
            return timer;
        }
        catch( JMException e )
        {
            e.printStackTrace();
            throw new InternalWikiException( "Could not register JMX timer: " + e.getMessage(), e );
        }
    }

    /**
     * Stops and unregisters the wiki-wide JMX timer.
     * @param engine the WikiEngine
     */
    public static final void unregisterTimer( WikiEngine engine )
    {
        try
        {
            // Stop the timer and unregister it
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName timer = getTimer( engine ); 
            server.invoke( timer, "stop", new Object[] {}, new String[] {} );
            server.unregisterMBean( timer );
        }
        catch( JMException e )
        {
            e.printStackTrace();
            throw new InternalWikiException( "Could not unregister JMX timer: " + e.getMessage(), e );
        }
    }

    private static ObjectName getTimer( WikiEngine engine ) throws JMException
    {
        return new ObjectName( Release.APPNAME + ":wiki=" + engine.getApplicationName() + ",component=timer,name=Timer service" );
    }

    private final Logger m_log;

    /**
     * Method signature parameters for
     * {@link javax.management.timer.Timer#addNotification(String, String, Object, Date, long)}.
     */
    private static final String[] ADD_NOTIFICATION_SIGNATURE = new String[] { String.class.getName(), String.class.getName(),
                                                                             Object.class.getName(), Date.class.getName(), "long" };

    /**
     * Method signature parameters for
     * {@link javax.management.timer.Timer#removeNotification(Integer)}.
     */
    private static final String[] REMOVE_NOTIFICATION_SIGNATURE = new String[] { Integer.class.getName() };
    
    /**
     * Method signature parameters for
     * {@link javax.management.MBeanServer#unregisterMBean(ObjectName)}.
     */
    private static final String[] UNREGISTER_TIMER_SIGNATURE = new String[] { ObjectName.class.getName() };

    /**
     * Returns the MBeanServer used to add notifications.
     * 
     * @throws JMException if the Timer cannot be retrieved, created or returned
     */
    private static MBeanServer getMBeanServer() throws JMException
    {
        // Get the platform MBeanServer and ObjectName
        return ManagementFactory.getPlatformMBeanServer();
    }

    private final WeakReference<WikiEngine> m_engine;

    private final ObjectName m_timer;

    private TimerListener m_timerListener = null;

    private String m_name;

    private final int m_sleepInterval;

    private int m_notificationId;

    /**
     * Constructs a new instance of this background thread with a specified
     * sleep interval, and adds the new instance to the wiki engine's event
     * listeners. It also lazily adds a {@link Timer} instance to the
     * MBeanServer if one has not previously been registered. Each WikiEngine
     * will have one Timer.
     * 
     * @param engine the wiki engine
     * @param sleepInterval the interval between invocations of the thread's
     *            {@link Thread#run()} method, in seconds
     * @throws InternalWikiException if the ObjectName for the timer object
     *             cannot be created
     */
    public WikiBackgroundThread( WikiEngine engine, int sleepInterval )
    {
        super();
        m_engine = new WeakReference<WikiEngine>( engine );
        m_name = getClass().getName();
        m_log = LoggerFactory.getLogger( this.getClass() );
        m_notificationId = 0;
        m_sleepInterval = sleepInterval;
        m_timer = registerTimer( engine );
        engine.addWikiEventListener( this );
    }

    /**
     * Listens for {@link org.apache.wiki.event.WikiEngineEvent#SHUTDOWN} and,
     * if detected, removes the task from the JMX timer.
     * 
     * @param event {@inheritDoc}
     * @see org.apache.wiki.event.WikiEventListener#actionPerformed(org.apache.wiki.event.WikiEvent)
     */
    public final void actionPerformed( WikiEvent event )
    {
        if( event instanceof WikiEngineEvent )
        {
            if( ((WikiEngineEvent) event).getType() == WikiEngineEvent.SHUTDOWN )
            {
                shutdown();
            }
        }
    }

    /**
     * Abstract method that performs the actual work for this background thread;
     * subclasses must implement this method.
     * 
     * @throws Exception Any exception can be thrown
     */
    public abstract void backgroundTask() throws Exception;

    /**
     * Returns the WikiEngine that created this background thread. Note that it
     * is possible that the garbage collector has already collected the
     * WikiEngine reference, so callers must check for {@code null} return
     * values.
     * 
     * @return the wiki engine, or {@code null} if no reference
     */
    public final WikiEngine getEngine()
    {
        return m_engine.get();
    }

    /**
     * Returns the name of the background task.
     * 
     * @return the name
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Sets the name of the background task.
     * 
     * @param name the name
     */
    public final void setName( String name )
    {
        m_name = name;
    }

    /**
     * Requests the shutdown of this background thread. Note that the shutdown
     * is not immediate.
     * 
     * @since 2.4.92
     */
    public final void shutdown()
    {
        m_log.info( "Stopping " + m_name + "." );
        try
        {
            try
            {
                removeJMXListener();
                if ( m_engine.get() != null )
                {
                    shutdownTask();
                }
            }
            catch( Exception e )
            {
                e.printStackTrace();
                throw new InternalWikiException( "Could not stop " + m_name + "!", e );
            }
        }
        catch( Throwable t )
        {
            m_log.error( "Background task shutdown error: " + t.getMessage() );
            t.printStackTrace();
            throw new InternalWikiException( t.getMessage() );
        }
    }

    /**
     * Executes a task after shutdown signal was detected. By default, this
     * method does nothing; override it to implement custom functionality.
     * 
     * @throws Exception Any exception can be thrown.
     */
    public void shutdownTask() throws Exception
    {
    }

    /**
     * Causes this background task to begin execution by calling
     * {@link #startupTask()}. After the startup task runs,
     * the background task will be scheduled to start one full
     * sleep interval later.
     */
    public final void start()
    {
        // Add a notification timer and start the task
        m_log.info( "Starting " + getName() + "." );
        try
        {
            if ( m_engine.get() != null )
            {
                startupTask();
                addJMXListener();
            }
        }
        catch( JMException e )
        {
            e.printStackTrace();
            throw new InternalWikiException( "Could not add JMX listener for " + m_name + "!", e );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            throw new InternalWikiException( "Could not start " + m_name + "!", e );
        }
    }

    /**
     * Executes a task just after the thread's {@link Thread#run()} method
     * starts, but before the {@link #backgroundTask()} task executes. By
     * default, this method does nothing; override it to implement custom
     * functionality.
     * 
     * @throws Exception Any exception can be thrown.
     */
    public void startupTask() throws Exception
    {
    }

    /**
     * Adds a notification and listener to the Timer MBean.
     * 
     * @throws JMException if either of these things can't be done for any
     *             reason
     */
    private final void addJMXListener() throws JMException
    {
        MBeanServer server = getMBeanServer();

        // Add notification
        long interval = Timer.ONE_SECOND * m_sleepInterval;
        Date startTime = new Date( new Date().getTime() + interval );
        Object[] timerArgs = new Object[] { this.getClass().getName(), "Timed task", null, startTime, interval };
        m_notificationId = (Integer) server.invoke( m_timer, "addNotification", timerArgs, ADD_NOTIFICATION_SIGNATURE );

        // Add the listener
        m_timerListener = new TimerListener( this, m_notificationId );
        server.addNotificationListener( m_timer, m_timerListener, null, null );

        // Start the timer
        server.invoke( m_timer, "start", new Object[] {}, new String[] {} );
    }

    /*
     * Removes the notification and listener from the Timer MBean.
     * @throws JMException if either of these things can't be done for any
     * reason
     */
    private final void removeJMXListener() throws JMException
    {
        try
        {
            MBeanServer server = getMBeanServer();

            // Remove the listener
            server.removeNotificationListener( getTimer( m_engine.get() ), m_timerListener );

            // Remove notification
            Object[] args = new Object[] { m_notificationId };
            server.invoke( m_timer, "removeNotification", args, REMOVE_NOTIFICATION_SIGNATURE );
        }
        catch( JMException e )
        {
            if( e.getCause() instanceof InstanceNotFoundException )
            {
                // Adding the listener failed the first time, so it's ok to eat
                // this exception
            }
            else
            {
                throw e;
            }
        }
    }

    /**
     * Called by {@link TimerListener#handleNotification(Notification, Object)},
     * this method wraps {@link #backgroundTask()} and checks to see if it runs
     * correctly. If it throws an Exception of any kind, {@link #shutdown()} is
     * executed to remove the listener and shut down the task.
     */
    protected final void doBackgroundTask()
    {
        try
        {
            if ( m_engine.get() != null )
            {
                m_log.info( "Running " + m_name + "." );
                backgroundTask();
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
            try
            {
                m_log.error( "Could not run " + m_name + "! Shutting it down." );
                shutdown();
            }
            catch( Exception e2 )
            {
                m_log.error( "Could not shut down " + m_name + "!" );
                e2.printStackTrace();
            }
        }
    }
}
