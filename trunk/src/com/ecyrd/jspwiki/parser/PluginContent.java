/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
*/
package com.ecyrd.jspwiki.parser;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.jdom.Text;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 *  Stores the contents of a plugin in a WikiDocument DOM tree.
 *  <p>
 *  If the RenderingManager.WYSIWYG_EDITOR_MODE is set to Boolean.TRUE in the context, 
 *  then the plugin
 *  is rendered as WikiMarkup.  This allows an HTML editor to work without
 *  rendering the plugin each time as well. 
 *  <p>
 *  If RenderingManager.VAR_EXECUTE_PLUGINS is set to Boolean.FALSE, then
 *  the plugin is not executed.
 *  
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

    private String                m_pluginName;
    private Map<String,Object>    m_params;
    
    /**
     *  Creates a new DOM element with the given plugin name and a map of parameters.
     *  
     *  @param pluginName The FQN of a plugin.
     *  @param parameters A Map of parameters.
     */
    @SuppressWarnings("unchecked")
    public PluginContent( String pluginName, Map parameters )
    {
        m_pluginName = pluginName;
        m_params     = parameters;
    }
    /**
     *  Returns the name of the plugin invoked by the DOM element.
     *  
     *  @return Name of the plugin
     *  @since 2.5.7
     */
    public String getPluginName()
    {
        return m_pluginName;
    }
    
    /**
     *  Returns a parameter value from the parameter map.
     *  
     *  @param name the name of the parameter.
     *  @return The value from the map, or null, if no such parameter exists.
     */
    public Object getParameter( String name )
    {
        return m_params.get(name);
    }
    
    /**
     *  Returns the parameter map given in the constructor.
     *  
     *  @return The parameter map.
     */
    public Map getParameters()
    {
        return m_params;
    }
    
    /**
     *  Returns the rendered plugin.  Only calls getText().
     *  
     *  @return HTML
     */
    public String getValue()
    {
        return getText();
    }
    
    /**
     *  The main invocation for the plugin.  When the getText() is called, it
     *  invokes the plugin and returns its contents.  If there is no Document
     *  yet, only returns the plugin name itself.
     *  
     *  @return The plugin rendered according to the options set in the WikiContext.
     */
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
        
        Boolean wysiwygVariable = (Boolean)context.getVariable( RenderingManager.WYSIWYG_EDITOR_MODE );
        boolean wysiwygEditorMode = false;
        if( wysiwygVariable != null )
        {
            wysiwygEditorMode = wysiwygVariable.booleanValue();
        }

        try
        {
            //
            //  Determine whether we should emit the actual code for this plugin or
            //  whether we should execute it.  For some plugins we always execute it,
            //  since they can be edited visually.
            //
            // FIXME: The plugin name matching should not be done here, but in a per-editor resource
            if( wysiwygEditorMode 
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
            
                HashMap<String,Object> parsedParams = new HashMap<String,Object>();
            
                //
                //  Parse any variable instances from the string
                //
                for( Map.Entry e : m_params.entrySet() )
                {
                    Object val = e.getValue();
                
                    if( val instanceof String )
                    {
                        val = engine.getVariableManager().expandVariables( context, (String)val );
                    }
                
                    parsedParams.put( (String)e.getKey(), val );
                }
            
                result = engine.getPluginManager().execute( context,
                                                            m_pluginName,
                                                            parsedParams );
            }
        }
        catch( Exception e )
        {
            if( wysiwygEditorMode )
            {
                result = "";
            }
            else
            {
                // log.info("Failed to execute plugin",e);
                ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
                Object[] args = { e.getMessage() };
                result = JSPWikiMarkupParser.makeError( 
                                 MessageFormat.format( rb.getString( "plugin.error.insertionfailed" ), args ) ).getText();
            }
        }
        
        
        return result;
    }
    
    /**
     *  Executes the executeParse() method.
     *  
     *  @param context The WikiContext
     *  @throws PluginException If something goes wrong.
     */
    public void executeParse(WikiContext context)
        throws PluginException
    {
        context.getEngine().getPluginManager().executeParse( this, context );
    }

}
