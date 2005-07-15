package com.ecyrd.jspwiki.auth.login;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.log4j.Logger;

/**
 * Abstract JAAS {@link javax.security.auth.spi.LoginModule}that implements
 * base functionality. The methods {@link #login()} and {@link #commit()} must
 * be implemented by subclasses. The default implementations of
 * {@link #initialize(Subject, CallbackHandler, Map, Map)}, {@link #abort()} and
 * {@link #logout()} should be sufficient for most purposes, but can be safely
 * over-ridden.
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-07-15 08:27:21 $
 * @since 2.3
 */
public abstract class AbstractLoginModule implements LoginModule
{

    protected static Logger   log = Logger.getLogger( AbstractLoginModule.class );

    protected CallbackHandler m_handler;

    protected Map             m_options;

    protected Collection      m_principals;

    protected Map             m_state;

    protected Subject         m_subject;

    /**
     * Aborts the login; always returns <code>true</code>.
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException
    {
        m_principals = new HashSet();
        m_subject = null;
        m_subject = null;
        m_handler = null;
        m_state = null;
        m_options = null;
        return true;
    }

    /**
     * Commits the login. If the {@link #login()} method succeeded, adds
     * principals to the Subject's set; generally, these will be the user's
     * actual Principal, plus one or more Role principals. The state of the
     * <code>m_principals</code> member variable is consulted to determine
     * whether to add the principals. If its size is 0 (because the login
     * failed), no principals are added. Otherwise, the principals added to
     * <code>m_principals</code> in the {@link #login()} method are added to
     * the Subject's set.
     * @return <code>true</code> if the commit succeeded, or
     *         <code>false</code> if the previous call to {@link #login()}
     *         failed
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException
    {
        if ( m_principals.size() > 0 )
        {
            m_subject.getPrincipals().addAll( m_principals );
            return true;
        }

        return false;
    }

    /**
     * Initializes the LoginModule with a given <code>Subject</code>,
     * callback handler, options and shared state. In particular, the member
     * variable <code>m_principals</code> is initialized as a blank Set.
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject,
     *      javax.security.auth.callback.CallbackHandler, java.util.Map,
     *      java.util.Map)
     */
    public void initialize( Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options )
    {
        m_principals = new HashSet();
        m_subject = subject;
        m_handler = callbackHandler;
        m_state = sharedState;
        m_options = options;
        if ( subject == null )
        {
            throw new IllegalStateException( "Subject cannot be null" );
        }
        if ( callbackHandler == null )
        {
            throw new IllegalStateException( "Callback handler cannot be null" );
        }
    }

    /**
     * Logs in the user by calling back to the registered CallbackHandler with a
     * series of callbacks. If the login succeeds, this method returns
     * <code>true</code>
     * @return <code>true</code> if the commit succeeded, or
     *         <code>false</code> if this LoginModule should be ignored.
     *         Implementing classes should set the member variable
     *         <code>m_loginSucceeded</code> to indicate whether the login for
     *         this module succeeded or failed.
     * @throws LoginException if the authentication fails
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public abstract boolean login() throws LoginException;

    /**
     * Logs the user out. Removes all principals from the Subject's principal
     * set.
     * @return <code>true</code> if the commit succeeded, or
     *         <code>false</code> if this LoginModule should be ignored
     * @throws LoginException if the authentication fails
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException
    {
        if ( m_subject != null )
        {
            m_subject.getPrincipals().clear();
        }
        return true;
    }

}