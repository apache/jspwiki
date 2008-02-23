/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.htmltowiki;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.AttachActionBean;
import com.ecyrd.jspwiki.action.EditActionBean;
import com.ecyrd.jspwiki.action.PageInfoActionBean;
import com.ecyrd.jspwiki.action.ViewActionBean;

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

    private String m_editJspPage = "Edit.jsp?page=";

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

        m_wikiJspPage = wikiContext.getContext().getURL( ViewActionBean.class, "" );

        m_editJspPage = wikiContext.getContext().getURL( EditActionBean.class, "" );

        m_attachPage = wikiContext.getContext().getURL( AttachActionBean.class, "" );

        m_pageInfoJsp = wikiContext.getContext().getURL( PageInfoActionBean.class, "" );
    }

    /*
    // FIXME: Unused.
    private String removeLast(String str, String remove )
    {
        int idx = str.lastIndexOf( remove );

        if( idx != -1 )
        {
            str = StringUtils.left( str, idx ) + StringUtils.substring( str, idx+remove.length() );
        }

        return str;
    }
    */
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

    public String getEditJspPage()
    {
        return m_editJspPage;
    }

    public void setEditJspPage( String editJspPage )
    {
        m_editJspPage = editJspPage;
    }
}
