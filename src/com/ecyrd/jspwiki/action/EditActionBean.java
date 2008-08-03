package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding("/Edit.jsp")
public class EditActionBean extends WikiContext
{
    @DefaultHandler
    @HandlesEvent("edit")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.name}", actions=PagePermission.EDIT_ACTION)
    @WikiRequestContext("edit")
    public Resolution edit()
    {
        return null;
    }
    
    /**
     * Event that extracts the current state of the edited page and forwards the user to the previewer JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "preview" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "preview" )
    public Resolution preview()
    {
        return new ForwardResolution( "/Preview.jsp" );
    }

    /**
     * Event that diffs the current state of the edited page and forwards the user to the diff JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @WikiRequestContext("diff")
    @HandlesEvent("diff")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.name}", actions=PagePermission.VIEW_ACTION)
    public Resolution diff()
    {
        return new ForwardResolution( "/Diff.jsp" );
    }
}
