package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Callback for requesting and supplying an Authorizer required by a
 * LoginModule. This Callback is used by LoginModules needing access to the
 * external authorizer or group manager.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-05-28 23:25:07 $
 * @since 2.3
 */
public class AuthorizerCallback implements Callback
{

    private Authorizer m_authorizer;

    /**
     * Sets the authorizer object. CallbackHandler objects call this method.
     * @param request the authorizer
     */
    public void setAuthorizer( Authorizer authorizer )
    {
        m_authorizer = authorizer;
    }

    /**
     * Returns the authorizer. LoginModules call this method after a
     * CallbackHandler sets the authorizer.
     * @return the authorizer
     */
    public Authorizer getAuthorizer()
    {
        return m_authorizer;
    }

}