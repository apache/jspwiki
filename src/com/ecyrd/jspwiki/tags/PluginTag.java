/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import java.util.Map;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.PluginException;

/**
 *  Inserts any Wiki plugin.
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>plugin - name of the plugin you want to insert.
 *    <LI>args   - An argument string for the tag.
 *  </UL>
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class PluginTag
    extends WikiTagBase
{
    private String m_plugin;
    private String m_args;

    public void setPlugin( String p )
    {
        m_plugin = p;
    }

    public void setArgs( String a )
    {
        m_args = a;
    }
    
    public final int doWikiStartTag()
        throws IOException,
               PluginException
    {
        WikiEngine engine = m_wikiContext.getEngine();

        PluginManager pm  = engine.getPluginManager();

        Map argmap = pm.parseArgs( m_args );

        String result = pm.execute( m_wikiContext, m_plugin, argmap );

        pageContext.getOut().write( result );
        
        return SKIP_BODY;
    }
}
