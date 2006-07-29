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
 * @version $Revision: 1.5 $ $Date: 2006-07-29 19:21:51 $
 * @since 2.3
 */
public class AnonymousLoginModule extends AbstractLoginModule
{

    /**
     * Bogus prompt sent to the callback handler.
     */
    public static final String PROMPT            = "User name";

    protected final static Logger log            = Logger.getLogger( AnonymousLoginModule.class );
    
    /**
     * Logs in the user by calling back to the registered CallbackHandler with an
     * HttpRequestCallback. The CallbackHandler must supply the current servlet
     * HTTP request as its response.
     * @return the result of the login; this will always be <code>false</code>
     * if the Subject's Principal set already contains either
     * {@link Role#ASSERTED} or {@link Role#AUTHENTICATED}; otherwise,
     * always returns <code>true</code>.
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException
    {
        // If already logged in or asserted, ignore this login module
        if ( m_subject.getPrincipals().contains( Role.AUTHENTICATED ) 
             || m_subject.getPrincipals().contains( Role.ASSERTED ) )
        {
            // If login ignored, remove anonymous role
            m_principalsToRemove.add( Role.ANONYMOUS );
            return false;
        }
        
        // Otherwise, let's go and make a Principal based on the IP address
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
            // If login succeeds, commit these principals/roles
            m_principals.add( ipAddr );
            m_principals.add( Role.ANONYMOUS );
            m_principals.add( Role.ALL );
            
            // If login succeeds, overwrite these principals/roles
            m_principalsToOverwrite.add( WikiPrincipal.GUEST );
            
            // If login fails, remove these roles
            m_principalsToRemove.add( Role.ANONYMOUS );
            
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