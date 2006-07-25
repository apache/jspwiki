package com.ecyrd.jspwiki.auth;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;

/**
 *  <p>Monitor thread that runs every 60 seconds
 *  and removes all sessions that have expired.  
 *  Only one instance exists per WikiEngine. It is a not a 
 *  daemon thread; rather, it listens for wiki engine shutdown 
 *  and persists in the JVM until it detects it.</p>
 *  <p>The <em>raison d'être</em> for this class is to work
 *  around the fact that we need to keep track of HttpSessions
 *  and know which ones have expired, and which haven't. We can't
 *  t query the web container directly to see which have expired. As
 *  a workaround, therefore, we keep a cache of the current <code>HttpSession</code>
 *  objects for this webapp, and periodically walk through the 
 *  cache to determine which sessions are still valid. Fortunately
 *  for us, the HttpSession interface specifies two extremely
 *  handy methods that make the job relatively easy:
 *  {@link HttpSession#getLastAccessedTime()} and 
 *  {@link HttpSession#getMaxInactiveInterval()}.</p>
 *  <p>SessionMonitors are lazily initialized; a a monitor will not start
 *  until the first WikiSession is requested.</p>
 */
public final class SessionMonitor extends WikiBackgroundThread
{
    private static final Logger log = Logger.getLogger( SessionMonitor.class );
    
    /** Map with WikiEngines as keys, and SessionMonitors as values. */
    private static final Map c_monitors = new HashMap();
    
    /** Weak hashmap with HttpSessions as keys, and WikiSessions as values. */
    private final Map m_sessions = new WeakHashMap();

    private final PrincipalComparator m_comparator = new PrincipalComparator();
    
    /**
     * Returns the instance of the SessionMonitor for this wiki.
     * Only one SessionMonitor exists per WikiEngine.
     * @return the session monitor
     */
    public final static SessionMonitor getInstance( WikiEngine engine ) {
        if ( engine == null ) 
        {
            throw new IllegalArgumentException( "Engine cannot be null." );
        }
        SessionMonitor monitor = (SessionMonitor)c_monitors.get( engine );
        if ( monitor == null )
        {
            monitor = new SessionMonitor( engine );
            synchronized ( c_monitors )
            {
                c_monitors.put( engine, monitor );
            }
        }
        return monitor;
    }
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private SessionMonitor( WikiEngine engine )
    {
        super( engine, 60 );
        setName("JSPWiki Session Monitor");
    }
    
    /**
     * <p>Runs the session monitor's cleanup method, which iterates
     * through the HttpSession keys in a private Map 
     * and checks for expired sessions. Each HttpSession is
     * evaluated for removal as follows:</p>
     * <ul>
     *   <li><em>Garbage collection.</em> If the session had 
     *     previously been garbage-collected, 
     *     it is removed from the map</li>
     *   <li><em>Expiration.</em> If the value returned by the 
     *     session's {@link HttpSession#getLastAccessedTime()} 
     *     method plus the value of {@link HttpSession#getMaxInactiveInterval()}
     *     is less than the current time, the session is treated
     *     as expired, and removed from the map</li>
     *   <li><em>Invalidation.</em> Certain web containers will 
     *     throw an exception like <code>IllegalStateException</code> 
     *     when the last-accessed or max-inactive methods are called
     *     on sessions marked invalid by the container. If this is 
     *     true when we call these methods, we similarly regard the
     *     session as invalid and remove it from the map</li>
     * </ul>
     * <p>All of these conditions mean the same thing: the session
     * isn't relevant to the container anymore, so we should stop
     * caching it.</p>
     * @see java.lang.Thread#run()
     */
    public final void backgroundTask()
    {
        synchronized( m_sessions )
        {
            Set entries = m_sessions.keySet();
            Set removeQueue = new HashSet();

            for( Iterator i = entries.iterator(); i.hasNext(); )
            {
                boolean expired = false;
                boolean invalid = false;
                boolean removed = false;
                String reason = null;
                HttpSession s = (HttpSession) i.next();
                
                if ( s == null )
                {
                    removed = true;
                    reason = "Garbage collector removed HttpSession";
                }
                else 
                {
                    try
                    {
                        long now = System.currentTimeMillis();
                        long lastAccessed = s.getLastAccessedTime();
                        long expiryTime = s.getMaxInactiveInterval() * 1000L;
                        if ( now > ( lastAccessed + expiryTime ) )
                        {
                            expired = true;
                            reason = "HttpSession has expired";
                        }
                    }
                    
                    // We will get an exception if the container
                    // has invalidated the session already
                    catch ( Exception e )
                    {
                        invalid = true;
                        reason = "Container marked HttpSession as invalid";
                    }
                }
                
                if ( expired || invalid || removed )
                {
                    log.info( "Removing expired wiki session: " + reason );
                    removeQueue.add( s );
                }
            }
            
            // Remove everything we marked for removal
            for ( Iterator it = removeQueue.iterator(); it.hasNext(); )
            {
                m_sessions.remove( it.next() );
            }
        }
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
        // Look for a WikiSession associated with the user's Http Session
        // and create one if it isn't there yet.
        WikiSession wikiSession;
        String sid = ( session == null ) ? "(null)" : session.getId();
        WeakReference storedSession = ((WeakReference)m_sessions.get( session ));

        // If the weak reference returns a wiki session, return it
        if ( storedSession != null && storedSession.get() instanceof WikiSession )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... found it" );
            }
            wikiSession = (WikiSession) storedSession.get();
        }
        
        // Otherwise, create a new guest session and stash it.
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... not found. Creating guestSession()" );
            }
            wikiSession = WikiSession.guestSession( getEngine() );
            synchronized ( m_sessions )
            {
                m_sessions.put( session, new WeakReference( wikiSession ) );
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
            m_sessions.remove( session );
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
            WeakReference ref = (WeakReference)it.next();
            if ( ref != null && ref.get() instanceof WikiSession )
            {
                WikiSession session = (WikiSession)ref.get();
                principals.add( session.getUserPrincipal() );
            }
        }
        Principal[] p = (Principal[])principals.toArray( new Principal[principals.size()] );
        Arrays.sort( p, m_comparator );
        return p;
    }
}