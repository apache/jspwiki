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
package org.apache.wiki.plugin;

import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.ParserStagePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.PluginContent;

/**
 *  Implements a simple plugin that just returns its text.
 *  <P>
 *  Parameters: text - text to return.
 *  Any _body content gets appended between brackets.
 *
 */
public class SamplePlugin implements WikiPlugin, ParserStagePlugin {
	
    protected static boolean c_rendered = false;
    
    public String execute( WikiContext context, Map< String, String > params ) throws PluginException {
    	StringBuilder sb = new StringBuilder();

        String text = params.get("text");

        if( text != null ) {
            sb.append( text );
        }

        String body = params.get("_body");
        if( body != null ) {
            sb.append( " ("+body.replace('\n','+')+")" );
        }

        return sb.toString();
    }

    public void executeParser(PluginContent element, WikiContext context, Map< String, String > params) {
        if( element.getParameter("render") != null ) c_rendered = true;
    }

}
