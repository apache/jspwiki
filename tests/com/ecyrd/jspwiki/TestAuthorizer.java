/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.authorize.WebAuthorizer;

/**
 * A very fast authorizer that does almost nothing. The WebContainerAuthorizer module
 * is very slow, as it parses the web.xml each time, so we use this for most of
 * the different tests.
 * @author Janne Jalkanen
 * @author Andrew R. Jaquith
 * @version $Revision: 1.6 $ $Date: 2006-07-29 19:23:14 $
 * @since 2.3
 */
public class TestAuthorizer implements WebAuthorizer
{
    private Role[] m_roles = new Role[]{ 
            new Role( "Admin" ), 
            Role.AUTHENTICATED,
            new Role( "IT" ),
            new Role( "Finance" ),
            new Role( "Engineering" ) };
    
    public TestAuthorizer()
    {
        super();
    }

    public Principal findRole( String role )
    {
        return null;
    }

    public void initialize( WikiEngine engine, Properties props )
    {
    }

    /**
     * Returns an array of Principal objects containing five elements:
     * Role "Admin", Role.AUTHENTICATED, Role "IT", Role "Finance" and 
     * Role "Engineering."
     */
    public Principal[] getRoles()
    {
        return (Principal[])m_roles.clone();
    }
    
    /**
     * Returns <code>true</code> if the WikiSession's Subject contains 
     * a particular role principal.
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        if ( session == null || role == null )
        {
            return false;
        }
        
        return session.hasPrincipal( role );
    }

    /**
     * Returns <code>true</code> if the HTTP request contains 
     * a particular role principal. Delegates to
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}.
     * @see com.ecyrd.jspwiki.auth.authorize.WebAuthorizer#isUserInRole(javax.servlet.http.HttpServletRequest, java.security.Principal)
     */
    public boolean isUserInRole( HttpServletRequest request, Principal role )
    {
        return request.isUserInRole( role.getName() );
    }

}
