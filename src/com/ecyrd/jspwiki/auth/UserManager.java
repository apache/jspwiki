package com.ecyrd.jspwiki.auth;

import java.util.Properties;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import java.security.Principal;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.TextUtil;

/**
 *  Manages user accounts, logins/logouts, passwords, etc.
 */
public class UserManager
{
    static Category log = Category.getInstance( UserManager.class );

    /** The name the UserProfile is stored in a Session by. */
    public static final String WIKIUSER          = "currentUser";

    /** If true, logs the IP address of the editor on saving. */
    public static final String PROP_STOREIPADDRESS= "jspwiki.storeIPAddress";

    /** If true, logs the IP address of the editor */
    private boolean            m_storeIPAddress = true;


    public UserManager( WikiEngine engine, Properties props )
    {
        m_storeIPAddress = TextUtil.getBooleanProperty( props,
                                                        PROP_STOREIPADDRESS, 
                                                        m_storeIPAddress );

    }

    // FIXME: Should really in the future use a cache of known user profiles,
    //        otherwise we're swamped with userprofile instances.

    public UserProfile getUserProfile( String name )
    {
        UserProfile wup = new UserProfile();

        wup.setName( name );

        return wup;
    }

    // FIXME: Ditto
    public WikiGroup getWikiGroup( String name )
    {
        WikiGroup group = new WikiGroup();

        group.setName( name );

        return group;
    }

    public Principal getPrincipal( String name )
    {
        return getUserProfile(name); // FIXME: This is a kludge to get things compiling.
    }

    public void login( String username, String password, HttpSession session )
    {
        log.info("Logged in user "+username);
        // FIXME
    }

    /**
     *  Logs a web user out.
     */
    public void logout( HttpSession session )
    {
        if( session != null )
        {
            UserProfile wup = (UserProfile)session.getAttribute( WIKIUSER );
            if( wup != null )
            {
                log.info( "logged out user " + wup.getName() );
            }
            session.invalidate();
        }
    }

    /**
     *  Gets a UserProfile, either from the request (presumably
     *  authenticated and with auth information) or a new one
     *  (with default permissions).
     *  @since 2.1.10.
     */
    public UserProfile getUserProfile( HttpServletRequest request )
    {
        // First, see if we already have a user profile.
        HttpSession session = request.getSession( true );
        UserProfile wup = (UserProfile)session.getAttribute( UserManager.WIKIUSER );

        if( wup != null )
        {
            return( wup );
        }

        // Try to get a limited login. This will be inserted into the request.

        wup = limitedLogin( request );
        if( wup != null )
        {
            return( wup );
        }

        log.error( "Unable to get a default UserProfile!" );

        return( null );
    }

    /**
     *  Performs a "limited" login: sniffs for a user name from a cookie or the
     *  client, and creates a limited user profile based on it.
     */
    protected UserProfile limitedLogin( HttpServletRequest request )
    {
        UserProfile wup  = null;
        String      role = null;

        // See if a cookie exists, and create a 'preferred guest' account if so.
        String storedUsername = retrieveCookieValue( request, WikiEngine.PREFS_COOKIE_NAME );

        log.debug("Stored username="+storedUsername);

        if( storedUsername != null )
        {
            wup = UserProfile.parseStringRepresentation( storedUsername );

            log.debug("wup="+wup);
            // wup.setStatus( UserProfile.NAMED );
            // JSPWiki special: named readers belong to special group 'participant'
            // m_authorizer.addRole( wup, Authorizer.AUTH_ROLE_PARTICIPANT );
        }
        else
        {
            String uid = request.getRemoteUser();
            if( uid == null && m_storeIPAddress )
            {
                uid = request.getRemoteAddr();
            }
            if( uid == null )
            {
                uid = "unknown"; // FIXME: Magic
            }
            wup = getUserProfile( uid );
            // wup.setStatus( UserProfile.UNKNOWN );
            // JSPWiki special: unknown people belong to group 'guest'
            // m_authorizer.addRole( wup, Authorizer.AUTH_ROLE_GUEST );
        }

        // Limited login hasn't been authenticated. Just to emphasize the point:
        // wup.setPassword( null );

        HttpSession session = request.getSession( true );
        session.setAttribute( WIKIUSER, wup );

        return wup;
    }

    /**
     *  Attempts to retrieve the given cookie value from the request.
     *  Returns the string value (which may or may not be decoded
     *  correctly, depending on browser!), or null if the cookie is
     *  not found.
     */
    // FIXME: Does not belong here and should be moved elsewhere.
    private String retrieveCookieValue( HttpServletRequest request, String cookieName )
    {
        Cookie[] cookies = request.getCookies();

        if( cookies != null )
        {
            for( int i = 0; i < cookies.length; i++ )
            {
                if( cookies[i].getName().equals( cookieName ) )
                {
                    return( cookies[i].getValue() );
                }
            }
        }

        return( null );
    }


}
