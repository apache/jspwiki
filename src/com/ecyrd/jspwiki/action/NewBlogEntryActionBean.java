package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

@UrlBinding("/NewBlogEntry.jsp")
public class NewBlogEntryActionBean extends AbstractActionBean
{
    @HandlesEvent("create")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.CREATE_PAGES_ACTION)
    @WikiRequestContext("newBlogEntry")
    public Resolution create()
    {
        return null;
    }
}
