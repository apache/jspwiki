/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

package org.apache.wiki.action;

import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.exception.StripesRuntimeException;
import net.sourceforge.stripes.validation.*;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.PrincipalComparator;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.permissions.GroupPermission;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.LineDelimitedTypeConverter;
import org.apache.wiki.ui.stripes.WikiInterceptor;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * <p>
 * Views, creates, modifies and deletes
 * {@link org.apache.wiki.auth.authorize.Group} objects. The event handler
 * methods are the following:
 * </p>
 * <ul>
 * <li>{@link #create()} - redirects to the <code>/CreateGroup.jsp</code>
 * display page</li>
 * <li>{@link #view()} - views a Group</li>
 * <li>{@link #save()} - saves a Group</li>
 * <li>{@link #delete()} - deletes a Group</li>
 * </ul>
 * <p>
 * The Group that the <code>view</code>, <code>save</code> and
 * <code>delete</code> event handler methods operate on is set by
 * {@link #setGroup(Group)}. Normally, this is set automatically by the Stripes
 * controller, which extracts the parameter <code>group</code> from the HTTP
 * request and converts it into a Group via
 * {@link org.apache.wiki.ui.stripes.GroupTypeConverter}.
 * </p>
 * <p>
 * All of the event handlers require the user to possess an appropriate
 * {@link org.apache.wiki.auth.permissions.GroupPermission} to execute. See each
 * handler method's {@link org.apache.wiki.ui.stripes.HandlerPermission} for
 * details.
 * </p>
 */
@UrlBinding( "/Group.action" )
public class GroupActionBean extends AbstractActionBean
{
    private static final String DEFAULT_NEW_GROUP_NAME = "MyGroup";

    private static final Comparator<? super Principal> PRINCIPAL_COMPARATOR = new PrincipalComparator();

    private Group m_group = null;

    private static Logger log = LoggerFactory.getLogger( GroupActionBean.class );

    private List<Principal> m_members = new ArrayList<Principal>();

    /**
     * <p>
     * When a group is saved, this method checks that the user has the correct
     * permissions. For a new group that does not exist, the user must possess
     * the WikiPermission {@link WikiPermission#CREATE_GROUPS_ACTION}. If the
     * group already exists, the user must possess GroupPermission with target
     * {@link GroupPermission#EDIT_ACTION} for the group. This method intercepts
     * the Stripes request lifecycle and executes after
     * {@link LifecycleStage#BindingAndValidation}, which ensures that the
     * handler method is known and that all values are bound.
     * </p>
     * <p>
     * Now, you might be asking yourself, why go to all this trouble when we
     * have a perfectly good permission-checking method in
     * {@link WikiInterceptor}? The answer is that the permission is different
     * depending on whether the group is being created or edited. It's not
     * possible to specify more than one {@link HandlerPermission} in a method,
     * so this method does that instead.
     * </p>
     */
    @Before( stages = LifecycleStage.CustomValidation )
    public Resolution checkSavePermission()
    {
        // If not the save event, don't continue
        String handler = getContext().getEventName();
        if( !"save".equals( handler ) )
        {
            return null;
        }

        // Create the required permission
        Permission permission;
        if( isNew() )
        {
            permission = new WikiPermission( "*", WikiPermission.CREATE_GROUPS_ACTION );
        }
        else
        {
            permission = new GroupPermission( "*:" + m_group.getName(), GroupPermission.EDIT_ACTION );
        }

        // Does the user have it?
        WikiSession session = getContext().getWikiSession();
        AuthorizationManager mgr = getContext().getEngine().getAuthorizationManager();
        boolean allowed = mgr.checkPermission( session, permission );

        // If not allowed, redirect to login page with all parameters intact;
        // otherwise proceed
        Resolution r = null;
        if( !allowed )
        {
            r = new RedirectResolution( LoginActionBean.class );
            ((RedirectResolution) r).includeRequestParameters( true );
            if( log.isDebugEnabled() )
            {
                log.debug( "CheckSavePermission rejected access to ActionBean=" + this.getClass().getCanonicalName() + ", method="
                           + handler );
            }
        }
        return r;
    }

    /**
     * Redirects the user to the <code>/EditGroup.jsp</code> display JSP.
     * 
     * @return {@link net.sourceforge.stripes.action.ForwardResolution} to the
     *         display JSP
     */
    @HandlesEvent( "create" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "*", actions = WikiPermission.CREATE_GROUPS_ACTION )
    @WikiRequestContext( "createGroup" )
    public Resolution create()
    {
        String groupName = m_group == null ? DEFAULT_NEW_GROUP_NAME : m_group.getName();
        RedirectResolution r = new RedirectResolution( "/EditGroup.jsp" );
        r.addParameter( "group", groupName );
        r.addParameter( "members", getContext().getWikiSession().getUserPrincipal() );
        return r;
    }

    /**
     * Handler method that deletes the group supplied by {@link #getGroup()}. If
     * the GroupManager throws a WikiSecurityException because it cannot delete
     * the group for some reason, the exception is re-thrown as a
     * StripesRuntimeException. <code>/Group.jsp</code>.
     * 
     * @throws StripesRuntimeException if the group cannot be deleted for any
     *             reason
     */
    @HandlesEvent( "delete" )
    @HandlerPermission( permissionClass = GroupPermission.class, target = "${group.name}", actions = GroupPermission.DELETE_ACTION )
    @WikiRequestContext( "deleteGroup" )
    public Resolution delete() throws StripesRuntimeException
    {
        try
        {
            WikiEngine engine = getContext().getEngine();
            GroupManager groupMgr = engine.getGroupManager();
            groupMgr.removeGroup( m_group.getName() );
        }
        catch( WikiSecurityException e )
        {
            throw new StripesRuntimeException( e );
        }
        return new RedirectResolution( ViewActionBean.class );
    }

    /**
     * Forwards the user to the <code>/EditGroup.jsp</code> display JSP.
     * 
     * @return {@link net.sourceforge.stripes.action.ForwardResolution} to the
     *         display JSP
     */
    @HandlesEvent( "edit" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "*", actions = WikiPermission.CREATE_GROUPS_ACTION )
    @WikiRequestContext( "editGroup" )
    public Resolution edit()
    {
        return new ForwardResolution( "/EditGroup.jsp" ).addParameter( "group", m_group.getName() );
    }

    public Group getGroup()
    {
        return m_group;
    }

    public List<Principal> getMembers()
    {
        return m_members;
    }

    /**
     * Returns {@code true} if the group returned by {@link #getGroup()} does
     * not exist.
     * 
     * @return the result
     */
    public boolean isNew()
    {
        if( m_group == null )
        {
            return true;
        }

        GroupManager mgr = getContext().getEngine().getGroupManager();
        try
        {
            mgr.getGroup( m_group.getName() );
            return false;
        }
        catch( NoSuchPrincipalException e )
        {
            // Group not created yet
        }
        return true;
    }

    /**
     * Saves an existing wiki Group. The members of the group to be saved are
     * returned by the method {@link #getMembers()}; these members are usually
     * set automatically by the
     * {@link org.apache.wiki.ui.stripes.PrincipalTypeConverter}.
     * 
     * @return {@link net.sourceforge.stripes.action.RedirectResolution} to the
     *         view page.
     * @throws WikiSecurityException if the group cannot be saved for any reason
     */
    @HandlesEvent( "save" )
    public Resolution save() throws WikiSecurityException
    {
        GroupManager mgr = getContext().getEngine().getGroupManager();
        mgr.setGroup( getContext().getWikiSession(), m_group );
        return new RedirectResolution( GroupActionBean.class, "view" ).addParameter( "group", m_group.getName() );
    }

    /**
     * Sets the Group used by this ActionBean. When the Group is assigned, the
     * list of Principals returned by {@link #getMembers()} is cleared and
     * populated by the members of the Group.
     * 
     * @param group the group
     */
    @Validate( required = true, on = "view,edit,delete,save" )
    public void setGroup( Group group )
    {
        m_group = group;
        m_members.clear();
        for( Principal member : group.members() )
        {
            m_members.add( member );
        }
    }

    /**
     * Sets the member list for the ActionBean, and for the underlying Group.
     * The member is sorted when it is assigned.
     * 
     * @param members the members, separated by carriage returns
     */
    @Validate( required = true, on = "save", converter = LineDelimitedTypeConverter.class )
    public void setMembers( List<Principal> members )
    {
        m_members = members;
        Collections.sort( m_members, PRINCIPAL_COMPARATOR );
        m_group.clear();
        for( Principal member : members )
        {
            m_group.add( member );
        }
    }

    /**
     * Validates that the group name is legal.
     * 
     * @param errors the current set of validation errors
     */
    @ValidationMethod( on = "save", when = ValidationState.NO_ERRORS )
    public void validateIsLegalName( ValidationErrors errors )
    {
        // Name cannot be one of the restricted names
        String name = m_group.getName();
        if( Role.isReservedName( name ) )
        {
            errors.add( "group", new LocalizableError( "editgroup.illegalname" ) );
        }
    }

    /**
     * Default handler method for "view" events that simply forwards the user to
     * <code>/Group.jsp</code> if the group requested by the user exists. If
     * not, it will redirect the user to the {@link #create()} event. If the
     * group supplied by the user in the request was illegal -- which can be
     * determined by the presence of a validation error
     * {@link net.sourceforge.stripes.validation.LocalizableError} with key
     * <code>editgroup.illegalname</code> -- this method forwards to the error
     * page. Lastly, if {@link #getGroup()} returns <code>null</code> because
     * the user supplied no <code>group</code> parameter, this method redirects
     * to {@link #create()} with a suggested sample group name
     * <code>Group<em>x</em></code>, where <em>x</em> is an integer value.
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = GroupPermission.class, target = "${group.name}", actions = GroupPermission.VIEW_ACTION )
    @WikiRequestContext( "group" )
    public Resolution view()
    {
        GroupManager mgr = getContext().getEngine().getGroupManager();

        // User supplied a group, and it already exists
        if( m_group != null && mgr.findRole( m_group.getName() ) != null )
        {
            return new ForwardResolution( "/Group.jsp" );
        }

        // It is an error if we see LocalizableError with key
        // editgroup.illegalname!
        ValidationErrors errors = getContext().getValidationErrors();
        List<ValidationError> fieldErrors = errors.get( "group" );
        if( fieldErrors != null )
        {
            for( ValidationError fieldError : fieldErrors )
            {
                if( fieldError instanceof LocalizableError )
                {
                    LocalizableError error = (LocalizableError) fieldError;
                    if( "editgroup.illegalname".equals( error.getMessageKey() ) )
                    {
                        return new ForwardResolution( ErrorActionBean.class );
                    }
                }
            }
        }

        // User didn't bother to supply a group at all, so suggest one
        if( m_group == null )
        {
            int suffix = 1;
            while ( mgr.findRole( "Group" + suffix ) != null )
            {
                suffix++;
            }
            try
            {
                m_group = mgr.getGroup( "Group" + suffix, true );
            }
            catch( Exception e )
            {
                return new RedirectResolution( ErrorActionBean.class ).flash( this );
            }
        }

        // Redirect to create-group page
        return new RedirectResolution( GroupActionBean.class, "create" ).addParameter( "group", m_group.getName() );
    }
}
