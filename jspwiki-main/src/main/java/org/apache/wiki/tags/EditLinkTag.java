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
package org.apache.wiki.tags;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 *  Writes an edit link.  Body of the link becomes the link text.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - Format, either "anchor" or "url".
 *    <LI>version - Version number of the page to refer to.  Possible values
 *        are "this", meaning the version of the current page; or a version
 *        number.  Default is always to point at the latest version of the page.
 *    <LI>title - Is used in page actions to display hover text (tooltip)
 *    <LI>accesskey - Set an accesskey (ALT+[Char])
 *  </UL>
 *
 *  @since 2.0
 */
public class EditLinkTag
    extends WikiLinkTag
{
    private static final long serialVersionUID = 0L;
    
    public String m_version = null;
    public String m_title = "";
    public String m_accesskey = "";
    
    public void initTag()
    {
        super.initTag();
        m_version = null;
    }

    public void setVersion( String vers )
    {
        m_version = vers;
    }
    
    public void setTitle( String title )
    {
        m_title = title;
    }

    public void setAccesskey( String access )
    {
        m_accesskey = access;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        WikiPage   page     = null;
        String     versionString = "";
        String     pageName = null;
        
        //
        //  Determine the page and the link.
        //
        if( m_pageName == null )
        {
            page = m_wikiContext.getPage();
            if( page == null )
            {
                // You can't call this on the page itself anyways.
                return SKIP_BODY;
            }

            pageName = page.getName();
        }
        else
        {
            pageName = m_pageName;
        }

        //
        //  Determine the latest version, if the version attribute is "this".
        //
        if( m_version != null )
        {
            if( "this".equalsIgnoreCase(m_version) )
            {
                if( page == null )
                {
                    // No page, so go fetch according to page name.
                    page = engine.getPageManager().getPage( m_pageName );
                }
                
                if( page != null )
                {
                    versionString = "version="+page.getVersion();
                }
            }
            else
            {
                versionString = "version="+m_version;
            }
        }

        //
        //  Finally, print out the correct link, according to what
        //  user commanded.
        //
        JspWriter out = pageContext.getOut();

        switch( m_format )
        {
          case ANCHOR:
            out.print("<a href=\""+m_wikiContext.getURL(WikiContext.EDIT,pageName, versionString)
                     +"\" accesskey=\"" + m_accesskey + "\" title=\"" + m_title + "\">");
            break;

          case URL:
            out.print( m_wikiContext.getURL(WikiContext.EDIT,pageName,versionString) );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }
}
