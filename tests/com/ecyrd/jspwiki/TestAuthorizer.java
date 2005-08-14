/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Properties;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.auth.Authorizer;

/**
 *  A very fast authorizer that does nothing.  The WebContainerAuthorizer
 *  module is very slow, as it parses the web.xml each time, so we use
 *  this for most of the different tests.
 *  
 *  @author jalkanen
 *
 *  @since
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

    public boolean isUserInRole( WikiContext context, Subject subject, Principal role )
    {
        return false;
    }

}
