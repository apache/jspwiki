/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class TopLevelDavItem extends DirectoryItem
{
    public TopLevelDavItem( DavProvider provider )
    {
        super( provider, new DavPath("/") );
        addDavItem( new DirectoryItem( provider, new DavPath("raw") ) );
        addDavItem( new DirectoryItem( provider, new DavPath("html") ) );
    }
}
