package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.*;

@UrlBinding( "/Workflow.jsp" )
public class WorkflowActionBean extends AbstractActionBean
{
    /**
     * Default handler that simply forwards the user back to the view page.
     * Every ActionBean needs a default handler to function properly, so we use
     * this (very simple) one.
     * 
     * @return a forward resolution back to the view page
     */
    @DefaultHandler
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "workflow" )
    public Resolution view()
    {
        return new ForwardResolution( ViewActionBean.class );
    }
}
