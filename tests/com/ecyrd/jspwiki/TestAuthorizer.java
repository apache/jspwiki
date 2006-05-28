/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;
import java.util.Set;

import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * A very fast authorizer that does nothing. The WebContainerAuthorizer module
 * is very slow, as it parses the web.xml each time, so we use this for most of
 * the different tests.
 * @author Janne Jalkanen
 * @author Andrew R. Jaquith
 * @version $Revision: 1.5 $ $Date: 2006-05-28 23:26:47 $
 * @since 2.3
 */
public class TestAuthorizer implements Authorizer
{
    private Role[] m_roles = new Role[]{ new Role( "Admin"), Role.AUTHENTICATED };
    
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
     * Returns an array of Principal objects containing two elements:
     * Role "Admin" and Role.AUTHENTICATED.
     */
    public Principal[] getRoles()
    {
        return (Principal[])m_roles.clone();
    }
    
    /**
     * We have a special case if we're using the TestHttpServletRequest under
     * the covers, because we can actually make role decisions based on what's 
     * in the test-request. It's a cheap and nasty hack, but it's a test, right?
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        if ( session == null || role == null )
        {
            return false;
        }
        Set principals = session.getSubject().getPrincipals();
        return principals.contains( role );
    }

}
