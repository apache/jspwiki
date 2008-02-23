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
import com.ecyrd.jspwiki.action.WikiActionBean;


/**
 *  Contains useful utilities for JSPWiki blogging functionality.
 *
 *  @author Janne Jalkanen
 *  @since 2.2.
 */
public final class BlogUtil
{
    /**
     * Private constructor to prevent direct instantiation.
     */
    private BlogUtil()
    {
    }
    
    /** Wiki variable storing the blog's name. */
    public static final String VAR_BLOGNAME = "blogname";

    /**
     * Figure out a site name for a feed.
     * @param context the wiki context
     * @return the site name
     */
    public static String getSiteName( WikiActionBean actionBean )
    {
        WikiEngine engine = actionBean.getEngine();

        String blogname = null;

        try
        {
            blogname = engine.getVariableManager().getValue( actionBean, VAR_BLOGNAME );
        }
        catch( NoSuchVariableException e ) {}

        if( blogname == null )
        {
            if ( actionBean instanceof WikiContext )
            {
                blogname = engine.getApplicationName()+": "+((WikiContext)actionBean).getPage().getName();
            }
            else
            {
                blogname = engine.getApplicationName();
            }
        }

        return blogname;
    }
}
