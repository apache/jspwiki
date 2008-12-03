package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding( "/Upload.jsp" )
public class UploadActionBean extends AbstractActionBean
{
    @HandlesEvent( "upload" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.UPLOAD_ACTION )
    @WikiRequestContext( "upload" )
    public Resolution upload()
    {
        return null;
    }
}
