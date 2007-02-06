/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.event;

import java.util.*;

import org.apache.log4j.Logger;

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
 *
 * @author Murray Altheim
 * @since 2.4.20
 */
public class WikiEventManager
{
    private static final Logger log = Logger.getLogger(WikiEventManager.class);

    /* The Map of client object to WikiEventDelegate. */
    private final Map m_delegates = new HashMap();

    /* The Vector containing any preloaded WikiEventDelegates. */
    private final Vector m_preloadCache = new Vector();

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
     */
    public static WikiEventManager getInstance()
    {
        if( c_instance == null )
        {
            synchronized( WikiEventManager.class ) // see Larman/Guthrie, Java 2 Perf/Idiom Guide, p.100
            {
                if( c_instance == null )
                {
                    WikiEventManager mgr = new WikiEventManager();
                    // start up any post-instantiation services here
                    return mgr;
                }
            }
        }
        return c_instance;
    }


    // public/API methods ......................................................


    /**
     *  Registers a WikiEventListener with a WikiEventDelegate for
     *  the provided client object.
     *
     * @param client   the client of the event source
     * @param listener the event listener
     * @return true if the listener was added (i.e., it was not already in the list and was added)
     */
    public static final boolean addWikiEventListener(
            Object client, WikiEventListener listener )
    {
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
    {
        WikiEventDelegate delegate = (WikiEventDelegate)getInstance().getDelegateFor(client);
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
     * @param client the client Object
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
        if ( source != null ) ((WikiEventDelegate)source).fireEvent(event);
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
                    WikiEventDelegate delegate = (WikiEventDelegate)m_preloadCache.elementAt(i);
                    if( delegate.getClientClass() == null
                        || delegate.getClientClass().equals(client.getClass()) )
                    {
                        // we have a hit, so use it, but only on a client we haven't seen before
                        if( !m_delegates.keySet().contains(client) ) 
                        {
                            m_preloadCache.remove(delegate);
                            delegate.setClient( client );
                            m_delegates.put( client, delegate );
                            return delegate;
                        }
                    }
                }
            }
            // otherwise treat normally...
            WikiEventDelegate delegate = (WikiEventDelegate)m_delegates.get( client );
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
        //private final EventListenerList m_listenerList = new EventListenerList();

        private ArrayList m_listenerList = new ArrayList();
        
        private Class  m_class  = null;
        private Object m_client = null;

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
            else 
            {
                m_client = client;
            }
        }


        /**
         *  Set this WikiEventDelegateImpl's client Object to <tt>client</tt>.
         */
        protected void setClient( Object client )
        {
            m_class  = null;
            m_client = client;
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
                TreeSet set = new TreeSet( new WikiEventListenerComparator() );
                set.addAll( m_listenerList );
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
                return m_listenerList.add( listener );
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
                return m_listenerList.remove(listener);
            }
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
            try
            {
                synchronized( m_listenerList )
                {
                    for( int i = 0; i < m_listenerList.size(); i++ )
                    {
                        WikiEventListener listener = (WikiEventListener) m_listenerList.get(i);
                
                        listener.actionPerformed( event );
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

    private static class WikiEventListenerComparator implements Comparator
    {
        // TODO: This method is a critical performance bottleneck
        public int compare(Object arg0, Object arg1)
        {
            if( arg0 instanceof WikiEventListener && arg1 instanceof WikiEventListener )
            {
                WikiEventListener w0 = (WikiEventListener) arg0;
                WikiEventListener w1 = (WikiEventListener) arg1;
                
                if( w1 == w0 || w0.equals(w1) ) return 0;
                
                return w1.hashCode() - w0.hashCode();
            }
            
            throw new ClassCastException( arg1.getClass().getName() + " != " + arg0.getClass().getName() );
        }
        
    }
} // end com.ecyrd.jspwiki.event.WikiEventManager
