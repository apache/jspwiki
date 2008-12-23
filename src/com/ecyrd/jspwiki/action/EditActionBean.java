package com.ecyrd.jspwiki.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.filters.SpamFilter;
import com.ecyrd.jspwiki.ui.EditorManager;
import com.ecyrd.jspwiki.ui.stripes.HandlerPermission;
import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

@HttpCache( allow = false )
@UrlBinding( "/Edit.jsp" )
public class EditActionBean extends AbstractPageActionBean
{
    @DefaultHandler
    @HandlesEvent( "edit" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "edit" )
    public Resolution edit()
    {
        return null;
    }

    /**
     * Event that extracts the current state of the edited page from the HTTP
     * session and redirects the user to the previewer JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "preview" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "preview" )
    public Resolution preview()
    {
        WikiContext context = getContext();
        HttpServletRequest request = context.getHttpRequest();
        HttpSession session = request.getSession();

        request.setAttribute( EditorManager.ATTR_EDITEDTEXT, session.getAttribute( EditorManager.REQ_EDITEDTEXT ) );

        String lastchange = SpamFilter.getSpamHash( context.getPage(), request );
        request.setAttribute( "lastchange", lastchange );

        return new ForwardResolution( "/Preview.jsp" );
    }

    /**
     * Event that diffs the current state of the edited page and forwards the
     * user to the diff JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @WikiRequestContext( "diff" )
    @HandlesEvent( "diff" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    public Resolution diff()
    {
        return new ForwardResolution( "/Diff.jsp" );
    }
}
