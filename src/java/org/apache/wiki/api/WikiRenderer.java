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

import java.io.InputStream;

import org.apache.wiki.WikiContext;


/**
 *  The WikiRenderer interface provides access to the JSPWiki rendering
 *  engine.  The job of the WikiRenderer is to grab content in a
 *  particular type, and shove it out in its native type.
 *  <p>
 *  Typical WikiRenderers might be:
 *  <ul>
 *    <li>XHTMLRenderer - takes in WikiMarkup and outputs XHTML</li>
 *    <li>TextRenderer - takes in WikiMarkup and produces plain text</li>
 *    <li>CleanRenderer - takes in WikiMarkup and makes it such that it can be included in HTML content.</li>
 *    <li>PDFRenderer - takes in any sort of content and turns it into PDF content.</li>
 *  </ul>
 *
 */
public interface WikiRenderer
{
    /**
     *  Returns the MIME type for the content that this Renderer
     *  outputs.  For example, a PDF renderer might be returning
     *  something like "application/pdf", and a HTML renderer might
     *  be returning "text/html".
     *  
     *  @return A MIME type describing the output of the Renderer.
     */
    public String getContentType();
    
    /**
     *  Returns the rendered content.
     */
    public InputStream render( WikiContext context, String content );
    
    /**
     *  Returns the rendered content as a String.  This is just a simplification
     *  for those content types where it can be rendered as a String.
     */
    public String renderString( WikiContext context, String content );
}
