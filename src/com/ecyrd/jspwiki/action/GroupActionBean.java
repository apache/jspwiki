package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.exception.StripesRuntimeException;
import net.sourceforge.stripes.validation.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.ui.stripes.LineDelimitedTypeConverter;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

/**
 * <p>
 * Views, creates, modifies and deletes
 * {@link com.ecyrd.jspwiki.auth.authorize.Group} objects. The event handler
 * methods are the following:
 * </p>
 * <ul>
 * <li>{@link #create()} - redirects to the <code>/CreateGroup.jsp</code>
 * display page</li>
 * <li>{@link #view()} - views a Group</li>
 * <li>{@link #save()} - saves an existing Group</li>
 * <li>{@link #save()} - saves a new Group</li>
 * <li>{@link #delete()} - deletes a Group</li>
 * </ul>
 * <p>
 * The Group that the <code>view</code>, <code>save</code> and
 * <code>delete</code> event handler methods operate on is set by
 * {@link #setGroup(Group)}. Normally, this is set automatically by the Stripes
 * controller, which extracts the parameter <code>group</code> from the HTTP
 * request and converts it into a Group via
 * {@link com.ecyrd.jspwiki.ui.stripes.GroupTypeConverter}.
 * </p>
 * <p>
 * All of the event handlers require the user to possess an appropriate
 * {@link com.ecyrd.jspwiki.auth.permissions.GroupPermission} to execute. See
 * each handler method's {@link com.ecyrd.jspwiki.ui.stripes.HandlerPermission}
 * for details.
 * </p>
 */
@UrlBinding( "/Group.action" )
public class GroupActionBean extends AbstractActionBean
{
    private Group m_group = null;

    private List<Principal> m_members = new ArrayList<Principal>();

    /**
     * Forwards the user to the <code>/CreateGroup.jsp</code> display JSP.
     * 
     * @return {@link net.sourceforge.stripes.action.ForwardResolution} to the
     *         display JSP
     */
    @HandlesEvent( "create" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "*", actions = WikiPermission.CREATE_GROUPS_ACTION )
    @WikiRequestContext( "createGroup" )
    public Resolution create()
    {
        return new ForwardResolution( "/CreateGroup.jsp" );
    }

    /**
     * Handler method for that deletes the group supplied by {@link #getGroup()}.
     * If the GroupManager throws a WikiSecurityException because it cannot
     * delete the group for some reason, the exception is re-thrown as a
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

    public Group getGroup()
    {
        return m_group;
    }

    public List<Principal> getMembers()
    {
        return m_members;
    }

    public boolean isNew()
    {
        return(m_group.getCreated() == null);
    }

    /**
     * Saves an existing wiki Group. The members of the group to be saved are
     * returned by the method {@link #getMembers()}; these members are usually
     * set automatically by the
     * {@link com.ecyrd.jspwiki.ui.stripes.PrincipalTypeConverter}.
     * 
     * @return {@link net.sourceforge.stripes.action.ForwardResolution} to the
     *         view page.
     * @throws WikiSecurityException if the group cannot be saved for any reason
     */
    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = GroupPermission.class, target = "${group.name}", actions = GroupPermission.EDIT_ACTION )
    @WikiRequestContext( "editGroup" )
    public Resolution save() throws WikiSecurityException
    {
        GroupManager mgr = getContext().getEngine().getGroupManager();
        mgr.setGroup( getContext().getWikiSession(), m_group );
        return new RedirectResolution( GroupActionBean.class, "view" ).addParameter( "group", m_group.getName() );
    }

    /**
     * Saves a new wiki Group. The members of the group to be saved are returned
     * by the method {@link #getMembers()}; these members are usually set
     * automatically by the
     * {@link com.ecyrd.jspwiki.ui.stripes.PrincipalTypeConverter}. This method
     * functions identically to {@link #save()} except that the
     * {@link com.ecyrd.jspwiki.auth.permissions.GroupPermission} required to
     * execute it is different.
     * 
     * @return {@link net.sourceforge.stripes.action.ForwardResolution} to the
     *         view page.
     * @throws WikiSecurityException if the group cannot be saved for any reason
     */
    @HandlesEvent( "saveNew" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "*", actions = WikiPermission.CREATE_GROUPS_ACTION )
    public Resolution saveNew() throws WikiSecurityException
    {
        GroupManager mgr = getContext().getEngine().getGroupManager();
        mgr.setGroup( getContext().getWikiSession(), m_group );
        return new RedirectResolution( GroupActionBean.class, "view" ).addParameter( "group", m_group.getName() );
    }

    @Validate( required = true )
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
     * 
     * @param members the members, separated by carriage returns
     */
    @Validate( required = true, on = "save,saveNew,delete", converter = LineDelimitedTypeConverter.class )
    public void setMembers( List<Principal> members )
    {
        m_members = members;
        m_group.clear();
        for( Principal member : members )
        {
            m_group.add( member );
        }
    }

    @ValidationMethod( on = "save, saveNew" )
    public void validateBeforeSave( ValidationErrors errors )
    {
        // Name cannot be one of the restricted names either
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
     * the user supplied no <code>group</code> parameter, this method
     * redirects to {@link #create()} with a suggested sample group name
     * <code>Group<em>x</em></code>, where <em>x</em> is an integer
     * value.
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
