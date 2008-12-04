package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

@UrlBinding("/NewPage.jsp")
public class NewPageActionBean extends AbstractActionBean
{
    @WikiRequestContext("newPage")
    @HandlesEvent("create")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.CREATE_PAGES_ACTION)
    public Resolution create()
    {
        return null;
    }
}
