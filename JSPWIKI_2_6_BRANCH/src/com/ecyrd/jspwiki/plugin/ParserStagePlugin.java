/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
import com.ecyrd.jspwiki.parser.PluginContent;

/**
 *  Implements a Plugin interface for the parser stage.  Please see PluginManager
 *  for further documentation.
 * 
 *  @author jalkanen
 *  @since 2.5.30
 */
public interface ParserStagePlugin
{
    /**
     *  Method which is executed during parsing.
     *  
     *  @param element The JDOM element which has already been connected to the Document.
     *  @param context WikiContext, as usual.
     *  @param params  Parsed parameters for the plugin.
     */
    public void executeParser( PluginContent element, WikiContext context, Map params );
}
