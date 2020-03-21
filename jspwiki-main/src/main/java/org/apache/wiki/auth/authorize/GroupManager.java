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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.Authorizer;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;

import javax.servlet.http.HttpServletRequest;


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
public interface GroupManager extends Authorizer, WikiEventListener {

    /** Key used for adding UI messages to a user's Session. */
    String MESSAGES_KEY = "group";

    String PROP_GROUPDATABASE = "jspwiki.groupdatabase";

    /**
     * Returns the Group matching a given name. If the group cannot be found, this method throws a <code>NoSuchPrincipalException</code>.
     *
     * @param name the name of the group to find
     * @return the group
     * @throws NoSuchPrincipalException if the group cannot be found
     */
    Group getGroup( final String name ) throws NoSuchPrincipalException;

    /**
     * Returns the current external {@link GroupDatabase} in use. This method is guaranteed to return a properly-initialized GroupDatabase,
     * unless it could not be initialized. In that case, this method throws a {@link org.apache.wiki.api.exceptions.WikiException}. The
     * GroupDatabase is lazily initialized.
     *
     * @throws org.apache.wiki.auth.WikiSecurityException if the GroupDatabase could not be initialized
     * @return the current GroupDatabase
     * @since 2.3
     */
    GroupDatabase getGroupDatabase() throws WikiSecurityException;

    /**
     * <p>
     * Extracts group name and members from passed parameters and populates an existing Group with them. The Group will either be a copy of
     * an existing Group (if one can be found), or a new, unregistered Group (if not). Optionally, this method can throw a
     * WikiSecurityException if the Group does not yet exist in the GroupManager cache.
     * </p>
     * <p>
     * The <code>group</code> parameter in the HTTP request contains the Group name to look up and populate. The <code>members</code>
     * parameter contains the member list. If these differ from those in the existing group, the passed values override the old values.
     * </p>
     * <p>
     * This method does not commit the new Group to the GroupManager cache. To do that, use {@link #setGroup(Session, Group)}.
     * </p>
     * @param name the name of the group to construct
     * @param memberLine the line of text containing the group membership list
     * @param create whether this method should create a new, empty Group if one with the requested name is not found. If <code>false</code>,
     *            groups that do not exist will cause a <code>NoSuchPrincipalException</code> to be thrown
     * @return a new, populated group
     * @see org.apache.wiki.auth.authorize.Group#RESTRICTED_GROUPNAMES
     * @throws WikiSecurityException if the group name isn't allowed, or if <code>create</code> is <code>false</code>
     *                               and the Group named <code>name</code> does not exist
     */
    Group parseGroup( String name, String memberLine, boolean create ) throws WikiSecurityException;

    /**
     * <p>
     * Extracts group name and members from the HTTP request and populates an existing Group with them. The Group will either be a copy of
     * an existing Group (if one can be found), or a new, unregistered Group (if not). Optionally, this method can throw a
     * WikiSecurityException if the Group does not yet exist in the GroupManager cache.
     * </p>
     * <p>
     * The <code>group</code> parameter in the HTTP request contains the Group name to look up and populate. The <code>members</code>
     * parameter contains the member list. If these differ from those in the existing group, the passed values override the old values.
     * </p>
     * <p>
     * This method does not commit the new Group to the GroupManager cache. To do that, use {@link #setGroup(Session, Group)}.
     * </p>
     * @param context the current wiki context
     * @param create whether this method should create a new, empty Group if one with the requested name is not found. If <code>false</code>,
     *            groups that do not exist will cause a <code>NoSuchPrincipalException</code> to be thrown
     * @return a new, populated group
     * @throws WikiSecurityException if the group name isn't allowed, or if <code>create</code> is <code>false</code>
     *                               and the Group does not exist
     */
    default Group parseGroup( final Context context, final boolean create ) throws WikiSecurityException {
        // Extract parameters
        final HttpServletRequest request = context.getHttpRequest();
        final String name = request.getParameter( "group" );
        final String memberLine = request.getParameter( "members" );

        // Create the named group; we pass on any NoSuchPrincipalExceptions
        // that may be thrown if create == false, or WikiSecurityExceptions
        final Group group = parseGroup( name, memberLine, create );

        // If no members, add the current user by default
        if( group.members().length == 0 ) {
            group.add( context.getWikiSession().getUserPrincipal() );
        }

        return group;
    }

    /**
     * Removes a named Group from the group database. If not found, throws a <code>NoSuchPrincipalException</code>. After removal, this
     * method will commit the delete to the back-end group database. It will also fire a
     * {@link org.apache.wiki.event.WikiSecurityEvent#GROUP_REMOVE} event with the GroupManager instance as the source and the Group as target.
     * If <code>index</code> is <code>null</code>, this method throws an {@link IllegalArgumentException}.
     *
     * @param index the group to remove
     * @throws WikiSecurityException if the Group cannot be removed by the back-end
     * @see org.apache.wiki.auth.authorize.GroupDatabase#delete(Group)
     */
    void removeGroup( final String index ) throws WikiSecurityException;

    /**
     * <p>
     * Saves the {@link Group} created by a user in a wiki session. This method registers the Group with the GroupManager and saves it to
     * the back-end database. If an existing Group with the same name already exists, the new group will overwrite it. After saving the
     * Group, the group database changes are committed.
     * </p>
     * <p>
     * This method fires the following events:
     * </p>
     * <ul>
     * <li><strong>When creating a new Group</strong>, this method fires a {@link org.apache.wiki.event.WikiSecurityEvent#GROUP_ADD} with
     * the GroupManager instance as its source and the new Group as the target.</li>
     * <li><strong>When overwriting an existing Group</strong>, this method fires a new
     * {@link org.apache.wiki.event.WikiSecurityEvent#GROUP_REMOVE} with this GroupManager instance as the source, and the new Group as the
     * target. It then fires a {@link org.apache.wiki.event.WikiSecurityEvent#GROUP_ADD} event with the same source and target.</li>
     * </ul>
     * <p>
     * In addition, if the save or commit actions fail, this method will attempt to restore the older version of the wiki group if it
     * exists. This will result in a <code>GROUP_REMOVE</code> event (for the new version of the Group) followed by a <code>GROUP_ADD</code>
     * event (to indicate restoration of the old version).
     * </p>
     * <p>
     * This method will register the new Group with the GroupManager. For example, {@link org.apache.wiki.auth.AuthenticationManager}
     * attaches each Session as a GroupManager listener. Thus, the act of registering a Group with <code>setGroup</code> means that
     * all Sessions will automatically receive group add/change/delete events immediately.
     * </p>
     *
     * @param session the wiki session, which may not be <code>null</code>
     * @param group the Group, which may not be <code>null</code>
     * @throws WikiSecurityException if the Group cannot be saved by the back-end
     */
    void setGroup( final Session session, final Group group ) throws WikiSecurityException;

    /**
     * Validates a Group, and appends any errors to the session errors list. Any validation errors are added to the wiki session's messages
     * collection (see {@link Session#getMessages()}.
     *
     * @param context the current wiki context
     * @param group the supplied Group
     */
    void validateGroup( final Context context, final Group group );

    /**
     * Checks if a String is blank or a restricted Group name, and if it is, appends an error to the Session's message list.
     *
     * @param context the wiki context
     * @param name the Group name to test
     * @throws WikiSecurityException if <code>session</code> is <code>null</code> or the Group name is illegal
     * @see Group#RESTRICTED_GROUPNAMES
     */
    void checkGroupName( final Context context, final String name ) throws WikiSecurityException;

    // events processing .......................................................

    /**
     * Registers a WikiEventListener with this instance. This is a convenience method.
     *
     * @param listener the event listener
     */
    void addWikiEventListener( WikiEventListener listener );

    /**
     * Un-registers a WikiEventListener with this instance. This is a convenience method.
     *
     * @param listener the event listener
     */
    void removeWikiEventListener( WikiEventListener listener );

    /**
     *  Fires a WikiSecurityEvent of the provided type, Principal and target Object to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiSecurityEvent
     * @param type       the event type to be fired
     * @param target     the changed Object, which may be <code>null</code>
     */
    default void fireEvent( final int type, final Object target ) {
        if( WikiEventManager.isListening( this ) ) {
            WikiEventManager.fireEvent( this, new WikiSecurityEvent( this, type, target ) );
        }
    }

}
