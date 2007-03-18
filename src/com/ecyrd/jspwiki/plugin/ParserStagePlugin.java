package com.ecyrd.jspwiki.plugin;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.PluginContent;

public interface ParserStagePlugin
{
    public void executeParser( PluginContent element, WikiContext context, Map params );
}
