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
package org.apache.wiki.auth.permissions;

import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Arrays;

/**
 * <p> Permission to perform an global wiki operation, such as self-registering
 * or creating new pages. Permission actions include: <code>createGroups</code>,
 * <code>createPages</code>, <code>editPreferences</code>,
 * <code>editProfile</code> and <code>login</code>. </p> <p>The target is
 * a given wiki. The syntax for the target is the wiki name. "All wikis" can be
 * specified using a wildcard (*). Page collections may also be specified using
 * a wildcard. For pages, the wildcard may be a prefix, suffix, or all by
 * itself. <p> Certain permissions imply others. Currently,
 * <code>createGroups</code> implies <code>createPages</code>. </p>
 * @since 2.3
 */
public final class WikiPermission extends Permission implements Serializable
{
    private static final long          serialVersionUID        = 1L;

    /** Name of the action for createGroups permission. */
    public static final String         CREATE_GROUPS_ACTION    = "createGroups";

    /** Name of the action for createPages permission. */   
    public static final String         CREATE_PAGES_ACTION     = "createPages";

    /** Name of the action for login permission. */
    public static final String         LOGIN_ACTION            = "login";

    /** Name of the action for editPreferences permission. */
    public static final String         EDIT_PREFERENCES_ACTION = "editPreferences";

    /** Name of the action for editProfile permission. */
    public static final String         EDIT_PROFILE_ACTION     = "editProfile";

    /** Value for a generic wildcard. */
    public static final String         WILDCARD                = "*";

    protected static final int         CREATE_GROUPS_MASK      = 0x1;

    protected static final int         CREATE_PAGES_MASK       = 0x2;

    protected static final int         EDIT_PREFERENCES_MASK   = 0x4;

    protected static final int         EDIT_PROFILE_MASK       = 0x8;

    protected static final int         LOGIN_MASK              = 0x10;

    /** A static instance of the createGroups permission. */
    public static final WikiPermission CREATE_GROUPS           = new WikiPermission( WILDCARD, CREATE_GROUPS_ACTION );

    /** A static instance of the createPages permission. */
    public static final WikiPermission CREATE_PAGES            = new WikiPermission( WILDCARD, CREATE_PAGES_ACTION );

    /** A static instance of the login permission. */
    public static final WikiPermission LOGIN                   = new WikiPermission( WILDCARD, LOGIN_ACTION );

    /** A static instance of the editPreferences permission. */
    public static final WikiPermission EDIT_PREFERENCES        = new WikiPermission( WILDCARD, EDIT_PREFERENCES_ACTION );

    /** A static instance of the editProfile permission. */
    public static final WikiPermission EDIT_PROFILE            = new WikiPermission( WILDCARD, EDIT_PROFILE_ACTION );

    private final String               m_actionString;

    private final String               m_wiki;

    private final int                  m_mask;

    /**
     * Creates a new WikiPermission for a specified set of actions.
     * @param actions the actions for this permission
     * @param wiki The name of the wiki the permission belongs to.
     */
    public WikiPermission( String wiki, String actions )
    {
        super( wiki );
        String[] pageActions = actions.toLowerCase().split( "," );
        Arrays.sort( pageActions, String.CASE_INSENSITIVE_ORDER );
        m_mask = createMask( actions );
        StringBuilder buffer = new StringBuilder();
        for( int i = 0; i < pageActions.length; i++ )
        {
            buffer.append( pageActions[i] );
            if ( i < ( pageActions.length - 1 ) )
            {
                buffer.append( "," );
            }
        }
        m_actionString = buffer.toString();
        m_wiki = ( wiki == null ) ? WILDCARD : wiki;
    }

    /**
     * Two WikiPermission objects are considered equal if their wikis and
     * actions (after normalization) are equal.
     * @param obj the object to test
     * @return the result
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof WikiPermission ) )
        {
            return false;
        }
        WikiPermission p = (WikiPermission) obj;
        return  p.m_mask == m_mask && p.m_wiki != null && p.m_wiki.equals( m_wiki );
    }

    /**
     * Returns the actions for this permission: "createGroups", "createPages",
     * "editPreferences", "editProfile", or "login". The actions
     * will always be sorted in alphabetic order, and will always appear in
     * lower case.
     * @return the actions
     * @see java.security.Permission#getActions()
     */
    public String getActions()
    {
        return m_actionString;
    }

    /**
     * Returns the name of the wiki containing the page represented by this
     * permission; may return the wildcard string.
     * @return the wiki
     */
    public String getWiki()
    {
        return m_wiki;
    }

    /**
     * Returns the hash code for this WikiPermission.
     * @return {@inheritDoc}
     */
    public int hashCode()
    {
        return m_mask + ( ( 13 * m_actionString.hashCode() ) * 23 * m_wiki.hashCode() );
    }

    /**
     * WikiPermission can only imply other WikiPermissions; no other permission
     * types are implied. One WikiPermission implies another if all of the other
     * WikiPermission's actions are equal to, or a subset of, those for this
     * permission.
     * @param permission the permission which may (or may not) be implied by
     * this instance
     * @return <code>true</code> if the permission is implied,
     * <code>false</code> otherwise
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public boolean implies( Permission permission )
    {
        // Permission must be a WikiPermission
        if ( !( permission instanceof WikiPermission ) )
        {
            return false;
        }
        WikiPermission p = (WikiPermission) permission;

        // See if the wiki is implied
        boolean impliedWiki = PagePermission.isSubset( m_wiki, p.m_wiki );

        // Build up an "implied mask" for actions
        int impliedMask = impliedMask( m_mask );

        // If actions aren't a proper subset, return false
        return impliedWiki && ( impliedMask & p.m_mask ) == p.m_mask;
    }

    /**
     * Returns a new {@link AllPermissionCollection}.
     * @return {@inheritDoc}
     */
    public PermissionCollection newPermissionCollection()
    {
        return new AllPermissionCollection();
    }

    /**
     * Prints a human-readable representation of this permission.
     * @return {@inheritDoc}
     */
    public String toString()
    {
        return "(\"" + this.getClass().getName() + "\",\"" + m_wiki + "\",\"" + getActions() + "\")";
    }

    /**
     * Creates an "implied mask" based on the actions originally assigned: for
     * example, <code>createGroups</code> implies <code>createPages</code>.
     * @param mask the initial mask
     * @return the implied mask
     */
    protected static int impliedMask( int mask )
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
    protected static int createMask( String actions )
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
            else if ( action.equalsIgnoreCase( LOGIN_ACTION ) )
            {
                mask |= LOGIN_MASK;
            }
            else if ( action.equalsIgnoreCase( EDIT_PREFERENCES_ACTION ) )
            {
                mask |= EDIT_PREFERENCES_MASK;
            }
            else if ( action.equalsIgnoreCase( EDIT_PROFILE_ACTION ) )
            {
                mask |= EDIT_PROFILE_MASK;
            }
            else
            {
                throw new IllegalArgumentException( "Unrecognized action: " + action );
            }
        }
        return mask;
    }
}
