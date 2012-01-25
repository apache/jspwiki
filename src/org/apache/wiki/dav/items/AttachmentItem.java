/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.dav.items;

import java.util.Collection;

import javax.servlet.ServletContext;

import org.jdom.Element;

import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.dav.AttachmentDavProvider;
import org.apache.wiki.dav.DavPath;

/**
 * Represents a DAV attachment.
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
     * @see org.apache.wiki.dav.items.DavItem#getPropertySet()
     */
    public Collection getPropertySet()
    {
        Collection<Element> set = getCommonProperties();

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
     * @see org.apache.wiki.dav.items.DavItem#getLength()
     */
    public long getLength()
    {
        return m_page.getSize();
    }
}
