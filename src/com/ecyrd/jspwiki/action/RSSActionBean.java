package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

@WikiRequestContext("rss")
@UrlBinding("/rss.action")
public class RSSActionBean extends WikiContext
{
    /**
     * Retrieves a new RSSActionBean for the given WikiPage.
     * 
     * @param engine
     *            The WikiEngine that is handling the request.
     * @param page
     *            The WikiPage. If you want to create an RSSActionBean for an older
     *            version of a page, you must use this constructor.
     */
    public static RSSActionBean getRSSActionBean(WikiEngine engine, WikiPage page)
    {
        if (engine == null)
        {
            throw new IllegalArgumentException("Parameter engine must not be null.");
        }
        WikiActionBeanContext context = new WikiActionBeanContext();
        context.setWikiEngine( engine );
        RSSActionBean rssBean = new RSSActionBean();
        rssBean.setContext(context);
        rssBean.setPage(page);
        return rssBean;
    }
    
    @HandlesEvent("rss")
    @EventPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    public Resolution rss()
    {
        return null;
    }
}
