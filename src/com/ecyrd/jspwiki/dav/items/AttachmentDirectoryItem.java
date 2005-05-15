package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

public class AttachmentDirectoryItem extends DirectoryItem
{
    public AttachmentDirectoryItem( WikiEngine engine, String parentpage )
    {
        super( engine, parentpage );
    }

    public String getHref()
    {
        return m_engine.getURL( WikiContext.ATTACH, "", null, true );
    }

    public Collection getPropertySet()
    {
        // TODO Auto-generated method stub
        return super.getPropertySet();
    }
    
    
}
