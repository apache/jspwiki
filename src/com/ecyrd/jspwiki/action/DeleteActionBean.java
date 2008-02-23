package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("del")
@UrlBinding("/Delete.action")
public class DeleteActionBean extends WikiContext
{
    @HandlesEvent("delete")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.DELETE_ACTION)
    public Resolution delete()
    {
        return null;
    }
}
