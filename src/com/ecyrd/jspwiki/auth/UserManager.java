package com.ecyrd.jspwiki.auth;

import java.util.*;
import java.security.Principal;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.security.Principal;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.TranslatorReader;
import com.ecyrd.jspwiki.util.ClassUtil;

import com.ecyrd.jspwiki.auth.modules.*;

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

    public static final String PROP_AUTHENTICATOR = "jspwiki.authenticator";

    /** If true, logs the IP address of the editor */
    private boolean            m_storeIPAddress = true;

    private HashMap            m_groups = new HashMap();

    // FIXME: These should probably be localized.
    // FIXME: All is used as a catch-all.

    public static final String GROUP_GUEST       = "Guest";
    public static final String GROUP_NAMEDGUEST  = "NamedGuest";
    public static final String GROUP_KNOWNPERSON = "KnownPerson";

    private WikiAuthenticator  m_authenticator;
    private UserDatabase       m_database;

    private WikiEngine         m_engine;

    public UserManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        m_engine = engine;

        m_storeIPAddress = TextUtil.getBooleanProperty( props,
                                                        PROP_STOREIPADDRESS, 
                                                        m_storeIPAddress );

        WikiGroup all = new AllGroup();
        all.setName( "All" );
        m_groups.put( GROUP_GUEST,       new AllGroup() );
        // m_groups.put( "All",             all );
        m_groups.put( GROUP_NAMEDGUEST,  new NamedGroup() );
        m_groups.put( GROUP_KNOWNPERSON, new KnownGroup() );

        String authClassName = props.getProperty( PROP_AUTHENTICATOR );

        if( authClassName != null )
        {
            try
            {
                Class authenticatorClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.modules",
                                                                authClassName );

                m_authenticator = (WikiAuthenticator)authenticatorClass.newInstance();
                m_authenticator.initialize( props );

                log.info("Initialized "+authClassName+" for authentication.");
            }
            catch( ClassNotFoundException e )
            {
                log.fatal( "Authenticator "+authClassName+" cannot be found", e );
                throw new WikiException("Authenticator cannot be found");
            }
            catch( InstantiationException e )
            {
                log.fatal( "Authenticator "+authClassName+" cannot be created", e );
                throw new WikiException("Authenticator cannot be created");
            }
            catch( IllegalAccessException e )
            {
                log.fatal( "You are not allowed to access this authenticator class", e );
                throw new WikiException("You are not allowed to access this authenticator class");
            }
        }

        //
        //  FIXME: These should not be hardcoded.
        //

        m_database = new WikiDatabase();
        m_database.initialize( m_engine, props );
    }

    // FIXME: Should really in the future use a cache of known user profiles,
    //        otherwise we're swamped with userprofile instances.

    public UserProfile getUserProfile( String name )
    {
        UserProfile wup = new UserProfile();

        wup.setName( name );

        return wup;
    }

    /**
     *  Returns a WikiGroup instance for a given name.  WikiGroups are cached,
     *  so there is basically a singleton across the Wiki for a group.
     *  The reason why this class caches them instead of the WikiGroup
     *  class itself is that it is the business of the User Manager to
     *  handle such issues.
     *
     *  @param name Name of the group.  This is case-sensitive.
     *  @return A WikiGroup instance.
     */
    // FIXME: Someone should really check when groups cease to be used,
    //        and release groups that are not being used.

    public WikiGroup getWikiGroup( String name )
    {
        WikiGroup group;

        synchronized( m_groups )
        {
            group = (WikiGroup) m_groups.get( name );

            /*
            if( group == null )
            {
                group = new WikiGroup();
                group.setName( name );
                m_groups.put( name, group );
            }
            */
        }

        return group;
    }

    /**
     *  Returns a list of all WikiGroups this Principal is a member
     *  of.
     */
    // FIXME: This is not a very good solution; UserProfile
    //        should really cache the information.
    // FIXME: Should really query the page manager.

    public List getGroupsForPrincipal( Principal user )
        throws NoSuchPrincipalException
    {
        List list = m_database.getGroupsForPrincipal( user );

        if( list == null ) list = new ArrayList();

        //
        //  Add the default groups.
        //

        synchronized( m_groups )
        {
            for( Iterator i = m_groups.values().iterator(); i.hasNext(); )
            {
                WikiGroup g = (WikiGroup) i.next();

                if( g.isMember( user ) )
                {
                    log.debug("User "+user.getName()+" is a member of "+g.getName());
                    list.add( g );
                }
            }
        }

        return list;
    }

    /**
     *  Attempts to find a Principal from the list of known principals.
     */
    public Principal getPrincipal( String name )
    {
        Principal p = null;
        // Principal p = m_principalist.getPrincipal( name );

        if( p == null )
        {
            p = new UndefinedPrincipal( name );
        }

        return p;
    }

    public boolean login( String username, String password, HttpSession session )
    {
        if( m_authenticator == null ) return false;

        if( session == null )
        {
            log.error("No session provided, cannot log in.");
            return false;
        }

        UserProfile wup = getUserProfile( username );
        wup.setPassword( password );

        boolean isValid = m_authenticator.authenticate( wup );

        if( isValid )
        {
            wup.setLoginStatus( UserProfile.PASSWORD );
            session.setAttribute( WIKIUSER, wup );
            log.info("Logged in user "+username);
        }
        else
        {
            log.info("Username "+username+" attempted to log in with the wrong password.");
        }

        return isValid;
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
                wup.setLoginStatus( UserProfile.NONE );
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
            return wup;
        }

        // Try to get a limited login. This will be inserted into the request.

        wup = limitedLogin( request );
        if( wup != null )
        {
            return wup;
        }

        log.error( "Unable to get a default UserProfile!" );

        return null;
    }

    /**
     *  Performs a "limited" login: sniffs for a user name from a cookie or the
     *  client, and creates a limited user profile based on it.
     */
    protected UserProfile limitedLogin( HttpServletRequest request )
    {
        UserProfile wup  = null;
        String      role = null;

        //
        //  First, checks whether container has done authentication for us.
        //
        String uid = request.getRemoteUser();

        if( uid != null )
        {
            wup = getUserProfile( uid );
            wup.setLoginStatus( UserProfile.CONTAINER );            

            HttpSession session = request.getSession( true );
            session.setAttribute( WIKIUSER, wup );
        }
        else
        {
            // 
            //  See if a cookie exists, and create a default account.
            //
            uid = retrieveCookieValue( request, WikiEngine.PREFS_COOKIE_NAME );

            log.debug("Stored username="+uid);

            if( uid != null )
            {
                wup = UserProfile.parseStringRepresentation( uid );

                log.debug("wup="+wup);
                wup.setLoginStatus( UserProfile.COOKIE );
            }
            else
            {
                //
                //  No username either, so fall back to the IP address.
                // 
                if( m_storeIPAddress )
                {
                    uid = request.getRemoteAddr();
                }
                if( uid == null )
                {
                    uid = "unknown"; // FIXME: Magic
                }
                wup = getUserProfile( uid );
                wup.setLoginStatus( UserProfile.NONE );
            }
        }

        //
        //  FIXME:
        //
        //  We cannot store the UserProfile into the session, because of the following:
        //  Assume that Edit.jsp is protected through container auth.
        //
        //  User without a cookie arrives through Wiki.jsp.  A
        //  UserProfile is created, which essentially contains his IP
        //  address.  If this is stored in the session, then, when the user
        //  tries to access the Edit.jsp page and container does auth, he will
        //  always be then known by his IP address, regardless of what the 
        //  request.getRemoteUser() says.

        //  So, until this is solved, we create a new UserProfile on each
        //  access.  Ouch.

        // Limited login hasn't been authenticated. Just to emphasize the point: 
        // wup.setPassword( null );

        // HttpSession session = request.getSession( true );
        // session.setAttribute( WIKIUSER, wup );

        return wup;
    }

    /**
     *  Attempts to retrieve the given cookie value from the request.
     *  Returns the string value (which may or may not be decoded
     *  correctly, depending on browser!), or null if the cookie is
     *  not found.
     */
    // FIXME: Does not belong here and should be moved elsewhere.  HttpUtil?
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

    /**
     *  Sets the username cookie.
     *
     *  @since 2.1.47.
     */
    public void setUserCookie( HttpServletResponse response, String name )
    {
        UserProfile profile = getUserProfile( TranslatorReader.cleanLink(name) );

        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, 
                                   profile.getStringRepresentation() );

        prefs.setMaxAge( 1001*24*60*60 ); // 1001 days is default.

        response.addCookie( prefs );
    }

}
