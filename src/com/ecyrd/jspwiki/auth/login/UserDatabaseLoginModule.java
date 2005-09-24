package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * <p>
 * Logs in a user based on a username, password, and static password file
 * location. This module must be used with a CallbackHandler (such as
 * {@link WikiCallbackHandler}) that supports the following Callback types:
 * </p>
 * <ol>
 * <li>{@link javax.security.auth.callback.NameCallback}- supplies the
 * username</li>
 * <li>{@link javax.security.auth.callback.PasswordCallback}- supplies the
 * password</li>
 * <li>{@link com.ecyrd.jspwiki.auth.login.UserDatabaseCallback}- supplies the
 * {@link com.ecyrd.jspwiki.auth.user.UserDatabase}</li>
 * </ol>
 * <p>
 * After authentication, Principals based on the login name, full name, and wiki
 * name will be created and associated with the Subject, as returned by
 * {@link com.ecyrd.jspwiki.auth.user.UserDatabase#getPrincipals(String)}.
 * Also, principals {@link com.ecyrd.jspwiki.auth.authorize.Role#ALL} and
 * {@link com.ecyrd.jspwiki.auth.authorize.Role#AUTHENTICATED} will be added to
 * the Subject's principal set.
 * </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-09-24 14:25:59 $
 * @since 2.3
 */
public class UserDatabaseLoginModule extends AbstractLoginModule
{

    private static final Class[] principalClasses = new Class[]
                                                  { WikiPrincipal.class };

    protected static Logger      log              = Logger.getLogger( UserDatabaseLoginModule.class );
    
    /**
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException
    {
        UserDatabaseCallback ucb = new UserDatabaseCallback();
        NameCallback ncb = new NameCallback( "User name" );
        PasswordCallback pcb = new PasswordCallback( "Password", false );
        Callback[] callbacks = new Callback[]
        { ucb, ncb, pcb };
        try
        {
            m_handler.handle( callbacks );
            UserDatabase db = ucb.getUserDatabase();
            String username = ncb.getName();
            String password = new String( pcb.getPassword() );

            // Look up the user and compare the password hash
            if ( db == null )
            {
                throw new FailedLoginException( "No user database: check the callback handler code!" );
            }
            UserProfile profile = db.findByLoginName( username );
            Principal[] principals = db.getPrincipals( username );
            String storedPassword = profile.getPassword();
            if ( storedPassword != null && db.validatePassword( username, password ) )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Logged in loginName=" + username );
                    log.debug( "Added Principals Role.AUTHENTICATED,Role.ALL" );
                }
                for( int i = 0; i < principals.length; i++ )
                {
                    m_principals.add( principals[i] );
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Added Principal " + principals[i].getName() );
                    }
                }
                m_principals.add( Role.AUTHENTICATED );
                m_principals.add( Role.ALL );
                return true;
            }
            throw new FailedLoginException( "The username or password is incorrect." );
        }
        catch( IOException e )
        {
            String message = "IO exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
        catch( NoSuchPrincipalException e )
        {
            throw new FailedLoginException( "The username or password is incorrect." );
        }
    }

}