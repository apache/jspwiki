package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("edit")
@UrlBinding("/Edit.action")
public class EditActionBean extends WikiContext
{
    @HandlesEvent("edit")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.EDIT_ACTION)
    public Resolution edit()
    {
        return null;
    }
}
