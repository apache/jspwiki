package com.ecyrd.jspwiki;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.PrincipalWrapper;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;

/**
 * Represents a long-running wiki session, with an associated user Principal,
 * user Subject, and authentication status. This class is initialized with
 * minimal, default-deny values: authentication is set to <code>false</code>,
 * and the user principal is set to <code>null</code>.
 * @author Andrew R. Jaquith
 * @version $Revision: 2.21 $ $Date: 2006-06-17 23:09:03 $
 */
public class WikiSession implements WikiEventListener
{

    public static final String ANONYMOUS             = "anonymous";

    public static final String ASSERTED              = "asserted";

    public static final String AUTHENTICATED         = "authenticated";
    
    private Subject            m_subject             = new Subject();

    protected String           m_cachedCookieIdentity= null;
    
    protected String           m_cachedRemoteUser    = null;

    protected Principal        m_cachedUserPrincipal = null;

    private boolean            m_isNew               = true;
    
    private Map                m_messages            = new HashMap();

    protected static int       ONE                   = 48;

    protected static int       NINE                  = 57;

    protected static int       DOT                   = 46;

    protected static Logger    log                   = Logger.getLogger( WikiSession.class );
    
    protected static final String ALL                = "*";
    
    /**
     *  This is a simple monitor thread that runs roughly every minute
     *  or so (it's not really that important, as long as it runs),
     *  and removes all sessions that have expired.  
     *  Only one instance exists per JVM. It is a not a 
     *  daemon thread; rather, it listens for wiki shutdown 
     *  and persists in the JVM until it detects it.
     */
    private static class SessionMonitor extends WikiBackgroundThread
    {
        private static Logger monitorLog = Logger.getLogger( SessionMonitor.class );
        
        /** Map with WikiEngines as keys, and SessionMonitors as values. */
        private static Map c_monitors = new HashMap();
        
        /** Weak hashmap with HttpSessions as keys, and WikiSessions as values. */
        protected final Map m_sessions;

        /**
         * Returns the instance of the SessionMonitor for this wiki.
         * @return the session monitor
         */
        public static SessionMonitor getInstance( WikiEngine engine ) {
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
            m_sessions = new WeakHashMap();
            setName("JSPWiki Session Monitor");
        }
        
        /**
         * Runs the session monitor's cleanup method, which iterates
         * through the HttpSession keys in protected map
         * {@link WikiSession#m_sessions} and checks for expired sessions.
         * If the session has previously been garbage-collected, it is 
         * removed from the map. If the value returned by the session's
         * {@link HttpSession#getLastAccessedTime()} method
         * plus the value of {@link HttpSession#getMaxInactiveInterval()}
         * is less than the current time, the session is treated
         * as expired, and removed from the map. Certain web containers
         * will throw an exception like <code>IllegalStateException</code>
         * when the last-accessed or max-inactive methods are called on
         * sessions marked invalid by the container. If this is true,
         * we similarly regard the session as invalid and remove it
         * from the protected map.
         * @see java.lang.Thread#run()
         */
        public void backgroundTask()
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
         * Looks up the wiki session associated with a user's Http session.
         * This method will return the "guest session" if not found, and
         * add the session to the session cache. This method is guaranteed
         * to return a non-null WikiSession.
         * @param session the HTTP session
         * @return the wiki session
         */
        public WikiSession find( HttpSession session )
        {
            // Look for a WikiSession associated with the user's Http Session
            // and create one if it isn't there yet.
            WikiSession wikiSession;
            String sid = ( session == null ) ? "(null)" : session.getId();
            WeakReference storedSession = ((WeakReference)m_sessions.get( session ));

            // If the weak reference returns a wiki session, return it
            if ( storedSession != null && storedSession.get() instanceof WikiSession )
            {
                if ( monitorLog.isDebugEnabled() )
                {
                    monitorLog.debug( "Looking up WikiSession for session ID=" + sid + "... found it" );
                }
                wikiSession = (WikiSession) storedSession.get();
            }
            
            // Otherwise, create a new guest session and stash it.
            else
            {
                if ( monitorLog.isDebugEnabled() )
                {
                    monitorLog.debug( "Looking up WikiSession for session ID=" + sid + "... not found. Creating guestSession()" );
                }
                wikiSession = guestSession();
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
         * @param session the user's HttpSession
         */
        public void remove( HttpSession session )
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
        public int sessions()
        {
            return m_sessions.size();
        }
        
        /**
         * Returns the current users as an array of Principal objects.
         * The principals are those returned by each WikiSession's
         * {@link WikiSession#getUserPrincipal()}'s method.
         * @return the array of user principals
         */
        public Principal[] userPrincipals()
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
            return (Principal[])principals.toArray( new Principal[principals.size()] );
        }
    }
    
    /**
     * Returns <code>true</code> if one of this WikiSession's user Principals
     * can be shown to belong to a particular wiki group.
     * @param group the group to test
     * @return the result
     */
    protected boolean isInGroup( Group group )
    {
        Principal[] principals = getPrincipals();
        for ( int i = 0; i < principals.length; i++ )
        {
          if ( group.isMember( principals[i] ) )
          {
              return true;
          }
        }
        return false;
    }
    
    /**
     * Returns <code>true</code> if the wiki session is newly initialized.
     */
    protected boolean isNew()
    {
        return m_isNew;
    }

    /**
     * Sets the status of this wiki session.
     * @param isNew whether this session should be considered "new".
     */
    protected void setNew( boolean isNew )
    {
        m_isNew = isNew;
    }

    /**
     * Private constructor to prevent WikiSession from being instantiated
     * directly.
     */
    private WikiSession()
    {
    }

    /**
     * Returns the authentication status of the user's session. The user is
     * considered authenticated if the Subject contains the Principal
     * Role.AUTHENTICATED;
     * @return Returns <code>true</code> if the user is authenticated
     */
    public boolean isAuthenticated()
    {
        return ( m_subject.getPrincipals().contains( Role.AUTHENTICATED ) );
    }

    /**
     * <p>Determines whether the current session is anonymous. This will be
     * true if any of these conditions are true:</p>
     * <ul>
     *   <li>The session's Principal set contains 
     *       {@link com.ecyrd.jspwiki.auth.authorize.Role#ANONYMOUS}</li>
     *   <li>The session's Principal set contains 
     *       {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}</li>
     *   <li>The Principal returned by {@link #getUserPrincipal()} evaluates
     *       to an IP address.</li> 
     * </ul>
     * <p>The criteria above are listed in the order in which they are 
     * evaluated.</p>
     * @return whether the current user's identity is equivalent to an IP
     * address
     */
    public boolean isAnonymous()
    {
        Set principals = m_subject.getPrincipals();
        return ( principals.contains( Role.ANONYMOUS ) ||
                 principals.contains( WikiPrincipal.GUEST ) ||
                 isIPV4Address( getUserPrincipal().getName() ) );
    }

    /**
     * <p> Returns the Principal used to log in to an authenticated session. The
     * login principal is determined by examining the Subject's Principal set
     * for PrincipalWrappers or WikiPrincipals with type designator
     * <code>LOGIN_NAME</code>; the first one found is the login principal.
     * If one is not found, this method returns the first principal that isn't
     * of type Role or GroupPrincipal. If neither of these conditions hold, this method returns
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}.
     * @return the login Principal. If it is a PrincipalWrapper containing an
     * externally-provided Principal, the object returned is the Principal, not
     * the wrapper around it.
     */
    public Principal getLoginPrincipal()
    {
        Set principals = m_subject.getPrincipals();
        Principal secondChoice = null;

        // Take the first PrincipalWrapper or WikiPrincipal of type LOGIN_NAME
        for( Iterator it = principals.iterator(); it.hasNext(); )
        {
            Principal currentPrincipal = (Principal) it.next();
            if ( !( currentPrincipal instanceof Role || currentPrincipal instanceof GroupPrincipal ) )
            {
                if ( currentPrincipal instanceof WikiPrincipal )
                {
                    WikiPrincipal wp = (WikiPrincipal) currentPrincipal;
                    if ( wp.getType().equals( WikiPrincipal.LOGIN_NAME ) )
                    {
                        return currentPrincipal;
                    }
                }
                else if ( currentPrincipal instanceof PrincipalWrapper )
                {
                    return ( (PrincipalWrapper) currentPrincipal ).getPrincipal();
                }
                if ( secondChoice == null )
                {
                    secondChoice = currentPrincipal;
                }
            }
        }
        return ( secondChoice == null ? WikiPrincipal.GUEST : secondChoice );
    }

    /**
     * <p>Returns the primary user Principal associated with this session. The
     * primary user principal is determined as follows:</p> <ol> <li>If the
     * Subject's Principal set contains WikiPrincipals, the first WikiPrincipal
     * with type designator <code>FULL_NAME</code> or (alternatively)
     * <code>WIKI_NAME</true> is the primary Principal.</li>
     *   <li>For all other cases, the first Principal in the Subject's principal
     *       collection that that isn't of type Role or GroupPrincipal is the primary.</li>
     * </ol> 
     * If no primary user Principal is found, this method returns
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}.
     * @return the primary user Principal
     */
    public Principal getUserPrincipal()
    {
        Set principals = m_subject.getPrincipals();
        Principal secondChoice = null;
        Principal thirdChoice = null;

        // Take the first WikiPrincipal of type FULL_NAME as primary
        // Take the first non-Role as the alternate
        for( Iterator it = principals.iterator(); it.hasNext(); )
        {
            Principal currentPrincipal = (Principal) it.next();
            if ( !( currentPrincipal instanceof Role || currentPrincipal instanceof GroupPrincipal ) )
            {
                if ( currentPrincipal instanceof WikiPrincipal )
                {
                    WikiPrincipal wp = (WikiPrincipal) currentPrincipal;
                    if ( wp.getType().equals( WikiPrincipal.FULL_NAME ) )
                    {
                        return currentPrincipal;
                    }
                    else if ( wp.getType().equals( WikiPrincipal.WIKI_NAME ) )
                    {
                        secondChoice = currentPrincipal;
                    }
                }
                if ( currentPrincipal instanceof PrincipalWrapper )
                {
                    currentPrincipal = ( (PrincipalWrapper) currentPrincipal ).getPrincipal();
                }
                if ( thirdChoice == null )
                {
                    thirdChoice = currentPrincipal;
                }
            }
        }
        if ( secondChoice != null )
        {
          return secondChoice;
        }
        return ( thirdChoice != null ? thirdChoice : WikiPrincipal.GUEST );
    }
    
    /**
     * Adds a message to the generic list of messages associated with the
     * session. These messages retain their order of insertion and remain until
     * the {@link #clearMessages()} method is called.
     * @param message the message to add; if <code>null</code> it is ignored.
     */
    public void addMessage(String message)
    {
        addMessage( ALL, message );
    }
    
    
    /**
     * Adds a message to the specific set of messages associated with the
     * session. These messages retain their order of insertion and remain until
     * the {@link #clearMessages()} method is called.
     * @param topic the topic to associate the message to; 
     * @param message the message to add
     */
    public void addMessage(String topic, String message)
    {
        if ( topic == null )
        {
            throw new IllegalArgumentException( "addMessage: topic cannot be null." );
        }
        if ( message == null )
        {
            message = "";
        }
        Set messages = (Set)m_messages.get( topic );
        if (messages == null ) 
        {
            messages = new LinkedHashSet();
            m_messages.put( topic, messages );
        }
        messages.add( message );
    }
    
    /**
     * Clears all messages associated with this session.
     */
    public void clearMessages()
    {
        m_messages.clear();
    }
    
    /**
     * Clears all messages associated with a session topic.
     * @param topic the topic whose messages should be cleared.
     */
    public void clearMessages( String topic )
    {
        Set messages = (Set)m_messages.get( topic );
        if ( messages != null )
        {
            m_messages.clear();
        }
    }
    
    /**
     * Returns all generic messages associated with this session.
     * The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages()}.
     * @return the current messsages.
     */
    public String[] getMessages() 
    {
        return getMessages( ALL );
    }

    /**
     * Returns all messages associated with a session topic.
     * The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages(String)}.
     * @return the current messsages.
     */
    public String[] getMessages( String topic ) 
    {
        Set messages = (Set)m_messages.get( topic );
        if ( messages == null || messages.size() == 0 )
        {
            return new String[0];
        }
        return (String[])messages.toArray( new String[messages.size()] );
    }
    
    /**
     * Returns all user Principals associated with this session. User principals
     * are those in the Subject's principal collection that aren't of type Role or
     * of type GroupPrincipal.
     * This is a defensive copy.
     * @return Returns the user principal
     */
    public Principal[] getPrincipals()
    {
        ArrayList principals = new ArrayList();
        {
            // Take the first non Role as the main Principal
            for( Iterator it = m_subject.getPrincipals().iterator(); it.hasNext(); )
            {
                Principal currentPrincipal = (Principal) it.next();
                if ( !( currentPrincipal instanceof Role ) &&
                      !( currentPrincipal instanceof GroupPrincipal ) )
                {
                    principals.add( currentPrincipal );
                }
            }
        }
        return (Principal[]) principals.toArray( new Principal[principals.size()] );
    }

    /**
     * Removes the wiki session associated with the user's HTTP request
     * from the cache of wiki sessions, typically as part of a logout 
     * process.
     * @param engine the current wiki engine
     * @param request the users's HTTP request
     */
    public static void removeWikiSession( WikiEngine engine, HttpServletRequest request )
    {
        if ( engine == null || request == null )
        {
            throw new IllegalArgumentException( "Request or engine cannot be null." ); 
        }
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        monitor.remove( request.getSession() );
    }
    
    /**
     * Sets the Subject representing the user.
     * @param subject
     */
    public void setSubject( Subject subject )
    {
        // TODO: this should be a privileged action
        m_subject = subject;
    }

    /**
     * Returns the Subject representing the user.
     * @return the subject
     */
    public Subject getSubject()
    {
        // TODO: this should be a privileged action
        return m_subject;
    }

    /**
     * Listens for WikiEvents generated by source objects such as the
     * GroupManager.
     * @see com.ecyrd.jspwiki.event.WikiEventListener#actionPerformed(com.ecyrd.jspwiki.event.WikiEvent)
     */
    public void actionPerformed( WikiEvent event )
    {
        if ( event instanceof WikiSecurityEvent )
        {
            WikiSecurityEvent e = (WikiSecurityEvent)event;
            if ( e.getTarget() != null )
            {
                switch (e.getType() )
                {
                    case WikiSecurityEvent.GROUP_ADD:
                    {
                        Group group = (Group)e.getTarget();
                        if ( isInGroup( group ) )
                        {   
                            m_subject.getPrincipals().add( new GroupPrincipal( group ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_REMOVE:  
                    {
                        Group group = (Group)e.getTarget();
                        if ( isInGroup( group ) )
                        {   
                            m_subject.getPrincipals().remove( new GroupPrincipal( group ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_CLEAR_GROUPS:
                    {
                        m_subject.getPrincipals().removeAll( m_subject.getPrincipals( GroupPrincipal.class ) );
                        break;
                    }
                    case WikiSecurityEvent.GROUP_ADD_MEMBER:
                    {
                        Group group = (Group)e.getSource();
                        if ( isInGroup( group ) )
                        {   
                            m_subject.getPrincipals().add( new GroupPrincipal( group ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_REMOVE_MEMBER:
                    {
                        Group group = (Group)e.getSource();
                        Principal principal = (Principal)e.getTarget();
                        if ( m_subject.getPrincipals().contains( principal ) )
                        {   
                            m_subject.getPrincipals().remove( new GroupPrincipal( group ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_CLEAR_MEMBERS:
                    {
                        Group group = (Group)e.getSource();
                        GroupPrincipal principal = new GroupPrincipal( group );
                        if ( m_subject.getPrincipals().contains( principal ) )
                        {   
                            m_subject.getPrincipals().remove( principal ); 
                        }
                        break;
                    }
                }
            }
        }
    }
    
    /** 
     * Invalidates the WikiSession and resets its Subject's 
     * Principal set to the equivalent of a "guest session".
     */
    public void invalidate()
    {
        m_subject.getPrincipals().clear();
        m_subject.getPrincipals().add( WikiPrincipal.GUEST );
        m_subject.getPrincipals().add( Role.ANONYMOUS );
        m_subject.getPrincipals().add( Role.ALL );
        m_cachedCookieIdentity = null;
        m_cachedRemoteUser = null;
        m_cachedUserPrincipal = null;
    }
    
    /**
     * Returns whether the Http servlet container's authentication status has
     * changed. Used to detect whether the container has logged in a user since
     * the last call to this function. This method is stateful. After calling
     * this function, the cached values are set to those in the current request.
     * If the servlet request is null, this method always returns false.
     * @param request the current servlet request
     * @return <code>true</code> if the status has changed, <code>false</code>
     * otherwise
     */
    protected boolean isContainerStatusChanged( HttpServletRequest request )
    {
        if ( request == null )
        {
            return false;
        }
        
        String remoteUser = request.getRemoteUser();
        Principal userPrincipal = request.getUserPrincipal();
        String cookieIdentity = CookieAssertionLoginModule.getUserCookie( request );
        boolean changed= false;
        
        // If request contains non-null remote user, update cached value if changed
        if ( remoteUser != null && !remoteUser.equals( m_cachedRemoteUser) )
        {
            m_cachedRemoteUser = remoteUser;
            log.info( "Remote user changed to " + remoteUser );
            changed = true;
        }
        
        // If request contains non-null user principal, updated cached value if changed
        if ( userPrincipal != null && !userPrincipal.equals( m_cachedUserPrincipal ) )
        {
            m_cachedUserPrincipal = userPrincipal;
            log.info( "User principal changed to " + userPrincipal.getName() );
            changed = true;
        }
        
        // If cookie identity changed (to a different value or back to null), update cache
        if ( ( cookieIdentity != null && !cookieIdentity.equals( m_cachedCookieIdentity ) )
               || ( cookieIdentity == null && m_cachedCookieIdentity != null ) )
        {
            m_cachedCookieIdentity = cookieIdentity;
            log.info( "Cookie changed to " + cookieIdentity );
            changed = true;
        }
        return changed;
    }

    /**
     * <p>Returns the status of the session as a text string. Valid values are:</p>
     * <ul> <li>{@link #AUTHENTICATED}</li> <li>{@link #ASSERTED}</li> <li>{@link #ANONYMOUS}</li>
     * </ul>
     * @return the session status
     */
    public String getStatus()
    {
        if ( isAuthenticated() )
        {
            return AUTHENTICATED;
        }

        if ( isAnonymous() )
        {
            return ANONYMOUS;
        }
        else if ( m_subject.getPrincipals().contains( Role.ASSERTED ) )
        {
            return ASSERTED;
        }
        return "ILLEGAL STATUS!";
    }

    /**
     * Static factory method that returns the WikiSession object associated with
     * the current HTTP request. This method looks up the associated HttpSession
     * in an internal WeakHashMap and attempts to retrieve the WikiSession. If
     * not found, one is created. This method is guaranteed to always return a
     * WikiSession, although the authentication status is unpredictable until
     * the user attempts to log in. If the servlet request parameter is
     * <code>null</code>, a synthetic {@link #guestSession()}is returned.
     * @param engine the current wiki engine
     * @param request the current servlet request object
     * @return the existing (or newly created) wiki session
     */
    public static WikiSession getWikiSession( WikiEngine engine, HttpServletRequest request )
    {
        // If request is null, return guest session
        if ( request == null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for NULL HttpRequest: returning guestSession()" );
            }
            return guestSession();
        }

        // Look for a WikiSession associated with the user's Http Session
        // and create one if it isn't there yet.
        HttpSession session = request.getSession();
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        WikiSession wikiSession = monitor.find( session );
        
        // Start the session monitor thread if not started already
        if ( !monitor.isAlive() )
        {
            monitor.start();
        }
        return wikiSession;
    }

    /**
     * Factory method that creates a new "guest" session containing a single
     * user Principal,
     * @link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}, plus the role
     * principals
     * @link Role#ALL and
     * @link Role#ANONYMOUS.
     * @return the guest wiki session
     */
    public static WikiSession guestSession()
    {
        WikiSession session = new WikiSession();
        session.invalidate();
        return session;
    }
    
    /**
     * Returns the total number of active wiki sessions.
     * @param engine the current wiki session
     * @return the number of sessions
     */
    public static int sessions( WikiEngine engine )
    {
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.sessions();
    }
    
    /**
     * Returns Principals representing the current users known
     * to the wiki. Each Principal will correspond to the
     * value returned by each WikiSession's {@link #getUserPrincipal()}
     * method.
     * @param engine the wiki engine
     * @return an array of Principal objects
     */
    public static Principal[] userPrincipals( WikiEngine engine )
    {
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.userPrincipals();
    }

    /**
     * Verifies whether a String represents an IP address. The algorithm is
     * extremely efficient and does not allocate any objects.
     * @param name the address to test
     * @return the result
     */
    protected static boolean isIPV4Address( String name )
    {
        if ( name.charAt( 0 ) == DOT || name.charAt( name.length() - 1 ) == DOT )
        {
            return false;
        }

        int[] addr = new int[]
        { 0, 0, 0, 0 };
        int currentOctet = 0;
        for( int i = 0; i < name.length(); i++ )
        {
            int ch = name.charAt( i );
            boolean isDigit = ( ch >= ONE && ch <= NINE );
            boolean isDot = ( ch == DOT );
            if ( !isDigit && !isDot )
            {
                return false;
            }
            if ( isDigit )
            {
                addr[currentOctet] = 10 * addr[currentOctet] + ( ch - ONE );
                if ( addr[currentOctet] > 255 )
                {
                    return false;
                }
            }
            else if ( name.charAt( i - 1 ) == DOT )
            {
                return false;
            }
            else
            {
                currentOctet++;
            }
        }
        return ( currentOctet == 3 );
    }
    
}