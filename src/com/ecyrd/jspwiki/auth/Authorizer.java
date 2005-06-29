package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Interface for service providers of authorization information.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public interface Authorizer
{

    /**
     * Looks up and returns a role principal matching a given string.
     * If a matching role cannot be found, this method returns null.
     * Note that it may not be feasible for an Authorizer implementation
     * to return a role principal.
     * @param role the name of the role to retrieve
     * @return the role principal
     */
    public Principal findRole( String role );

    /**
     * Determines whether the user represented by a supplied Subject is in a
     * particular role. This method takes three parameters. Context may be null;
     * however, if a Authorizer implementation requires it (e.g.,
     * WebContainerAuthorizer), this method must return false.
     * @param context the current WikiContext
     * @param subject the current subject
     * @param role the role to check
     * @return <code>true</code> if the user is considered in the role,
     *         <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role );

}