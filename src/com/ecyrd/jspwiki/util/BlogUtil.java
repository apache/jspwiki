/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.util;

import com.ecyrd.jspwiki.*;


/**
 *  Contains useful utilities for JSPWiki blogging functionality.
 *
 *  @author Janne Jalkanen
 *  @since 2.2.
 */
public class BlogUtil
{
    public static final String VAR_BLOGNAME = "blogname";

    /**
     *  Figure out a site name for a feed.
     */
    
    public static String getSiteName( WikiContext context )
    {
        WikiEngine engine = context.getEngine();

        String blogname = null;

        try
        {
            blogname = engine.getVariableManager().getValue( context, VAR_BLOGNAME );
        }
        catch( NoSuchVariableException e ) {}

        if( blogname == null )
        {
            blogname = engine.getApplicationName()+": "+context.getPage().getName();
        }

        return blogname;
    }
}
