package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding( "/PageModified.jsp" )
public class PageModifiedActionBean extends AbstractActionBean
{
    @HandlesEvent( "conflict" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "conflict" )
    public Resolution conflict()
    {
        return null;
    }
}
