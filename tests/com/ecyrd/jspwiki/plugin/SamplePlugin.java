package com.ecyrd.jspwiki.plugin;

import java.util.Map;

import org.apache.jspwiki.api.ModuleData;
import org.apache.jspwiki.api.PluginException;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.PluginContent;

/**
 *  Implements a simple plugin that just returns its text.
 *  <P>
 *  Parameters: text - text to return.
 *  Any _body content gets appended between brackets.
 */
@ModuleData( author = "Urgle Burgle", 
             aliases = { "samplealias2", "samplealias" } )
public class SamplePlugin
    implements WikiPlugin, ParserStagePlugin
{
    protected static boolean c_rendered = false;
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        StringBuffer sb = new StringBuffer();

        String text = (String) params.get("text");

        if( text != null )
        {
            sb.append( text );
        }

        String body = (String)params.get("_body");
        if( body != null )
        {
            sb.append( " ("+body.replace('\n','+')+")" );
        }

        return sb.toString();
    }

    public void executeParser(PluginContent element, WikiContext context, Map params)
    {
        if( element.getParameter("render") != null ) c_rendered = true;
    }

}
