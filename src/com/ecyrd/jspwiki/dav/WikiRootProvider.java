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
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.TopLevelDavItem;

public class WikiRootProvider extends WikiDavProvider
{
    public WikiRootProvider( WikiEngine engine )
    {
        super( engine );
    }

    public Collection listItems( DavPath path )
    {
        ArrayList list = new ArrayList();
        
        list.add( new TopLevelDavItem(this) );
        
        return list;
    }

    public DavItem getItem( DavPath path )
    {
        return new TopLevelDavItem(this);
    }

    public DavItem refreshItem( DavItem old, DavPath path )
    {
        return new TopLevelDavItem(this);
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

    public String getURL( DavPath path )
    {
        return m_engine.getURL( WikiContext.NONE, "dav/"+path.getPath(), null, false );
    }

}
