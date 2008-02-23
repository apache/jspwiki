package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * Displays the wiki page a users requested, resolving special page names and
 * redirecting if needed.
 * @author Andrew Jaquith
 *
 */
@WikiRequestContext("preview")
@UrlBinding("/Preview.action")
public class PreviewActionBean extends WikiContext
{
    /**
     * Default handler that simply forwards the user back to the same page. 
     * Every ActionBean needs a default handler to function properly, so we use
     * this (very simple) one.
     * @return a forward resolution back to the same page
     */
    @DefaultHandler
    @HandlesEvent("preview")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    public Resolution view()
    {
        return new ForwardResolution(PreviewActionBean.class);
    }
    
}
