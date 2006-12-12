/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom.Text;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 *  Stores the contents of a plugin in a WikiDocument DOM tree.
 *  
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class PluginContent extends Text
{
    private static final long serialVersionUID = 1L;

    private String m_pluginName;
    private Map    m_params;
    
    public PluginContent( String pluginName, Map parameters )
    {
        m_pluginName = pluginName;
        m_params     = parameters;
    }
    /**
     * @since 2.5.7
     */
    public String getPluginName()
    {
        return m_pluginName;
    }
    
    public String getValue()
    {
        return getText();
    }
    
    public String getText()
    {
        String result;
        
        WikiContext context = ((WikiDocument)getDocument()).getContext();
        
        Boolean b = (Boolean)context.getVariable( RenderingManager.VAR_EXECUTE_PLUGINS );
        if( b != null && !b.booleanValue() ) return "";
        
        try
        {
            WikiEngine engine = context.getEngine();
            
            HashMap parsedParams = new HashMap();
            
            //
            //  Parse any variable instances from the string
            //
            for( Iterator i = m_params.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry e = (Map.Entry) i.next();
                
                Object val = e.getValue();
                
                if( val instanceof String )
                {
                    val = engine.getVariableManager().expandVariables( context, (String)val );
                }
                
                parsedParams.put( e.getKey(), val );
            }
            
            result = engine.getPluginManager().execute( context,
                                                        m_pluginName,
                                                        parsedParams );
        }
        catch( Exception e )
        {
            // log.info("Failed to execute plugin",e);
            return JSPWikiMarkupParser.makeError("Plugin insertion failed: "+e.getMessage()).getText();
        }
        
        return result;
    }

}
