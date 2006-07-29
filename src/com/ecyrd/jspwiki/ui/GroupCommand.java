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
 * @version $Revision: 1.3 $ $Date: 2006-07-29 19:31:11 $
 * @since 2.4.22
 */
public class GroupCommand extends AbstractCommand
{

    public static final Command DELETE_GROUP 
        = new GroupCommand( "deleteGroup", "%uDeleteGroup.jsp?group=%n", null, null, GroupPermission.DELETE_ACTION );

    public static final Command EDIT_GROUP   
        = new GroupCommand( "editGroup", "%uEditGroup.jsp?group=%n", "EditGroupContent.jsp", null, GroupPermission.EDIT_ACTION );

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
            m_permission = new GroupPermission( target.getWiki() + ":" + target.getName(), action );
        }
    }

    /**
     * Creates and returns a targeted Command by combining a GroupPrincipal 
     * with this Command. The supplied <code>target</code> object 
     * must be non-<code>null</code> and of type GroupPrincipal.
     * @param target the GroupPrincipal to combine into the current Command
     * @return the new, targeted command
     * @throws IllegalArgumentException if the target is not of the correct type
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
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return m_permission;
    }

}
