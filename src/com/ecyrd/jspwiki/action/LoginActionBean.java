package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

@WikiRequestContext("login")
@UrlBinding("/Login.action")
public class LoginActionBean extends AbstractActionBean
{
    @HandlesEvent("login")
    @EventPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.LOGIN_ACTION)
    public Resolution login()
    {
        return null;
    }

    @HandlesEvent("logout")
    @EventPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.LOGIN_ACTION)
    public Resolution logout()
    {
        return null;
    }
}
