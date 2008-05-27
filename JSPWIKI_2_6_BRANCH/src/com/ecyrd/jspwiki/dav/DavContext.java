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
package com.ecyrd.jspwiki.dav;

import javax.servlet.http.HttpServletRequest;


public class DavContext
{
    protected int    m_depth = -1;
    protected DavPath m_path;
    
    public DavContext( HttpServletRequest req, DavPath dp )
    {
        m_path = dp;
        
        String depth = req.getHeader("Depth");
        
        if( depth == null )
        {
            m_depth = -1;
        }
        else
        {
            m_depth = Integer.parseInt( depth );
        }
    }

    /**
     * @return Returns the m_depth.
     */
    public int getDepth()
    {
        return m_depth;
    }

    /**
     * @return Returns the m_path.
     */
    public DavPath getPath()
    {
        return m_path;
    }
}