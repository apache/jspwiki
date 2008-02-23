package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("att")
@UrlBinding("/attach/")
public class AttachActionBean extends WikiContext
{
    @HandlesEvent("upload")
    @EventPermission(permissionClass=PagePermission.class,target="${page.qualifiedName}",actions=PagePermission.UPLOAD_ACTION)
    public Resolution upload()
    {
        return null;
    }
}
