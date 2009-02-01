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
package org.apache.wiki.api;

import java.util.Iterator;

/**
 *  Provides the master interface to the content repository and
 *  main JSPWiki functionality.
 */
public interface WikiEngine
{
    /**
     *  Locates a WikiPage object based on the given path.
     *  
     *  @param path The JCR path, relative to the WikiEngine.
     *  @param version The version which to look for
     *  @return A WikiPage object, or null, if it could not be located.
     */
    public WikiPage getPage( String path, int version );
    
    /**
     *  Returns a Renderer object for a particular type.  Allowed types
     *  are "xhtml" for XHTML renderer, or whatever might be available as plugins.
     *  
     *  @param type A string describing the destination format.
     *  @return A Renderer object.
     */
    public WikiRenderer getRenderer( String type );
    
    /**
     *  Gets a configuration parameter.
     */
    
    public String getConfigParameter( String key );
    
    /**
     *  Returns an iterator for all of the parameters keys.
     * 
     *  @return An immutable iterator instance.
     */
    public Iterator getAllConfigParameters();
}
