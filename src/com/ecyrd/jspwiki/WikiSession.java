package com.ecyrd.jspwiki;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * Represents a long-running wiki session, with an associated user Principal,
 * user Subject, and authentication status. This class is initialized with
 * minimal, default-deny values: authentication is set to false, and the user
 * principal is set to null.
 * @author Andrew R. Jaquith
 * @version $Revision: 2.4 $ $Date: 2005-08-03 03:47:36 $
 */
public class WikiSession
{

    /**
     * Anonymous session that contains a user principal
     * of {@link com.ecyrd.jspwiki.auth.WikiPrincipal#GUEST}
     * and a role of {@link com.ecyrd.jspwiki.auth.authorize.Role#ANONYMOUS}.
     */
    public static final WikiSession GUEST_SESSION;

    public static final String      ANONYMOUS           = "anonymous";

    public static final String      ASSERTED            = "asserted";

    public static final String      AUTHENTICATED       = "authenticated";

    protected static final String   WIKI_SESSION        = "WikiSession";

    private Subject                 m_subject             = new Subject();

    protected String                m_cachedRemoteUser    = null;

    protected Principal             m_cachedUserPrincipal = null;

    static
    {
        GUEST_SESSION = new WikiSession();
        GUEST_SESSION.getSubject().getPrincipals().add( WikiPrincipal.GUEST );
        GUEST_SESSION.getSubject().getPrincipals().add( Role.ANONYMOUS );
    }

    /**
     * Returns <code>true</code> if this WikiSession's Subject contains
     * a given Principal in its Principal set.
     * @param principal the Principal to look for
     * @return <code>true</code> if the Set of Principals returned by 
     * {@link javax.security.auth.Subject#getPrincipals()} contains
     * the specified Principal; <code>false</code> otherwise.
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
     * Determines whether the current user principal represents an 
     * anonymous user represented by an IP
     * address. If the user is not authenticated, this might be the case.
     * This method works with either IPv4 or IPv6 addresses.
     * @return whether the current user's identity is equivalent to an IP address
     */
    public boolean isAnonymous() 
    {
        boolean isAddress = false;
    
        if( getUserPrincipal() != null )
        {
            byte[] addr = new byte[4];
          
            try
            {
                InetAddress ip = InetAddress.getByAddress( getUserPrincipal().getName(), addr );
                isAddress = true;
            }
            catch( UnknownHostException e )
            {
            }
        }
        return isAddress;
    }
    
    /**
     * <p>Returns the Principal used to log in to an authenticated session. The
     * login principal is determined by examining the Subject's Principal set 
     * for WikiPrincipals; the first one with type designator 
     * <code>LOGIN_NAME</code></li> is the login principal. If one is not
     * found, this method returns the first principal that isn't of type Role.
     * If neither of these conditions hold, this method returns <code>null</code>.
     * @return the login Principal
     */
    public Principal getLoginPrincipal()
    {
        Set principals = m_subject.getPrincipals();
        Principal secondChoice = null;

        // Take the first WikiPrincipal of type LOGIN_NAME
        for( Iterator it = principals.iterator(); it.hasNext(); )
        {
            Principal currentPrincipal = (Principal) it.next();
            if( !( currentPrincipal instanceof Role ) )
            {
                if( currentPrincipal instanceof WikiPrincipal )
                {
                    WikiPrincipal wp = (WikiPrincipal)currentPrincipal;
                    if ( wp.getType().equals( WikiPrincipal.LOGIN_NAME ) )
                    {
                        return currentPrincipal;
                    }
                }
                if( secondChoice == null )
                {
                    secondChoice = currentPrincipal;
                }
            }
        }
        return secondChoice;
    }
    
    /**
     * <p>Returns the primary user Principal associated with this session. The
     * primary user principal is determined as follows:</p>
     * <ol>
     *   <li>If the Subject's Principal set contains WikiPrincipals,
     *       the first WikiPrincipal with type designator <code>FULL_NAME</code>
     *       or (alternatively) <code>WIKI_NAME</true> is the primary Principal.</li>
     *   <li>For all other cases, the first Principal in the Subject's principal
     *       collection that that isn't of type Role is the primary.</li>
     * </ol> 
     * If no primary user Principal is found, this method returns <code>null</code>.
     * @return the primary user Principal
     */
    public Principal getUserPrincipal()
    {
        Set principals = m_subject.getPrincipals();
        Principal secondChoice = null;

        // Take the first WikiPrincipal of type FULL_NAME as primary
        // Take the first non-Role as the alternate
        for( Iterator it = principals.iterator(); it.hasNext(); )
        {
            Principal currentPrincipal = (Principal) it.next();
            if( !( currentPrincipal instanceof Role ) )
            {
                if( currentPrincipal instanceof WikiPrincipal )
                {
                    WikiPrincipal wp = (WikiPrincipal)currentPrincipal;
                    if ( wp.getType().equals( WikiPrincipal.FULL_NAME ) )
                    {
                        return currentPrincipal;
                    }
                    else if ( wp.getType().equals( WikiPrincipal.WIKI_NAME ) )
                    {
                        return currentPrincipal;
                    }
                }
                if( secondChoice == null )
                {
                    secondChoice = currentPrincipal;
                }
            }
        }
        return secondChoice;
    }

    /**
     * Returns all user Principals associated with this session. User principals
     * are those in the Subject's principal collection that aren't of type Role.
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
                if ( !( currentPrincipal instanceof Role ) )
                {
                    principals.add(currentPrincipal);
                }
            }
        }
        return (Principal[])principals.toArray(new Principal[principals.size()]);
    }

    /**
     * Identifies whether the WikiSession's Subject is currently unknown to the
     * application. This will return <code>true</code> if the size of the
     * Subject's Principal collection is 0.
     * @return <code>true</code> if the subject contains zero principals,
     *         <code>false</code> otherwise
     */
    public boolean isUnknown()
    {
        return ( m_subject.getPrincipals().size() == 0 );
    }

    /**
     * Sets the Subject representing the user.
     * @param subject
     */
    public void setSubject( Subject subject )
    {
        //TODO: this should be a privileged action
        m_subject = subject;
    }

    /**
     * Returns the Subject representing the user.
     * @return the subject
     */
    public Subject getSubject()
    {
        return m_subject;
    }

    /**
     * Static factory method that returns the WikiSession object associated with
     * the current HTTP request. This method looks for an object associated with
     * the session with the value {@link #WIKI_SESSION}. If not found, one is
     * created. This method is guaranteed to always return a WikiSession,
     * although the authentication status is unpredictable until the user
     * attempts to log in. If the servlet request parameter is null, a synthetic
     * {@link #GUEST_SESSION}is returned.
     * @param request the current servlet request object
     * @return the existing (or newly created) wiki session
     */
    public static WikiSession getWikiSession( HttpServletRequest request )
    {
        // If request is null, return guest session
        if ( request == null )
        {
            return GUEST_SESSION;
        }

        // Look for a WikiSession associated with the user's Http Session
        // and create one if it isn't there yet.
        WikiSession wikiSession;
        HttpSession session = request.getSession( true );
        Object att = session.getAttribute( WIKI_SESSION );
        if ( att != null && att instanceof WikiSession )
        {
            wikiSession = (WikiSession) att;
        }
        else
        {
            wikiSession = new WikiSession();
            session.setAttribute( WIKI_SESSION, wikiSession );
        }
        return wikiSession;
    }

    /**
     * Returns whether the Http servlet container's authentication status has
     * changed. Used to detect whether the container has logged in a user since
     * the last call to this function. This method is stateful. After calling
     * this function, the cached values are set to those in the current request.
     * If the servlet request is null, this method always returns false.
     * @param request the current servlet request
     * @return <code>true</code> if the status has changed, <code>false</code>
     *         otherwise
     */
    public boolean isContainerStatusChanged( HttpServletRequest request )
    {
        if ( request == null )
        {
            return false;
        }
        
        String currentRemoteUser = request.getRemoteUser();
        Principal currentUserPrincipal = request.getUserPrincipal();
        boolean isChanged = false;
        if ( currentRemoteUser != m_cachedRemoteUser )
        {
            m_cachedRemoteUser = currentRemoteUser;
            isChanged = true;
        }
        if ( currentUserPrincipal != m_cachedUserPrincipal )
        {
            m_cachedUserPrincipal = currentUserPrincipal;
            isChanged = true;
        }
        return isChanged;
    }

    /**
     * Stash the Http request's <code>getUserPrincipal</code>
     * and <code>getRemoteUser</code> so we can compare them
     * later with {@link isStatusChanged()}.
     * @param request
     */
    private void cacheContainerCredentials( HttpServletRequest request )
    {
        m_cachedRemoteUser = request.getRemoteUser();
        m_cachedUserPrincipal = request.getUserPrincipal();
    }

    /**
     * <p>Returns the status of the session as a text string. Valid values are:</p>
     * <ul>
     *   <li>{@link #AUTHENTICATED}</li>
     *   <li>{@link #ASSERTED}</li>
     *   <li>{@link #ANONYMOUS}</li>
     * </ul>
     * @return the session status
     */
    public String getStatus()
    {
        if ( isAuthenticated() )
        {
            return AUTHENTICATED;
        }

        if ( hasPrincipal( Role.ANONYMOUS ) )
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