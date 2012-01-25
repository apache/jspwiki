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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.jdom.Element;
import org.jdom.Namespace;

import org.apache.wiki.dav.DavPath;
import org.apache.wiki.dav.DavProvider;

/**
 *
 *  @since 
 */
public class DirectoryItem extends DavItem
{
    public DirectoryItem( DavProvider provider, DavPath path )
    {
        super( provider, path );
    }
    
    public String getContentType()
    {
        return "text/plain; charset=UTF-8";
    }

    public long getLength()
    {
        return -1;
    }

    public Collection getPropertySet()
    {
        ArrayList<Element> ts = new ArrayList<Element>();
        Namespace davns = Namespace.getNamespace( "DAV:" );
        
        ts.add( new Element("resourcetype",davns).addContent(new Element("collection",davns)) );
        
        Element txt = new Element("displayname",davns);
        txt.setText( m_path.getName() );
        ts.add( txt );

        ts.add( new Element("getcontentlength",davns).setText("0") );
        ts.add( new Element("getlastmodified", davns).setText(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date())));
        
        
        return ts;
    }

    public String getHref()
    {
        return m_provider.getURL( m_path );
    }
    
    public void addDavItem( DavItem di )
    {
        m_items.add( di );
    }

    @SuppressWarnings("unchecked")
    public void addDavItems( Collection c )
    {
        m_items.addAll( c );
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.dav.items.DavItem#getInputStream()
     */
    public InputStream getInputStream()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
