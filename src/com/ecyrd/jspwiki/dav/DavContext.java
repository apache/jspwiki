/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import javax.servlet.http.HttpServletRequest;


public class DavContext
{
    public String m_davcontext = "";
    public String m_page = null;
    public int    m_depth = -1;
    
    public DavContext( HttpServletRequest req )
    {
        String path = req.getPathInfo();
        
        if( path.startsWith("/") ) path = path.substring(1);
        
        int idx = path.indexOf('/');
        
        if( idx != -1 )
        {
            m_davcontext = path.substring( 0, idx );
            
            m_page = path.substring( idx+1 );
        }
        else
        {
            m_davcontext = path;
        }
        
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
}