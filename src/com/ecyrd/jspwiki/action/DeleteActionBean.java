package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding("/Delete.jsp")
public class DeleteActionBean extends AbstractActionBean
{
    @HandlesEvent("delete")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.name}", actions=PagePermission.DELETE_ACTION)
    @WikiRequestContext("del")
    public Resolution delete()
    {
        return null;
    }
}
