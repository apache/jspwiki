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
package org.apache.wiki.url;

import java.util.Properties;

import org.apache.wiki.WikiEngine;


/**
 *  Provides an interface through which JSPWiki constructs URLs.
 *  JSPWiki calls the methods of this interface whenever an URL
 *  that points to any JSPWiki internals is required.  For example,
 *  if you need to find an URL to the editor page for page "TextFormattingRules",
 *  you would call makeURL( WikiContext.EDIT, "TextFormattingRules", false, null );
 *  
 *  @since 2.2
 */
public interface URLConstructor
{
    /**
     *  Initializes.  Note that the engine is not fully initialized
     *  at this point, so don't do anything fancy here - use lazy
     *  init, if you have to.
     *  
     *  @param  engine The WikiEngine that this URLConstructor belongs to
     *  @param properties Properties used to initialize
     */
    public void initialize( WikiEngine engine, 
                            Properties properties );

    /**
     *  Constructs the URL with a bunch of parameters.
     *  
     *  @param context The request context (@see WikiContext) that you want the URL for
     *  @param name The page name (or in case of WikiContext.NONE, the auxiliary JSP page 
     *              or resource you want to point at.  This must be URL encoded.  Null is NOT safe.
     *  @param absolute True, if you need an absolute URL.  False, if both relative and absolute
     *                  URLs are fine.
     *  @param parameters An URL parameter string (these must be URL-encoded, and separated with &amp;amp;)
     *  @return An URL pointing to the resource.  Must never return null - throw an InternalWikiException
     *          if something goes wrong.
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters );
}
