package com.ecyrd.jspwiki.ui;

import java.security.Permission;

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
 * @version $Revision: 1.1 $ $Date: 2006-07-23 20:10:52 $
 * @since 2.4.21
 */
public class WikiCommand extends AbstractCommand
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
        = new WikiCommand( "login", "%uLogin.jsp?page=%n", "LoginContent.jsp", null, WikiPermission.LOGIN_ACTION );

    public static final Command LOGOUT
        = new WikiCommand( "logout", "%uLogout.jsp", null, null, WikiPermission.LOGIN_ACTION );

    public static final Command PREFS
        = new WikiCommand( "prefs", "%uUserPreferences.jsp", "PreferencesContent.jsp", null, WikiPermission.EDIT_PROFILE_ACTION );

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
     * @return IllegalArgumentException if the request content, URL pattern, or
     *         type is <code>null</code>
     */
    private WikiCommand( String requestContext, String urlPattern, String contentTemplate, String target, String action )
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
     * @see com.ecyrd.jspwiki.ui.Command#getName()
     */
    public final String getName()
    {
        Object target = getTarget();
        if ( target == null )
        {
            return getJSP();
        }
        return target.toString();
    }

    /**
     * @see com.ecyrd.jspwiki.ui.Command#requiredPermission()
     */
    public final Permission requiredPermission()
    {
        return m_permission;
    }
}
