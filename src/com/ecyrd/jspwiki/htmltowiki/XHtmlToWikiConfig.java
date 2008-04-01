/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.htmltowiki;

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

        m_wikiJspPage = wikiContext.getURL( WikiContext.VIEW, "" );

        m_editJspPage = wikiContext.getURL( WikiContext.EDIT, "" );

        m_attachPage = wikiContext.getURL( WikiContext.ATTACH, "" );

        m_pageInfoJsp = wikiContext.getURL( WikiContext.INFO, "" );
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
