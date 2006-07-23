package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Extends the {@link com.ecyrd.jspwiki.auth.Authorizer} interface by
 * including a delgate method for 
 * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-07-23 20:05:07 $
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