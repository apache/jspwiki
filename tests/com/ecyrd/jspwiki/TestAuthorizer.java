/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * A very fast authorizer that does nothing. The WebContainerAuthorizer module
 * is very slow, as it parses the web.xml each time, so we use this for most of
 * the different tests.
 * @author Janne Jalkanen
 * @author Andrew R. Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-08-21 01:34:55 $
 * @since 2.3
 */
public class TestAuthorizer implements Authorizer
{
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
     * We have a special case if we're using the TestHttpServletRequest under
     * the covers, because we can actually make role decisions based on what's 
     * in the test-request. It's a cheap and nasty hack, but it's a test, right?
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role )
    {
        HttpServletRequest request = context.getHttpRequest();
        if ( request != null && request instanceof TestHttpServletRequest )
        {
            return ( (TestHttpServletRequest) request ).m_roles.contains( role.getName() );
        }
        return false;
    }

}
