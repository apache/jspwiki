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
import org.apache.wiki.api.core.Page;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;

import java.security.Permission;

/**
 * <p>Defines Commands for editing, renaming, and viewing wiki pages. PageCommands can be combined with WikiPages to produce
 * targeted Commands.</p>
 *
 * @since 2.4.22
 */
public final class PageCommand extends AbstractCommand {

    public static final Command ATTACH = new PageCommand( ContextEnum.PAGE_ATTACH, null, PagePermission.UPLOAD_ACTION );
    public static final Command COMMENT = new PageCommand( ContextEnum.PAGE_COMMENT, null, PagePermission.COMMENT_ACTION );
    public static final Command CONFLICT = new PageCommand( ContextEnum.PAGE_CONFLICT, null, PagePermission.VIEW_ACTION );
    public static final Command DELETE = new PageCommand( ContextEnum.PAGE_DELETE, null, PagePermission.DELETE_ACTION );
    public static final Command DIFF = new PageCommand( ContextEnum.PAGE_DIFF, null, PagePermission.VIEW_ACTION );
    public static final Command EDIT = new PageCommand( ContextEnum.PAGE_EDIT, null, PagePermission.EDIT_ACTION );
    public static final Command INFO = new PageCommand( ContextEnum.PAGE_INFO, null, PagePermission.VIEW_ACTION );
    public static final Command PREVIEW = new PageCommand( ContextEnum.PAGE_PREVIEW, null, PagePermission.VIEW_ACTION );
    public static final Command RENAME = new PageCommand( ContextEnum.PAGE_RENAME, null, PagePermission.RENAME_ACTION );
    public static final Command RSS = new PageCommand( ContextEnum.PAGE_RSS, null, PagePermission.VIEW_ACTION );
    public static final Command UPLOAD = new PageCommand( ContextEnum.PAGE_UPLOAD, null, PagePermission.UPLOAD_ACTION );
    public static final Command VIEW = new PageCommand( ContextEnum.PAGE_VIEW, null, PagePermission.VIEW_ACTION );
    public static final Command NONE = new PageCommand( ContextEnum.PAGE_NONE, null, null );
    public static final Command OTHER = NONE;

    private final String m_action;
    
    private final Permission m_permission;

    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The target for this command is
     * initialized to <code>null</code>.
     *
     * @param currentContext the current context.
     * @param target the target of the command (a WikiPage); may be <code>null</code>
     * @param action the action used to construct a suitable PagePermission
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private PageCommand( final ContextEnum currentContext, final Page target, final String action ) {
        this( currentContext.getRequestContext(), currentContext.getUrlPattern(), currentContext.getContentTemplate(), target, action );
    }
    
    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The target for this command is
     * initialized to <code>null</code>.
     *
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param target the target of the command (a WikiPage); may be <code>null</code>
     * @param action the action used to construct a suitable PagePermission
     * @param contentTemplate the content template; may be <code>null</code>
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private PageCommand( final String requestContext,
                         final String urlPattern,
                         final String contentTemplate,
                         final Page target,
                         final String action ) {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if( target == null || m_action == null ) {
            m_permission = null;
        } else {
            m_permission = PermissionFactory.getPagePermission( target, action );
        }
    }

    /**
     * Creates and returns a targeted Command by combining a WikiPage with this Command. The supplied <code>target</code> object
     * must be non-<code>null</code> and of type WikiPage.
     *
     * @param target the WikiPage to combine into the current Command
     * @return the new targeted command
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public Command targetedCommand( final Object target ) {
        if( !( target instanceof Page ) ) {
            throw new IllegalArgumentException( "Target must non-null and of type Page." );
        }
        return new PageCommand( getRequestContext(), getURLPattern(), getContentTemplate(), ( Page )target, m_action );
    }

    /**
     * @see org.apache.wiki.api.core.Command#getName()
     */
    public String getName() {
        final Object target = getTarget();
        if( target == null ) {
            return getJSPFriendlyName();
        }
        return ( ( Page )target ).getName();
    }

    /**
     * @see org.apache.wiki.api.core.Command#requiredPermission()
     */
    public Permission requiredPermission() {
        return m_permission;
    }

}
