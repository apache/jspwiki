package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("info")
@UrlBinding("/PageInfo.action")
public class PageInfoActionBean extends WikiContext
{
    @HandlesEvent("info")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    public Resolution info()
    {
        return null;
    }
}
