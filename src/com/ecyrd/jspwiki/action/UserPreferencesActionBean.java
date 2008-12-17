package com.ecyrd.jspwiki.action;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.ui.EditorManager;
import com.ecyrd.jspwiki.ui.TemplateManager;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

/**
 * @author Andrew Jaquith
 */
@UrlBinding( "/UserPreferences.action" )
public class UserPreferencesActionBean extends AbstractActionBean
{
    private Logger log = LoggerFactory.getLogger( "JSPWiki" );

    private String m_assertedName = null;

    private String m_editor = null;

    private String m_redirect = null;

    /**
     * Clears the user's asserted name by removing the cookie from the user's
     * session, then logs out the user by redirecting to <code>/Login.jsp</code>.
     * 
     * @return a redirection to the logout page
     */
    @HandlesEvent( "clearAssertedName" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PREFERENCES_ACTION )
    public Resolution clearAssertedName()
    {
        HttpServletResponse response = getContext().getResponse();
        CookieAssertionLoginModule.clearUserCookie( response );
        return new RedirectResolution( "/Logout.jsp" );
    }

    /**
     * Redirects the user to their favorites page.
     * 
     * @return a redirection to the favorites page
     */
    @HandlesEvent( "editFavorites" )
    @WikiRequestContext( "favorites" )
    public Resolution editFavorites()
    {
        Principal principal = getContext().getCurrentUser();
        return new RedirectResolution( "/Edit.jsp?" + principal.getName() + "Favorites" );
    }

    /**
     * Sets the user's asserted name by setting a cookie in the user's session,
     * then redirects to the wiki front page. This method will <em>not</em>
     * set the cookie if the user is already authenticated.
     * 
     * @return a redirection to the front page
     */
    @HandlesEvent( "createAssertedName" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PREFERENCES_ACTION )
    public Resolution createAssertedName()
    {
        // FIXME: should reload preferences (see Preferences.reloadPreferences)
        if( !getContext().getWikiSession().isAuthenticated() )
        {
            HttpServletRequest request = getContext().getRequest();
            HttpServletResponse response = getContext().getResponse();
            String assertedName = request.getParameter( "assertedName" );
            CookieAssertionLoginModule.setUserCookie( response, assertedName );
        }
        if( m_redirect != null )
        {
            RedirectResolution r = new RedirectResolution( ViewActionBean.class );
            r.addParameter( "page", m_redirect );
            log.info( "Redirecting user to wiki page " + m_redirect );
            return r;
        }
        return new RedirectResolution( "/" );
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
     * @param name the asserted name
     */
    @Validate( required = true, on = "createAssertedName", minlength=1, maxlength=128 )
    public void setAssertedName( String name )
    {
        m_assertedName = name;
    }

    /**
     * Returns the editor specified by the user.
     * 
     * @return the editor
     */
    public String getEditor()
    {
        return m_editor;
    }

    /**
     * Sets the editor for editing preferences. If this
     * UserPreferencesActionBean has an associated WikiActionBeanContext, and
     * that context's request is not <code>null</code>, this method also sets
     * the editor as an HTTP session attribute.
     * <em>Note: this functionality was taken directly from the 2.6 UserPreferences.jsp.</em>
     * 
     * @param editor the editor
     */
    @Validate()
    public void setEditor( String editor )
    {
        m_editor = editor;
        if( getContext().getRequest() != null )
        {
            HttpSession session = getContext().getRequest().getSession();
            session.setAttribute( EditorManager.PARA_EDITOR, editor );
        }
    }

    /**
     * Sets the URL to redirect to after the event handler methods fire.
     * 
     * @param url the URL to redirect to
     */
    @Validate()
    public void setRedirect( String url )
    {
        m_redirect = url;
    }

    /**
     * Returns the URL to redirect to after the event handler methods fire.
     * 
     * @return the URL to redirect to
     */
    public String getRedirect()
    {
        return m_redirect;
    }

    /**
     * Handler for displaying user preferences that simply forwards to the
     * preferences display JSP <code>PreferencesContent.jsp</code>.
     * 
     * @return a forward to the content template
     */
    @DefaultHandler
    @HandlesEvent("prefs")
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PREFERENCES_ACTION )
    @WikiRequestContext( "prefs" )
    public Resolution view()
    {
        WikiActionBeanContext context = getContext();
        TemplateManager.addResourceRequest( context, "script", "scripts/jspwiki-prefs.js" );
        return new ForwardResolution( "/UserPreferences.jsp" );
    }
}
