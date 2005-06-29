package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 * <p>
 * This default UserDatabase implementation provides groups to JSPWiki.
 * </p>
 * <p>
 * Groups are based on WikiPages. The name of the page determines the group name
 * (as a convention, we suggest the name of the page ends in DefaultGroup, e.g.
 * EditorGroup). By setting attribute 'members' on the page, the named members
 * are added to the group:
 * 
 * <pre>
 * 
 *  
 *   [{SET members fee fie foe foo}]
 *   
 *  
 * </pre>
 * 
 * <p>
 * The list of members can be separated by commas or spaces.
 * <p>
 * TODO: are 'named members' supposed to be usernames, or are group names
 * allowed? (Suggestion: both)
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class DefaultGroupManager implements GroupManager

{

    /**
     * This special filter class is used to refresh the database after a page
     * has been changed.
     */

    // FIXME: JSPWiki should really take care of itself that any metadata
    //        relevant to a page is refreshed.
    public class SaveFilter extends BasicPageFilter
    {
        public void postSave( WikiContext context, String content )
        {
            AuthorizationManager auth = m_engine.getAuthorizationManager();
            if (auth.checkPermission( context, WikiPermission.CREATE_GROUPS )) {
                
                // Parse groups if name starts with GROUP_PREFIX
                WikiPage p = context.getPage();
                if (p.getName().startsWith(DefaultGroupManager.GROUP_PREFIX)) {
                    log.debug( "Skimming through page " + p.getName() + " to see if there are new groups..." );

                    m_engine.textToHTML( context, content );
                    
                    String groupName = p.getName().substring(DefaultGroupManager.GROUP_PREFIX.length());
                    String members = (String) p.getAttribute( ATTR_MEMBERLIST );

                    updateGroup( groupName, parseMemberList( members ) );
                }
            }
        }
    }

    /**
     * The attribute to set on a page - [{SET members ...}] - to define members
     * of the group named by that page.
     */
    public static final String ATTR_MEMBERLIST = "members";

    static final Logger        log             = Logger.getLogger( DefaultGroupManager.class );

    private WikiEngine         m_engine;

    private final HashMap      m_groups        = new HashMap();

    public static final String GROUP_PREFIX = "Group";

    /**
     * Adds a Group to the group cache. Note that this method fail, and will
     * throw an <code>IllegalArgumentException</code>, if the proposed group
     * is the same name as one of the built-in Roles: e.g., Admin,
     * Authenticated, etc.
     * @param group the Group to add
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#add(Group)
     * @throws IllegalArgumentException if the group name isn't allowed
     */
    public synchronized void add( Group group )
    {
        //TODO: this should throw a checked exception so callers can recover
        for( int i = 0; i < Group.RESTRICTED_GROUPNAMES.length; i++ )
        {
            if ( group.equals( Group.RESTRICTED_GROUPNAMES[i] ) )
            {
                throw new IllegalArgumentException( "Group name " + group.getName() + " is not allowed." );
            }
        }
        m_groups.put( group.getName(), group );
    }

    /**
     * Commits the groups to disk. This method is a no-op, since the wiki's page
     * SaveFilter does this.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#commit()
     */
    public void commit()
    {
    }

    /**
     * Returns <code>true</code> if a Group is known to the GroupManager
     * (contained in the group cache), <code>false</code> otherwise.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#exists(Group)
     */
    public boolean exists( Group group )
    {
        Object found = m_groups.get( group.getName() );
        return ( found != null );
    }

    /**
     * <p>
     * Returns a Group matching a given name. If a group cannot be found,
     * return null.
     * </p>
     * @param name Name of the group. This is case-sensitive.
     * @return A DefaultGroup instance.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#findRole(java.lang.String)
     */
    public Principal findRole( String name )
    {
        return (Group) m_groups.get( name );
    }

    /**
     * Initializes the group cache by adding a {@link SaveFilter}to the page
     * manager, so that groups are updated when pages are saved. This method
     * also calls {@link #reload()}.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;

        m_engine.getFilterManager().addPageFilter( new SaveFilter(), 1000000 );
     
        reload();
    }

    /**
     * Determines whether a particular Subject is considered a member of a given
     * Group. This method simply finds the group in question, then delegates to
     * {@link Group#isMember(Principal)}for each of the principals in the
     * Subject's principal set.
     * @param context the wiki context. Not used in this implementation, so
     *            <code>null</code> is permitted
     * @param subject the subject about whom membership statis is sought
     * @param role the Group to search. If null, this method always returns false
     * @see com.ecyrd.jspwiki.auth.Authorizer#isUserInRole(WikiContext, Subject, Principal)
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role )
    {
        Object group = m_groups.get( role.getName() );
        if ( group != null && subject != null )
        {
            for( Iterator it = subject.getPrincipals().iterator(); it.hasNext(); )
            {
                Principal principal = (Principal) it.next();
                if ( ( (Group) group ).isMember( principal ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reloads the group cache by iterating through all wiki pages and updating
     * groups whenever a [{SET members ...}] tag is found. For each group
     * definition found, the protected method {@link #updateGroup(String, List)}
     * is called.
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#reload()
     */
    public void reload()
    {
        log.info( "Loading user database group information from wiki pages..." );

        try
        {
            Collection allPages = m_engine.getPageManager().getAllPages();

            m_groups.clear();

            for( Iterator i = allPages.iterator(); i.hasNext(); )
            {
                WikiPage p = (WikiPage) i.next();

                // lazy loading of pages with ACLManager not possible,
                // because the authentication information must be
                // present on wiki initialization

                List memberList = parseMemberList( (String) p.getAttribute( ATTR_MEMBERLIST ) );

                if ( memberList != null )
                {
                    String groupName = p.getName().substring(DefaultGroupManager.GROUP_PREFIX.length());
                    updateGroup( groupName, memberList );
                }
            }
        }
        catch( ProviderException e )
        {
            log.fatal( "Cannot start database", e );
        }
    }

    /**
     * Removes a Group from the group cache.
     * @param group the group to remove
     * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#remove(Group)
     */
    public synchronized void remove( Group group )
    {
        if ( group == null )
        {
            throw new IllegalArgumentException( "Group cannot be null." );
        }
        m_groups.remove( group.getName() );
    }

    /**
     * Protected method that parses through the group membership list on a wiki
     * page, and returns a List containing the member names as Strings.
     * @param memberLine the line of text containing the group membership list
     * @return the member names, as a List of Strings
     */
    protected List parseMemberList( String memberLine )
    {
        if ( memberLine == null )
            return null;

        log.debug( "Parsing member list: " + memberLine );

        StringTokenizer tok = new StringTokenizer( memberLine, ", " );

        ArrayList members = new ArrayList();

        while( tok.hasMoreTokens() )
        {
            String uid = tok.nextToken();

            log.debug( "  Adding member: " + uid );

            members.add( uid );
        }

        return members;
    }

    /**
     * Updates a named group with a List of new members. The List is a
     * collection of Strings that denotes members of this group. Each member is
     * added to the group as a WikiPrincipal. If the group already exists in the
     * cache, the List contents are added to the existing membership. If the
     * group doesn't exist, it is created. If the List contains no members, the
     * group is removed from the cache.
     * @param groupName the name of the group to update
     * @param memberList the members to add to the group definition
     */
    protected synchronized void updateGroup( String groupName, List memberList )
    {
        Group group = (Group) m_groups.get( groupName );

        if ( group == null && memberList == null )
        {
            return;
        }

        if ( group == null && memberList != null )
        {
            log.debug( "Adding new group: " + groupName );

            group = new DefaultGroup( groupName );
        }

        if ( group != null && memberList == null )
        {
            log.debug( "Detected removed group: " + groupName );

            m_groups.remove( groupName );

            return;
        }

        for( Iterator j = memberList.iterator(); j.hasNext(); )
        {
            Principal udp = new WikiPrincipal( (String) j.next() );

            group.add( udp );

            log.debug( "** Added member: " + udp.getName() );
        }

        add( group );
    }
}