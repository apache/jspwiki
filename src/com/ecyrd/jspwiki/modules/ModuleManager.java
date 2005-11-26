package com.ecyrd.jspwiki.modules;

/**
 *  Superclass for all JSPWiki managers for modules (plugins, etc).
 *  @author jalkanen
 */
public abstract class ModuleManager
{

    /**
     * Location of the property-files of plugins.
     *  (Each plugin should include this property-file in its jar-file)
     */
    public static final String PLUGIN_RESOURCE_LOCATION = "ini/jspwiki_module.xml";
    
}
