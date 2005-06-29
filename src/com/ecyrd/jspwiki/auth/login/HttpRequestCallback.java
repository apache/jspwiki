package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;

/**
 * Callback for requesting and supplying a HttpServletRequest required by a
 * LoginModule. This Callback is used by LoginModules needing access to the
 * servlet request.
 * @link javax.servlet.http.HttpServletRequest#getUserPrincipal() or
 * @link javax.servlet.http.HttpServletRequest#getRemoteUser() methods.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class HttpRequestCallback implements Callback
{

    private HttpServletRequest m_request;

    /**
     * Sets the request object. CallbackHandler objects call this method..
     * @param request the servlet request
     */
    public void setRequest( HttpServletRequest request )
    {
        m_request = request;
    }

    /**
     * Returns the request object. LoginModules call this method after a
     * CallbackHandler sets the request.
     * @return the servlet request
     */
    public HttpServletRequest getRequest()
    {
        return m_request;
    }

}