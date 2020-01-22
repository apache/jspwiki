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
package org.apache.wiki.url;

import org.apache.wiki.WikiEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;


/**
 *  Provides an interface through which JSPWiki constructs URLs.
 *  JSPWiki calls the methods of this interface whenever an URL
 *  that points to any JSPWiki internals is required.  For example,
 *  if you need to find an URL to the editor page for page "TextFormattingRules",
 *  you would call makeURL( WikiContext.EDIT, "TextFormattingRules", false, null );
 *
 *  @since 2.2
 */
public interface URLConstructor {
    /**
     *  Initializes.  Note that the engine is not fully initialized at this
     *  point, so don't do anything fancy here - use lazy init, if you have to.
     *
     *  @param  engine The WikiEngine that this URLConstructor belongs to
     *  @param properties Properties used to initialize
     */
    void initialize( WikiEngine engine, Properties properties );

    /**
     *  Constructs the URL with a bunch of parameters.
     *
     *  @param context The request context (@see WikiContext) that you want the URL for
     *  @param name The page name (or in case of WikiContext.NONE, the auxiliary JSP page
     *              or resource you want to point at).  This must be URL encoded.  Null is NOT safe.
     *  @param parameters An URL parameter string (these must be URL-encoded, and separated with &amp;amp;)
     *  @return An URL pointing to the resource.  Must never return null - throw an InternalWikiException  if something goes wrong.
     */
    String makeURL( String context, String name, String parameters );

    /**
     *  Should parse the "page" parameter from the actual request. This is essentially the reverse of makeURL() - whenever
     *  a request constructed by calls to makeURL() is passed to this routine, it MUST be able to parse the resource name
     *  (WikiPage, Attachment, other resource) from the request.
     *
     *  @param context In which request context the request was made (this should help in parsing)
     *  @param request The HTTP request that was used when coming here
     *  @param encoding The encoding with which the request was made (UTF-8 or ISO-8859-1).
     *  @return This method must return the name of the resource.
     *  @throws IOException If parsing failes
     */
    String parsePage( String context, HttpServletRequest request, Charset encoding ) throws IOException;

    /**
     *  Returns information which JSP page should continue handling this type of request.
     *
     * @param request The HTTP Request that was used to end up in this page.
     * @return "Wiki.jsp", "PageInfo.jsp", etc.  Just return the name, JSPWiki will figure out the page.
     */
    String getForwardPage( HttpServletRequest request );

}
