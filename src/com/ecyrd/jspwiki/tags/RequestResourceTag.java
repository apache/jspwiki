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

import com.ecyrd.jspwiki.ui.TemplateManager;

/**
 *  Provides easy access to TemplateManager.addResourceRequest().  You may use
 *  any of the request types defined there.
 * 
 *  @author jalkanen
 *
 */
public class RequestResourceTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private String m_type;
    private String m_resource;

    public void initTag()
    {
        super.initTag();
        m_type = m_resource = null;
    }
    
    public int doWikiStartTag() throws Exception
    {   
        if( m_type != null && m_resource != null )
        {
            TemplateManager.addResourceRequest( m_wikiContext, m_type, m_resource );
        }

        return SKIP_BODY;
    }

    public String getResource()
    {
        return m_resource;
    }

    public void setResource(String r)
    {
        m_resource = r;
    }

    public String getType()
    {
        return m_type;
    }

    public void setType(String type)
    {
        m_type = type;
    }

}
