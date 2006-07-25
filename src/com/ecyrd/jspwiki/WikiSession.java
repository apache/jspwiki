package com.ecyrd.jspwiki;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.auth.WikiPrincipal.PrincipalComparator;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.login.PrincipalWrapper;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;

/**
 * <p>Represents a long-running wiki session, with an associated user Principal,
 * user Subject, and authentication status. This class is initialized with
 * minimal, default-deny values: authentication is set to <code>false</code>,
 * and the user principal is set to <code>null</code>.</p>
 * <p>The WikiSession class allows callers to:</p>
 * <ul>
 *   <li>Obtain the authentication status of the user via 
 *     {@link #isAnonymous()} and {@link #isAuthenticated()}</li>
 *   <li>Query the session for Principals representing the
 *     user's identity via {@link #getLoginPrincipal()},
 *     {@link #getUserPrincipal()} and {@link #getPrincipals()}</li>
 *   <li>Store, retrieve and clear UI messages via 
 *     {@link #addMessage(String)}, {@link #getMessages(String)}
 *     and {@link #clearMessages(String)}</li>
 * </ul>
 * <p>To keep track of the Principals each user posseses, each WikiSession
 * stores a JAAS Subject. Various login processes add or remove Principals
 * when users authenticate or log out.</p>
 * <p>WikiSession implements the {@link com.ecyrd.jspwiki.event.WikiEventListener}
 * interface and listens for group add/change/delete events fired by
 * event sources the WikiSession is registered with. Normally, 
 * {@link com.ecyrd.jspwiki.auth.AuthenticationManager} registers each WikiSession
 * with the {@link com.ecyrd.jspwiki.auth.authorize.GroupManager}
 * so it can catch group events. Thus, when a user is added to a 
 * {@link com.ecyrd.jspwiki.auth.authorize.Group}, a corresponding
 * {@link com.ecyrd.jspwiki.auth.GroupPrincipal} is injected into
 * the Subject's Principal set. Likewise, when the user is removed from 
 * the Group or the Group is deleted, the GroupPrincipal is removed
 * from the Subject.</p>
 * <p>In addition to methods for examining individual <code>WikiSession</code>
 * objects, this class also contains a number of static methods for
 * managing WikiSessions for an entire wiki. These methods allow callers
 * to find, query and remove WikiSession objects, and
 * to obtain a list of the current wiki session users.</p>
 * <p>WikiSession encloses a protected static class, {@link SessionMonitor},
 * to keep track of WikiSessions registered with each wiki.</p>
 * @author Andrew R. Jaquith
 * @version $Revision: 2.24 $ $Date: 2006-07-25 03:45:27 $
 */
public final class WikiSession implements WikiEventListener
{

    /** An anonymous user's session status. */
    public static final String  ANONYMOUS             = "anonymous";

    /** An asserted user's session status. */
    public static final String  ASSERTED              = "asserted";

    /** An authenticated user's session status. */
    public static final String  AUTHENTICATED         = "authenticated";
    
    private static final int    ONE                   = 48;

    private static final int    NINE                  = 57;

    private static final int    DOT                   = 46;

    private static final Logger log                   = Logger.getLogger( WikiSession.class );
    
    private static final String ALL                   = "*";
    
    private final Subject       m_subject             = new Subject();

    private final Map           m_messages            = new HashMap();

    private String              m_cachedCookieIdentity= null;
    
    private String              m_cachedRemoteUser    = null;

    private Principal           m_cachedUserPrincipal = null;

    /** The WikiEngine that created this session. */
    private WikiEngine          m_engine              = null;
    
    private boolean             m_isNew               = true;
    
    /**
     * Returns <code>true</code> if one of this WikiSession's user Principals
     * can be shown to belong to a particular wiki group.
     * @param group the group to test
     * @return the result
     */
    protected final boolean isInGroup( Group group )
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
    protected final boolean isNew()
    {
        return m_isNew;
    }

    /**
     * Sets the status of this wiki session.
     * @param isNew whether this session should be considered "new".
     */
    protected final void setNew( boolean isNew )
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
    public final boolean isAuthenticated()
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
    public final boolean isAnonymous()
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
    public final Principal getLoginPrincipal()
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
     * <code>WIKI_NAME</code> is the primary Principal.</li>
     *   <li>For all other cases, the first Principal in the Subject's principal
     *       collection that that isn't of type Role or GroupPrincipal is the primary.</li>
     * </ol> 
     * If no primary user Principal is found, this method returns
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}.
     * @return the primary user Principal
     */
    public final Principal getUserPrincipal()
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
    public final void addMessage(String message)
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
    public final void addMessage(String topic, String message)
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
    public final void clearMessages()
    {
        m_messages.clear();
    }
    
    /**
     * Clears all messages associated with a session topic.
     * @param topic the topic whose messages should be cleared.
     */
    public final void clearMessages( String topic )
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
    public final String[] getMessages() 
    {
        return getMessages( ALL );
    }

    /**
     * Returns all messages associated with a session topic.
     * The messages stored with the session persist throughout the
     * session unless they have been reset with {@link #clearMessages(String)}.
     * @return the current messsages.
     */
    public final String[] getMessages( String topic ) 
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
    public final Principal[] getPrincipals()
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
     * @param engine the wiki engine
     * @param request the users's HTTP request
     */
    public static final void removeWikiSession( WikiEngine engine, HttpServletRequest request )
    {
        if ( engine == null || request == null )
        {
            throw new IllegalArgumentException( "Request or engine cannot be null." ); 
        }
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        monitor.remove( request.getSession() );
    }
    
    /**
     * <p>Returns the JAAS Subject representing the user and
     * all of his or her Principals.</p>
     * <em>Note: this method is highly privileged. Try to
     * avoid using it. In the future, this method will explicitly
     * check for {@link javax.security.auth.AuthPermission} with
     * the target <code>getSubject</code>.</em>
     * @return the wiki user's Subject
     */
    public final Subject getSubject()
    {
        // TODO: this should be a privileged action
        return m_subject;
    }

    /**
     * Listens for WikiEvents generated by source objects such as the
     * GroupManager.
     * @see com.ecyrd.jspwiki.event.WikiEventListener#actionPerformed(com.ecyrd.jspwiki.event.WikiEvent)
     */
    public final void actionPerformed( WikiEvent event )
    {
        if ( event instanceof WikiSecurityEvent )
        {
            WikiSecurityEvent e = (WikiSecurityEvent)event;
            String wiki = m_engine.getApplicationName();
            if ( e.getTarget() != null )
            {
                switch (e.getType() )
                {
                    case WikiSecurityEvent.GROUP_ADD:
                    {
                        Group group = (Group)e.getTarget();
                        if ( isInGroup( group ) )
                        {   
                            m_subject.getPrincipals().add( new GroupPrincipal( wiki, group.getName() ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_REMOVE:  
                    {
                        Group group = (Group)e.getTarget();
                        if ( isInGroup( group ) )
                        {   
                            m_subject.getPrincipals().remove( new GroupPrincipal( wiki, group.getName() ) ); 
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
                            m_subject.getPrincipals().add( new GroupPrincipal( wiki, group.getName() ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_REMOVE_MEMBER:
                    {
                        Group group = (Group)e.getSource();
                        Principal principal = (Principal)e.getTarget();
                        if ( m_subject.getPrincipals().contains( principal ) )
                        {   
                            m_subject.getPrincipals().remove( new GroupPrincipal( wiki, group.getName() ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_CLEAR_MEMBERS:
                    {
                        Group group = (Group)e.getSource();
                        GroupPrincipal principal = new GroupPrincipal( wiki, group.getName() );
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
     * Principals to the equivalent of a "guest session".
     */
    public final void invalidate()
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
     * Returns whether the HTTP servlet container's authentication status has
     * changed. Used to detect whether the container has logged in a user since
     * the last call to this function. This method is stateful. After calling
     * this function, the cached values are set to those in the current request.
     * If the servlet request is null, this method always returns false.
     * @param request the servlet request
     * @return <code>true</code> if the status has changed, <code>false</code>
     * otherwise
     */
    protected final boolean isContainerStatusChanged( HttpServletRequest request )
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
     * <p>Returns the status of the wiki session as a text string. Valid values are:</p>
     * <ul>
     *   <li>{@link #AUTHENTICATED}</li> 
     *   <li>{@link #ASSERTED}</li> 
     *   <li>{@link #ANONYMOUS}</li>
     * </ul>
     * @return the user's session status
     */
    public final String getStatus()
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
     * @param engine the wiki engine
     * @param request the servlet request object
     * @return the existing (or newly created) wiki session
     */
    public final static WikiSession getWikiSession( WikiEngine engine, HttpServletRequest request )
    {
        // If request is null, return guest session
        if ( request == null )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for NULL HttpRequest: returning guestSession()" );
            }
            return guestSession( engine );
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
     * Static factory method that creates a new "guest" session containing a single
     * user Principal {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}, 
     * plus the role principals {@link Role#ALL} and
     * {@link Role#ANONYMOUS}.
     * @param engine the wiki engine
     * @return the guest wiki session
     */
    public static final WikiSession guestSession( WikiEngine engine )
    {
        WikiSession session = new WikiSession();
        session.m_engine = engine;
        session.invalidate();
        return session;
    }
    
    /**
     * Returns the total number of active wiki sessions for a
     * particular wiki. This method delegates to the wiki's
     * {@link SessionMonitor#sessions()} method.
     * @param engine the wiki session
     * @return the number of sessions
     */
    public static final int sessions( WikiEngine engine )
    {
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.sessions();
    }
    
    /**
     * Returns Principals representing the current users known
     * to a particular wiki. Each Principal will correspond to the
     * value returned by each WikiSession's {@link #getUserPrincipal()}
     * method. This method delegates to {@link SessionMonitor#userPrincipals()}.
     * @param engine the wiki engine
     * @return an array of Principal objects, sorted by name
     */
    public static final Principal[] userPrincipals( WikiEngine engine )
    {
        SessionMonitor monitor = SessionMonitor.getInstance( engine );
        return monitor.userPrincipals();
    }

    /**
     * Verifies whether a String represents an IPv4 address. The algorithm is
     * extremely efficient and does not allocate any objects.
     * @param name the address to test
     * @return the result
     */
    protected static final boolean isIPV4Address( String name )
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