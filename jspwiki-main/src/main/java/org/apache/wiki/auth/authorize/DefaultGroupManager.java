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
package org.apache.wiki.auth.authorize;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Authorizer;
import org.apache.wiki.auth.GroupPrincipal;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.ui.InputValidator;
import org.apache.wiki.util.ClassUtil;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * <p>
 * Facade class for storing, retrieving and managing wiki groups on behalf of AuthorizationManager, JSPs and other presentation-layer
 * classes. GroupManager works in collaboration with a back-end {@link GroupDatabase}, which persists groups to permanent storage.
 * </p>
 * <p>
 * <em>Note: prior to JSPWiki 2.4.19, GroupManager was an interface; it is now a concrete, final class. The aspects of GroupManager
 * which previously extracted group information from storage (e.g., wiki pages) have been refactored into the GroupDatabase interface.</em>
 * </p>
 * @since 2.4.19
 */
public class DefaultGroupManager implements GroupManager, Authorizer, WikiEventListener {

    private static final Logger log = Logger.getLogger( DefaultGroupManager.class );

    protected Engine m_engine;

    protected WikiEventListener m_groupListener;

    private GroupDatabase m_groupDatabase;

    /** Map with GroupPrincipals as keys, and Groups as values */
    private final Map< Principal, Group > m_groups = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public Principal findRole( final String name ) {
        try {
            final Group group = getGroup( name );
            return group.getPrincipal();
        } catch( final NoSuchPrincipalException e ) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Group getGroup( final String name ) throws NoSuchPrincipalException {
        final Group group = m_groups.get( new GroupPrincipal( name ) );
        if( group != null ) {
            return group;
        }
        throw new NoSuchPrincipalException( "Group " + name + " not found." );
    }

    /** {@inheritDoc} */
    @Override
    public GroupDatabase getGroupDatabase() throws WikiSecurityException {
        if( m_groupDatabase != null ) {
            return m_groupDatabase;
        }

        String dbClassName = "<unknown>";
        String dbInstantiationError = null;
        Throwable cause = null;
        try {
            final Properties props = m_engine.getWikiProperties();
            dbClassName = props.getProperty( PROP_GROUPDATABASE );
            if( dbClassName == null ) {
                dbClassName = XMLGroupDatabase.class.getName();
            }
            log.info( "Attempting to load group database class " + dbClassName );
            final Class< ? > dbClass = ClassUtil.findClass( "org.apache.wiki.auth.authorize", dbClassName );
            m_groupDatabase = ( GroupDatabase )dbClass.newInstance();
            m_groupDatabase.initialize( m_engine, m_engine.getWikiProperties() );
            log.info( "Group database initialized." );
        } catch( final ClassNotFoundException e ) {
            log.error( "GroupDatabase class " + dbClassName + " cannot be found.", e );
            dbInstantiationError = "Failed to locate GroupDatabase class " + dbClassName;
            cause = e;
        } catch( final InstantiationException e ) {
            log.error( "GroupDatabase class " + dbClassName + " cannot be created.", e );
            dbInstantiationError = "Failed to create GroupDatabase class " + dbClassName;
            cause = e;
        } catch( final IllegalAccessException e ) {
            log.error( "You are not allowed to access group database class " + dbClassName + ".", e );
            dbInstantiationError = "Access GroupDatabase class " + dbClassName + " denied";
            cause = e;
        } catch( final NoRequiredPropertyException e ) {
            log.error( "Missing property: " + e.getMessage() + "." );
            dbInstantiationError = "Missing property: " + e.getMessage();
            cause = e;
        }

        if( dbInstantiationError != null ) {
            throw new WikiSecurityException( dbInstantiationError + " Cause: " + cause.getMessage(), cause );
        }

        return m_groupDatabase;
    }

    /** {@inheritDoc} */
    @Override
    public Principal[] getRoles() {
        return m_groups.keySet().toArray( new Principal[0] );
    }

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws WikiSecurityException {
        m_engine = engine;

        try {
            m_groupDatabase = getGroupDatabase();
        } catch( final WikiException e ) {
            throw new WikiSecurityException( e.getMessage(), e );
        }

        // Load all groups from the database into the cache
        final Group[] groups = m_groupDatabase.groups();
        synchronized( m_groups ) {
            for( final Group group : groups ) {
                // Add new group to cache; fire GROUP_ADD event
                m_groups.put( group.getPrincipal(), group );
                fireEvent( WikiSecurityEvent.GROUP_ADD, group );
            }
        }

        // Make the GroupManager listen for WikiEvents (WikiSecurityEvents for changed user profiles)
        engine.getManager( UserManager.class ).addWikiEventListener( this );

        // Success!
        log.info( "Authorizer GroupManager initialized successfully; loaded " + groups.length + " group(s)." );
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserInRole( final Session session, final Principal role ) {
        // Always return false if session/role is null, or if role isn't a GroupPrincipal
        if ( session == null || !( role instanceof GroupPrincipal ) || !session.isAuthenticated() ) {
            return false;
        }

        // Get the group we're examining
        final Group group = m_groups.get( role );
        if( group == null ) {
            return false;
        }

        // Check each user principal to see if it belongs to the group
        for( final Principal principal : session.getPrincipals() ) {
            if( AuthenticationManager.isUserPrincipal( principal ) && group.isMember( principal ) ) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Group parseGroup( String name, String memberLine, final boolean create ) throws WikiSecurityException {
        // If null name parameter, it's because someone's creating a new group
        if( name == null ) {
            if( create ) {
                name = "MyGroup";
            } else {
                throw new WikiSecurityException( "Group name cannot be blank." );
            }
        } else if( ArrayUtils.contains( Group.RESTRICTED_GROUPNAMES, name ) ) {
            // Certain names are forbidden
            throw new WikiSecurityException( "Illegal group name: " + name );
        }
        name = name.trim();

        // Normalize the member line
        if( InputValidator.isBlank( memberLine ) ) {
            memberLine = "";
        }
        memberLine = memberLine.trim();

        // Create or retrieve the group (may have been previously cached)
        final Group group = new Group( name, m_engine.getApplicationName() );
        try {
            final Group existingGroup = getGroup( name );

            // If existing, clone it
            group.setCreator( existingGroup.getCreator() );
            group.setCreated( existingGroup.getCreated() );
            group.setModifier( existingGroup.getModifier() );
            group.setLastModified( existingGroup.getLastModified() );
            for( final Principal existingMember : existingGroup.members() ) {
                group.add( existingMember );
            }
        } catch( final NoSuchPrincipalException e ) {
            // It's a new group.... throw error if we don't create new ones
            if( !create ) {
                throw new NoSuchPrincipalException( "Group '" + name + "' does not exist." );
            }
        }

        // If passed members not empty, overwrite
        final String[] members = extractMembers( memberLine );
        if( members.length > 0 ) {
            group.clear();
            for( final String member : members ) {
                group.add( new WikiPrincipal( member ) );
            }
        }

        return group;
    }

    /** {@inheritDoc} */
    @Override
    public void removeGroup( final String index ) throws WikiSecurityException {
        if( index == null ) {
            throw new IllegalArgumentException( "Group cannot be null." );
        }

        final Group group = m_groups.get( new GroupPrincipal( index ) );
        if( group == null ) {
            throw new NoSuchPrincipalException( "Group " + index + " not found" );
        }

        // Delete the group
        // TODO: need rollback procedure
        synchronized( m_groups ) {
            m_groups.remove( group.getPrincipal() );
        }
        m_groupDatabase.delete( group );
        fireEvent( WikiSecurityEvent.GROUP_REMOVE, group );
    }

    /** {@inheritDoc} */
    @Override
    public void setGroup( final Session session, final Group group ) throws WikiSecurityException {
        // TODO: check for appropriate permissions

        // If group already exists, delete it; fire GROUP_REMOVE event
        final Group oldGroup = m_groups.get( group.getPrincipal() );
        if( oldGroup != null ) {
            fireEvent( WikiSecurityEvent.GROUP_REMOVE, oldGroup );
            synchronized( m_groups ) {
                m_groups.remove( oldGroup.getPrincipal() );
            }
        }

        // Copy existing modifier info & timestamps
        if( oldGroup != null ) {
            group.setCreator( oldGroup.getCreator() );
            group.setCreated( oldGroup.getCreated() );
            group.setModifier( oldGroup.getModifier() );
            group.setLastModified( oldGroup.getLastModified() );
        }

        // Add new group to cache; announce GROUP_ADD event
        synchronized( m_groups ) {
            m_groups.put( group.getPrincipal(), group );
        }
        fireEvent( WikiSecurityEvent.GROUP_ADD, group );

        // Save the group to back-end database; if it fails, roll back to previous state. Note that the back-end
        // MUST timestammp the create/modify fields in the Group.
        try {
            m_groupDatabase.save( group, session.getUserPrincipal() );
        }

        // We got an exception! Roll back...
        catch( final WikiSecurityException e ) {
            if( oldGroup != null ) {
                // Restore previous version, re-throw...
                fireEvent( WikiSecurityEvent.GROUP_REMOVE, group );
                fireEvent( WikiSecurityEvent.GROUP_ADD, oldGroup );
                synchronized( m_groups ) {
                    m_groups.put( oldGroup.getPrincipal(), oldGroup );
                }
                throw new WikiSecurityException( e.getMessage() + " (rolled back to previous version).", e );
            }
            // Re-throw security exception
            throw new WikiSecurityException( e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void validateGroup( final Context context, final Group group ) {
        final InputValidator validator = new InputValidator( MESSAGES_KEY, context );

        // Name cannot be null or one of the restricted names
        try {
            checkGroupName( context, group.getName() );
        } catch( final WikiSecurityException e ) {
        }

        // Member names must be "safe" strings
        final Principal[] members = group.members();
        for( final Principal member : members ) {
            validator.validateNotNull( member.getName(), "Full name", InputValidator.ID );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkGroupName( final Context context, final String name ) throws WikiSecurityException {
        // TODO: groups cannot have the same name as a user

        // Name cannot be null
        final InputValidator validator = new InputValidator( MESSAGES_KEY, context );
        validator.validateNotNull( name, "Group name" );

        // Name cannot be one of the restricted names either
        if( ArrayUtils.contains( Group.RESTRICTED_GROUPNAMES, name ) ) {
            throw new WikiSecurityException( "The group name '" + name + "' is illegal. Choose another." );
        }
    }

    /**
     * Extracts carriage-return separated members into a Set of String objects.
     *
     * @param memberLine the list of members
     * @return the list of members
     */
    protected String[] extractMembers( final String memberLine ) {
        final Set< String > members = new HashSet<>();
        if( memberLine != null ) {
            final StringTokenizer tok = new StringTokenizer( memberLine, "\n" );
            while( tok.hasMoreTokens() ) {
                final String uid = tok.nextToken().trim();
                if( !uid.isEmpty() ) {
                    members.add( uid );
                }
            }
        }
        return members.toArray( new String[0] );
    }

    // events processing .......................................................

    /** {@inheritDoc} */
    @Override
    public synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( !( event instanceof WikiSecurityEvent ) ) {
            return;
        }

        final WikiSecurityEvent se = ( WikiSecurityEvent )event;
        if( se.getType() == WikiSecurityEvent.PROFILE_NAME_CHANGED ) {
            final Session session = se.getSrc();
            final UserProfile[] profiles = ( UserProfile[] )se.getTarget();
            final Principal[] oldPrincipals = new Principal[] { new WikiPrincipal( profiles[ 0 ].getLoginName() ),
                    new WikiPrincipal( profiles[ 0 ].getFullname() ), new WikiPrincipal( profiles[ 0 ].getWikiName() ) };
            final Principal newPrincipal = new WikiPrincipal( profiles[ 1 ].getFullname() );

            // Examine each group
            int groupsChanged = 0;
            try {
                for( final Group group : m_groupDatabase.groups() ) {
                    boolean groupChanged = false;
                    for( final Principal oldPrincipal : oldPrincipals ) {
                        if( group.isMember( oldPrincipal ) ) {
                            group.remove( oldPrincipal );
                            group.add( newPrincipal );
                            groupChanged = true;
                        }
                    }
                    if( groupChanged ) {
                        setGroup( session, group );
                        groupsChanged++;
                    }
                }
            } catch( final WikiException e ) {
                // Oooo! This is really bad...
                log.error( "Could not change user name in Group lists because of GroupDatabase error:" + e.getMessage() );
            }
            log.info( "Profile name change for '" + newPrincipal.toString() + "' caused " + groupsChanged + " groups to change also." );
        }
    }

}
