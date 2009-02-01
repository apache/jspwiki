/*
    JSPWiki - a JSP-based WikiWiki clone.

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
import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.plugin.WikiPlugin;


/**
 *  Implements a simple plugin that just returns its text.
 *  <P>
 *  Parameters: text - text to return.
 *
 *  @author Janne Jalkanen
 */
public class SamplePlugin2
    implements WikiPlugin
{
    public void initialize( WikiEngine engine )
        throws PluginException
    {
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        return (String)params.get("text");
    }

}
