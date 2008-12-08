package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

@UrlBinding("/PageInfo.jsp")
public class PageInfoActionBean extends AbstractPageActionBean
{
    @DefaultHandler
    @HandlesEvent("info")
    @WikiRequestContext("info")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.name}", actions=PagePermission.VIEW_ACTION)
    public Resolution info()
    {
        return null;
    }
}
