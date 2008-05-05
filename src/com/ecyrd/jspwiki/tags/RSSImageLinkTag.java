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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Writes an image link to the RSS file.
 *
 *  @since 2.0
 */
public class RSSImageLinkTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    protected String m_title;

    public void initTag()
    {
        super.initTag();
        m_title = null;
    }

    public void setTitle( String title )
    {
        m_title = title;
    }

    public String getTitle()
    {
        return m_title;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();

        String rssURL = engine.getGlobalRSSURL();

        if( rssURL != null )
        {
            JspWriter out = pageContext.getOut();
            out.print("<a class=\"feed\" href=\""+rssURL+"\">&nbsp;</a>");
        }

        return SKIP_BODY;
    }
}
