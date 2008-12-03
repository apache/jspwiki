package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding( "/rss.jsp" )
public class RSSActionBean extends AbstractActionBean
{
    @HandlesEvent( "rss" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "rss" )
    public Resolution rss()
    {
        return null;
    }
}
