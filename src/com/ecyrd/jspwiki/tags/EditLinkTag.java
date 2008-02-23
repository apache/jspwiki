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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.action.EditActionBean;

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
 *  @author Janne Jalkanen
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
        WikiEngine engine   = m_actionBean.getEngine();
        WikiPage   page     = null;
        String     pageName = null;
        
        //
        //  Determine the page and the link.
        //
        if( m_pageName == null )
        {
            if( m_page == null )
            {
                // You can't call this on the page itself anyways.
                return SKIP_BODY;
            }

            pageName = m_page.getName();
        }
        else
        {
            pageName = m_pageName;
        }

        //
        //  Determine the latest version, if the version attribute is "this".
        //
        Map<String,String> urlParams = new HashMap<String,String>();
        if( m_version != null )
        {
            if( "this".equalsIgnoreCase(m_version) )
            {
                if( m_page == null )
                {
                    // No page, so go fetch according to page name.
                    page = engine.getPage( m_pageName );
                }
                
                if( page != null )
                {
                    urlParams.put("version", String.valueOf(page.getVersion()));
                }
            }
            else
            {
                urlParams.put("version", String.valueOf(m_version));
            }
        }

        //
        //  Finally, print out the correct link, according to what
        //  user commanded.
        //
        JspWriter out = pageContext.getOut();
        WikiContext context = (WikiContext)m_actionBean;

        switch( m_format )
        {
          case ANCHOR:
              urlParams.put("accesskey", m_accesskey);
              urlParams.put("title", m_title);
              out.print("<a href=\""+context.getContext().getURL(EditActionBean.class,pageName, urlParams) + "\">");
              break;

          case URL:
              out.print( context.getContext().getURL(EditActionBean.class,pageName,urlParams) );
              break;
        }

        return EVAL_BODY_INCLUDE;
    }
}
