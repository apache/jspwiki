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
package com.ecyrd.jspwiki.plugin;

import java.util.Map;
import com.ecyrd.jspwiki.WikiContext;

/**
 *  If a plugin defines this interface, it is called during JSPWiki
 *  initialization, if it occurs on a page.
 *
 *  @since 2.2
 *
 *  @author Janne Jalkanen
 */
public interface InitializablePlugin
{
    /**
     *  The initialization routine.  The context is to a Wiki page,
     *  and the parameters are exactly like in the execute() -routine.
     *  However, this routine is not expected to return anything,
     *  as any output will be discarded. 
     */

    public void initialize( WikiContext context, Map params )
        throws PluginException;
}
