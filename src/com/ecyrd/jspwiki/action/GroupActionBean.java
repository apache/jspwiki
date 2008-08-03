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

@UrlBinding( "/Group.jsp" )
public class GroupActionBean extends AbstractActionBean
{
    private Group m_group = null;

    private List<Principal> m_members = new ArrayList<Principal>();
    
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
    @Validate( required = true, converter = OneToManyTypeConverter.class )
    public void setMembers( List<Principal> members )
    {
        m_members = members;
        m_group.clear();
        for( Principal member : members )
        {
            m_group.add( member );
        }
    }

    @ValidationMethod( on = "save" )
    public void validateBeforeSave( ValidationErrors errors )
    {
        // Name cannot be one of the restricted names either
        String name = m_group.getName();
        if( Role.isReservedName( name ) )
            ;
        {
            errors.add( "group", new SimpleError( "The group name '" + name + "' is illegal. Choose another." ) );
        }
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
            WikiEngine engine = getEngine();
            GroupManager groupMgr = engine.getGroupManager();
            groupMgr.removeGroup( m_group.getName() );
        }
        catch( WikiSecurityException e )
        {
            throw new StripesRuntimeException( e );
        }
        return new RedirectResolution( ViewActionBean.class );
    }

    @HandlesEvent( "save" )
    @HandlerPermission( permissionClass = GroupPermission.class, target = "${group.name}", actions = GroupPermission.EDIT_ACTION )
    @WikiRequestContext( "editGroup" )
    public Resolution save() throws WikiSecurityException
    {
        GroupManager mgr = getContext().getWikiEngine().getGroupManager();
        mgr.setGroup( getContext().getWikiSession(), m_group );
        RedirectResolution r = new RedirectResolution( "/Group.jsp" );
        r.addParameter( "group", m_group.getName() );
        return r;
    }

    @HandlesEvent( "create" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = WikiPermission.CREATE_GROUPS_ACTION, actions = "" )
    @WikiRequestContext( "createGroup" )
    public Resolution create()
    {
        return new ForwardResolution( "/CreateGroup.jsp" );
    }
    
    /**
     * Default handler method for "view" events that simply forwards the user to
     * <code>/Group.jsp</code>.
     */
    @DefaultHandler
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = GroupPermission.class, target = "${group.name}", actions = GroupPermission.VIEW_ACTION )
    @WikiRequestContext( "group" )
    public Resolution view()
    {
        return new ForwardResolution( "/Group,jsp" );
    }

}
