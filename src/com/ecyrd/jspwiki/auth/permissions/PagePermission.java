package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.util.Arrays;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager;

/**
 * <p>
 * Permission to perform an operation on a single page or collection of pages in
 * a given wiki. Permission actions include: <code>view</code>,&nbsp;
 * <code>edit</code>,&nbsp;<code>comment</code>,&nbsp;
 * <code>upload</code>,&nbsp;<code>delete</code> and <code>rename</code>.
 * </p>
 * <p>
 * The target of a permission is a single page or collection in a given wiki.
 * The syntax for the target is the wiki name, followed by a colon (:) and the
 * name of the page. "All wikis" can be specified using a wildcard (*). Page
 * collections may also be specified using a wildcard. For pages, the wildcard
 * may be a prefix, suffix, or all by itself. Examples of targets include:
 * </p>
 * <blockquote><code>*:*<br/>
 * *:JanneJalkanen<br/>
 * *:Jalkanen<br/>
 * *:Janne*<br/>
 * mywiki:JanneJalkanen<br/>
 * mywiki:*Jalkanen<br/>
 * mywiki:Janne*</code>
 * </blockquote>
 * <p>
 * For a given target, certain permissions imply others: <code>delete</code>
 * and <code>rename</code> imply <code>edit</code> ;&nbsp; <code>edit</code>
 * implies <code>comment</code> ,&nbsp; <code>upload</code>, and
 * <code>view</code> ;&nbsp; <code>comment</code> and <code>upload</code>
 * imply <code>view</code>. Targets that do not include a wiki prefix
 * <i>never </i> imply others.
 * </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.5 $ $Date: 2005-09-17 18:21:06 $
 * @since 2.3
 */
public final class PagePermission extends Permission
{
    private static final long          serialVersionUID = 1L;

    private static final String        COMMENT_ACTION = "comment";

    private static final String        DELETE_ACTION  = "delete";

    private static final String        EDIT_ACTION    = "edit";

    private static final String        RENAME_ACTION  = "rename";

    private static final String        UPLOAD_ACTION  = "upload";

    private static final String        VIEW_ACTION    = "view";

    protected static final int         COMMENT_MASK   = 0x4;

    protected static final int         DELETE_MASK    = 0x10;

    protected static final int         EDIT_MASK      = 0x2;

    protected static final int         RENAME_MASK    = 0x20;

    protected static final int         UPLOAD_MASK    = 0x8;

    protected static final int         VIEW_MASK      = 0x1;

    public static final PagePermission COMMENT        = new PagePermission( COMMENT_ACTION );

    public static final PagePermission DELETE         = new PagePermission( DELETE_ACTION );

    public static final PagePermission EDIT           = new PagePermission( EDIT_ACTION );

    public static final PagePermission RENAME         = new PagePermission( RENAME_ACTION );

    public static final PagePermission UPLOAD         = new PagePermission( UPLOAD_ACTION );

    public static final PagePermission VIEW           = new PagePermission( VIEW_ACTION );

    private static final String        ACTION_SEPARATOR = ",";
    
    private static final String        WILDCARD       = "*";
    
    private static final String        WIKI_SEPARATOR = ":";

    private final String               m_actionString;

    private final int                  m_mask;

    private final String               m_page;

    private final String               m_wiki;

    /**
     * Private convenience constructor that creates a new PagePermission for all wikis and pages
     * (*:*) and set of actions.
     * @param actions
     */
    private PagePermission( String actions )
    {
        this( WILDCARD + WIKI_SEPARATOR + WILDCARD, actions );
    }

    /**
     * Creates a new PagePermission for a specified page name and set of
     * actions. Page should include a prepended wiki name followed by a slash.
     * If the wiki name is not supplied or starts with a colon (:), the page
     * refers no wiki in particular, and will never imply any other
     * PagePermission.
     * @param page the wiki page
     * @param actions the allowed actions for this page
     */
    public PagePermission( String page, String actions )
    {
        super( page );

        // Parse wiki and page (which may include wiki name and page)
        String pathParams[] = page.split( WIKI_SEPARATOR );
        if ( pathParams.length >= 2 )
        {
            m_wiki = pathParams[0].length() > 0 ? pathParams[0] : null;
            m_page = pathParams[1];
        }
        else
        {
            m_wiki = null;
            m_page = pathParams[0];
        }

        // Parse actions
        String pageActions[] = actions.toLowerCase().split( ACTION_SEPARATOR );
        Arrays.sort( pageActions, String.CASE_INSENSITIVE_ORDER );
        m_mask = createMask( actions );
        StringBuffer buffer = new StringBuffer();
        for( int i = 0; i < pageActions.length; i++ )
        {
            buffer.append( pageActions[i] );
            if ( i < ( pageActions.length - 1 ) )
            {
                buffer.append( ACTION_SEPARATOR );
            }
        }
        m_actionString = buffer.toString();
    }

    /**
     * Creates a new PagePermission for a specified page and set of actions.
     * @param page
     * @param actions
     */
    public PagePermission( WikiPage page, String actions )
    {
        this( page.getWiki() + WIKI_SEPARATOR + page.getName(), actions );
    }

    /**
     * Two PagePermission objects are considered equal if their actions (after
     * normalization), wiki and target are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( !( obj instanceof PagePermission ) )
        {
            return false;
        }
        PagePermission p = (PagePermission) obj;
        return ( p.m_mask == m_mask && p.m_page.equals( m_page ) 
                 && p.m_wiki != null && p.m_wiki.equals( m_wiki ));
    }

    /**
     * Returns the actions for this permission: "view", "edit", "comment",
     * "upload" or "delete". The actions will always be sorted in alphabetic
     * order, and will always appear in lower case.
     * @see java.security.Permission#getActions()
     */
    public final String getActions()
    {
        return m_actionString;
    }
    
    /**
     * Returns the name of the wiki page represented by this permission.
     * @return the page name
     */
    public final String getPage()
    {
        return m_page;
    }
    
    /**
     * Returns the name of the wiki containing the page represented by
     * this permission; may return the wildcard string.
     * @return the wiki
     */
    public final String getWiki()
    {
        return m_wiki;
    }

    /**
     * Returns the hash code for this PagePermission.
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode()
    {
        return m_mask + ( ( 13 * m_actionString.hashCode() ) * 23 * m_wiki.hashCode() );
    }

    /**
     * <p>
     * PagePermission can only imply other PagePermissions; no other permission
     * types are implied. One PagePermission implies another if its actions if
     * three conditions are met:
     * </p>
     * <ol>
     * <li>The other PagePermission's wiki is equal to, or a subset of, that of
     * this permission. This permission's wiki is considered a superset of the
     * other if it contains a matching prefix plus a wildcard, or a wildcard
     * followed by a matching suffix.</li>
     * <li>The other PagePermission's target is equal to, or a subset of, the
     * target specified by this permission. This permission's target is
     * considered a superset of the other if it contains a matching prefix plus
     * a wildcard, or a wildcard followed by a matching suffix.</li>
     * <li>All of other PagePermission's actions are equal to, or a subset of,
     * those of this permission</li>
     * </ol>
     * <p>
     * Note: a significant (hard-coded) exception to the rule occurs with pages
     * starting in
     * {@link com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager#GROUP_PREFIX},
     * because these are group member list pages. For a permission whose target
     * is the wildcard "*", permission for Group* pages is <em>not</em>
     * implied. The target "Group*", however, works normally. This is most
     * definitely a horrible hack.
     * </p>
     * @see java.security.Permission#implies(java.security.Permission)
     */
    public final boolean implies( Permission permission )
    {
        // Permission must be a PagePermission
        if ( !( permission instanceof PagePermission ) )
        {
            return false;
        }

        // Build up an "implied mask"
        PagePermission p = (PagePermission) permission;
        int impliedMask = impliedMask( m_mask );

        // If actions aren't a proper subset, return false
        if ( ( impliedMask & p.m_mask ) != p.m_mask )
        {
            return false;
        }

        // See if the tested permission's wiki is implied
        boolean impliedWiki = isSubset( m_wiki, p.m_wiki );

        // Special case: if this page is "*", the tested permission's
        // page is implied UNLESS it starts with "Group"
        boolean impliedPage;
        if ( m_page.equals( WILDCARD ) && p.m_page.startsWith( DefaultGroupManager.GROUP_PREFIX ) )
        {
            impliedPage = false;
        }
        else
        {
            impliedPage = isSubset( m_page, p.m_page );
        }

        return ( impliedWiki && impliedPage );
    }

    /**
     * Prints a human-readable representation of this permission.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        String wiki = ( m_wiki == null ) ? "" : m_wiki;
        return "(\"" + this.getClass().getName() + "\",\"" + wiki + WIKI_SEPARATOR + m_page + "\",\"" + getActions() + "\")";
    }

    /**
     * Creates an "implied mask" based on the actions originally assigned: for
     * example, delete implies edit, comment, upload and view.
     * @param mask binary mask for actions
     * @return binary mask for implied actions
     */
    protected static final int impliedMask( int mask )
    {
        if ( ( mask & DELETE_MASK ) > 0 )
        {
            mask |= EDIT_MASK;
        }
        if ( ( mask & RENAME_MASK ) > 0 )
        {
            mask |= EDIT_MASK;
        }
        if ( ( mask & EDIT_MASK ) > 0 )
        {
            mask |= ( COMMENT_MASK | UPLOAD_MASK );
        }
        if ( ( mask & COMMENT_MASK ) > 0 )
        {
            mask |= VIEW_MASK;
        }
        if ( ( mask & UPLOAD_MASK ) > 0 )
        {
            mask |= VIEW_MASK;
        }
        return mask;
    }

    /**
     * Determines whether one target string is a logical subset of the other.
     * @param superSet the prospective superset
     * @param subSet the prospective subset
     * @return the results of the test, where <code>true</code> indicates that
     *         <code>subSet</code> is a subset of <code>superSet</code>
     */
    protected static final boolean isSubset( String superSet, String subSet )
    {
        // If either is null, return false
        if ( superSet == null || subSet == null )
        {
            return false;
        }

        // If targets are identical, it's a subset
        if ( superSet.equals( subSet ) )
        {
            return true;
        }

        // If super is "*", it's a subset
        if ( superSet.equals( WILDCARD ) )
        {
            return true;
        }

        // If super starts with "*", sub must end with everything after the *
        if ( superSet.startsWith( WILDCARD ) )
        {
            String suffix = superSet.substring( 1 );
            return subSet.endsWith( suffix );
        }

        // If super ends with "*", sub must start with everything before *
        if ( superSet.endsWith( WILDCARD ) )
        {
            String prefix = superSet.substring( 0, superSet.length() - 2 );
            return subSet.startsWith( prefix );
        }

        return false;
    }

    /**
     * Private method that creates a binary mask based on the actions specified.
     * This is used by {@link #implies(Permission)}.
     * @param actions the actions for this permission, separated by commas
     * @return the binary actions mask
     */
    protected static final int createMask( String actions )
    {
        if ( actions == null || actions.length() == 0 )
        {
            throw new IllegalArgumentException( "Actions cannot be blank or null" );
        }
        int mask = 0;
        String[] actionList = actions.split( ACTION_SEPARATOR );
        for( int i = 0; i < actionList.length; i++ )
        {
            String action = actionList[i];
            if ( action.equalsIgnoreCase( VIEW_ACTION ) )
            {
                mask |= VIEW_MASK;
            }
            else if ( action.equalsIgnoreCase( EDIT_ACTION ) )
            {
                mask |= EDIT_MASK;
            }
            else if ( action.equalsIgnoreCase( COMMENT_ACTION ) )
            {
                mask |= COMMENT_MASK;
            }
            else if ( action.equalsIgnoreCase( UPLOAD_ACTION ) )
            {
                mask |= UPLOAD_MASK;
            }
            else if ( action.equalsIgnoreCase( DELETE_ACTION ) )
            {
                mask |= DELETE_MASK;
            }
            else if ( action.equalsIgnoreCase( RENAME_ACTION ) )
            {
                mask |= RENAME_MASK;
            }
            else
            {
                throw new IllegalArgumentException( "Unrecognized action: " + action );
            }
        }
        return mask;
    }
}