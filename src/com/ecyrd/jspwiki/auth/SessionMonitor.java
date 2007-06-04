/*
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 JSPWiki Development Team

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
package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.*;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventManager;
import com.ecyrd.jspwiki.event.WikiSecurityEvent;
import com.ecyrd.jspwiki.rpc.json.JSONRPCManager;

/**
 *  <p>Manages WikiSession's for different WikiEngine's.</p>
 *  <p>The WikiSession's are stored both in the remote user
 *  HttpSession and in the SessionMonitor for the WikeEngine.
 *  This class must be configured as a session listener in the
 *  web.xml for the wiki web application.
 *  </p>
 */
public class SessionMonitor implements HttpSessionListener
{
    private static Logger log = Logger.getLogger( SessionMonitor.class );

    /** Map with WikiEngines as keys, and SessionMonitors as values. */
    private static Map          c_monitors   = new HashMap();

    /** Weak hashmap with HttpSessions as keys, and WikiSessions as values. */
    private final Map                 m_sessions   = new WeakHashMap();

    private       WikiEngine          m_engine;

    private final PrincipalComparator m_comparator = new PrincipalComparator();

    /**
     * Returns the instance of the SessionMonitor for this wiki.
     * Only one SessionMonitor exists per WikiEngine.
     * @param engine the wiki engine
     * @return the session monitor
     */
    public static final SessionMonitor getInstance( WikiEngine engine )
    {
        if( engine == null )
        {
            throw new IllegalArgumentException( "Engine cannot be null." );
        }
        SessionMonitor monitor;

        synchronized( c_monitors )
        {
            monitor = (SessionMonitor) c_monitors.get(engine);
            if( monitor == null )
            {
                monitor = new SessionMonitor(engine);

                c_monitors.put( engine, monitor );
            }
        }
        return monitor;
    }

    /**
     * Construct the SessionListener
     */
    public SessionMonitor()
    {
    }

    private SessionMonitor( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     *  Just looks for a WikiSession; does not create a new one.
     * This method may return <code>null</code>, <em>and
     * callers should check for this value</em>.
     *
     *  @param session the user's HTTP session
     *  @return the WikiSession, if found
     */
    private WikiSession findSession( HttpSession session )
    {
        WikiSession wikiSession = null;
        String sid = ( session == null ) ? "(null)" : session.getId();
        WikiSession storedSession = (WikiSession)m_sessions.get( sid );

        // If the weak reference returns a wiki session, return it
        if( storedSession != null )
        {
            if( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... found it" );
            }
            wikiSession = storedSession;
        }

        return wikiSession;
    }
    /**
     * <p>Looks up the wiki session associated with a user's Http session
     * and adds it to the session cache. This method will return the
     * "guest session" as constructed by {@link WikiSession#guestSession(WikiEngine)}
     * if the HttpSession is not currently associated with a WikiSession.
     * This method is guaranteed to return a non-<code>null</code> WikiSession.</p>
     * <p>Internally, the session is stored in a HashMap; keys are
     * the HttpSession objects, while the values are
     * {@link java.lang.ref.WeakReference}-wrapped WikiSessions.</p>
     * @param session the HTTP session
     * @return the wiki session
     */
    public final WikiSession find( HttpSession session )
    {
        WikiSession wikiSession = findSession(session);
        String sid = ( session == null ) ? "(null)" : session.getId();

        // Otherwise, create a new guest session and stash it.
        if( wikiSession == null )
        {
            if( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... not found. Creating guestSession()" );
            }
            wikiSession = WikiSession.guestSession( m_engine );
            synchronized( m_sessions )
            {
                m_sessions.put( sid, wikiSession );
            }
        }

        return wikiSession;
    }

    /**
     * Removes the wiki session associated with the user's HttpSession
     * from the session cache.
     * @param session the user's HTTP session
     */
    public final void remove( HttpSession session )
    {
        if ( session == null )
        {
            throw new IllegalArgumentException( "Session cannot be null." );
        }
        synchronized ( m_sessions )
        {
            m_sessions.remove( session.getId() );
        }
    }

    /**
     * Returns the current number of active wiki sessions.
     * @return the number of sessions
     */
    public final int sessions()
    {
        return userPrincipals().length;
    }

    /**
     * <p>Returns the current wiki users as a sorted array of
     * Principal objects. The principals are those returned by
     * each WikiSession's {@link WikiSession#getUserPrincipal()}'s
     * method.</p>
     * <p>To obtain the list of current WikiSessions, we iterate
     * through our session Map and obtain the list of values,
     * which are WikiSessions wrapped in {@link java.lang.ref.WeakReference}
     * objects. Those <code>WeakReference</code>s whose <code>get()</code>
     * method returns non-<code>null</code> values are valid
     * sessions.</p>
     * @return the array of user principals
     */
    public final Principal[] userPrincipals()
    {
        Collection principals = new ArrayList();
        for ( Iterator it = m_sessions.values().iterator(); it.hasNext(); )
        {
            WikiSession session = (WikiSession)it.next();

            principals.add( session.getUserPrincipal() );
        }
        Principal[] p = (Principal[])principals.toArray( new Principal[principals.size()] );
        Arrays.sort( p, m_comparator );
        return p;
    }

    /**
     * Registers a WikiEventListener with this instance.
     * @param listener the event listener
     * @since 2.4.75
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     * @param listener the event listener
     * @since 2.4.75
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     * Fires a WikiSecurityEvent to all registered listeners.
     * @param type  the event type
     * @param principal the user principal associated with this session
     * @param session the wiki session
     * @since 2.4.75
     */
    protected final void fireEvent( int type, Principal principal, WikiSession session )
    {
        if( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiSecurityEvent(this,type,principal,session));
        }
    }

    /**
     * Fires when the web container creates a new HTTP session.
     * 
     * @param se the HTTP session event
     */
    public void sessionCreated( HttpSessionEvent se )
    {
        HttpSession session = se.getSession();
        
        JSONRPCManager.sessionCreated(session);
    }

    /**
     * Removes the user's WikiSession from the internal session cache when the web
     * container destoys an HTTP session.
     * @param se the HTTP session event
     */
    public void sessionDestroyed( HttpSessionEvent se )
    {
        HttpSession session = se.getSession();
        Iterator it = c_monitors.values().iterator();
        while( it.hasNext() )
        {
            SessionMonitor monitor = (SessionMonitor)it.next();

            WikiSession storedSession = monitor.findSession(session);

            monitor.remove(session);

            log.debug("Removed session "+session.getId()+".");

            if( storedSession != null )
            {
                fireEvent( WikiSecurityEvent.SESSION_EXPIRED,
                           storedSession.getLoginPrincipal(),
                           storedSession );
            }
        }
    }
}
