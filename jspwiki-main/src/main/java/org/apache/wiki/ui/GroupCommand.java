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

import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.permissions.GroupPermission;

import java.security.Permission;

/**
 * <p>Defines Commands for viewing, editing and deleting wiki groups. GroupCommands can be combined with GroupPrincipals to produce
 * targeted Commands.</p>
 * <p>This class is not <code>final</code>; it may be extended in the future.</p>
 * @since 2.4.22
 */
public final class GroupCommand extends AbstractCommand {

    /** GroupCommand for deleting a group. */
    public static final Command DELETE_GROUP = new GroupCommand( "deleteGroup",
                                                       "%uDeleteGroup.jsp?group=%n",
                                                  null,
                                                          null,
                                                                 GroupPermission.DELETE_ACTION );

    /** GroupCommand for editing a group. */
       public static final Command EDIT_GROUP = new GroupCommand( "editGroup",
                                                        "%uEditGroup.jsp?group=%n",
                                                   "EditGroupContent.jsp",
                                                            null,
                                                                 GroupPermission.EDIT_ACTION );

       /** GroupCommand for viewing a group. */
    public static final Command VIEW_GROUP = new GroupCommand( "viewGroup",
                                                     "%uGroup.jsp?group=%n",
                                                "GroupContent.jsp",
                                                        null,
                                                               GroupPermission.VIEW_ACTION );

    private final String m_action;
    
    private final Permission m_permission;
    
    /**
     * Constructs a new Command with a specified wiki context, URL pattern, type, and content template. The WikiPage for this command is
     * initialized to <code>null</code>.
     *
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param target the target of this command (a GroupPrincipal representing a Group); may be <code>null</code>
     * @param action the action used to construct a suitable GroupPermission
     * @param contentTemplate the content template; may be <code>null</code>
     * @throws IllegalArgumentException if the request content, URL pattern, or type is <code>null</code>
     */
    private GroupCommand( final String requestContext,
                          final String urlPattern,
                          final String contentTemplate,
                          final GroupPrincipal target,
                          final String action ) {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if ( target == null || m_action == null ) {
            m_permission = null;
        } else {
            m_permission = new GroupPermission( target.getName(), action );
        }
    }

    /**
     * Creates and returns a targeted Command by combining a GroupPrincipal with this Command. The supplied <code>target</code> object
     * must be non-<code>null</code> and of type GroupPrincipal. If the target is not of the correct type, this method throws an
     * {@link IllegalArgumentException}.
     *
     * @param target the GroupPrincipal to combine into the current Command
     * @return the new, targeted command
     */
    public Command targetedCommand( final Object target ) {
        if( !( target instanceof GroupPrincipal ) ) {
            throw new IllegalArgumentException( "Target must non-null and of type GroupPrincipal." );
        }
        return new GroupCommand( getRequestContext(), getURLPattern(), getContentTemplate(), ( GroupPrincipal )target, m_action );
    }
    
    /**
     * Returns the name of the command, which will either be the target (if specified), or the "friendly name" for the JSP.
     *
     * @return the name
     * @see org.apache.wiki.ui.Command#getName()
     */
    public String getName() {
        final Object target = getTarget();
        if ( target == null ) {
            return getJSPFriendlyName();
        }
        return ( ( GroupPrincipal ) target ).getName();
    }

    /**
     * Returns the permission required to execute this GroupCommand.
     *
     * @return the permission
     * @see org.apache.wiki.ui.Command#requiredPermission()
     */
    public Permission requiredPermission() {
        return m_permission;
    }

}
