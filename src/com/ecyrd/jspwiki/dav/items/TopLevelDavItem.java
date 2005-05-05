/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class TopLevelDavItem extends DirectoryItem
{
    public TopLevelDavItem( WikiEngine engine )
    {
        super( engine, "/");
        addDavItem( new DirectoryItem( engine, "raw") );
        addDavItem( new DirectoryItem( engine, "html") );
    }
}
