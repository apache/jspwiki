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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.plugin.ParserStagePlugin;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.plugin.PluginElement;

import java.util.Map;

/**
 *  Implements a simple plugin that just returns its text.
 *  <P>
 *  Parameters: text - text to return.
 *  Any _body content gets appended between brackets.
 */
public class SamplePlugin implements Plugin, ParserStagePlugin {
	
    protected static boolean c_rendered = false;
    
    @Override
    public String execute( final Context context, final Map< String, String > params ) {
        final StringBuilder sb = new StringBuilder();
        final String text = params.get("text");

        if( text != null ) {
            sb.append( text );
        }

        final String body = params.get("_body");
        if( body != null ) {
            sb.append( " (" ).append( body.replace( '\n', '+' ) ).append( ")" );
        }

        return sb.toString();
    }

    @Override
    public void executeParser( final PluginElement element, final Context context, final Map< String, String > params) {
        if( element.getParameter("render") != null ) {
            c_rendered = true;
        }
    }

}
