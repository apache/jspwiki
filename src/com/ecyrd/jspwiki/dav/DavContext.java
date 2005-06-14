/*
 * (C) Janne Jalkanen 2005
 * 
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