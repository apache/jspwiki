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
package org.apache.wiki.content;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.providers.ProviderException;

/**
 *  Resolves a given WikiName to a new WikiName.  For example,
 *  a subclass could check for plural forms of the WikiName
 *  and return the corresponding version.  It could also be
 *  used to sanitize a WikiName or whatever you feel is useful.
 */
public abstract class PageNameResolver
{
    protected WikiEngine m_engine;

    /**
     *  Construct a PageNameResolver against a given WikiEngine.
     *  
     *  @param engine The Engine.
     */
    public PageNameResolver( WikiEngine engine )
    {
        m_engine = engine;
    }   

    /**
     *  Resolves the page name to another page.
     *  
     *  @param name The name to check for
     *  @return A new name that you should getPage() on.
     *  @throws ProviderException If the resolution fails in any way.
     */
    public abstract WikiPath resolve( WikiPath name ) throws ProviderException;
}
