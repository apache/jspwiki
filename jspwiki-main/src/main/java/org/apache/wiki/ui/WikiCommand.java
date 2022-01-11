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

import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.WikiPermission;

import java.security.Permission;

/**
 * <p>Defines Commands for wiki-wide operations such as creating groups, editing preferences and profiles, and logging in/out.
 * WikiCommands can be combined with Strings (representing the name of a wiki instance) to produce targeted Commands.</p>
 *
 * @see org.apache.wiki.api.core.Engine#getApplicationName()
 * @since 2.4.22
 */
public final class WikiCommand extends AbstractCommand {

    public static final Command ADMIN = new WikiCommand( ContextEnum.WIKI_ADMIN, null );
    public static final Command CREATE_GROUP = new WikiCommand( ContextEnum.WIKI_CREATE_GROUP, null, WikiPermission.CREATE_GROUPS_ACTION );
    public static final Command ERROR = new WikiCommand( ContextEnum.WIKI_ERROR, null, null );
    public static final Command FIND = new WikiCommand( ContextEnum.WIKI_FIND, null, null );
    public static final Command INSTALL = new WikiCommand( ContextEnum.WIKI_INSTALL, null, null );
    public static final Command LOGIN = new WikiCommand( ContextEnum.WIKI_LOGIN, null, WikiPermission.LOGIN_ACTION );
    public static final Command LOGOUT = new WikiCommand( ContextEnum.WIKI_LOGOUT, null, WikiPermission.LOGIN_ACTION );
    public static final Command MESSAGE = new WikiCommand( ContextEnum.WIKI_MESSAGE, null, null );
    public static final Command PREFS = new WikiCommand( ContextEnum.WIKI_PREFS, null, WikiPermission.EDIT_PROFILE_ACTION );
    public static final Command WORKFLOW = new WikiCommand( ContextEnum.WIKI_WORKFLOW, null, null );

    private final String m_action;
    
    private final Permission m_permission;

    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The WikiPage for this action is
     * initialized to <code>null</code>.
     *
     * @param currentContext the current context.
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private WikiCommand( final ContextEnum currentContext, final String target ) {
        this( currentContext.getRequestContext(), currentContext.getUrlPattern(), currentContext.getContentTemplate(), target );
    }

    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The WikiPage for this action is
     * initialized to <code>null</code>.
     *
     * @param currentContext the current context.
     * @param action The action
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private WikiCommand( final ContextEnum currentContext, final String target, final String action ) {
        this( currentContext.getRequestContext(), currentContext.getUrlPattern(), currentContext.getContentTemplate(), target, action );
    }
    
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
     * Constructs an admin command.
     *  
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param contentTemplate the content template; may be <code>null</code>
     * @param target the target of the command, such as a WikiPage; may be <code>null</code>
     * @throws IllegalArgumentException if the request content or URL pattern is <code>null</code>
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
    @Override
    public Command targetedCommand( final Object target ) {
        if ( !( target instanceof String ) ) {
            throw new IllegalArgumentException( "Target must non-null and of type String." );
        }
        return new WikiCommand( getRequestContext(), getURLPattern(), getContentTemplate(), (String)target, m_action );
    }
    
    /**
     * Always returns the "friendly" JSP name.
     *
     * @see org.apache.wiki.api.core.Command#getName()
     */
    @Override
    public String getName() {
        return getJSPFriendlyName();
    }

    /**
     * @see org.apache.wiki.api.core.Command#requiredPermission()
     */
    @Override
    public Permission requiredPermission() {
        return m_permission;
    }

}
