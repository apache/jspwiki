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

import org.apache.wiki.providers.ProviderException;

/**
 *  A particular kind of exception noting that the WikiPage or attachment 
 *  in question was not found.
 *  
 *  @since 3.0
 */
public class PageNotFoundException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     *  Construct an exception from a String path. 
     *  @param path The path to the nonexistant WikiPage.
     */
    public PageNotFoundException( String path )
    {
        super( path );
    }

    /**
     *  Construct an exception from a WikiName path. 
     *  @param path The path to the nonexistant WikiPage.
     */
    public PageNotFoundException( WikiPath path )
    {
        super( path.toString() );
    }
}
