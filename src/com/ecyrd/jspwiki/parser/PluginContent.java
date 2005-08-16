/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.parser;

import java.util.Map;

import org.jdom.Text;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

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
    
    public String getValue()
    {
        return getText();
    }
    
    public String getText()
    {
        String result;
        
        WikiContext context = ((WikiDocument)getDocument()).getContext();
        
        try
        {
            WikiEngine engine = context.getEngine();
            result = engine.getPluginManager().execute( context,
                                                        m_pluginName,
                                                        m_params );
        }
        catch( Exception e )
        {
            // log.info("Failed to execute plugin",e);
            return JSPWikiMarkupParser.makeError("Plugin insertion failed: "+e.getMessage()).getText();
        }
        
        return result;
    }

}
