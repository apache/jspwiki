package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * <p>
 * Logs in a user based solely on IP address; no other authentication is
 * performed. Barring a mis-configuration or I/O error, this LoginModule
 * <em>always</em> succeeds.
 * </p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 * <ol>
 * <li>{@link HttpRequestCallback}- supplies the IP address, which is used as
 * a backup in case no name is supplied.</li>
 * </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the IP address will be
 * created and associated with the Subject. Principals 
 * {@link com.ecyrd.jspwiki.auth.authorize.Role#ALL} and
 * {@link com.ecyrd.jspwiki.auth.authorize.Role#ANONYMOUS} will be added.
 * @see javax.security.auth.spi.LoginModule#commit()
 *      </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-09-24 14:25:59 $
 * @since 2.3
 */
public class AnonymousLoginModule extends AbstractLoginModule
{

    /**
     * Bogus prompt sent to the callback handler.
     */
    public static final String PROMPT            = "User name";

    protected static Logger    log               = Logger.getLogger( AnonymousLoginModule.class );
    
    /**
     * Logs in the user by calling back to the registered CallbackHandler with an
     * HttpRequestCallback. The CallbackHandler must supply the current servlet
     * HTTP request as its response.
     * @return the result of the login; this will always be <code>true</code>
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
            WikiPrincipal ipAddr = new WikiPrincipal( request.getRemoteAddr() );
            if ( log.isDebugEnabled() )
            {
                HttpSession session = ( request == null ) ? null : request.getSession( false );
                String sid = (session == null) ? NULL : session.getId(); 
                log.debug("Logged in session ID=" + sid);
                log.debug("Added Principals " + ipAddr + ",Role.ANONYMOUS,Role.ALL" );
            }
            m_principals.add( ipAddr );
            m_principals.add( Role.ANONYMOUS );
            m_principals.add( Role.ALL );
            return true;
        }
        catch( IOException e )
        {
            log.error("IOException: " + e.getMessage());
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback, disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

    }

}