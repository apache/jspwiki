package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Authorizes users by delegating role membership checks to the servlet
 * container.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class WebContainerAuthorizer implements Authorizer
{
    // TODO: the CONTAINER_ROLES field is an outrageous hack
    // This should really be initialized by the wiki engine from properties
    // or from parsing the web.xml (yuck)

    /**
     * A fixed set of Roles that the container knows about.
     */
    protected static final Role[] CONTAINER_ROLES = new Role[]
                                     { Role.ADMIN, Role.AUTHENTICATED };

    /**
     * Returns <code>true</code> if the
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)}
     * method also returns <code>true</code>. The servlet request object is
     * obtained by calling
     * {@link com.ecyrd.jspwiki.WikiContext#getHttpRequest()}. If
     * <code>context</code> or <code>group</code> are <code>null</code>,
     * this method will return <code>false</code>.
     * @param context the current wiki context
     * @param subject the subject to check. In this implementation, the
     *            value of this parameter has no effect on the result, and may
     *            be <code>null</code>.
     * @param role the wiki group (role) being tested for
     * @see com.ecyrd.jspwiki.auth.Authorizer#isUserInRole(WikiContext, Subject, Principal)
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role )
    {
        if ( context == null || role == null )
        {
            return false;
        }
        HttpServletRequest request = context.getHttpRequest();
        if (request == null) {
            return false;
        }
        return request.isUserInRole( role.getName() );
    }

    /**
     * Looks up and returns a role principal matching a given string. If the
     * role does not match one of the container roles (see
     * {@link #CONTAINER_ROLES}), this method returns null.
     * @param role the name of the role to retrieve
     * @return a Role principal, or null
     * @see com.ecyrd.jspwiki.auth.Authorizer#findRole(java.lang.String)
     */
    public Principal findRole( String role )
    {
        for( int i = 0; i < CONTAINER_ROLES.length; i++ )
        {
            if ( CONTAINER_ROLES[i].getName().equals( role ) )
            {
                return CONTAINER_ROLES[i];
            }
        }
        return null;
    }
}