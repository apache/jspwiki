/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.ui;

import java.security.Permission;

import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 * <p>Defines Commands for wiki-wide operations such as creating groups, editing
 * preferences and profiles, and logging in/out. WikiCommands can be combined 
 * with Strings (representing the name of a wiki instance) to produce
 * targeted Commands.</p>
 * <p>This class is not <code>final</code>; it may be extended in
 * the future.</p>
 * @see com.ecyrd.jspwiki.WikiEngine#getApplicationName().
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public final class WikiCommand extends AbstractCommand
{

    public static final Command CREATE_GROUP
        = new WikiCommand( "createGroup", "%uNewGroup.jsp", "NewGroupContent.jsp", null, WikiPermission.CREATE_GROUPS_ACTION );

    public static final Command ERROR
        = new WikiCommand( "error", "%uError.jsp", "DisplayMessage.jsp", null, null );

    public static final Command FIND
        = new WikiCommand( "find", "%uSearch.jsp", "FindContent.jsp", null, null );

    public static final Command INSTALL
        = new WikiCommand( "install", "%uInstall.jsp", null, null, null );

    public static final Command LOGIN
        = new WikiCommand( "login", "%uLogin.jsp?redirect=%n", "LoginContent.jsp", null, WikiPermission.LOGIN_ACTION );

    public static final Command LOGOUT
        = new WikiCommand( "logout", "%uLogout.jsp", null, null, WikiPermission.LOGIN_ACTION );

    public static final Command MESSAGE
        = new WikiCommand( "message", "%uMessage.jsp", "DisplayMessage.jsp", null, null );
    
    public static final Command PREFS
        = new WikiCommand( "prefs", "%uUserPreferences.jsp", "PreferencesContent.jsp", null, WikiPermission.EDIT_PROFILE_ACTION );

    public static final Command WORKFLOW
        = new WikiCommand( "workflow", "%uWorkflow.jsp", "WorkflowContent.jsp", null, null );

    public static final Command ADMIN
        = new WikiCommand( "admin", "%uadmin/Admin.jsp", "AdminContent.jsp", null );

    private final String m_action;
    
    private final Permission m_permission;
    
    /**
     * Constructs a new Command with a specified wiki context, URL pattern,
     * type, and content template. The WikiPage for this action is initialized
     * to <code>null</code>.
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param type the type
     * @param contentTemplate the content template; may be <code>null</code>
     * @param action The action
     * @return IllegalArgumentException if the request content, URL pattern, or
     *         type is <code>null</code>
     */
    private WikiCommand( String requestContext, 
                         String urlPattern, 
                         String contentTemplate, 
                         String target, 
                         String action )
    {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if ( target == null || m_action == null )
        {
            m_permission = null;
        }
        else
        {
            m_permission = new WikiPermission( target, action );
        }
    }

    /**
     *  Constructs an admin command.
     *  
     *  @param requestContext
     *  @param urlPattern
     *  @param contentTemplate
     */
    private WikiCommand( String requestContext, 
                         String urlPattern, 
                         String contentTemplate,
                         String target )
    {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = null;

        m_permission = new AllPermission( target );
    }
    /**
     * Creates and returns a targeted Command by combining a wiki
     * (a String) with this Command. The supplied <code>target</code>
     * object must be non-<code>null</code> and of type String.
     * @param target the name of the wiki to combine into the current Command
     * @return the new targeted command
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public final Command targetedCommand( Object target )
    {
        if ( !( target != null && target instanceof String ) )
        {
            throw new IllegalArgumentException( "Target must non-null and of type String." );
        }
        return new WikiCommand( getRequestContext(), getURLPattern(), getContentTemplate(), (String)target, m_action );
    }
    
    /**
     * Always returns the "friendly" JSP name.
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public final String getName()
    {
        return getJSPFriendlyName();
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return m_permission;
    }
}
