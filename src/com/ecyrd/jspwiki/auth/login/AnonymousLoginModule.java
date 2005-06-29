package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

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
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class AnonymousLoginModule extends AbstractLoginModule
{

    /**
     * Bogus prompt sent to the callback handler.
     */
    public static final String PROMPT            = "User name";

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
            m_principals.add( new WikiPrincipal( request.getRemoteAddr() ) );
            m_principals.add( Role.ANONYMOUS );
            m_principals.add( Role.ALL );
            return true;
        }
        catch( IOException e )
        {
            e.printStackTrace();
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