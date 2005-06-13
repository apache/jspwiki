/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.dav.DavProvider;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class AttachmentItem extends PageDavItem
{

    /**
     * @param engine
     * @param page
     */
    public AttachmentItem( DavProvider provider, Attachment att )
    {
        super( provider, att );
    }

   
    public Collection getPropertySet()
    {
        Collection props = getCommonProperties();
        
        return props;
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.items.PageDavItem#getHref()
     */
    public String getHref()
    {
        return "";
        /*
        return m_engine.getURL( WikiContext.NONE,
                                "dav/raw/"+m_page.getName(),
                                null,
                                true );
                                */
    }
}
