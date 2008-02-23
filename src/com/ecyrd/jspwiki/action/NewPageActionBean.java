package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

@WikiRequestContext("newPage")
@UrlBinding("/NewPage.action")
public class NewPageActionBean extends WikiContext
{
    @HandlesEvent("create")
    @EventPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.CREATE_PAGES_ACTION)
    public Resolution create()
    {
        return null;
    }
}
