/* 
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
package org.apache.wiki.ui;

import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.WikiPermission;

import java.security.Permission;

/**
 * <p>Defines Commands for wiki-wide operations such as creating groups, editing preferences and profiles, and logging in/out.
 * WikiCommands can be combined with Strings (representing the name of a wiki instance) to produce targeted Commands.</p>
 *
 * @see org.apache.wiki.WikiEngine#getApplicationName()
 * @since 2.4.22
 */
public final class WikiCommand extends AbstractCommand {

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
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The WikiPage for this action is
     * initialized to <code>null</code>.
     *
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param contentTemplate the content template; may be <code>null</code>
     * @param action The action
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private WikiCommand( final String requestContext,
                         final String urlPattern,
                         final String contentTemplate,
                         final String target,
                         final String action ) {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if ( target == null || m_action == null ) {
            m_permission = null;
        } else {
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
    private WikiCommand( final String requestContext, final String urlPattern, final String contentTemplate, final String target ) {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = null;
        m_permission = new AllPermission( target );
    }

    /**
     * Creates and returns a targeted Command by combining a wiki (a String) with this Command. The supplied <code>target</code>
     * object must be non-<code>null</code> and of type String.
     *
     * @param target the name of the wiki to combine into the current Command
     * @return the new targeted command
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public Command targetedCommand( final Object target ) {
        if ( !( target instanceof String ) ) {
            throw new IllegalArgumentException( "Target must non-null and of type String." );
        }
        return new WikiCommand( getRequestContext(), getURLPattern(), getContentTemplate(), (String)target, m_action );
    }
    
    /**
     * Always returns the "friendly" JSP name.
     *
     * @see org.apache.wiki.ui.Command#getName()
     */
    public String getName() {
        return getJSPFriendlyName();
    }

    /**
     * @see org.apache.wiki.ui.Command#requiredPermission()
     */
    public Permission requiredPermission() {
        return m_permission;
    }

}
