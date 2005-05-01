package com.ecyrd.jspwiki.htmltowiki;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Config class for XHtmlToWikiTranslator
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class XHtmlToWikiConfig
{

    private String outlink = "outlink";

    private String pageInfoJsp = "PageInfo.jsp";

    private String wikiJspPage = "Wiki.jsp?page=";

    private String attachPage = "attach?page=";

    private String pageName;

    public XHtmlToWikiConfig()
    {}

    public XHtmlToWikiConfig( WikiContext wikiContext )
    {
        setWikiContext( wikiContext );
    }

    private void setWikiContext( WikiContext wikiContext )
    {
        if( wikiContext.getPage() != null )
        {
            setPageName( wikiContext.getPage().getName() + "/" );
        }
    }

    public String getAttachPage()
    {
        return attachPage;
    }

    public void setAttachPage( String attachPage )
    {
        this.attachPage = attachPage;
    }

    public String getOutlink()
    {
        return outlink;
    }

    public void setOutlink( String outlink )
    {
        this.outlink = outlink;
    }

    public String getPageInfoJsp()
    {
        return pageInfoJsp;
    }

    public void setPageInfoJsp( String pageInfoJsp )
    {
        this.pageInfoJsp = pageInfoJsp;
    }

    public String getPageName()
    {
        return pageName;
    }

    public void setPageName( String pageName )
    {
        this.pageName = pageName;
    }

    public String getWikiJspPage()
    {
        return wikiJspPage;
    }

    public void setWikiJspPage( String wikiJspPage )
    {
        this.wikiJspPage = wikiJspPage;
    }
}
