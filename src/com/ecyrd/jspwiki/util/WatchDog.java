/*
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.util;

import java.util.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  WatchDog is a general system watchdog.  You can attach any Watchable
 *  or a Thread object to it, and it will notify you if a timeout has been
 *  exceeded.
 *  <p>
 *  The notification of the timeouts is done from a separate WatchDog thread,
 *  of which there is one per watched thread.  This Thread is named 'WatchDog for
 *  XXX', where XXX is your Thread name.
 *  <p>
 *  The suggested method of obtaining a WatchDog is via the static factory
 *  method, since it will return you the correct watchdog for the current
 *  thread.  However, we do not prevent you from creating your own watchdogs
 *  either.
 *  <p>
 *  If you create a WatchDog for a Thread, the WatchDog will figure out when
 *  the Thread is dead, and will stop itself accordingly.  However, this object
 *  is not automatically released, so you might want to check it out after a while.
 *  
 *  @author Janne Jalkanen
 *  @since  2.4.92
 */
public class WatchDog
{
    private Watchable m_watchable;
    private Stack     m_stateStack = new Stack();
    private boolean   m_enabled    = true;
    private WikiBackgroundThread m_thread;
    private WikiEngine m_engine;
    
    Logger log = Logger.getLogger(WatchDog.class.getName());
    
    private static HashMap c_kennel = new HashMap();
    
    /**
     *  Returns the current watchdog for the current thread. This
     *  is the preferred method of getting you a Watchdog, since it
     *  keeps an internal list of Watchdogs for you so that there
     *  won't be more than one watchdog per thread.
     *  
     *  @param engine The WikiEngine to which the Watchdog should
     *                be bonded to.
     *  @return A usable WatchDog object.
     */
    public static WatchDog getCurrentWatchDog( WikiEngine engine )
    {
        Thread t = Thread.currentThread();
        
        WatchDog w = (WatchDog)c_kennel.get( new Integer(t.hashCode()) );
        
        if( w == null )
        {
            w = new WatchDog( engine, t );
            
            synchronized( c_kennel )
            {
                c_kennel.put( new Integer(t.hashCode()), w );
            }
        }
        
        return w;
    }
    
    /**
     *  Creates a new WatchDog for a Watchable.
     *  
     *  @param engine  The WikiEngine.
     *  @param watch   A Watchable object.
     */
    public WatchDog(WikiEngine engine, Watchable watch)
    {
        m_engine = engine;
        m_watchable = watch;

        m_thread = new WatchDogThread( engine );
        
        m_thread.start();
    }

    /**
     *  Creates a new WatchDog for a Thread.  The Thread is wrapped
     *  in a Watchable wrapper for this purpose.
     *  
     *  @param engine The WikiEngine
     *  @param thread A Thread for watching.
     */
    public WatchDog(WikiEngine engine, Thread thread)
    {
        this( engine, new ThreadWrapper(thread) );
    }

    /**
     *  Hopefully finalizes this properly.  This is rather untested
     *  for now...
     */
    protected void release()
    {
        log.debug("Finalizing watch on "+m_watchable.getName());
        if( m_thread != null )
        {
            m_thread.shutdown();
            m_thread = null;
        }
        
        synchronized( c_kennel )
        {
            for( Iterator i = c_kennel.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry e = (Map.Entry) i.next();
            
                if( e.getValue() == this )
                {
                    c_kennel.remove( e.getKey() );
                    break;
                }
            }
        }
    }

    /**
     *  Can be used to enable the WatchDog.  Will cause a new
     *  Thread to be created, if none was existing previously.
     *
     */
    public void enable()
    {
        if( !m_enabled )
        {
            m_enabled = true;
            m_thread = new WatchDogThread( m_engine );
        }
    }
    
    /**
     *  Is used to disable a WatchDog.  The watchdog thread is
     *  shut down and resources released.
     *
     */
    public void disable()
    {
        if( m_enabled )
        {
            m_enabled = false;
            m_thread.shutdown();
            m_thread = null;
        }
    }
    
    /**
     *  Enters a watched state with no expectation of the expected completion time.
     *  In practice this method is used when you have no idea, but would like to figure
     *  out, e.g. via debugging, where exactly your Watchable is.
     *  
     *  @param state A free-form string description of your state.
     */
    public void enterState( String state )
    {
        enterState( state, Integer.MAX_VALUE );
    }
    
    /**
     *  Enters a watched state which has an expected completion time.  This is the
     *  main method for using the WatchDog.  For example:
     *  
     *  <code>
     *     WatchDog w = m_engine.getCurrentWatchDog();
     *     w.enterState("Processing Foobar", 60);
     *     foobar();
     *     w.exitState();
     *  </code>
     *  
     *  If the call to foobar() takes more than 60 seconds, you will receive an
     *  ERROR in the log stream.
     *  
     *  @param state A free-form string description of the state
     *  @param expectedCompletionTime The timeout in seconds.
     */
    public void enterState( String state, int expectedCompletionTime )
    {
        if( log.isDebugEnabled() )
            log.debug( m_watchable.getName()+": Entering state "+state+", expected completion in "+expectedCompletionTime+" s");
                
        synchronized( m_stateStack )
        {
            State st = new State( state, expectedCompletionTime );
        
            m_stateStack.push( st );
        }
    }
    
    /**
     *  Exits a state entered with enterState().  This method does not check
     *  that the state is correct, it'll just pop out whatever is on the top
     *  of the state stack.
     *
     */
    public void exitState()
    {
        exitState(null);
    }
    
    /**
     *  Exits a particular state entered with enterState().  The state is 
     *  checked against the current state, and if they do not match, an error
     *  is flagged.
     *  
     *  @param state The state you wish to exit.
     */
    public void exitState( String state )
    {
        try
        {
            synchronized( m_stateStack )
            {
                State st = (State)m_stateStack.peek();
            
                if( state == null || st.getState().equals(state) )
                {
                    m_stateStack.pop();

                    if( log.isDebugEnabled() )
                        log.debug(m_watchable.getName()+": Exiting state "+st.getState());
                }
                else
                {
                    // FIXME: should actually go and fix things for that
                    log.error("exitState() called before enterState()");
                }
            }
        }
        catch( EmptyStackException e )
        {
            log.error("Stack is empty!", e);
        }
        
    }
    
    /**
     *  Strictly for debugging/informative purposes.
     */
    public String toString()
    {
        synchronized( m_stateStack )
        {
            String state = "Idle";
            
            try
            {
                State st = (State) m_stateStack.peek();
                state = st.getState();
            }
            catch( EmptyStackException e ) {}
            return "WatchDog state="+state;
        }
    }
    
    /**
     *  This is the chief watchdog thread.
     *  
     *  @author jalkanen
     *
     */
    private class WatchDogThread extends WikiBackgroundThread
    {
        public WatchDogThread(WikiEngine engine)
        {
            super(engine, 60);
            
            setName("WatchDog for '"+m_watchable.getName()+"'");
        }

        public void startupTask()
        {
            log.debug("Started watching '"+m_watchable.getName()+"'");
        }
        
        public void shutdownTask()
        {
            log.debug("Stopped watching '"+m_watchable.getName()+"'");
        }
        
        /**
         *  Checks if the watchable is alive, and if it is, checks if
         *  the stack is finished.
         *  
         *  If the watchable has been deleted in the mean time, will
         *  simply shut down itself.
         */
        public void backgroundTask() throws Exception
        {
            if( m_watchable != null && m_watchable.isAlive() )
            {
                synchronized( m_stateStack )
                {
                    try
                    {
                        WatchDog.State st = (WatchDog.State)m_stateStack.peek();
         
                        long now = System.currentTimeMillis();
            
                        if( now > st.getExpiryTime() )
                        {
                            log.error("Watchable '"+m_watchable.getName()+"' exceeded timeout in state "+st.getState());
                            m_watchable.timeoutExceeded( st.getState() );
                        }
                    }
                    catch( EmptyStackException e )
                    {
                        // FIXME: Do something?
                    }
                }
            }
            else
            {
                shutdown();
                release();
            }
        }
    }

    /**
     *  A class which just stores the state in our State stack.
     *  
     *  @author Janne Jalkanen
     */
    private static class State
    {
        protected String m_state;
        protected long   m_enterTime;
        protected long   m_expiryTime;
        
        protected State( String state, int expiry )
        {
            m_state      = state;
            m_enterTime  = System.currentTimeMillis();
            m_expiryTime = m_enterTime + (expiry * 1000L);
        }
        
        protected String getState()
        {
            return m_state;
        }
        
        protected long getExpiryTime()
        {
            return m_expiryTime;
        }
    }

    /**
     *  This class wraps a Thread so that it can become Watchable.
     *  
     *  @author Janne Jalkanen
     *
     */
    private static class ThreadWrapper implements Watchable
    {
        private Thread m_thread;
        
        public ThreadWrapper( Thread thread )
        {
            m_thread = thread;
        }
        
        public void timeoutExceeded( String state )
        {
            // TODO: Figure out something sane to do here.
        }
        
        public String getName()
        {
            return m_thread.getName();
        }
        
        public boolean isAlive()
        {
            return m_thread.isAlive();
        }
    }
}
