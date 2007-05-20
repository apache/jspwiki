/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Callback for requesting and supplying an Authorizer required by a
 * LoginModule. This Callback is used by LoginModules needing access to the
 * external authorizer or group manager.
 * @author Andrew Jaquith
 * @since 2.3
 */
public class AuthorizerCallback implements Callback
{

    private Authorizer m_authorizer;

    /**
     * Sets the authorizer object. CallbackHandler objects call this method.
     * @param authorizer the authorizer
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