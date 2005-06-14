/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import com.ecyrd.jspwiki.WikiEngine;
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
        super( provider, "/");
        addDavItem( new DirectoryItem( provider, "raw") );
        // addDavItem( new DirectoryItem( provider, "html") );
    }
}
