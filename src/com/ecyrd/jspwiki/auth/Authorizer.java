package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * Interface for service providers of authorization information.
 * @author Andrew Jaquith
 * @version $Revision: 1.6 $ $Date: 2006-07-29 19:44:33 $
 * @since 2.3
 */
public interface Authorizer
{

    /**
     * Returns an array of role Principals this Authorizer knows about. 
     * This method will always return an array; an implementing class may 
     * choose to return an zero-length array if it has no ability to identify
     * the roles under its control.
     * @return an array of Principals representing the roles
     */
    public Principal[] getRoles();
    
    /**
     * Looks up and returns a role Principal matching a given String.
     * If a matching role cannot be found, this method returns <code>null</code>.
     * Note that it may not always be feasible for an Authorizer 
     * implementation to return a role Principal.
     * @param role the name of the role to retrieve
     * @return the role Principal
     */
    public Principal findRole( String role );

    /**
     * Initializes the authorizer.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException;

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>.
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     * <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiSession session, Principal role );

}