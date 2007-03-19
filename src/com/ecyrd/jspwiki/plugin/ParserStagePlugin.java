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
