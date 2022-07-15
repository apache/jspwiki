/* 
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
package org.apache.wiki.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.util.comparators.PrincipalComparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  <p>Manages Sessions for different Engines.</p>
 *  <p>The Sessions are stored both in the remote user HttpSession and in the SessionMonitor for the Engine.
 *  This class must be configured as a session listener in the web.xml for the wiki web application.</p>
 */
public class SessionMonitor implements HttpSessionListener {

    private static final Logger LOG = LogManager.getLogger( SessionMonitor.class );

    /** Map with Engines as keys, and SessionMonitors as values. */
    private static final ConcurrentHashMap< Engine, SessionMonitor > c_monitors = new ConcurrentHashMap<>();

    /** Weak hashmap with HttpSessions as keys, and WikiSessions as values. */
    private final Map< String, Session > m_sessions = new WeakHashMap<>();

    private Engine m_engine;

    private final PrincipalComparator m_comparator = new PrincipalComparator();

    /**
     * Returns the instance of the SessionMonitor for this wiki. Only one SessionMonitor exists per Engine.
     *
     * @param engine the wiki engine
     * @return the session monitor
     */
    public static SessionMonitor getInstance( final Engine engine ) {
        if( engine == null ) {
            throw new IllegalArgumentException( "Engine cannot be null." );
        }
        SessionMonitor monitor = c_monitors.get( engine );
        if( monitor == null ) {
            monitor = new SessionMonitor( engine );
            c_monitors.put( engine, monitor );
        }

        return monitor;
    }

    /** Construct the SessionListener */
    public SessionMonitor() {
    }

    private SessionMonitor( final Engine engine ) {
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
    private Session findSession( final HttpSession session ) {
        final String sid = ( session == null ) ? "(null)" : session.getId();
        return findSession( sid );
    }

    /**
     *  Just looks for a WikiSession; does not create a new one.
     * This method may return <code>null</code>, <em>and
     * callers should check for this value</em>.
     *
     *  @param sessionId the user's HTTP session id
     *  @return the WikiSession, if found
     */
    private Session findSession( final String sessionId ) {
        Session wikiSession = null;
        final String sid = ( sessionId == null ) ? "(null)" : sessionId;
        final Session storedSession = m_sessions.get( sid );

        // If the weak reference returns a wiki session, return it
        if( storedSession != null ) {
            LOG.debug( "Looking up WikiSession for session ID={}... found it", sid );
            wikiSession = storedSession;
        }

        return wikiSession;
    }

    /**
     * <p>Looks up the wiki session associated with a user's Http session and adds it to the session cache. This method will return the
     * "guest session" as constructed by {@link org.apache.wiki.api.spi.SessionSPI#guest(Engine)} if the HttpSession is not currently
     * associated with a WikiSession. This method is guaranteed to return a non-<code>null</code> WikiSession.</p>
     * <p>Internally, the session is stored in a HashMap; keys are the HttpSession objects, while the values are
     * {@link java.lang.ref.WeakReference}-wrapped WikiSessions.</p>
     *
     * @param session the HTTP session
     * @return the wiki session
     */
    public final Session find( final HttpSession session ) {
        final Session wikiSession = findSession( session );
        final String sid = ( session == null ) ? "(null)" : session.getId();
        if( wikiSession == null ) {
            return createGuestSessionFor( sid );
        }

        return wikiSession;
    }

    /**
     * <p>Looks up the wiki session associated with a user's Http session and adds it to the session cache. This method will return the
     * "guest session" as constructed by {@link org.apache.wiki.api.spi.SessionSPI#guest(Engine)} if the HttpSession is not currently
     * associated with a WikiSession. This method is guaranteed to return a non-<code>null</code> WikiSession.</p>
     * <p>Internally, the session is stored in a HashMap; keys are the HttpSession objects, while the values are
     * {@link java.lang.ref.WeakReference}-wrapped WikiSessions.</p>
     *
     * @param sessionId the HTTP session
     * @return the wiki session
     */
    public final Session find( final String sessionId ) {
        final Session wikiSession = findSession( sessionId );
        if( wikiSession == null ) {
            return createGuestSessionFor( sessionId );
        }

        return wikiSession;
    }

    /**
     * Creates a new session and stashes it
     *
     * @param sessionId id looked for before creating the guest session
     * @return a new guest session
     */
    private Session createGuestSessionFor( final String sessionId ) {
        LOG.debug( "Session for session ID={}... not found. Creating guestSession()", sessionId );
        final Session wikiSession = Wiki.session().guest( m_engine );
        synchronized( m_sessions ) {
            m_sessions.put( sessionId, wikiSession );
        }
        return wikiSession;
    }

    /**
     * Removes the wiki session associated with the user's HttpRequest from the session cache.
     *
     * @param request the user's HTTP request
     */
    public final void remove( final HttpServletRequest request ) {
        if( request == null ) {
            throw new IllegalArgumentException( "Request cannot be null." );
        }
        remove( request.getSession() );
    }

    /**
     * Removes the wiki session associated with the user's HttpSession from the session cache.
     *
     * @param session the user's HTTP session
     */
    public final void remove( final HttpSession session ) {
        if( session == null ) {
            throw new IllegalArgumentException( "Session cannot be null." );
        }
        synchronized( m_sessions ) {
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
     * <p>Returns the current wiki users as a sorted array of Principal objects. The principals are those returned by
     * each WikiSession's {@link Session#getUserPrincipal()}'s method.</p>
     * <p>To obtain the list of current WikiSessions, we iterate through our session Map and obtain the list of values,
     * which are WikiSessions wrapped in {@link java.lang.ref.WeakReference} objects. Those <code>WeakReference</code>s
     * whose <code>get()</code> method returns non-<code>null</code> values are valid sessions.</p>
     *
     * @return the array of user principals
     */
    public final Principal[] userPrincipals() {
        final Collection<Principal> principals = new ArrayList<>();
        synchronized ( m_sessions ) {
            for ( final Session session : m_sessions.values()) {
                principals.add( session.getUserPrincipal() );
            }
        }
        final Principal[] p = principals.toArray( new Principal[0] );
        Arrays.sort( p, m_comparator );
        return p;
    }

    /**
     * Registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     * @since 2.4.75
     */
    public final synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     * Un-registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     * @since 2.4.75
     */
    public final synchronized void removeWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     * Fires a WikiSecurityEvent to all registered listeners.
     *
     * @param type  the event type
     * @param principal the user principal associated with this session
     * @param session the wiki session
     * @since 2.4.75
     */
    protected final void fireEvent( final int type, final Principal principal, final Session session ) {
        if( WikiEventManager.isListening( this ) ) {
            WikiEventManager.fireEvent( this, new WikiSecurityEvent( this, type, principal, session ) );
        }
    }

    /**
     * Fires when the web container creates a new HTTP session.
     * 
     * @param se the HTTP session event
     */
    @Override
    public void sessionCreated( final HttpSessionEvent se ) {
        final HttpSession session = se.getSession();
        LOG.debug( "Created session: " + session.getId() + "." );
    }

    /**
     * Removes the user's WikiSession from the internal session cache when the web
     * container destroys an HTTP session.
     * @param se the HTTP session event
     */
    @Override
    public void sessionDestroyed( final HttpSessionEvent se ) {
        final HttpSession session = se.getSession();
        for( final SessionMonitor monitor : c_monitors.values() ) {
            final Session storedSession = monitor.findSession( session );
            monitor.remove( session );
            LOG.debug( "Removed session " + session.getId() + "." );
            if( storedSession != null ) {
                fireEvent( WikiSecurityEvent.SESSION_EXPIRED, storedSession.getLoginPrincipal(), storedSession );
            }
        }
    }

}
