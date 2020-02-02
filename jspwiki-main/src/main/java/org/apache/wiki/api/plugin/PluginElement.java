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

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;

import java.util.Map;


/**
 * Exposes the contents of a plugin in a WikiDocument DOM tree.
 */
public interface PluginElement {

    /**
     * Returns the name of the plugin invoked by the DOM element.
     *
     * @return Name of the plugin
     * @since 2.5.7
     */
    String getPluginName();

    /**
     * Returns a parameter value from the parameter map.
     *
     * @param name the name of the parameter.
     * @return The value from the map, or null, if no such parameter exists.
     */
    String getParameter( String name);

    /**
     * Returns the parameter map given in the constructor.
     *
     * @return The parameter map.
     */
    Map< String, String > getParameters();

    /**
     * Returns the rendered plugin.  Only calls getText().
     *
     * @return HTML
     */
    String getValue();

    /**
     * The main invocation for the plugin.  When the getText() is called, it invokes the plugin and returns its contents.  If there is
     * no Document yet, only returns the plugin name itself.
     *
     * @return The plugin rendered according to the options set in the WikiContext.
     */
    String getText();

    /**
     * Performs plugin invocation and return its contents.
     *
     * @param context WikiContext in which the plugin is executed. Must NOT be null.
     * @return plugin contents.
     */
    String invoke( WikiContext context );

    /**
     * Executes the executeParse() method.
     *
     * @param context The WikiContext
     * @throws PluginException If something goes wrong.
     */
    void executeParse( WikiContext context ) throws PluginException;

}
