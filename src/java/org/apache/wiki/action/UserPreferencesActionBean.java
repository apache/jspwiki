/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.action;

import java.security.Principal;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.WikiSession;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.ui.EditorManager;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.TemplateResolution;
import org.apache.wiki.ui.stripes.WikiRequestContext;


/**
 */
@UrlBinding( "/UserPreferences.jsp" )
public class UserPreferencesActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( "JSPWiki" );

    private String m_assertedName = null;

    private String m_editor = null;

    private String m_redirect = null;

    private Locale m_locale = null;

    private Preferences.Orientation m_orientation = null;

    private String m_timeFormat = null;

    private TimeZone m_timeZone = null;

    private boolean m_showQuickLinks = false;

    private boolean m_sectionEditing = false;

    private String m_skin = null;

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
        return new RedirectResolution( LoginActionBean.class, "logout" );
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
        return new RedirectResolution( EditActionBean.class ).addParameter( "page", principal.getName() + "Favorites" );
    }

    /**
     * Returns the asserted name for the user preferences.
     * 
     * @return the asserted name
     */
    public String getAssertedName()
    {
        return m_assertedName;
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

    public Locale getLocale()
    {
        return m_locale;
    }

    public Preferences.Orientation getOrientation()
    {
        return m_orientation;
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

    public String getSkin()
    {
        return m_skin;
    }

    public String getTimeFormat()
    {
        return m_timeFormat;
    }

    public TimeZone getTimeZone()
    {
        return m_timeZone;
    }

    /**
     * Pre-action that loads the available set of skins, languages, time formats
     * and time zones based on the current template returned by
     * {@link org.apache.wiki.ui.stripes.WikiActionBeanContext#getTemplate()}.
     * It also pre-populates the form values with the preferences stored in the
     * session attribute and retrieved via
     * {@link org.apache.wiki.preferences.Preferences#getPreferences(HttpServletRequest)}.
     * 
     * @return always returns <code>null</code>
     */
    @Before( stages = LifecycleStage.BindingAndValidation )
    public Resolution initPreferenceOptions()
    {
        // Load the asserted name
        WikiSession wikiSession = getContext().getWikiSession();
        if ( wikiSession.isAsserted() )
        {
            m_assertedName = wikiSession.getUserPrincipal().getName();
        }

        // Load preferences
        HttpServletRequest request = getContext().getRequest();
        Preferences prefs = Preferences.getPreferences( request );
        m_editor = (String)prefs.get( Preferences.PREFS_EDITOR );
        m_locale = (Locale)prefs.get( Preferences.PREFS_LOCALE );
        m_orientation = (Preferences.Orientation)prefs.get( Preferences.PREFS_ORIENTATION );
        m_sectionEditing = (Boolean)prefs.get( Preferences.PREFS_SECTION_EDITING );
        m_skin = (String)prefs.get( Preferences.PREFS_SKIN );
        m_timeFormat = (String)prefs.get( Preferences.PREFS_TIME_FORMAT );
        m_timeZone = (TimeZone)prefs.get( Preferences.PREFS_TIME_ZONE );
        return null;
    }

    public boolean isSectionEditing()
    {
        return m_sectionEditing;
    }

    public boolean isShowQuickLinks()
    {
        return m_showQuickLinks;
    }

    /**
     * Saves the user's currently selected preferences to cookies, and to the
     * Preferences map associated with the user's session. It then redirects
     * to the wiki front page. Also sets the user's asserted name by setting
     * if the user is already authenticated.
     * 
     * @return resolution redirecting user to the display JSP for this ActionBean
     */
    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PREFERENCES_ACTION )
    public Resolution save() throws WikiException
    {
        HttpServletRequest request = getContext().getRequest();
        HttpServletResponse response = getContext().getResponse();
        
        if( !getContext().getWikiSession().isAuthenticated() )
        {
            CookieAssertionLoginModule.setUserCookie( response, m_assertedName );
        }
        Preferences prefs = Preferences.getPreferences( request );
        prefs.put( Preferences.PREFS_EDITOR, m_editor );
        prefs.put( Preferences.PREFS_LOCALE, m_locale );
        prefs.put( Preferences.PREFS_ORIENTATION, m_orientation );
        prefs.put( Preferences.PREFS_SECTION_EDITING, m_sectionEditing );
        prefs.put( Preferences.PREFS_SKIN, m_skin );
        prefs.put( Preferences.PREFS_TIME_FORMAT, m_timeFormat );
        prefs.put( Preferences.PREFS_TIME_ZONE, m_timeZone );
        prefs.save( getContext() );
        
        // Redirect user
        if( m_redirect != null )
        {
            log.info( "Redirecting user to wiki page " + m_redirect );
            return new RedirectResolution( ViewActionBean.class ).addParameter( "page", m_redirect );
        }
        return new RedirectResolution( ViewActionBean.class );
    }

    /**
     * Sets the asserted name for the user preferences.
     * 
     * @param name the asserted name
     */
    @Validate( required = false, on = "save", minlength = 1, maxlength = 128 )
    public void setAssertedName( String name )
    {
        m_assertedName = name;
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
    @Validate( required = true, on = "save" )
    public void setEditor( String editor )
    {
        m_editor = editor;
        if( getContext().getRequest() != null )
        {
            HttpSession session = getContext().getRequest().getSession();
            session.setAttribute( EditorManager.PARA_EDITOR, editor );
        }
    }

    @Validate( required = true, on = "save" )
    public void setLocale( Locale locale )
    {
        m_locale = locale;
    }

    @Validate( required = true, on = "save" )
    public void setOrientation( Preferences.Orientation orientation )
    {
        m_orientation = orientation;
    }

    /**
     * Sets the URL to redirect to after the event handler methods fire.
     * 
     * @param url the URL to redirect to
     */
    @Validate( required = false )
    public void setRedirect( String url )
    {
        m_redirect = url;
    }

    @Validate( required = false, on = "save" )
    public void setSectionEditing( boolean sectionEditing )
    {
        m_sectionEditing = sectionEditing;
    }

    @Validate( required = false )
    public void setShowQuickLinks( boolean showQuickLinks )
    {
        m_showQuickLinks = showQuickLinks;
    }

    @Validate( required = true, on = "save" )
    public void setSkin( String skin )
    {
        m_skin = skin;
    }

    @Validate( required = true, on = "save" )
    public void setTimeFormat( String timeFormat )
    {
        m_timeFormat = timeFormat;
    }

    @Validate( required = true, on = "save" )
    public void setTimeZone( TimeZone timeZone )
    {
        m_timeZone = timeZone;
    }

    /**
     * Handler for displaying user preferences that simply forwards to the
     * preferences template JSP <code>PreferencesContent.jsp</code>.
     * 
     * @return a forward to the content template
     */
    @DefaultHandler
    @HandlesEvent( "prefs" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.EDIT_PREFERENCES_ACTION )
    @WikiRequestContext( "prefs" )
    public Resolution view()
    {
        return new TemplateResolution( "Preferences.jsp" ).addParameter( "tab", "prefs" );
    }
}
