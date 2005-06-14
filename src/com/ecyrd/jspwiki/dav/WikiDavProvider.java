/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.Collection;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.items.DavItem;

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
