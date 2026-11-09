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
import java.util.Arrays;


/**
 * <p>
 * Permission to perform an operation on a group in a given wiki. Permission
 * actions include: <code>view</code>, <code>edit</code>, <code>delete</code>.
 * </p>
 * <p>
 * The target of a permission is a single group or collection in a given wiki.
 * The syntax for the target is the wiki name, followed by a colon (:) and the
 * name of the group. &#8220;All wikis&#8221; can be specified using a wildcard (*). Group
 * collections may also be specified using a wildcard. For groups, the wildcard
 * may be a prefix, suffix, or all by itself. Examples of targets include:
 * </p>
 * <blockquote><code>*:*<br/>
 * *:TestPlanners<br/>
 * *:*Planners<br/>
 * *:Test*<br/>
 * mywiki:TestPlanners<br/>
 * mywiki:*Planners<br/>
 * mywiki:Test*</code>
 * </blockquote>
 * <p>
 * For a given target, certain permissions imply others:
 * </p>
 * <ul>
 * <li><code>edit</code>&nbsp;implies&nbsp;<code>view</code></li>
 * <li><code>delete</code>&nbsp;implies&nbsp;<code>edit</code> and
 * <code>view</code></li>
 * </ul>
 * <P>Targets that do not include a wiki prefix <em>never </em> imply others.</p>
 * <p>
 * GroupPermission accepts a special target called
 * <code>&lt;groupmember&gt;</code> that means &#8220;all groups that a user is a
 * member of.&#8221; When included in a policy file <code>grant</code> block, it
 * functions like a wildcard. Thus, this block:
 *
 * <pre>
 *  grant signedBy &quot;jspwiki&quot;,
 *    principal org.apache.wiki.auth.authorize.Role &quot;Authenticated&quot; {
 *      permission org.apache.wiki.auth.permissions.GroupPermission &quot;*:&lt;groupmember&gt;&quot;, &quot;edit&quot;;
 * </pre>
 *
 * means, &#8220;allow Authenticated users to edit any groups they are members of.&#8221;
 * The wildcard target (*) does <em>not</em> imply <code>&lt;groupmember&gt;</code>; it
 * must be granted explicitly.
 * </p>
 * <p>
 * A permission does not know who is asking, so it is up to the one who checks it to find out if the user is a member of the group:
 * {@link #toMemberPermission()} gives the permission to look for in the policy then.
 * @since 2.4.17
 */
public final class GroupPermission extends Permission implements Serializable
{
    /** Special target token that denotes all groups that a Subject's Principals are members of. */
    public static final String         MEMBER_TOKEN     = "<groupmember>";

    private static final long           serialVersionUID = 1L;

    /** Action for deleting a group or collection of groups. */
    public static final String          DELETE_ACTION    = "delete";

    /** Action for editing a group or collection of groups. */
    public static final String          EDIT_ACTION      = "edit";

    /** Action for viewing a group or collection of groups. */
    public static final String          VIEW_ACTION      = "view";

    static final int          DELETE_MASK      = 0x4;

    static final int          EDIT_MASK        = 0x2;

    static final int          VIEW_MASK        = 0x1;

    /** Convenience constant that denotes <code>GroupPermission( "*:*, "delete" )</code>. */
    public static final GroupPermission DELETE           = new GroupPermission( DELETE_ACTION );

    /** Convenience constant that denotes <code>GroupPermission( "*:*, "edit" )</code>. */
    public static final GroupPermission EDIT             = new GroupPermission( EDIT_ACTION );

    /** Convenience constant that denotes <code>GroupPermission( "*:*, "view" )</code>. */
    public static final GroupPermission VIEW             = new GroupPermission( VIEW_ACTION );

    private static final String         ACTION_SEPARATOR = ",";

    private static final String         WILDCARD         = "*";

    private static final String         WIKI_SEPARATOR   = ":";

    private final String                m_actionString;

    private final int                   m_mask;

    private final String                m_group;

    private final String                m_wiki;

    /** For serialization purposes */
    GroupPermission()
    {
        this("");
    }
    
    /**
     * Private convenience constructor that creates a new GroupPermission for
     * all wikis and groups (*:*) and set of actions.
     * @param actions
     */
    private GroupPermission(final String actions )
    {
        this( WILDCARD + WIKI_SEPARATOR + WILDCARD, actions );
    }

    /**
     * Creates a new GroupPermission for a specified group and set of actions.
     * Group should include a prepended wiki name followed by a colon (:). If
     * the wiki name is not supplied or starts with a colon, the group refers to
     * all wikis.
     * @param group the wiki group
     * @param actions the allowed actions for this group
     */
    public GroupPermission(final String group, final String actions )
    {
        super( group );

        // Parse wiki and group (which may include wiki name and group)
        // Strip out attachment separator; it is irrelevant.
        final String[] pathParams = group.split( WIKI_SEPARATOR );
        final String groupName;
        if ( pathParams.length >= 2 )
        {
            m_wiki = !pathParams[0].isEmpty() ? pathParams[0] : null;
            groupName = pathParams[1];
        }
        else
        {
            m_wiki = WILDCARD;
            groupName = pathParams[0];
        }
        m_group = groupName;

        // Parse actions
        final String[] groupActions = actions.toLowerCase().split( ACTION_SEPARATOR );
        Arrays.sort( groupActions, String.CASE_INSENSITIVE_ORDER );
        m_mask = createMask( actions );
        final StringBuilder buffer = new StringBuilder();
        final int groupActionsLength = groupActions.length;
        for( int i = 0; i < groupActionsLength; i++ )
        {
            buffer.append( groupActions[i] );
            if ( i < ( groupActionsLength - 1 ) )
            {
                buffer.append( ACTION_SEPARATOR );
            }
        }
        m_actionString = buffer.toString();
    }

    /**
     * Two PagePermission objects are considered equal if their actions (after
     * normalization), wiki and target are equal.
     * @param obj the object to compare
     * @return the result of the comparison
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object obj )
    {
        if ( !( obj instanceof GroupPermission ) )
        {
            return false;
        }
        final GroupPermission p = (GroupPermission) obj;
        return  p.m_mask == m_mask && p.m_group.equals( m_group ) && p.m_wiki != null && p.m_wiki.equals( m_wiki );
    }

    /**
     * Returns the actions for this permission: &#8220;view&#8221;, &#8220;edit&#8221;, or &#8220;delete&#8221;. The
     * actions will always be sorted in alphabetic order, and will always appear
     * in lower case.
     * @return the actions
     * @see java.security.Permission#getActions()
     */
    @Override
    public String getActions()
    {
        return m_actionString;
    }

    /**
     * Returns the name of the wiki group represented by this permission.
     * @return the page name
     */
    public String getGroup()
    {
        return m_group;
    }

    /**
     * Returns the name of the wiki containing the group represented by this
     * permission; may return the wildcard string.
     * @return the wiki
     */
    public String getWiki()
    {
        return m_wiki;
    }

    /**
     * Returns the hash code for this GroupPermission.
     * @return the hash code
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        // If the wiki has not been set, uses a dummy value for the hashcode
        // calculation. This may occur if the page given does not refer
        // to any particular wiki
        final String wiki =  m_wiki != null ? m_wiki : "dummy_value";
        return m_mask + ( ( 13 * m_actionString.hashCode() ) * 23 * wiki.hashCode() );
    }

    /**
     * <p>
     * GroupPermissions can only imply other GroupPermissions; no other
     * permission types are implied. One GroupPermission implies another if its
     * actions if three conditions are met:
     * </p>
     * <ol>
     * <li>The other GroupPermission&#8217;s wiki is equal to, or a subset of, that
     * of this permission. This permission&#8217;s wiki is considered a superset of
     * the other if it contains a matching prefix plus a wildcard, or a wildcard
     * followed by a matching suffix.</li>
     * <li>The other GroupPermission&#8217;s target is equal to, or a subset of, the
     * target specified by this permission. This permission&#8217;s target is
     * considered a superset of the other if it contains a matching prefix plus
     * a wildcard, or a wildcard followed by a matching suffix.</li>
     * <li>All of other GroupPermission&#8217;s actions are equal to, or a subset of,
     * those of this permission</li>
     * </ol>
     * @param permission the Permission to examine
     * @return <code>true</code> if the GroupPermission implies the
     * supplied Permission; <code>false</code> otherwise
     * @see java.security.Permission#implies(java.security.Permission)
     */
    @Override
    public boolean implies(final Permission permission )
    {
        // Permission must be a GroupPermission
        if ( !( permission instanceof GroupPermission ) )
        {
            return false;
        }

        // Build up an "implied mask"
        final GroupPermission p = (GroupPermission) permission;
        final int impliedMask = impliedMask( m_mask );

        // If actions aren't a proper subset, return false
        if ( ( impliedMask & p.m_mask ) != p.m_mask )
        {
            return false;
        }

        // See if the tested permission's wiki is implied
        final boolean impliedWiki = PagePermission.isSubset( m_wiki, p.m_wiki );

        // If this page is "*", the tested permission's
        // group is implied, unless implied permission has <groupmember> token
        final boolean impliedGroup;
        if ( MEMBER_TOKEN.equals( p.m_group ) )
        {
            impliedGroup = MEMBER_TOKEN.equals( m_group );
        }
        else
        {
            impliedGroup = PagePermission.isSubset( m_group, p.m_group );
        }

        return impliedWiki && impliedGroup;
    }

    /**
     * Prints a human-readable representation of this permission.
     * @return the string
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        final String wiki = ( m_wiki == null ) ? "" : m_wiki;
        return "(\"" + this.getClass().getName() + "\",\"" + wiki + WIKI_SEPARATOR + m_group + "\",\"" + getActions()
                + "\")";
    }

    /**
     * Creates an &#8220;implied mask&#8221; based on the actions originally assigned: for
     * example, delete implies edit; edit implies view.
     * @param mask binary mask for actions
     * @return binary mask for implied actions
     */
    static int impliedMask( int mask )
    {
        if ( ( mask & DELETE_MASK ) > 0 )
        {
            mask |= EDIT_MASK;
        }
        if ( ( mask & EDIT_MASK ) > 0 )
        {
            mask |= VIEW_MASK;
        }
        return mask;
    }

    /**
     * Protected method that creates a binary mask based on the actions specified.
     * This is used by {@link #implies(Permission)}.
     * @param actions the actions for this permission, separated by commas
     * @return the binary actions mask
     */
    static int createMask( final String actions )
    {
        if ( actions == null || actions.isEmpty() )
        {
            throw new IllegalArgumentException( "Actions cannot be blank or null" );
        }
        int mask = 0;
        final String[] actionList = actions.split( ACTION_SEPARATOR );
        for( final String action : actionList )
        {
            if ( action.equalsIgnoreCase( VIEW_ACTION ) )
            {
                mask |= VIEW_MASK;
            }
            else if ( action.equalsIgnoreCase( EDIT_ACTION ) )
            {
                mask |= EDIT_MASK;
            }
            else if ( action.equalsIgnoreCase( DELETE_ACTION ) )
            {
                mask |= DELETE_MASK;
            }
            else
            {
                throw new IllegalArgumentException( "Unrecognized action: " + action );
            }
        }
        return mask;
    }

    /**
     * Returns the permission for the same wiki and actions whose group is the {@code <groupmember>} token, which is the one a policy
     * grants to the members of a group. To find out if a Subject may do something to a group because it is a member of it, check
     * that this permission, not the one of the group, is granted.
     *
     * @return the permission of the {@code <groupmember>} token
     * @since 3.0.1
     */
    public GroupPermission toMemberPermission() {
        return new GroupPermission( ( m_wiki == null ? "" : m_wiki ) + WIKI_SEPARATOR + MEMBER_TOKEN, m_actionString );
    }

}
