/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * A very fast authorizer that does nothing. The WebContainerAuthorizer module
 * is very slow, as it parses the web.xml each time, so we use this for most of
 * the different tests.
 * @author Janne Jalkanen
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-09-02 23:38:03 $
 * @since 2.3
 */
public class TestAuthorizer implements Authorizer
{
    private Role[] m_roles = new Role[]{ Role.ADMIN, Role.AUTHENTICATED };
    
    public TestAuthorizer()
    {
        super();
        // TODO Auto-generated constructor stub
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
     * Role.ADMIN and Role.AUTHENTICATED.
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
        WikiContext context = session.getLastContext();
        if ( context != null )
        {
            HttpServletRequest request = context.getHttpRequest();
            if ( request != null && request instanceof TestHttpServletRequest )
            {
                return ( (TestHttpServletRequest) request ).m_roles.contains( role.getName() );
            }
        }
        return false;
    }

}
