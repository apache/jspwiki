/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;

import javax.servlet.ServletContext;

import org.jdom.Element;

import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.dav.AttachmentDavProvider;
import com.ecyrd.jspwiki.dav.DavPath;

/**
 * Represents a DAV attachment.
 *  @author jalkanen
 *
 *  @since 
 */
public class AttachmentItem extends PageDavItem
{

    /**
     * Constructs a new DAV attachment.
     * @param provider the dav provider
     * @param path the current dav path
     * @param att the attachment
     */
    public AttachmentItem( AttachmentDavProvider provider, DavPath path, Attachment att )
    {
        super( provider, path,  att );
    }

   
    /**
     * Returns a collection of properties for this attachment.
     * @return the attachment properties
     * @see com.ecyrd.jspwiki.dav.items.DavItem#getPropertySet()
     */
    public Collection getPropertySet()
    {
        Collection set = getCommonProperties();
        
        set.add( new Element("getcontentlength",m_davns).setText( Long.toString(getLength())) );
        set.add( new Element("getcontenttype",m_davns).setText( getContentType() ));

        return set;
    }
    
    public String getHref()
    {
        return m_provider.getURL( m_path );
    }
    
    /**
     *  Returns the content type as defined by the servlet container;
     *  or if the container cannot be found, returns "application/octet-stream".
     *  @return the content type
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
    
    /**
     * Returns the length of the attachment.
     * @return the length
     * @see com.ecyrd.jspwiki.dav.items.DavItem#getLength()
     */
    public long getLength()
    {
        return m_page.getSize();
    }
}
