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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import org.jdom.Element;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.dav.DavPath;
import org.apache.wiki.dav.DavProvider;
import org.apache.wiki.dav.WikiDavProvider;
import org.apache.wiki.parser.MarkupParser;

/**
 * Represents a DAV HTML page item.
 *
 *  @since
 */
public class HTMLPageDavItem extends PageDavItem
{
    private long m_cachedLength = -1;

    /**
     * @param provider the DAV provider
     * @param path the DAV path
     * @param page the wiki page
     */
    public HTMLPageDavItem( DavProvider provider, DavPath path, WikiPage page )
    {
        super( provider, path, page );
    }

    /**
     * @see org.apache.wiki.dav.items.DavItem#getHref()
     */
    public String getHref()
    {
        return m_provider.getURL( m_path );
    }

    /**
     * Returns the content type for the item. Always returns
     * <code>text/html; charset=UTF-8</code>.
     * @see org.apache.wiki.dav.items.DavItem#getContentType()
     */
    public String getContentType()
    {
        return "text/html; charset=UTF-8";
    }

    private byte[] getText()
    {
        WikiEngine engine = ((WikiDavProvider)m_provider).getEngine();

        WikiContext context = new WikiContext( engine, m_page );
        context.setRequestContext( WikiContext.VIEW );

        context.setVariable( MarkupParser.PROP_RUNPLUGINS, "false" );
        context.setVariable( WikiEngine.PROP_RUNFILTERS, "false" );

        String text = engine.getHTML( context, m_page );

        try
        {
            return text.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e )
        {
            return null; // Should never happen
        }
    }

    public InputStream getInputStream()
    {
        byte[] text = getText();

        ByteArrayInputStream in = new ByteArrayInputStream( text );

        return in;
    }

    public long getLength()
    {
        if( m_cachedLength == -1 )
        {
            byte[] text = getText();

            m_cachedLength = text.length;
        }

        return m_cachedLength;
    }

    public Collection getPropertySet()
    {
        Collection<Element> set = getCommonProperties();

        //
        //  Rendering the page for every single time is not really a very good idea.
        //

        set.add( new Element("getcontentlength",m_davns).setText( Long.toString(getLength()) ) );
        set.add( new Element("getcontenttype",m_davns).setText("text/html; charset=\"UTF-8\""));

        return set;
    }
}
