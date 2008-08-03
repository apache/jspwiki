package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@UrlBinding("/Comment.jsp")
public class CommentActionBean extends WikiContext
{
    @HandlesEvent("comment")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.name}", actions=PagePermission.COMMENT_ACTION)
    @WikiRequestContext("comment")
    public Resolution comment()
    {
        return null;
    }
}
