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
package com.ecyrd.jspwiki.plugin;

import java.util.Map;

import org.apache.jspwiki.api.PluginException;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.json.JSONRPCManager;

/**
 *  Simple plugin which shows how to add JSON calls to your plugin.
 * 
 *  <p>Parameters : </p>
 *  NONE
 *  
 *  @since  2.5.4
 */
public class RPCSamplePlugin implements WikiPlugin, RPCCallable
{  
    /**
     *  This method is called when the Javascript is encountered by
     *  the browser.
     *  @param echo The parameter
     *  @return the string <code>JSON says:</code>, plus the value 
     *  supplied by the <code>echo</code> parameter
     *  
     *  <p>Parameters : </p>
     * NONE  
     */
    public String myFunction(String echo)
    {
        return "JSON says: "+echo;
    }
    

    /**
     *  {@inheritDoc}
     */
    public String execute(WikiContext context, Map params) throws PluginException
    {
        JSONRPCManager.registerJSONObject( context, this );
        
        String s = JSONRPCManager.emitJSONCall( context, this, "myFunction", "'foo'" );
        
        return s;
    }

}
