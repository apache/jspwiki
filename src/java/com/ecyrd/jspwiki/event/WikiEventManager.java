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

package com.ecyrd.jspwiki.event;

import java.lang.ref.WeakReference;
import java.util.*;

import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 *  A singleton class that manages the addition and removal of WikiEvent
 *  listeners to a event source, as well as the firing of events to those
 *  listeners. An "event source" is the object delegating its event
 *  handling to an inner delegating class supplied by this manager. The
 *  class being serviced is considered a "client" of the delegate. The
 *  WikiEventManager operates across any number of simultaneously-existing
 *  WikiEngines since it manages all delegation on a per-object basis.
 *  Anything that might fire a WikiEvent (or any of its subclasses) can be
 *  a client.
 *  </p>
 *
 *  <h3>Using a Delegate for Event Listener Management</h3>
 *  <p>
 *  Basically, rather than have all manner of client classes maintain their
 *  own listener lists, add and remove listener methods, any class wanting
 *  to attach listeners can simply request a delegate object to provide that
 *  service. The delegate handles the listener list, the add and remove
 *  listener methods. Firing events is then a matter of calling the
 *  WikiEventManager's {@link #fireEvent(Object,WikiEvent)} method, where
 *  the Object is the client. Prior to instantiating the event object, the
 *  client can call {@link #isListening(Object)} to see there are any
 *  listeners attached to its delegate.
 *  </p>
 *
 *  <h3>Adding Listeners</h3>
 *  <p>
 *  Adding a WikiEventListener to an object is very simple:
 *  </p>
 *  <pre>
 *      WikiEventManager.addWikiEventListener(object,listener);
 *  </pre>
 *
 *  <h3>Removing Listeners</h3>
 *  <p>
 *  Removing a WikiEventListener from an object is very simple:
 *  </p>
 *  <pre>
 *      WikiEventManager.removeWikiEventListener(object,listener);
 *  </pre>
 *  If you only have a reference to the listener, the following method
 *  will remove it from any clients managed by the WikiEventManager:
 *  <pre>
 *      WikiEventManager.removeWikiEventListener(listener);
 *  </pre>
 *
 *  <h3>Backward Compatibility: Replacing Existing <tt>fireEvent()</tt> Methods</h3>
 *  <p>
 *  Using one manager for all events processing permits consolidation of all event
 *  listeners and their associated methods in one place rather than having them
 *  attached to specific subcomponents of an application, and avoids a great deal
 *  of event-related cut-and-paste code. Convenience methods that call the
 *  WikiEventManager for event delegation can be written to maintain existing APIs.
 *  </p>
 *  <p>
 *  For example, an existing <tt>fireEvent()</tt> method might look something like
 *  this:
 *  </p>
 *  <pre>
 *    protected final void fireEvent( WikiEvent event )
 *    {
 *        for ( Iterator it = m_listeners.iterator(); it.hasNext(); )
 *        {
 *            WikiEventListener listener = (WikiEventListener)it.next();
 *            listener.actionPerformed(event);
 *        }
 *    }
 *  </pre>
 *  <p>
 *  One disadvantage is that the above method is supplied with event objects,
 *  which are created even when no listener exists for them. In a busy wiki
 *  with many users unused/unnecessary event creation could be considerable.
 *  Another advantage is that in addition to the iterator, there must be code
 *  to support the addition and remove of listeners. The above could be
 *  replaced with the below code (and with no necessary local support for
 *  adding and removing listeners):
 *  </p>
 *  <pre>
 *    protected final void fireEvent( int type )
 *    {
 *        if ( WikiEventManager.isListening(this) )
 *        {
 *            WikiEventManager.fireEvent(this,new WikiEngineEvent(this,type));
 *        }
 *    }
 *  </pre>
 *  <p>
 *  This only needs to be customized to supply the specific parameters for
 *  whatever WikiEvent you want to create.
 *  </p>
 *
 *  <h3 id="preloading">Preloading Listeners</h3>
 *  <p>
 *  This may be used to create listeners for objects that don't yet exist,
 *  particularly designed for embedded applications that need to be able
 *  to listen for the instantiation of an Object, by maintaining a cache
 *  of client-less WikiEvent sources that set their client upon being
 *  popped from the cache. Each time any of the methods expecting a client
 *  parameter is called with a null parameter it will preload an internal
 *  cache with a client-less delegate object that will be popped and
 *  returned in preference to creating a new object. This can have unwanted
 *  side effects if there are multiple clients populating the cache with
 *  listeners. The only check is for a Class match, so be aware if others
 *  might be populating the client-less cache with listeners.
 *  </p>
 *  <h3>Listener lifecycle</h3>
 *  <p>
 *  Note that in most cases it is not necessary to remove a listener.
 *  As of 2.4.97, the listeners are stored as WeakReferences, and will be
 *  automatically cleaned at the next garbage collection, if you no longer
 *  hold a reference to them.  Of course, until the garbage is collected,
 *  your object might still be getting events, so if you wish to avoid that,
 *  please remove it explicitly as described above.
 *  </p>
 * @author Murray Altheim
 * @since 2.4.20
 */
public final class WikiEventManager
{
    private static final Logger log = LoggerFactory.getLogger(WikiEventManager.class);

    /* If true, permits a WikiEventMonitor to be set. */
    private static boolean c_permitMonitor = false;

    /* Optional listener to be used as all-event monitor. */
    private static WikiEventListener c_monitor = null;

    /* The Map of client object to WikiEventDelegate. */
    private final Map<Object, WikiEventDelegate> m_delegates = new HashMap<Object, WikiEventDelegate>();

    /* The Vector containing any preloaded WikiEventDelegates. */
    private final Vector<WikiEventDelegate> m_preloadCache = new Vector<WikiEventDelegate>();

    /* Singleton instance of the WikiEventManager. */
    private static WikiEventManager c_instance = null;

    // ............

    /**
     *  Constructor for a WikiEventManager.
     */
    private WikiEventManager()
    {
        c_instance = this;
        log.debug("instantiated WikiEventManager");
    }

    /**
     *  As this is a singleton class, this returns the single
     *  instance of this class provided with the property file
     *  filename and bit-wise application settings.
     *
     *  @return A shared instance of the WikiEventManager
     */
    public static WikiEventManager getInstance()
    {
        if( c_instance == null )
        {
            synchronized( WikiEventManager.class )
            {
                WikiEventManager mgr = new WikiEventManager();
                // start up any post-instantiation services here
                return mgr;
            }
        }
        return c_instance;
    }


    // public/API methods ......................................................


    /**
     *  Registers a WikiEventListener with a WikiEventDelegate for
     *  the provided client object.
     *
     *  <h3>Monitor Listener</h3>
     *
     *  If <tt>client</tt> is a reference to the WikiEventManager class
     *  itself and the compile-time flag {@link #c_permitMonitor} is true,
     *  this attaches the listener as an all-event monitor, overwriting
     *  any previous listener (hence returning true).
     *  <p>
     *  You can remove any existing monitor by either calling this method
     *  with <tt>client</tt> as a reference to this class and the
     *  <tt>listener</tt> parameter as null, or
     *  {@link #removeWikiEventListener(Object,WikiEventListener)} with a
     *  <tt>client</tt> as a reference to this class. The <tt>listener</tt>
     *  parameter in this case is ignored.
     *
     * @param client   the client of the event source
     * @param listener the event listener
     * @return true if the listener was added (i.e., it was not already in the list and was added)
     */
    public static final boolean addWikiEventListener(
            Object client, WikiEventListener listener )
    {
        if ( client == WikiEventManager.class )
        {
            if ( c_permitMonitor ) c_monitor = listener;
            return c_permitMonitor;
        }
        WikiEventDelegate delegate = getInstance().getDelegateFor(client);
        return delegate.addWikiEventListener(listener);
    }


    /**
     *  Un-registers a WikiEventListener with the WikiEventDelegate for
     *  the provided client object.
     *
     * @param client   the client of the event source
     * @param listener the event listener
     * @return true if the listener was found and removed.
     */
    public static final boolean removeWikiEventListener(
            Object client, WikiEventListener listener )
    {
        if ( client == WikiEventManager.class )
        {
            c_monitor = null;
            return true;
        }
        WikiEventDelegate delegate = getInstance().getDelegateFor(client);
        return delegate.removeWikiEventListener(listener);
    }


    /**
     *  Return the Set containing the WikiEventListeners attached to the
     *  delegate for the supplied client. If there are no attached listeners,
     *  returns an empty Iterator rather than null.  Note that this will
     *  create a delegate for the client if it did not exist prior to the call.
     *
     *  <h3>NOTE</h3>
     *  <p>
     *  This method returns a Set rather than an Iterator because of the high
     *  likelihood of the Set being modified while an Iterator might be active.
     *  This returns an unmodifiable reference to the actual Set containing
     *  the delegate's listeners. Any attempt to modify the Set will throw an
     *  {@link java.lang.UnsupportedOperationException}. This method is not
     *  synchronized and it should be understood that the composition of the
     *  backing Set may change at any time.
     *  </p>
     *
     * @param client   the client of the event source
     * @return an unmodifiable Set containing the WikiEventListeners attached to the client
     * @throws java.lang.UnsupportedOperationException  if any attempt is made to modify the Set
     */
    public static final Set getWikiEventListeners( Object client )
        throws UnsupportedOperationException
    {
        WikiEventDelegate delegate = getInstance().getDelegateFor(client);
        return delegate.getWikiEventListeners();
    }


    /**
     *  Un-registers a WikiEventListener from any WikiEventDelegate client managed by this
     *  WikiEventManager. Since this is a one-to-one relation, the first match will be
     *  returned upon removal; a true return value indicates the WikiEventListener was
     *  found and removed.
     *
     * @param listener the event listener
     * @return true if the listener was found and removed.
     */
    public static final boolean removeWikiEventListener( WikiEventListener listener )
    {
        // get the Map.entry object for the entire Map, then check match on entry (listener)
        WikiEventManager mgr = getInstance();
        Map sources = mgr.getDelegates();
        synchronized( sources )
        {
            // get an iterator over the Map.Enty objects in the map
            Iterator it = sources.entrySet().iterator();
            while( it.hasNext() )
            {
                Map.Entry entry = (Map.Entry)it.next();
                // the entry value is the delegate
                WikiEventDelegate delegate = (WikiEventDelegate)entry.getValue();

                // now see if we can remove the listener from the delegate
                // (delegate may be null because this is a weak reference)
                if( delegate != null && delegate.removeWikiEventListener(listener) )
                {
                    return true; // was removed
                }
            }
        }
        return false;
    }


    /**
     *  Returns true if there are one or more listeners registered with
     *  the provided client Object (undelegated event source). This locates
     *  any delegate and checks to see if it has any listeners attached.
     *
     *  @param client the client Object
     *  @return True, if there is a listener for this client object.
     */
    public static boolean isListening( Object client )
    {
        WikiEventDelegate source = getInstance().getDelegateFor(client);
        return source != null ? source.isListening() : false ;
    }


    /**
     *  Notify all listeners of the WikiEventDelegate having a registered
     *  interest in change events of the supplied WikiEvent.
     *
     * @param client the client initiating the event.
     * @param event  the WikiEvent to fire.
     */
    public static void fireEvent( Object client, WikiEvent event )
    {
        WikiEventDelegate source = getInstance().getDelegateFor(client);
        if ( source != null ) source.fireEvent(event);
        if ( c_monitor != null ) c_monitor.actionPerformed(event);
    }


    // private and utility methods .............................................


    /**
     *  Return the client-to-delegate Map.
     */
    private Map getDelegates()
    {
        return m_delegates;
    }


    /**
     *  Returns a WikiEventDelegate for the provided client Object.
     *  If the parameter is a class reference, will generate and return a
     *  client-less WikiEventDelegate. If the parameter is not a Class and
     *  the delegate cache contains any objects matching the Class of any
     *  delegates in the cache, the first Class-matching delegate will be
     *  used in preference to creating a new delegate.
     *  If a null parameter is supplied, this will create a client-less
     *  delegate that will attach to the first incoming client (i.e.,
     *  there will be no Class-matching requirement).
     *
     * @param client   the client Object, or alternately a Class reference
     * @return the WikiEventDelegate.
     */
    private WikiEventDelegate getDelegateFor( Object client )
    {
        synchronized( m_delegates )
        {
            if( client == null || client instanceof Class ) // then preload the cache
            {
                WikiEventDelegate delegate = new WikiEventDelegate(client);
                m_preloadCache.add(delegate);
                m_delegates.put( client, delegate );
                return delegate;
            }
            else if( !m_preloadCache.isEmpty() )
            {
                // then see if any of the cached delegates match the class of the incoming client
                for( int i = m_preloadCache.size()-1 ; i >= 0 ; i-- ) // start with most-recently added
                {
                    WikiEventDelegate delegate = m_preloadCache.elementAt(i);
                    if( delegate.getClientClass() == null
                        || delegate.getClientClass().equals(client.getClass()) )
                    {
                        // we have a hit, so use it, but only on a client we haven't seen before
                        if( !m_delegates.keySet().contains(client) )
                        {
                            m_preloadCache.remove(delegate);
                            m_delegates.put( client, delegate );
                            return delegate;
                        }
                    }
                }
            }
            // otherwise treat normally...
            WikiEventDelegate delegate = m_delegates.get( client );
            if( delegate == null )
            {
                delegate = new WikiEventDelegate( client );
                m_delegates.put( client, delegate );
            }
            return delegate;
        }
    }


    // .........................................................................


    /**
     *  Inner delegating class that manages event listener addition and
     *  removal. Classes that generate events can obtain an instance of
     *  this class from the WikiEventManager and delegate responsibility
     *  to it. Interaction with this delegating class is done via the
     *  methods of the {@link WikiEventDelegate} API.
     *
     * @author Murray Altheim
     * @since 2.4.20
     */
    private static final class WikiEventDelegate
    {
        /* A list of event listeners for this instance. */

        private ArrayList<WeakReference<WikiEventListener>> m_listenerList = new ArrayList<WeakReference<WikiEventListener>>();

        private Class  m_class  = null;

        /**
         *  Constructor for an WikiEventDelegateImpl, provided
         *  with the client Object it will service, or the Class
         *  of client, the latter when used to preload a future
         *  incoming delegate.
         */
        protected WikiEventDelegate( Object client )
        {
            if( client instanceof Class )
            {
                m_class = (Class)client;
            }
        }

        /**
         *  Returns the class of the client-less delegate, null if
         *  this delegate is attached to a client Object.
         */
        protected Class getClientClass()
        {
            return m_class;
        }


        /**
         *  Return an unmodifiable Set containing the WikiEventListeners of
         *  this WikiEventDelegateImpl. If there are no attached listeners,
         *  returns an empty Set rather than null.
         *
         * @return an unmodifiable Set containing this delegate's WikiEventListeners
         * @throws java.lang.UnsupportedOperationException  if any attempt is made to modify the Set
         */
        public Set getWikiEventListeners()
        {
            synchronized( m_listenerList )
            {
                TreeSet<WikiEventListener> set = new TreeSet<WikiEventListener>( new WikiEventListenerComparator() );

                for( Iterator i = m_listenerList.iterator(); i.hasNext(); )
                {
                    WikiEventListener l = (WikiEventListener) ((WeakReference)i.next()).get();

                    if( l != null )
                    {
                        set.add( l );
                    }
                }

                return Collections.unmodifiableSet(set);
            }
        }


        /**
         *  Adds <tt>listener</tt> as a listener for events fired by the WikiEventDelegate.
         *
         * @param listener   the WikiEventListener to be added
         * @return true if the listener was added (i.e., it was not already in the list and was added)
         */
        public boolean addWikiEventListener( WikiEventListener listener )
        {
            synchronized( m_listenerList )
            {
                return m_listenerList.add( new WeakReference<WikiEventListener>(listener) );
            }
        }


        /**
         *  Removes <tt>listener</tt> from the WikiEventDelegate.
         *
         * @param listener   the WikiEventListener to be removed
         * @return true if the listener was removed (i.e., it was actually in the list and was removed)
         */
        public boolean removeWikiEventListener( WikiEventListener listener )
        {
            synchronized( m_listenerList )
            {
                for( Iterator i = m_listenerList.iterator(); i.hasNext(); )
                {
                    WikiEventListener l = (WikiEventListener) ((WeakReference)i.next()).get();

                    if( l == listener )
                    {
                        i.remove();
                        return true;
                    }
                }
            }

            return false;
        }


        /**
         *  Returns true if there are one or more listeners registered
         *  with this instance.
         */
        public boolean isListening()
        {
            synchronized( m_listenerList )
            {
                return !m_listenerList.isEmpty();
            }
        }


        /**
         *  Notify all listeners having a registered interest
         *  in change events of the supplied WikiEvent.
         */
        public void fireEvent( WikiEvent event )
        {
            boolean needsCleanup = false;

            try
            {
                synchronized( m_listenerList )
                {
                    for( int i = 0; i < m_listenerList.size(); i++ )
                    {
                        WikiEventListener listener = (WikiEventListener) ((WeakReference)m_listenerList.get(i)).get();

                        if( listener != null )
                        {
                            listener.actionPerformed( event );
                        }
                        else
                        {
                            needsCleanup  = true;
                        }
                    }

                    //
                    //  Remove all such listeners which have expired
                    //
                    if( needsCleanup )
                    {
                        for( int i = 0; i < m_listenerList.size(); i++ )
                        {
                            WeakReference w = m_listenerList.get(i);

                            if( w.get() == null ) m_listenerList.remove(i--);
                        }
                    }

                }
            }
            catch( ConcurrentModificationException e )
            {
                //
                //  We don't die, we just don't do notifications in that case.
                //
                log.info("Concurrent modification of event list; please report this.",e);
            }

        }

    } // end inner class WikiEventDelegate

    private static class WikiEventListenerComparator implements Comparator<WikiEventListener>
    {
        // TODO: This method is a critical performance bottleneck
        public int compare(WikiEventListener w0, WikiEventListener w1)
        {
            if( w1 == w0 || w0.equals(w1) ) return 0;

            return w1.hashCode() - w0.hashCode();
        }
    }
} // end com.ecyrd.jspwiki.event.WikiEventManager
