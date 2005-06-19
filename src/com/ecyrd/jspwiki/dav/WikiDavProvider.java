/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;


import com.ecyrd.jspwiki.WikiEngine;

public abstract class WikiDavProvider implements DavProvider
{
    protected WikiEngine m_engine;
    
    public WikiDavProvider( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    public WikiEngine getEngine()
    {
        return m_engine;
    }
}
