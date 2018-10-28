/* 
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
package org.apache.wiki.api.plugin;

import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;


/**
 *  Defines an interface for plugins.  Any instance of a wiki plugin should implement this interface.
 */
public interface WikiPlugin {
    /**
     *  Name of the default plugin resource bundle.
     */
    String CORE_PLUGINS_RESOURCEBUNDLE = "plugin.PluginResources";

    /**
     *  This is the main entry point for any plugin.  The parameters are parsed,
     *  and a special parameter called "_body" signifies the name of the plugin
     *  body, i.e. the part of the plugin that is not a parameter of
     *  the form "key=value".  This has been separated using an empty
     *  line.
     *  <P>
     *  Note that it is preferred that the plugin returns
     *  XHTML-compliant HTML (i.e. close all tags, use &lt;br /&gt;
     *  instead of &lt;br&gt;, etc.
     *
     *  @param context The current WikiContext.
     *  @param params  A Map which contains key-value pairs.  Any
     *                 parameter that the user has specified on the
     *                 wiki page will contain String-String
     *  parameters, but it is possible that at some future date,
     *  JSPWiki will give you other things that are not Strings.
     *
     *  @return HTML, ready to be included into the rendered page.
     *
     *  @throws PluginException In case anything goes wrong.
     */
    String execute( WikiContext context, Map< String, String > params ) throws PluginException;
    
}
