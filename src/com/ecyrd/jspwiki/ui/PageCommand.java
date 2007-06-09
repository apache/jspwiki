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

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.PermissionFactory;

/**
 * <p>Defines Commands for editing, renaming, and viewing wiki pages.
 * PageCommands can be combined with WikiPages to produce
 * targeted Commands.</p>
 * <p>This class is not <code>final</code>; it may be extended in
 * the future.</p>
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public final class PageCommand extends AbstractCommand
{

    public static final Command ATTACH
        = new PageCommand( "att", "%uattach/%n", null, null, PagePermission.UPLOAD_ACTION );

    public static final Command COMMENT
        = new PageCommand( "comment", "%uComment.jsp?page=%n", "CommentContent.jsp", null, PagePermission.COMMENT_ACTION );

    public static final Command CONFLICT 
        = new PageCommand( "conflict", "%uPageModified.jsp?page=%n", "ConflictContent.jsp", null, PagePermission.VIEW_ACTION );

    public static final Command DELETE
        = new PageCommand( "del", "%uDelete.jsp?page=%n", null, null, PagePermission.DELETE_ACTION );

    public static final Command DIFF
        = new PageCommand( "diff", "%uDiff.jsp?page=%n", "DiffContent.jsp", null, PagePermission.VIEW_ACTION );

    public static final Command EDIT
        = new PageCommand( "edit", "%uEdit.jsp?page=%n", "EditContent.jsp", null, PagePermission.EDIT_ACTION );

    public static final Command INFO
        = new PageCommand( "info", "%uPageInfo.jsp?page=%n", "InfoContent.jsp", null, PagePermission.VIEW_ACTION );

    public static final Command PREVIEW
        = new PageCommand( "preview", "%uPreview.jsp?page=%n", "PreviewContent.jsp", null, PagePermission.VIEW_ACTION );

    public static final Command RENAME
        = new PageCommand( "rename", "%uRename.jsp?page=%n", null, null, PagePermission.RENAME_ACTION );

    public static final Command RSS
        = new PageCommand( "rss", "%urss.jsp", null, null, PagePermission.VIEW_ACTION );

    public static final Command UPLOAD
        = new PageCommand( "upload", "%uUpload.jsp?page=%n", null, null, PagePermission.UPLOAD_ACTION );

    public static final Command VIEW
        = new PageCommand( "view", "%uWiki.jsp?page=%n", "PageContent.jsp", null, PagePermission.VIEW_ACTION );

    public static final Command NONE
        = new PageCommand( "", "%u%n", null, null, null );

    public static final Command OTHER = NONE;

    private final String m_action;
    
    private final Permission m_permission;
    
    /**
     * Constructs a new Command with a specified wiki context, URL pattern,
     * type, and content template. The target for this command is initialized to
     * <code>null</code>.
     * @param requestContext the request context
     * @param urlPattern the URL pattern
     * @param target the target of the command (a WikiPage); may be <code>null</code>
     * @param action the action used to construct a suitable PagePermission
     * @param contentTemplate the content template; may be <code>null</code>
     * @return IllegalArgumentException if the request content, URL pattern, or
     *         type is <code>null</code>
     */
    private PageCommand( String requestContext, String urlPattern, String contentTemplate, WikiPage target, String action )
    {
        super( requestContext, urlPattern, contentTemplate, target );
        m_action = action;
        if( target == null || m_action == null )
        {
            m_permission = null;
        }
        else
        {
            m_permission = PermissionFactory.getPagePermission( target, action );
        }
    }

    /**
     * Creates and returns a targeted Command by combining a WikiPage 
     * with this Command. The supplied <code>target</code> object 
     * must be non-<code>null</code> and of type WikiPage.
     * @param target the WikiPage to combine into the current Command
     * @return the new targeted command
     * @throws IllegalArgumentException if the target is not of the correct type
     */
    public final Command targetedCommand( Object target )
    {
        if( !( target != null && target instanceof WikiPage ) )
        {
            throw new IllegalArgumentException( "Target must non-null and of type WikiPage." );
        }
        return new PageCommand( getRequestContext(), getURLPattern(), getContentTemplate(), (WikiPage)target, m_action );
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public final String getName()
    {
        Object target = getTarget();
        if( target == null )
        {
            return getJSPFriendlyName();
        }
        return ( (WikiPage) target ).getName();
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return m_permission;
    }
}
