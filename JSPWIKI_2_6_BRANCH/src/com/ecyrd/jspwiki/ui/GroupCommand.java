/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;

/**
 * <p>Defines Commands for viewing, editing and deleting wiki groups.
 * GroupCommands can be combined with GroupPrincipals to produce
 * targeted Commands.</p>
 * <p>This class is not <code>final</code>; it may be extended in
 * the future.</p>
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public final class GroupCommand extends AbstractCommand
{

    /** GroupCommand for deleting a group. */
    public static final Command DELETE_GROUP 
        = new GroupCommand( "deleteGroup", "%uDeleteGroup.jsp?group=%n", null, null, GroupPermission.DELETE_ACTION );

    /** GroupCommand for editing a group. */
       public static final Command EDIT_GROUP   
        = new GroupCommand( "editGroup", "%uEditGroup.jsp?group=%n", "EditGroupContent.jsp", null, GroupPermission.EDIT_ACTION );

       /** GroupCommand for viewing a group. */
    public static final Command VIEW_GROUP   
        = new GroupCommand( "viewGroup", "%uGroup.jsp?group=%n", "GroupContent.jsp", null, GroupPermission.VIEW_ACTION );

    private final String m_action;
    
    private final Permission m_permission;
    
    /**
     * Constructs a new Command with a specified wiki context, URL pattern,
     * type, and content template. The WikiPage for this command is initialized
     * to <code>null</code>.
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param target the target of this command (a GroupPrincipal representing a Group); may be <code>null</code>
     * @param action the action used to construct a suitable GroupPermission
     * @param contentTemplate the content template; may be <code>null</code>
     * @return IllegalArgumentException if the request content, URL pattern, or
     *         type is <code>null</code>
     */
    private GroupCommand( String requestContext, String urlPattern, String contentTemplate, GroupPrincipal target, String action )
    {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if ( target == null || m_action == null )
        {
            m_permission = null;
        }
        else
        {
            m_permission = new GroupPermission( target.getName(), action );
        }
    }

    /**
     * Creates and returns a targeted Command by combining a GroupPrincipal 
     * with this Command. The supplied <code>target</code> object 
     * must be non-<code>null</code> and of type GroupPrincipal.
     * If the target is not of the correct type, this method throws an
     * {@link IllegalArgumentException}.
     * @param target the GroupPrincipal to combine into the current Command
     * @return the new, targeted command
     */
    public final Command targetedCommand( Object target )
    {
        if ( !( target != null && target instanceof GroupPrincipal ) )
        {
            throw new IllegalArgumentException( "Target must non-null and of type GroupPrincipal." );
        }
        return new GroupCommand( getRequestContext(), getURLPattern(), getContentTemplate(), (GroupPrincipal)target, m_action );
    }
    
    /**
     * Returns the name of the command, which will either be
     * the target (if specified), or the "friendly name" for the JSP.
     * @return the name
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public final String getName()
    {
        Object target = getTarget();
        if ( target == null )
        {
            return getJSPFriendlyName();
        }
        return ( (GroupPrincipal) target ).getName();
    }

    /**
     * Returns the permission required to execute this GroupCommand.
     * @return the permission
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return m_permission;
    }

}
