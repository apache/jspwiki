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
package org.apache.wiki.rpc;

/**
 *  A base class for managing RPC calls.
 *  
 *  @since 2.5.4
 */
public class RPCManager
{
    /**
     *  Private constructor to prevent initialization.
     */
    protected RPCManager() {}
    
    /**
     *  Gets an unique RPC ID for a callable object.  This is required because a plugin
     *  does not know how many times it is already been invoked.
     *  <p>
     *  The id returned contains only upper and lower ASCII characters and digits, and
     *  it always starts with an ASCII character.  Therefore the id is suitable as a
     *  programming language construct directly (e.g. object name).
     *  
     *  @param c An RPCCallable
     *  @return An unique id for the callable.
     */
    public static String getId( RPCCallable c )
    {
        return "RPC"+c.hashCode();
    }
    

}
