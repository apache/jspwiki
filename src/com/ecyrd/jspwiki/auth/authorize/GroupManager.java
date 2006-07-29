package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.event.EventSourceDelegate;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventSource;
import com.ecyrd.jspwiki.ui.InputValidator;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 * <p>
 * Facade class for storing, retrieving and managing wiki groups on behalf of
 * AuthorizationManager, JSPs and other presentation-layer classes. GroupManager
 * works in collaboration with a back-end {@link GroupDatabase}, which persists
 * groups to permanent storage.
 * </p>
 * <p>
 * <em>Note: prior to JSPWiki 2.4.19, GroupManager was an interface; it
 * is now a concrete, final class. The aspects of GroupManager which previously
 * extracted group information from storage (e.g., wiki pages) have been
 * refactored into the GroupDatabase interface.</em>
 * </p>
 * @author Andrew Jaquith
 * @version $Revision: 1.7 $ $Date: 2006-07-29 19:18:01 $
 * @since 2.4.19
 */
public final class GroupManager implements Authorizer, WikiEventSource

{
    /**
     * Tiny little listener that captures wiki events fired by Groups this
     * GroupDatabase knows about. When events are captured, they are forwarded
     * on to listeners registered with the GroupDatabase instance.
     */
    public static class GroupListener implements WikiEventListener
    {
        GroupManager m_manager;

        /**
         * Constructs a new instance of this listener.
         * @param manager the enclosing GroupManager
         */
        public GroupListener( GroupManager manager )
        {
            m_manager = manager;
        }

        /**
         * Captures wiki events fired by member wiki Groups and forwards them on
         * to WikiEventListeners registered with this instance. TODO: enclose
         * this in an inner class at some point...
         * @see com.ecyrd.jspwiki.event.WikiEventListener#actionPerformed(com.ecyrd.jspwiki.event.WikiEvent)
         */
        public void actionPerformed( WikiEvent event )
        {
            if ( event instanceof WikiSecurityEvent )
            {
                m_manager.fireEvent( event );
            }
        }

    }

    public static final String  MESSAGES_KEY       = "group";

    private static final String PROP_GROUPDATABASE = "jspwiki.groupdatabase";

    static final Logger         log                = Logger.getLogger( GroupManager.class );

    protected WikiEngine        m_engine;

    protected WikiEventListener m_groupListener;

    private GroupDatabase       m_groupDatabase    = null;

    /** Map with GroupPrincipals as keys, and Groups as values */
    private final Map           m_groups           = new HashMap();
    
    /** The name of this wiki. */
    private String              m_wiki             = null;

    /** Delegate for managing event listeners */
    private EventSourceDelegate m_listeners        = new EventSourceDelegate();

    /**
     * @see com.ecyrd.jspwiki.event.WikiEventSource#addWikiEventListener(WikiEventListener)
     */
    public final void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.addWikiEventListener( listener );
    }

    /**
     * <p>
     * Returns a GroupPrincipal matching a given name. If a group cannot be
     * found, return <code>null</code>.
     * </p>
     * @param name Name of the group. This is case-sensitive.
     * @return A DefaultGroup instance.
     */
    public Principal findRole( String name )
    {
        try
        {
            Group group = getGroup( name );
            return group.getPrincipal();
        }
        catch( NoSuchPrincipalException e )
        {
            return null;
        }
    }

    /**
     * Returns the Group matching a given name. If the group cannot be found,
     * this method throws a <code>NoSuchPrincipalException</code>.
     * @param name the name of the group to find
     * @return the group
     */
    public final Group getGroup( String name ) throws NoSuchPrincipalException
    {
        Group group = (Group) m_groups.get( new GroupPrincipal( m_wiki, name ) );
        if ( group != null )
        {
            return group;
        }
        throw new NoSuchPrincipalException( "Group " + name + " not found." );
    }

    /**
     * Returns the current external {@link GroupDatabase} in use. This method
     * is guaranteed to return a properly-initialized GroupDatabase, unless
     * it could not be initialized. In that case, this method throws
     * a {@link com.ecyrd.jspwiki.WikiException}. The GroupDatabase
     * is lazily initialized.
     * @throws com.ecyrd.jspwiki.auth.WikiSecurityException if the GroupDatabase could
     * not be initialized
     * @return the current GroupDatabase
     * @since 2.3
     */
    public final GroupDatabase getGroupDatabase() throws WikiSecurityException
    {
        if ( m_groupDatabase != null )
        {
            return m_groupDatabase;
        }

        String dbClassName = "<unknown>";
        String dbInstantiationError = null;
        Throwable cause = null;
        try
        {
            Properties props = m_engine.getWikiProperties();
            dbClassName = props.getProperty( PROP_GROUPDATABASE );
            if ( dbClassName == null )
            {
                dbClassName = XMLGroupDatabase.class.getName();
            }
            log.info( "Attempting to load group database class " + dbClassName );
            Class dbClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.authorize", dbClassName );
            m_groupDatabase = (GroupDatabase) dbClass.newInstance();
            m_groupDatabase.initialize( m_engine, m_engine.getWikiProperties() );
            log.info( "Group database initialized." );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "GroupDatabase class " + dbClassName + " cannot be found.", e );
            dbInstantiationError = "Failed to locate GroupDatabase class " + dbClassName;
            cause = e;
        }
        catch( InstantiationException e )
        {
            log.error( "GroupDatabase class " + dbClassName + " cannot be created.", e );
            dbInstantiationError = "Failed to create GroupDatabase class " + dbClassName;
            cause = e;
        }
        catch( IllegalAccessException e )
        {
            log.error( "You are not allowed to access group database class " + dbClassName + ".", e );
            dbInstantiationError = "Access GroupDatabase class " + dbClassName + " denied";
            cause = e;
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "Missing property: " + e.getMessage() + "." );
            dbInstantiationError = "Missing property: " + e.getMessage();
            cause = e;
        }

        if ( dbInstantiationError != null )
        {
            throw new WikiSecurityException( dbInstantiationError + " Cause: " + cause.getMessage() );
        }

        return m_groupDatabase;
    }

    /**
     * Returns an array of GroupPrincipals this GroupManager knows about. This
     * method will return an array of GroupPrincipal objects corresponding to
     * the wiki groups managed by this class. This method actually returns a
     * defensive copy of an internally stored hashmap.
     * @return an array of Principals representing the roles
     */
    public final Principal[] getRoles()
    {
        return (Principal[]) m_groups.keySet().toArray( new Principal[m_groups.size()] );
    }

    /**
     * Initializes the group cache by initializing the group database and
     * obtaining a list of all of the groups it stores.
     * @see GroupDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     * @see GroupDatabase#groups()
     */
    public final void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException
    {
        m_engine = engine;

        try
        {
            m_groupDatabase = getGroupDatabase();
        }
        catch ( WikiException e )
        {
            throw new WikiSecurityException( e.getMessage() );
        }

        m_groupListener = new GroupListener( this );
        m_wiki = engine.getApplicationName();

        // Load all groups from the database into the cache
        Group[] groups = m_groupDatabase.groups();
        synchronized( m_groups )
        {
            for( int i = 0; i < groups.length; i++ )
            {
                Group group = groups[i];
                // Add new group to cache; fire GROUP_ADD event
                m_groups.put( group.getPrincipal(), group );
                fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_ADD, group ) );
            }
        }

        // Success!
        log.info( "Authorizer GroupManager initialized successfully; loaded " + groups.length + " group(s)." );

    }

    /**
     * <p>
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, or if the user is
     * not authenticated, this method returns <code>false</code>.
     * </p>
     * <p>
     * With respect to this implementation, the supplied Principal must be a
     * GroupPrincipal. The Subject posesses the "role" if it the session is
     * authenticated <em>and</em> a Subject's principal is a member of the
     * corresponding Group. This method simply finds the Group in question, then
     * delegates to {@link Group#isMember(Principal)} for each of the principals
     * in the Subject's principal set.
     * </p>
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    public final boolean isUserInRole( WikiSession session, Principal role )
    {
        // Always return false if session/role is null, or if
        // role isn't a GroupPrincipal
        if ( session == null || role == null || !( role instanceof GroupPrincipal ) || !session.isAuthenticated() )
        {
            return false;
        }

        // Get the group we're examining
        Group group = (Group) m_groups.get( role );
        if ( group == null )
        {
            return false;
        }

        // Check each user principal to see if it belongs to the group
        Principal[] principals = session.getPrincipals();
        for ( int i = 0; i < principals.length; i++ )
        {
            if ( AuthenticationManager.isUserPrincipal( principals[i] ) && group.isMember( principals[i] ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Extracts group name and members from passed parameters and populates an
     * existing Group with them. The Group will either be a copy of an existing
     * Group (if one can be found), or a new, unregistered Group (if not).
     * Optionally, this method can throw a WikiSecurityException if the Group
     * does not yet exist in the GroupManager cache.
     * </p>
     * <p>
     * The <code>group</code> parameter in the HTTP request contains the Group
     * name to look up and populate. The <code>members</code> parameter
     * contains the member list. If these differ from those in the existing
     * group, the passed values override the old values.
     * </p>
     * <p>
     * This method does not commit the new Group to the GroupManager cache. To
     * do that, use {@link #setGroup(WikiSession, Group)}.
     * </p>
     * @param name the name of the group to construct
     * @param memberLine the line of text containing the group membership list
     * @param create whether this method should create a new, empty Group if one
     *            with the requested name is not found. If <code>false</code>,
     *            groups that do not exist will cause a
     *            <code>NoSuchPrincipalException</code> to be thrown
     * @return a new, populated group
     * @see com.ecyrd.jspwiki.auth.authorize.Group#RESTRICTED_GROUPNAMES
     * @throws WikiSecuityException if the group name isn't allowed
     */
    public final Group parseGroup( String name, String memberLine, boolean create ) throws NoSuchPrincipalException,
            WikiSecurityException
    {
        // If null name parameter, it's because someone's creating a new group
        if ( name == null )
        {   
            if ( create )
            {
                name = "MyGroup";
            }
            else
            {
                throw new WikiSecurityException( "Group name cannot be blank." );
            }
        } 
        else if ( ArrayUtils.contains( Group.RESTRICTED_GROUPNAMES, name ) )
        {
            // Certain names are forbidden
            throw new WikiSecurityException( "Illegal group name: " + name );
        }
        name = name.trim();

        // Normalize the member line
        if ( InputValidator.isBlank( memberLine ) )
        {
            memberLine = "";
        }
        memberLine.trim();

        // Create or retrieve the group (may have been previously cached)
        Group group = new Group( name, m_engine.getApplicationName() );
        try
        {
            Group existingGroup = getGroup( name );

            // If existing, clone it
            group.setCreator( existingGroup.getCreator() );
            group.setCreated( existingGroup.getCreated() );
            group.setModifier( existingGroup.getModifier() );
            group.setLastModified( existingGroup.getLastModified() );
            Principal[] existingMembers = existingGroup.members();
            for( int i = 0; i < existingMembers.length; i++ )
            {
                group.add( existingMembers[i] );
            }
        }
        catch( NoSuchPrincipalException e )
        {
            // It's a new group.... throw error if we don't create new ones
            if ( !create )
            {
                throw new NoSuchPrincipalException( "Group '" + name + "' does not exist." );
            }
        }

        // If passed members not empty, overwrite
        String[] members = extractMembers( memberLine );
        if ( members.length > 0 )
        {
            group.clear();
            for( int i = 0; i < members.length; i++ )
            {
                group.add( new WikiPrincipal( members[i] ) );
            }
        }

        return group;
    }

    /**
     * <p>
     * Extracts group name and members from the HTTP request and populates an
     * existing Group with them. The Group will either be a copy of an existing
     * Group (if one can be found), or a new, unregistered Group (if not).
     * Optionally, this method can throw a WikiSecurityException if the Group
     * does not yet exist in the GroupManager cache.
     * </p>
     * <p>
     * The <code>group</code> parameter in the HTTP request contains the Group
     * name to look up and populate. The <code>members</code> parameter
     * contains the member list. If these differ from those in the existing
     * group, the passed values override the old values.
     * </p>
     * <p>
     * This method does not commit the new Group to the GroupManager cache. To
     * do that, use {@link #setGroup(WikiSession, Group)}.
     * </p>
     * @param context the current wiki context
     * @param create whether this method should create a new, empty Group if one
     *            with the requested name is not found. If <code>false</code>,
     *            groups that do not exist will cause a
     *            <code>NoSuchPrincipalException</code> to be thrown
     * @return a new, populated group
     * @throws WikiSecuityException if the group name isn't allowed
     */
    public final Group parseGroup( WikiContext context, boolean create ) throws NoSuchPrincipalException,
            WikiSecurityException
    {
        // Extract parameters
        HttpServletRequest request = context.getHttpRequest();
        String name = request.getParameter( "group" );
        String memberLine = request.getParameter( "members" );

        // Create the named group; we pass on any NoSuchPrincipalExceptions
        // that may be thrown if create == false, or WikiSecurityExceptions
        Group group = parseGroup( name, memberLine, create );

        // If no members, add the current user by default
        if ( group.members().length == 0 )
        {
            group.add( context.getWikiSession().getUserPrincipal() );
        }

        return group;
    }

    /**
     * Removes a named Group from the group database. If not found, throws a
     * <code>NoSuchPrincipalException</code>. After removal, this method will
     * commit the delete to the back-end group database. It will also fire a
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE} event with
     * the GroupManager instance as the source and the Group as target.
     * @param index the group to remove
     * @see com.ecyrd.jspwiki.auth.authorize.GroupDatabase#delete(Group)
     * @throws IllegalArgumentException if <code>index</code> is
     *             <code>null</code>
     */
    public final void removeGroup( String index ) throws WikiSecurityException
    {
        if ( index == null )
        {
            throw new IllegalArgumentException( "Group cannot be null." );
        }

        Group group = (Group) m_groups.get( new GroupPrincipal( m_wiki, index ) );
        if ( group == null )
        {
            throw new NoSuchPrincipalException( "Group " + index + " not found" );
        }

        // Delete the group
        // TODO: need rollback procedure
        synchronized( m_groups )
        {
            m_groups.remove( group.getPrincipal() );
        }
        m_groupDatabase.delete( group );
        m_groupDatabase.commit();
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_REMOVE, group ) );
    }

    /**
     * @see com.ecyrd.jspwiki.event.WikiEventSource#removeWikiEventListener(WikiEventListener)
     */
    public final void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.removeWikiEventListener( listener );
    }

    /**
     * <p>
     * Saves the {@link Group} created by a user in a wiki session. This method
     * registers the Group with the GroupManager and saves it to the back-end
     * database. If an existing Group with the same name already exists, the new
     * group will overwrite it. After saving the Group, the group database
     * changes are committed.
     * </p>
     * <p>
     * This method fires the following events:
     * </p>
     * <ul>
     * <li><strong>When creating a new Group</strong>, this method fires a
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD} with the
     * GroupManager instance as its source and the new Group as the target.</li>
     * <li><strong>When overwriting an existing Group</strong>, this method
     * fires a new {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE}
     * with this GroupManager instance as the source, and the new Group as the
     * target. It then fires a
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD} event with the
     * same source and target.</li>
     * </ul>
     * <p>
     * In addition, if the save or commit actions fail, this method will attempt
     * to restore the older version of the wiki group if it exists. This will
     * result in a <code>GROUP_REMOVE</code> event (for the new version of the
     * Group) followed by a <code>GROUP_ADD</code> event (to indicate
     * restoration of the old version).
     * </p>
     * <p>
     * This method will register the new Group with the GroupManager. A
     * consequence of "registration" is that the GroupManager will listen for --
     * and forward -- group events such as
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD_MEMBER},
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_CLEAR_MEMBERS} and
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE_MEMBER} to
     * all event listeners attached to GroupManager via
     * {@link #addWikiEventListener(WikiEventListener)}. For example,
     * {@link com.ecyrd.jspwiki.auth.AuthenticationManager} attaches each
     * WikiSession as a GroupManager listener. Thus, the act of registering a
     * Group with <code>setGroup</code> means that all WikiSessions will
     * automatically receive group add/change/delete events immediately.
     * </p>
     * @param session the wiki session, which may not be <code>null</code>
     * @param group the Group, which may not be <code>null</code>
     */
    public final void setGroup( WikiSession session, Group group ) throws WikiSecurityException
    {
        // TODO: check for appropriate permissions

        // If group already exists, delete it; fire GROUP_REMOVE event
        Group oldGroup = (Group) m_groups.get( group.getPrincipal() );
        boolean groupExists = ( oldGroup != null );
        if ( groupExists )
        {
            fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_REMOVE, oldGroup ) );
            synchronized( m_groups )
            {
                m_groups.remove( oldGroup.getPrincipal() );
            }
        }

        // Copy existing modifier info & timestamps
        if ( groupExists )
        {
            group.setCreator( oldGroup.getCreator() );
            group.setCreated( oldGroup.getCreated() );
            group.setModifier( oldGroup.getModifier() );
            group.setLastModified( oldGroup.getLastModified() );
        }

        // Add new group to cache; announce GROUP_ADD event
        synchronized( m_groups )
        {
            m_groups.put( group.getPrincipal(), group );
        }
        group.addWikiEventListener( m_groupListener );
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_ADD, group ) );

        // Save the group to back-end database; if it fails,
        // roll back to previous state. Note that the back-end
        // MUST timestammp the create/modify fields in the Group.
        try
        {
            m_groupDatabase.save( group, session.getUserPrincipal() );
            m_groupDatabase.commit();
        }

        // We got an exception! Roll back...
        catch( WikiSecurityException e )
        {
            if ( groupExists )
            {
                // Restore previous version, re-throw...
                fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_REMOVE, group ) );
                fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_ADD, oldGroup ) );
                synchronized( m_groups )
                {
                    m_groups.put( oldGroup.getPrincipal(), oldGroup );
                }
                throw new WikiSecurityException( e.getMessage() + " (rolled back to previous version)." );
            }
            // Re-throw security exception
            throw new WikiSecurityException( e.getMessage() );
        }
    }

    /**
     * Validates a Group, and appends any errors to the session errors list. Any
     * validation errors are added to the wiki session's messages collection
     * (see {@link WikiSession#getMessages()}.
     * @param context the current wiki context
     * @param group the supplied Group
     */
    public final void validateGroup( WikiContext context, Group group )
    {
        WikiSession session = context.getWikiSession();
        InputValidator validator = new InputValidator( MESSAGES_KEY, session );

        // Name cannot be null or one of the restricted names
        try
        {
            checkGroupName( session, group.getName() );
        }
        catch( WikiSecurityException e )
        {

        }

        // Member names must be "safe" strings
        Principal[] members = group.members();
        for( int i = 0; i < members.length; i++ )
        {
            validator.validateNotNull( members[i].getName(), "Full name" );
        }
    }

    /**
     * Extracts carriage-return separated members into a Set of String objects.
     * @param memberLine the list of members
     * @return the list of members
     */
    protected final String[] extractMembers( String memberLine )
    {
        Set members = new HashSet();
        if ( memberLine != null )
        {
            StringTokenizer tok = new StringTokenizer( memberLine, "\n" );
            while( tok.hasMoreTokens() )
            {
                String uid = tok.nextToken().trim();
                if ( uid != null && uid.length() > 0 )
                {
                    members.add( uid );
                }
            }
        }
        return (String[]) members.toArray( new String[members.size()] );
    }

    /**
     * @see com.ecyrd.jspwiki.event.EventSourceDelegate#fireEvent(com.ecyrd.jspwiki.event.WikiEvent)
     */
    protected final void fireEvent( WikiEvent event )
    {
        m_listeners.fireEvent( event );
    }

    /**
     * Checks if a String is blank or a restricted Group name, and if it is,
     * appends an error to the WikiSession's message list.
     * @param session the wiki session
     * @param name the group name to test
     * @see Group#RESTRICTED_GROUPNAMES
     */
    protected final void checkGroupName( WikiSession session, String name ) throws WikiSecurityException
    {
        //TODO: groups cannot have the same name as a user
        
        if ( session == null )
        {
            throw new WikiSecurityException( "Session cannot be null." );
        }

        // Name cannot be null
        InputValidator validator = new InputValidator( MESSAGES_KEY, session );
        validator.validateNotNull( name, "Group name" );

        // Name cannot be one of the restricted names either
        if ( ArrayUtils.contains( Group.RESTRICTED_GROUPNAMES, name ) )
            ;
        {
            throw new WikiSecurityException( "The group name '" + name + "' is illegal. Choose another." );
        }
    }

}