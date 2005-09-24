package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.util.HttpUtil;

/**
 * <p>
 * Logs in a user based on assertion of a name supplied in a cookie. If the
 * cookie is not found, authentication fails.
 * </p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 * <ol>
 * <li>{@link HttpRequestCallback}- supplies the cookie, which should contain
 * a user name.</li>
 * </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the username will be
 * created and associated with the Subject. Principals 
 * {@link com.ecyrd.jspwiki.auth.authorize.Role#ALL} and
 * {@link com.ecyrd.jspwiki.auth.authorize.Role#ASSERTED} will be added.
 * @see javax.security.auth.spi.LoginModule#commit()
 *      </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.4 $ $Date: 2005-09-24 14:25:59 $
 * @since 2.3
 */
public class CookieAssertionLoginModule extends AbstractLoginModule
{

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiAssertedName";

    public static final String PROMPT            = "User name";

    protected static Logger    log               = Logger.getLogger( CookieAssertionLoginModule.class );
    
    /**
     * Logs in the user by calling back to the registered CallbackHandler with an
     * HttpRequestCallback. The CallbackHandler must supply the current servlet
     * HTTP request as its response.
     * @return the result of the login; this will be <code>true</code>
     * if a cookie is found. If not found, this method throws a 
     * <code>FailedLoginException</code>.
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException
    {
        HttpRequestCallback hcb = new HttpRequestCallback();
        Callback[] callbacks = new Callback[]
        { hcb };
        try
        {
            m_handler.handle( callbacks );
            HttpServletRequest request = hcb.getRequest();
            HttpSession session = ( request == null ) ? null : request.getSession( false );
            String sid = ( session == null ) ? NULL : session.getId();
            String name = getUserCookie( request );
            if ( name == null )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "No cookie " + PREFS_COOKIE_NAME + " present in session ID=:  " + sid );
                }
                throw new FailedLoginException( "The user cookie was not found." );
            }
            
            if ( log.isDebugEnabled() )
            {
                log.debug( "Logged in session ID=" + sid );
                log.debug( "Added Principals " + name + ",Role.ASSERTED,Role.ALL" );
            }
            m_principals.add( new WikiPrincipal( name, WikiPrincipal.LOGIN_NAME ) );
            m_principals.add( Role.ASSERTED );
            m_principals.add( Role.ALL );
            return true;
        }
        catch( IOException e )
        {
            log.error( "IOException: " + e.getMessage() );
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback, disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

    }

    /**
     * Returns the username cookie value.
     * @param request
     * @return the username, as retrieved from the cookie
     */
    public static String getUserCookie( HttpServletRequest request )
    {
        return HttpUtil.retrieveCookieValue( request, PREFS_COOKIE_NAME );
    }

    /**
     * Sets the username cookie.
     */
    public static void setUserCookie( HttpServletResponse response, String name )
    {
        Cookie userId = new Cookie( PREFS_COOKIE_NAME, name );
        userId.setMaxAge( 1001 * 24 * 60 * 60 ); // 1001 days is default.
        response.addCookie( userId );
    }

    public static void clearUserCookie( HttpServletResponse response )
    {
        Cookie userId = new Cookie( PREFS_COOKIE_NAME, "" );
        userId.setMaxAge( 0 );
        response.addCookie( userId );
    }
}