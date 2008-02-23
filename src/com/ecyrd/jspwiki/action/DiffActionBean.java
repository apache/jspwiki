package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("diff")
@UrlBinding("/Diff.action")
public class DiffActionBean extends WikiContext
{
    @HandlesEvent("diff")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    public Resolution diff()
    {
        return null;
    }
}
