/*
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
package org.apache.wiki.htmltowiki;

import org.apache.wiki.WikiContext;

/**
 *  Defines a Wiki configuration to XHtmlToWikiTranslator, including things like
 *  URLs.
 *
 */
public class XHtmlToWikiConfig
{
    private String m_outlink = "outlink";

    private String m_pageInfoJsp = "PageInfo.jsp";

    private String m_wikiJspPage = "Wiki.jsp?page=";

    private String m_editJspPage = "Edit.jsp?page=";

    private String m_attachPage = "attach?page=";

    private String m_pageName;

    /**
     *  Creates a new, empty config object.
     */
    public XHtmlToWikiConfig()
    {}

    /**
     *  The constructor initializes the different internal fields
     *  according to the current URLConstructor.
     *
     * @param wikiContext A WikiContext
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

    private void setWikiContext( WikiContext wikiContext )
    {
        if( wikiContext.getPage() != null )
        {
            setPageName( wikiContext.getPage().getName() + '/' );
        }
    }

    /**
     *  Return the URL for the attachments.
     *  
     *  @return URL for attachments.
     */
    public String getAttachPage()
    {
        return m_attachPage;
    }

    /**
     *  Set the URL for attachments.
     *  
     *  @param attachPage The attachment URL.
     */
    public void setAttachPage( String attachPage )
    {
        m_attachPage = attachPage;
    }

    /**
     *  Gets the URL of the outlink image.
     *  
     *  @return The URL of the outlink image.
     */
    public String getOutlink()
    {
        return m_outlink;
    }

    /**
     *  Set the outlink URL.
     *  
     *  @param outlink The outlink URL.
     */
    public void setOutlink( String outlink )
    {
        m_outlink = outlink;
    }

    /**
     *  Get the PageInfo.jsp URI.
     *  
     *  @return The URI for the page info display.
     */
    public String getPageInfoJsp()
    {
        return m_pageInfoJsp;
    }

    /**
     *  Set the URI for the page info display.
     *  
     *  @param pageInfoJsp URI for the page info.
     */
    public void setPageInfoJsp( String pageInfoJsp )
    {
        m_pageInfoJsp = pageInfoJsp;
    }

    /**
     *  Get the page name.
     *  
     *  @return The Page Name.
     */
    public String getPageName()
    {
        return m_pageName;
    }

    
    /**
     *  Set the page name.
     *  
     *  @param pageName The name of the page.
     */
    public void setPageName( String pageName )
    {
        m_pageName = pageName;
    }

    /**
     *  Get the URI to the Wiki.jsp view.
     *  
     *  @return The URI to the Wiki.jsp.
     */
    public String getWikiJspPage()
    {
        return m_wikiJspPage;
    }

    /**
     *  Set the URI to the Wiki.jsp.
     *  
     *  @param wikiJspPage The URI to the Wiki.jsp.
     */
    public void setWikiJspPage( String wikiJspPage )
    {
        m_wikiJspPage = wikiJspPage;
    }

    /**
     *  Return the URI to the Edit.jsp page.
     *  
     *  @return The URI to the Edit.jsp page.
     */
    public String getEditJspPage()
    {
        return m_editJspPage;
    }

    /**
     *  Set the URI to the Edit.jsp page.
     *  
     *  @param editJspPage The Edit.jsp URI.
     */
    public void setEditJspPage( String editJspPage )
    {
        m_editJspPage = editJspPage;
    }
}
