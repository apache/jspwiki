package com.ecyrd.jspwiki.auth.login;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
 * {@link #logout()} should be sufficient for most purposes.
 * @author Andrew Jaquith
 * @version $Revision: 1.5 $ $Date: 2005-11-08 18:27:51 $
 * @since 2.3
 */
public abstract class AbstractLoginModule implements LoginModule
{

    protected static Logger   log = Logger.getLogger( AbstractLoginModule.class );

    protected CallbackHandler m_handler;

    protected Map             m_options;

    /**
     * Implementing classes should add Principals to this collection; these
     * will be added to the principal set when the overall login succeeds. 
     * These Principals will be added to the Subject
     * during the {@link #commit()} phase of login.
     */
    protected Collection      m_principals;

    /**
     * Implementing classes should add Principals to this collection
     * to specify what Principals <em>must</em> be removed if login for
     * this module, or for the entire login configuration overall, fails.
     * Generally, these will be Principals of type
     * {@link com.ecyrd.jspwiki.auth.authorize.Role}.
     */
    protected Collection      m_principalsToRemove;
    
    /**
     * Implementing classes should add Principals to this collection to specify
     * what Principals, perhaps suppled by other LoginModules, <em>must</em>
     * be removed if login for this module, or for the entire login
     * configuration overall, succeeds. Generally, these will be Principals of
     * type {@link com.ecyrd.jspwiki.auth.authorize.Role}. For example,
     * {@link CookieAssertionDatabaseLoginModule} adds {@link Role#ANONYMOUS} to
     * its <code>m_principalsToOverwrite</code> collection because when it
     * succeeds, its own {@link Role#AUTHENTICATED} should over-write
     * {@link Role#ANONYMOUS}.
     */
    protected Collection      m_principalsToOverwrite;
    
    protected Map             m_state;

    protected Subject         m_subject;

    protected static final String NULL           = "(null)";
    
    /**
     * Aborts the login; called if the LoginContext's overall authentication
     * failed. (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed). Specifically, it removes
     * Principals from the Subject that are associated with the
     * individual LoginModule; these will be those contained in
     * {@link #m_principalsToRemove}.
     * It always returns <code>true</code>.
     * @see javax.security.auth.spi.LoginModule#abort()
     * @throws LoginException if the abort itself fails
     */
    public final boolean abort() throws LoginException
    {
        removePrincipals( m_principals );
        removePrincipals( m_principalsToRemove );

        // Clear the principals/principalsToRemove sets
        m_principals.clear();
        m_principalsToRemove.clear();
        
        return true;
    }

    /**
     * Commits the login. If the overall login method succeeded, adds
     * principals to the Subject's set; generally, these will be the user's
     * actual Principal, plus one or more Role principals. The state of the
     * <code>m_principals</code> member variable is consulted to determine
     * whether to add the principals. If its size is 0 (because the login
     * failed), the login is considered to have failed; in this case,
     * all principals in {@link #m_principalsToRemove} are removed
     * from the Subject's set. Otherwise, the principals added to
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
    public final boolean commit() throws LoginException
    {
        if ( succeeded() )
        {
            for ( Iterator it = m_principals.iterator(); it.hasNext(); )
            {
                Principal principal = (Principal)it.next();
                m_subject.getPrincipals().add( principal );
                if ( log.isDebugEnabled() )
                {
                    log.debug("Committed Principal " + principal.getName() );
                }
            }
            removePrincipals( m_principalsToOverwrite );
            return true;
        }
        
        // If login did not succeed, clean up after ourselves
        removePrincipals( m_principals );
        removePrincipals( m_principalsToRemove );

        // Clear the principals/principalsToRemove sets
        m_principals.clear();
        m_principalsToRemove.clear();

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
    public final void initialize( Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options )
    {
        m_principals = new HashSet();
        m_principalsToRemove = new HashSet();
        m_principalsToOverwrite = new HashSet();
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
     * @throws LoginException if the authentication fails
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public abstract boolean login() throws LoginException;

    /**
     * Logs the user out. Removes all principals in {@link #m_principalsToRemove}
     * from the Subject's principal set.
     * @return <code>true</code> if the commit succeeded, or
     *         <code>false</code> if this LoginModule should be ignored
     * @throws LoginException if the logout itself fails
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public final boolean logout() throws LoginException
    {
        removePrincipals( m_principals );
        removePrincipals( m_principalsToRemove );

        // Clear the principals/principalsToRemove sets
        m_principals.clear();
        m_principalsToRemove.clear();
        
        return true;
    }

    /**
     * Returns <code>true</code> if the number of principals
     * contained in {@link #m_principals} is non-zero;
     * <code>false</code> otherwise.
     * @return
     */
    private boolean succeeded() {
        return ( m_principals.size() > 0 );
    }
    
    /**
     * Removes a specified collection of Principals from the Subject's
     * Principal set. 
     * @param principals the principals to remove
     */
    private void removePrincipals( Collection principals )
    {
        for ( Iterator it = principals.iterator(); it.hasNext(); )
        {
            Principal principal = (Principal)it.next();
            if ( m_subject.getPrincipals().contains( principal ) )
            {
                m_subject.getPrincipals().remove( principal );
                if ( log.isDebugEnabled() )
                {
                    log.debug("Removed Principal " + principal.getName() );
                }
            }
        }
    }
    
}