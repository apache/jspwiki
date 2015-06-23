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
import org.apache.wiki.parser.PluginContent;

/**
 *  Implements a Plugin interface for the parser stage.  Please see org.apache.wiki.api.PluginManager
 *  for further documentation.
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
    void executeParser( PluginContent element, WikiContext context, Map< String, String > params );
    
}
