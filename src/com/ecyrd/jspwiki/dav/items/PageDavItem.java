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
package com.ecyrd.jspwiki.dav.items;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.jspwiki.api.WikiPage;
import org.jdom.Element;
import org.jdom.Namespace;

import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.WikiDavProvider;

/**
 *
 *  @since 
 */
public class PageDavItem extends DavItem
{
    protected WikiPage  m_page;
    protected Namespace m_dcns = Namespace.getNamespace( "dc", "http://purl.org/dc/elements/1.1/" );
    protected Namespace m_davns = Namespace.getNamespace( "DAV:" );
    
    /**
     * 
     */
    public PageDavItem( DavProvider provider, DavPath path, WikiPage page )
    {
        super( provider, path );
        m_page = page;
    }

    public WikiPage getPage()
    {
        return m_page;
    }
    
    protected Collection<Element> getCommonProperties()
    {
        ArrayList<Element> set = new ArrayList<Element>();
        
        set.add( new Element("resourcetype",m_davns) );
        set.add( new Element("creator",m_dcns).setText(m_page.getAuthor()) );
        set.add( new Element("getlastmodified",m_davns).setText(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(m_page.getLastModified())));
        set.add( new Element("displayname",m_davns).setText(m_page.getName()) );
        
        return set;
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.DavItem#getPropertySet(int)
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

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.items.DavItem#getContentType()
     */
    public String getContentType()
    {
        return "text/plain; charset=UTF-8";
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.items.DavItem#getInputStream()
     */
    public InputStream getInputStream()
    {
        String text = ((WikiDavProvider)m_provider).getEngine().getPureText( m_page );
        
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream( text.getBytes("UTF-8") );
            
            return in;
        }
        catch( UnsupportedEncodingException e ) {}
        
        return null;
    }

    public long getLength()
    {
        // FIXME: Use getBytes()
        String text = ((WikiDavProvider)m_provider).getEngine().getPureText( m_page );
        return text.length();
    }
}
