package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;

public class AttachmentDirectoryItem extends DirectoryItem
{
    public AttachmentDirectoryItem( DavProvider provider, DavPath parentpage )
    {
        super( provider, parentpage );
    }

    public String getHref()
    {
        return "";
        // return m_provider.getEngine().getURL( WikiContext.ATTACH, "", null, true );
    }

    public Collection getPropertySet()
    {
        // TODO Auto-generated method stub
        return super.getPropertySet();
    }
    
    
}
