package com.ecyrd.jspwiki.action;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 * @author Andrew Jaquith
 */
@WikiRequestContext("prefs")
@UrlBinding("/UserPreferences.action")
public class UserPreferencesActionBean extends AbstractActionBean
{
    private String m_assertedName = null;

    /**
     * Clears the user's asserted name by removing the cookie from the user's
     * session, then logs out the user by redirecting to <code>/Login.jsp</code>.
     * 
     * @return a redirection to the logout page
     */
    @HandlesEvent("clearAssertedName")
    @EventPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.EDIT_PREFERENCES_ACTION)
    public Resolution clearAssertedName()
    {
        HttpServletResponse response = getContext().getResponse();
        CookieAssertionLoginModule.clearUserCookie(response);
        return new RedirectResolution("/Logout.jsp");
    }

    /**
     * Redirects the user to their favorites page.
     * 
     * @return a redirection to the favorites page
     */
    @HandlesEvent("editFavorites")
    public Resolution editFavorites()
    {
        Principal principal = this.getCurrentUser();
        return new RedirectResolution("/Edit.jsp?"+principal.getName()+"Favorites");
    }
    
    /**
     * Sets the user's asserted name by setting a cookie in the user's session,
     * then redirects to the wiki front page. This method will <em>not</em>
     * set the cookie if the user is already authenticated.
     * 
     * @return a redirection to the front page
     */
    @DefaultHandler
    @HandlesEvent("createAssertedName")
    @EventPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.EDIT_PREFERENCES_ACTION)
    public Resolution createAssertedName()
    {
        if ( !getWikiSession().isAuthenticated() )
        {
            HttpServletRequest request = getContext().getRequest();
            HttpServletResponse response = getContext().getResponse();
            String assertedName = request.getParameter("assertedName");
            CookieAssertionLoginModule.setUserCookie(response, assertedName);
        }
        return new RedirectResolution("/");
    }

    /**
     * Returns the asserted name for the user prefererences.
     * 
     * @return the asserted name
     */
    public String getAssertedName()
    {
        return m_assertedName;
    }

    /**
     * Sets the asserted name for the user prefererences.
     * 
     * @param name
     *            the asserted name
     */
    @Validate(required=true, on="createAssertedName")
    public void setAssertedName(String name)
    {
        m_assertedName = name;
    }

}
