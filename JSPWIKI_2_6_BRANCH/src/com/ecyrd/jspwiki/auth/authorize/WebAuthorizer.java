/*
 * JSPWiki - a JSP-based WikiWiki clone. Copyright (C) 2001-2003 Janne Jalkanen
 * (Janne.Jalkanen@iki.fi) This program is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Extends the {@link com.ecyrd.jspwiki.auth.Authorizer} interface by
 * including a delgate method for 
 * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}.
 * @author Andrew Jaquith
 */
public interface WebAuthorizer extends Authorizer
{
    
    /**
     * Determines whether a user associated with an HTTP request possesses
     * a particular role. This method simply delegates to 
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}
     * by converting the Principal's name to a String.
     * @param request the HTTP request
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    public boolean isUserInRole( HttpServletRequest request, Principal role );
    
}
