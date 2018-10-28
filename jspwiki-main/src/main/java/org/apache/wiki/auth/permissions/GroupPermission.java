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
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.Principal;
import java.util.Arrays;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import org.apache.wiki.auth.GroupPrincipal;

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

    protected static final int          DELETE_MASK      = 0x4;

    protected static final int          EDIT_MASK        = 0x2;

    protected static final int          VIEW_MASK        = 0x1;

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
    protected GroupPermission()
    {
        this("");
    }
    
    /**
     * Private convenience constructor that creates a new GroupPermission for
     * all wikis and groups (*:*) and set of actions.
     * @param actions
     */
    private GroupPermission( String actions )
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
    public GroupPermission( String group, String actions )
    {
        super( group );

        // Parse wiki and group (which may include wiki name and group)
        // Strip out attachment separator; it is irrelevant.
        String[] pathParams = group.split( WIKI_SEPARATOR );
        String groupName;
        if ( pathParams.length >= 2 )
        {
            m_wiki = pathParams[0].length() > 0 ? pathParams[0] : null;
            groupName = pathParams[1];
        }
        else
        {
            m_wiki = WILDCARD;
            groupName = pathParams[0];
        }
        m_group = groupName;

        // Parse actions
        String[] groupActions = actions.toLowerCase().split( ACTION_SEPARATOR );
        Arrays.sort( groupActions, String.CASE_INSENSITIVE_ORDER );
        m_mask = createMask( actions );
        StringBuilder buffer = new StringBuilder();
        for( int i = 0; i < groupActions.length; i++ )
        {
            buffer.append( groupActions[i] );
            if ( i < ( groupActions.length - 1 ) )
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
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof GroupPermission ) )
        {
            return false;
        }
        GroupPermission p = (GroupPermission) obj;
        return  p.m_mask == m_mask && p.m_group.equals( m_group ) && p.m_wiki != null && p.m_wiki.equals( m_wiki );
    }

    /**
     * Returns the actions for this permission: &#8220;view&#8221;, &#8220;edit&#8221;, or &#8220;delete&#8221;. The
     * actions will always be sorted in alphabetic order, and will always appear
     * in lower case.
     * @return the actions
     * @see java.security.Permission#getActions()
     */
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
        String wiki =  m_wiki != null ? m_wiki : "dummy_value";
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
    public boolean implies( Permission permission )
    {
        // Permission must be a GroupPermission
        if ( !( permission instanceof GroupPermission ) )
        {
            return false;
        }

        // Build up an "implied mask"
        GroupPermission p = (GroupPermission) permission;
        int impliedMask = impliedMask( m_mask );

        // If actions aren't a proper subset, return false
        if ( ( impliedMask & p.m_mask ) != p.m_mask )
        {
            return false;
        }

        // See if the tested permission's wiki is implied
        boolean impliedWiki = PagePermission.isSubset( m_wiki, p.m_wiki );

        // If this page is "*", the tested permission's
        // group is implied, unless implied permission has <groupmember> token
        boolean impliedGroup;
        if ( MEMBER_TOKEN.equals( p.m_group ) )
        {
            impliedGroup = MEMBER_TOKEN.equals( m_group );
        }
        else
        {
            impliedGroup = PagePermission.isSubset( m_group, p.m_group );
        }

        // See if this permission is <groupmember> and Subject possesses
        // GroupPrincipal matching the implied GroupPermission's group
        boolean impliedMember = impliesMember( p );

        return  impliedWiki && ( impliedGroup || impliedMember );
    }

    /**
     * Prints a human-readable representation of this permission.
     * @return the string
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String wiki = ( m_wiki == null ) ? "" : m_wiki;
        return "(\"" + this.getClass().getName() + "\",\"" + wiki + WIKI_SEPARATOR + m_group + "\",\"" + getActions()
                + "\")";
    }

    /**
     * Creates an &#8220;implied mask&#8221; based on the actions originally assigned: for
     * example, delete implies edit; edit implies view.
     * @param mask binary mask for actions
     * @return binary mask for implied actions
     */
    protected static int impliedMask( int mask )
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
    protected static int createMask( String actions )
    {
        if ( actions == null || actions.length() == 0 )
        {
            throw new IllegalArgumentException( "Actions cannot be blank or null" );
        }
        int mask = 0;
        String[] actionList = actions.split( ACTION_SEPARATOR );
        for( String action : actionList )
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
     * <p>
     * Returns <code>true</code> if this GroupPermission was created with the
     * token <code>&lt;groupmember&gt;</code>
     * <em>and</em> the current
     * thread&#8217;s Subject is a member of the Group indicated by the implied
     * GroupPermission. Thus, a GroupPermission with the group
     * <code>&lt;groupmember&gt;</code> implies GroupPermission for group
     * "TestGroup" only if the Subject is a member of TestGroup.
     * </p>
     * <p>
     * We make this determination by obtaining the current {@link Thread}&#8217;s
     * {@link java.security.AccessControlContext} and requesting the
     * {@link javax.security.auth.SubjectDomainCombiner}. If the combiner is
     * not <code>null</code>, then we know that the access check was
     * requested using a {@link javax.security.auth.Subject}; that is, that an
     * upstream caller caused a Subject to be associated with the Thread&#8217;s
     * ProtectionDomain by executing a
     * {@link javax.security.auth.Subject#doAs(Subject, java.security.PrivilegedAction)}
     * operation.
     * </p>
     * <p>
     * If a SubjectDomainCombiner exists, determining group membership is
     * simple: just iterate through the Subject&#8217;s Principal set and look for all
     * Principals of type {@link org.apache.wiki.auth.GroupPrincipal}. If the
     * name of any Principal matches the value of the implied Permission&#8217;s
     * {@link GroupPermission#getGroup()} value, then the Subject is a member of
     * this group -- and therefore this <code>impliesMember</code> call
     * returns <code>true</code>.
     * </p>
     * <p>
     * This may sound complicated, but it really isn&#8217;t. Consider the following
     * examples:
     * </p>
     * <table border="1"> <thead>
     * <tr>
     * <th width="25%">This object</th>
     * <th width="25%"><code>impliesMember</code> parameter</th>
     * <th width="25%">Calling Subject&#8217;s Principals
     * <th width="25%">Result</th>
     * </tr>
     * <tr>
     * <td><code>GroupPermission ("&lt;groupmember&gt;")</code></td>
     * <td><code>GroupPermission ("*:TestGroup")</code></td>
     * <td><code>WikiPrincipal ("Biff"),<br/>GroupPrincipal ("TestGroup")</code></td>
     * <td><code>true</code></td>
     * </tr>
     * <tr>
     * <td><code>GroupPermission ("*:TestGroup")</code></td>
     * <td><code>GroupPermission ("*:TestGroup")</code></td>
     * <td><code>WikiPrincipal ("Biff"),<br/>GroupPrincipal ("TestGroup")</code></td>
     * <td><code>false</code> - this object does not contain
     * <code>&lt;groupmember&gt;</code></td>
     * </tr>
     * <tr>
     * <td><code>GroupPermission ("&lt;groupmember&gt;")</code></td>
     * <td><code>GroupPermission ("*:TestGroup")</code></td>
     * <td><code>WikiPrincipal ("Biff"),<br/>GroupPrincipal ("FooGroup")</code></td>
     * <td><code>false</code> - Subject does not contain GroupPrincipal
     * matching implied Permission&#8217;s group (TestGroup)</td>
     * </tr>
     * <tr>
     * <td><code>GroupPermission ("&lt;groupmember&gt;")</code></td>
     * <td><code>WikiPermission ("*:createGroups")</code></td>
     * <td><code>WikiPrincipal ("Biff"),<br/>GroupPrincipal ("TestGroup")</code></td>
     * <td><code>false</code> - implied permission not of type
     * GroupPermission</td>
     * </tr>
     * <tr>
     * <td><code>GroupPermission ("&lt;groupmember&gt;")</code></td>
     * <td><code>GroupPermission ("*:TestGroup")</code></td>
     * <td>-</td>
     * <td><code>false</code> - <code>Subject.doAs()</code> not called
     * upstream</td>
     * </tr>
     * </table>
     * <p>
     * Note that JSPWiki&#8217;s access control checks are made inside of
     * {@link org.apache.wiki.auth.AuthorizationManager#checkPermission(org.apache.wiki.WikiSession, Permission)},
     * which performs a <code>Subject.doAs()</code> call. Thus, this
     * Permission functions exactly the way it should during normal
     * operations.
     * </p>
     * @param permission the implied permission
     * @return <code>true</code> if the calling Thread&#8217;s Subject contains a
     *         GroupPrincipal matching the implied GroupPermission&#8217;s group;
     *         <code>false</code> otherwise
     */
    protected boolean impliesMember( Permission permission )
    {
        if ( !( permission instanceof GroupPermission ) )
        {
            return false;
        }
        GroupPermission gp = (GroupPermission) permission;
        if ( !MEMBER_TOKEN.equals( m_group ) )
        {
            return false;
        }

        // For the current thread, retrieve the SubjectDomainCombiner
        // (if one was used to create current AccessControlContext )
        AccessControlContext acc = AccessController.getContext();
        DomainCombiner dc = acc.getDomainCombiner();
        if ( dc != null && dc instanceof SubjectDomainCombiner )
        {
            // <member> implies permission if subject possesses
            // GroupPrincipal with same name as target
            Subject subject = ( (SubjectDomainCombiner) dc ).getSubject();
            Set<GroupPrincipal> principals = subject.getPrincipals( GroupPrincipal.class );
            for( Principal principal : principals )
            {
                if ( principal.getName().equals( gp.m_group ) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}
