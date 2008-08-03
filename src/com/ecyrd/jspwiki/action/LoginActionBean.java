package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

@UrlBinding("/Login.jsp")
public class LoginActionBean extends AbstractActionBean
{
    @DefaultHandler
    @HandlesEvent("login")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.LOGIN_ACTION)
    @WikiRequestContext("login")
    public Resolution login()
    {
        return null;
    }

    @HandlesEvent("logout")
    @HandlerPermission(permissionClass=WikiPermission.class, target="${engine.applicationName}", actions=WikiPermission.LOGIN_ACTION)
    @WikiRequestContext("logout")
    public Resolution logout()
    {
        return null;
    }
}
