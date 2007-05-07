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
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 *  Stores the contents of a plugin in a WikiDocument DOM tree.
 *  
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class PluginContent extends Text
{
    private static final String BLANK = "";
    private static final String CMDLINE = "_cmdline";
    private static final String ELEMENT_BR = "<br/>";
    private static final String EMITTABLE_PLUGINS = "Image|FormOpen|FormClose|FormInput|FormTextarea|FormSelect";
    private static final String LINEBREAK = "\n";
    private static final String PLUGIN_START = "[{";
    private static final String PLUGIN_END = "}]";
    private static final String SPACE = " ";

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
    
    public Object getParameter( String name )
    {
        return m_params.get(name);
    }
    
    public Map getParameters()
    {
        return m_params;
    }
    
    public String getValue()
    {
        return getText();
    }
    
    public String getText()
    {
        String result;
        
        WikiDocument doc = (WikiDocument)getDocument();

        if( doc == null )
        {
            //
            // This element has not yet been attached anywhere, so we simply assume there is
            // no rendering and return the plugin name.  This is required e.g. when the 
            // paragraphify() checks whether the element is empty or not.  We can't of course
            // know whether the rendering would result in an empty string or not, but let us
            // assume it does not.
            //
            
            return getPluginName();
        }
               
        WikiContext context = doc.getContext();

        try
        {
            Boolean wysiwygEditorMode = (Boolean)context.getVariable(RenderingManager.WYSIWYG_EDITOR_MODE);

            //
            //  Determine whether we should emit the actual code for this plugin or
            //  whether we should execute it.  For some plugins we always execute it,
            //  since they can be edited visually.
            //
            // FIXME: The plugin name matching should not be done here, but in a per-editor resource
            if( wysiwygEditorMode != null && wysiwygEditorMode.booleanValue() 
                && !m_pluginName.matches( EMITTABLE_PLUGINS ) )
            {        
                result = PLUGIN_START + m_pluginName + SPACE;            
            
                // convert newlines to <br> in case the plugin has a body.
                String cmdLine = ( (String)m_params.get( CMDLINE ) ).replaceAll( LINEBREAK, ELEMENT_BR );
            
                result = result + cmdLine + PLUGIN_END;
            }
            else
            {
                Boolean b = (Boolean)context.getVariable( RenderingManager.VAR_EXECUTE_PLUGINS );
                if( b != null && !b.booleanValue() ) return BLANK;

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
        }
        catch( Exception e )
        {
            // log.info("Failed to execute plugin",e);
            result = JSPWikiMarkupParser.makeError("Plugin insertion failed: "+e.getMessage()).getText();
        }
        
        
        return result;
    }
    
    /**
     *  Executes the executeParse() method.
     *  
     *  @param m_context
     */
    public void executeParse(WikiContext m_context)
        throws PluginException
    {
        m_context.getEngine().getPluginManager().executeParse( this, m_context );
    }

}
