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

import com.ecyrd.jspwiki.*;
import java.util.Map;

/**
 *  Defines an interface for plugins.  Any instance of a wiki plugin
 *  should implement this interface.
 *
 *  @author Janne Jalkanen
 */
public interface WikiPlugin
{
    /**
     *  This is the main entry point for any plugin.  The parameters are parsed,
     *  and a special parameter called "_body" signifies the name of the plugin
     *  body, i.e. the part of the plugin that is not a parameter of
     *  the form "key=value".  This has been separated using an empty
     *  line.
     *  <P>
     *  Note that it is preferred that the plugin returns
     *  XHTML-compliant HTML (i.e. close all tags, use &lt;br /&gt;
     *  instead of &lt;br&gt;, etc.
     *
     *  @param context The current WikiContext.
     *  @param params  A Map which contains key-value pairs.  Any
     *                 parameter that the user has specified on the
     *                 wiki page will contain String-String
     *  parameters, but it is possible that at some future date,
     *  JSPWiki will give you other things that are not Strings.
     *
     *  @return HTML, ready to be included into the rendered page.
     *
     *  @throws PluginException In case anything goes wrong.
     */

    public String execute( WikiContext context, Map params )
        throws PluginException;
}
