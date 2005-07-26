/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;

import javax.servlet.ServletContext;

import org.jdom.Element;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.dav.AttachmentDavProvider;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.sun.corba.se.connection.GetEndPointInfoAgainException;

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
    public AttachmentItem( AttachmentDavProvider provider, DavPath path, Attachment att )
    {
        super( provider, path,  att );
    }

   
    public Collection getPropertySet()
    {
        Collection set = getCommonProperties();
        
        set.add( new Element("getcontentlength",m_davns).setText( Long.toString(getLength())) );
        set.add( new Element("getcontenttype",m_davns).setText( getContentType() ));

        return set;
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.items.PageDavItem#getHref()
     */
    public String getHref()
    {
        return m_provider.getURL( m_path );
    }
    
    /**
     *  Returns the content type as defined by the servlet container;
     *  or if the container cannot be found, returns "application/octet-stream".
     */
    public String getContentType()
    {
        ServletContext ctx = ((AttachmentDavProvider)m_provider).getEngine().getServletContext();
        
        if( ctx != null )
        {
            String mimetype = ctx.getMimeType( m_page.getName() );
            
            if( mimetype != null ) return mimetype;
        }
        
        return "application/octet-stream"; // FIXME: This is not correct
    }
    
    public long getLength()
    {
        return m_page.getSize();
    }
}
