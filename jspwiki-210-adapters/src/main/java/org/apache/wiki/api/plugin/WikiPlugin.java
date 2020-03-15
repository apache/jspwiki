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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;

import java.util.Map;


/**
 *  Defines an interface for plugins.  Any instance of a wiki plugin should implement this interface.
 */
public interface WikiPlugin extends Plugin {

    /** {@inheritDoc} */
    @Override
    default String execute( final Context context, final Map< String, String > params ) throws PluginException {
        Logger.getLogger( WikiPlugin.class ).warn( this.getClass().getName() + " implements deprecated org.apache.wiki.api.plugin.WikiPlugin" );
        Logger.getLogger( WikiPlugin.class ).warn( "Please contact the plugin's author so there can be a new release of the plugin " +
                                                   "implementing the new org.apache.wiki.api.plugin.Plugin interface" );
        return execute( ( WikiContext )context, params );
    }

    String execute( WikiContext context, Map< String, String > params ) throws PluginException;
    
}
