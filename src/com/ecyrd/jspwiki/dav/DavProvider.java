/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.Collection;

import com.ecyrd.jspwiki.dav.items.DavItem;

public interface DavProvider
{
    
    public DavItem getItem( DavPath path );
    
    public void setItem( DavPath path, DavItem item );
    
    public String getURL( DavPath path );
}
