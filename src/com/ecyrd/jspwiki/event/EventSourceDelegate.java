package com.ecyrd.jspwiki.event;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Delegate class that manages event listener registration
 * and removal. Instead of writing (and re-writing) registation code
 * by hand, classes that generate events can instantiate this class 
 * and delegate responsibility to it. This implementation stores
 * listeners in a WeakHashMap to avoid dangling references that might
 * cause memory leaks. Its add/remove methods are <code>synchronized</code>,
 * which makes it thread-safe.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-07-23 20:09:04 $
 * @since 2.4.20
 */
public final class EventSourceDelegate implements WikiEventSource
{

    /** Listeners for security events */
    private final Map        m_listeners = new WeakHashMap();
    
    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.put( listener, new WeakReference( listener ) );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.remove( listener );
    }
    
    /**
     * Fires an event to all registered listeners.
     * @param event the event
     */
    public final void fireEvent( WikiEvent event )
    {
        for( Iterator it = m_listeners.keySet().iterator(); it.hasNext(); )
        {
            WikiEventListener listener = (WikiEventListener) it.next();
            listener.actionPerformed( event );
        }
    }

}
