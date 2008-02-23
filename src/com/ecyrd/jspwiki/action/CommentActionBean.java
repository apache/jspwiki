package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("comment")
@UrlBinding("/Comment.action")
public class CommentActionBean extends WikiContext
{
    @HandlesEvent("comment")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.COMMENT_ACTION)
    public Resolution comment()
    {
        return null;
    }
}
