package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.util.Arrays;

/**
 * <p>
 * Permission to perform an global wiki operation, such as self-registering or
 * creating new pages. Permission actions include: <code>createGroups</code>,
 * <code>createPages</code>, and <code>registerUser</code>.
 * </p>
 * <p>
 * Certain permissions imply others. Currently, <code>createGroups</code>
 * implies <code>createPages</code>.
 * </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.7 $ $Date: 2005-07-15 08:27:21 $
 * @since 2.3
 */
public final class WikiPermission extends Permission
{
    private static final long          serialVersionUID = 1L;

    private static final String        CREATE_GROUPS_ACTION = "createGroups";

    private static final String        CREATE_PAGES_ACTION  = "createPages";

    private static final String        REGISTER_ACTION      = "registerUser";

    protected static final int         CREATE_GROUPS_MASK   = 0x1;

    protected static final int         CREATE_PAGES_MASK    = 0x2;

    protected static final int         REGISTER_MASK        = 0x4;

    public static final WikiPermission CREATE_GROUPS        = new WikiPermission( CREATE_GROUPS_ACTION );

    public static final WikiPermission CREATE_PAGES         = new WikiPermission( CREATE_PAGES_ACTION );

    public static final WikiPermission REGISTER             = new WikiPermission( REGISTER_ACTION );

    private final String               m_actionString;

    private final int                  m_mask;

    /**
     * Creates a new WikiPermission for a specified set of actions.
     * @param actions the actions for this permission
     */
    public WikiPermission( String actions )
    {
        super( actions );
        String pageActions[] = actions.toLowerCase().split( "," );
        Arrays.sort( pageActions, String.CASE_INSENSITIVE_ORDER );
        m_mask = createMask( actions );
        StringBuffer buffer = new StringBuffer();
        for( int i = 0; i < pageActions.length; i++ )
        {
            buffer.append( pageActions[i] );
            if ( i < ( pageActions.length - 1 ) )
            {
                buffer.append( "," );
            }
        }
        m_actionString = buffer.toString();
    }

    /**
     * Two WikiPermission objects are considered equal if their actions (after
     * normalization) are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( !( obj instanceof WikiPermission ) )
        {
            return false;
        }
        WikiPermission p = (WikiPermission) obj;
        return ( p.m_mask == m_mask );
    }

    /**
     * Returns the actions for this permission: "createGroups", "createPages",
     * or "registerUser". The actions will always be sorted in alphabetic order,
     * and will always appear in lower case.
     * @see java.security.Permission#getActions()
     */
    public final String getActions()
    {
        return m_actionString;
    }

    /**
     * Returns the hash code for this WikiPermission.
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode()
    {
        int hash = 0;
        String actions = getActions();
        for( int i = 0; i < actions.length(); i++ )
        {
            hash += 13 * actions.hashCode();
        }
        return hash;
    }

    /**
     * WikiPermission can only imply other WikiPermissions; no other permission
     * types are implied. One WikiPermission implies another if all of the other
     * WikiPermission's actions are equal to, or a subset of, those for this
     * permission.
     * @param permission the permission which may (or may not) be implied by
     *            this instance
     * @return <code>true</code> if the permission is implied,
     *         <code>false</code> otherwise
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public final boolean implies( Permission permission )
    {
        // Permission must be a WikiPermission
        if ( !( permission instanceof WikiPermission ) )
        {
            return false;
        }

        // Build up an "implied mask"
        WikiPermission p = (WikiPermission) permission;
        int impliedMask = impliedMask( m_mask );

        // If actions aren't a proper subset, return false
        return ( ( impliedMask & p.m_mask ) == p.m_mask );
    }

    /**
     * Prints a human-readable representation of this permission.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "(\"" + this.getClass().getName() + "\",\"" + getActions() + "\")";
    }

    /**
     * Creates an "implied mask" based on the actions originally assigned: for
     * example, createGroups implies createPages.
     * @param mask the initial mask
     * @return the implied mask
     */
    protected static final int impliedMask( int mask )
    {
        if ( ( mask & CREATE_GROUPS_MASK ) > 0 )
        {
            mask |= CREATE_PAGES_MASK;
        }
        return mask;
    }

    /**
     * Private method that creates a binary mask based on the actions specified.
     * This is used by {@link #implies(Permission)}.
     * @param actions the permission actions, separated by commas
     * @return binary mask representing the permissions
     */
    protected static final int createMask( String actions )
    {
        if ( actions == null || actions.length() == 0 )
        {
            throw new IllegalArgumentException( "Actions cannot be blank or null" );
        }
        int mask = 0;
        String[] actionList = actions.split( "," );
        for( int i = 0; i < actionList.length; i++ )
        {
            String action = actionList[i];
            if ( action.equalsIgnoreCase( CREATE_GROUPS_ACTION ) )
            {
                mask |= CREATE_GROUPS_MASK;
            }
            else if ( action.equalsIgnoreCase( CREATE_PAGES_ACTION ) )
            {
                mask |= CREATE_PAGES_MASK;
            }
            else if ( action.equalsIgnoreCase( REGISTER_ACTION ) )
            {
                mask |= REGISTER_MASK;
            }
            else
            {
                throw new IllegalArgumentException( "Unrecognized action: " + action );
            }
        }
        return mask;
    }
}