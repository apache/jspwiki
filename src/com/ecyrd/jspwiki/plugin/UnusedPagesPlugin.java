/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

/**
 *  Parameters: none.
 *
 *  @author Janne Jalkanen
 */
public class UnusedPagesPlugin
    extends AbstractReferralPlugin
{
    private static Category log = Category.getInstance( UnusedPagesPlugin.class );

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        Collection links = refmgr.findUnreferenced();

        String wikitext = wikitizeCollection( links, "\\\\", ALL_ITEMS );
        
        return context.getEngine().textToHTML( context, wikitext );
    }

}
