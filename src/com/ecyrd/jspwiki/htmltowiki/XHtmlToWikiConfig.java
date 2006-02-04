package com.ecyrd.jspwiki.htmltowiki;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Defines a Wiki configuration to XHtmlToWikiTranslator, including things like
 *  URLs.
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class XHtmlToWikiConfig
{
    private String m_outlink = "outlink";

    private String m_pageInfoJsp = "PageInfo.jsp";

    private String m_wikiJspPage = "Wiki.jsp?page=";

    private String m_attachPage = "attach?page=";

    private String m_pageName;

    public XHtmlToWikiConfig()
    {}

    /**
     *  The constructor initializes the different internal fields
     *  according to the current URLConstructor.
     *  
     * @param wikiContext
     */
    public XHtmlToWikiConfig( WikiContext wikiContext )
    {
        setWikiContext( wikiContext );
        
        //
        //  Figure out the actual URLs.
        //
        //  NB: The logic here will fail if you add something else after
        //      the Wiki page name in VIEW or ATTACH
        //
        String page = wikiContext.getPage().getName();
        
        String href = wikiContext.getURL( WikiContext.VIEW, page );
        m_wikiJspPage = removeLast(href,page);
        
        href = wikiContext.getURL( WikiContext.ATTACH, page );
        m_attachPage = removeLast( href, page );
        
        href = wikiContext.getURL( WikiContext.INFO, page );
        m_pageInfoJsp = removeLast( href, page );
    }

    private String removeLast(String str, String remove )
    {
        int idx = str.lastIndexOf( remove );
        
        if( idx != -1 )
        {
            str = StringUtils.left( str, idx ) + StringUtils.substring( str, idx+remove.length() );
        }
        
        return str;
    }

    private void setWikiContext( WikiContext wikiContext )
    {
        if( wikiContext.getPage() != null )
        {
            setPageName( wikiContext.getPage().getName() + '/' );
        }
    }

    public String getAttachPage()
    {
        return m_attachPage;
    }

    public void setAttachPage( String attachPage )
    {
        m_attachPage = attachPage;
    }

    public String getOutlink()
    {
        return m_outlink;
    }

    public void setOutlink( String outlink )
    {
        m_outlink = outlink;
    }

    public String getPageInfoJsp()
    {
        return m_pageInfoJsp;
    }

    public void setPageInfoJsp( String pageInfoJsp )
    {
        m_pageInfoJsp = pageInfoJsp;
    }

    public String getPageName()
    {
        return m_pageName;
    }

    public void setPageName( String pageName )
    {
        m_pageName = pageName;
    }

    public String getWikiJspPage()
    {
        return m_wikiJspPage;
    }

    public void setWikiJspPage( String wikiJspPage )
    {
        m_wikiJspPage = wikiJspPage;
    }
}
