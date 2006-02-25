package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.ArrayList;
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

/**
 * Represents a long-running wiki session, with an associated user Principal,
 * user Subject, and authentication status. This class is initialized with
 * minimal, default-deny values: authentication is set to <code>false</code>,
 * and the user principal is set to <code>null</code>.
 * @author Andrew R. Jaquith
 * @version $Revision: 2.17 $ $Date: 2006-02-25 18:42:30 $
 */
public class WikiSession implements WikiEventListener
{

    public static final String ANONYMOUS             = "anonymous";

    public static final String ASSERTED              = "asserted";

    public static final String AUTHENTICATED         = "authenticated";
    
    /** Weak hashmap that maps wiki sessions to HttpSessions. */
    private static final Map   c_sessions            = new WeakHashMap();

    private Subject            m_subject             = new Subject();

    protected String           m_cachedCookieIdentity= null;
    
    protected String           m_cachedRemoteUser    = null;

    protected Principal        m_cachedUserPrincipal = null;

    private WikiContext        m_lastContext         = null;
    
    private Map                m_messages            = new HashMap();

    protected static int       ONE                   = 48;

    protected static int       NINE                  = 57;

    protected static int       DOT                   = 46;

    protected static Logger    log                   = Logger.getLogger( WikiSession.class );
    
    protected static final String ALL                = "*";

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
     * Returns <code>true</code> if this WikiSession's Subject contains a
     * given Principal in its Principal set.
     * @param principal the Principal to look for
     * @return <code>true</code> if the Set of Principals returned by
     * {@link javax.security.auth.Subject#getPrincipals()} contains the
     * specified Principal; <code>false</code> otherwise.
     */
    protected boolean hasPrincipal( Principal principal )
    {
        for( Iterator it = m_subject.getPrincipals().iterator(); it.hasNext(); )
        {
            Principal current = (Principal) it.next();
            if ( principal.equals( current ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Protected method that caches the most recent wiki context. This method is
     * called only from
     * {@link WikiContext#WikiContext(WikiEngine, HttpServletRequest, WikiPage)}
     * and {@link WikiContext#WikiContext(WikiEngine, WikiPage)}, and nowhere
     * else. Its primary function is to allow downstream classes such as
     * {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer}to access
     * the most recent WikiContext, and thus, the HttpServletRequest.
     * @param context the most recent wiki context, which may be
     * <code>null</code>
     */
    protected void setLastContext( WikiContext context )
    {
        // TODO: callers should supply the WikiSessionPermission "setContext"
        if ( context != null )
        {
            m_lastContext = context;
        }
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
        return ( hasPrincipal( Role.AUTHENTICATED ) );
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

    /**
     * Returns the most recently-accessed WikiContext for this session. This
     * method may return <code>null</code>.
     * @return the most recent wiki context
     */
    public WikiContext getLastContext()
    {
        return m_lastContext;
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
                        if ( hasPrincipal( principal ) )
                        {   
                            m_subject.getPrincipals().remove( new GroupPrincipal( group ) ); 
                        }
                        break;
                    }
                    case WikiSecurityEvent.GROUP_CLEAR_MEMBERS:
                    {
                        Group group = (Group)e.getSource();
                        GroupPrincipal principal = new GroupPrincipal( group );
                        if ( hasPrincipal( principal ) )
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
     * Static factory method that returns the WikiSession object associated with
     * the current HTTP request. This method looks up the associated HttpSession
     * in an internal WeakHashMap and attempts to retrieve the WikiSession. If
     * not found, one is created. This method is guaranteed to always return a
     * WikiSession, although the authentication status is unpredictable until
     * the user attempts to log in. If the servlet request parameter is
     * <code>null</code>, a synthetic {@link #guestSession()}is returned.
     * @param request the current servlet request object
     * @return the existing (or newly created) wiki session
     */
    public static WikiSession getWikiSession( HttpServletRequest request )
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
        WikiSession wikiSession;
        HttpSession session = request.getSession();
        String sid = ( session == null ) ? "(null)" : session.getId();
        Object storedSession = c_sessions.get( session );
        if ( storedSession != null && storedSession instanceof WikiSession )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... found it" );
            }
            wikiSession = (WikiSession) storedSession;
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Looking up WikiSession for session ID=" + sid + "... not found. Creating guestSession()" );
            }
            wikiSession = guestSession();
            c_sessions.put( session, wikiSession );
        }
        return wikiSession;
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
            log.info( "User principal changed to " + userPrincipal );
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
        else if ( hasPrincipal( Role.ASSERTED ) )
        {
            return ASSERTED;
        }
        return "ILLEGAL STATUS!";
    }
    
}