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
package org.apache.wiki.htmltowiki.syntax.jspwiki;

import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.htmltowiki.syntax.ImageDecorator;

import java.io.PrintWriter;
import java.util.Map;

/**
 * JSPWiki syntax implementation of {@link ImageDecorator}.
 */
class JSPWikiImageDecorator extends ImageDecorator {

    JSPWikiImageDecorator( final PrintWriter out, final XHtmlToWikiConfig config ) {
        super( out, config );
    }

    /** {@inheritDoc} */
    @Override
    protected String markupImageSimpleOpen() {
        return "[";
    }

    /** {@inheritDoc} */
    @Override
    protected String markupImageSimpleClose() {
        return "]";
    }

    /** {@inheritDoc} */
    @Override
    protected void markupImageWithAttributes( final String src, final Map< String, Object > imageAttrs ) {
        out.print( "[{Image src='" + src + "'" );
        for( final Map.Entry< String, Object > objectObjectEntry : imageAttrs.entrySet() ) {
            if ( !objectObjectEntry.getValue().equals( "" ) ) {
                out.print( " " + objectObjectEntry.getKey() + "='" + objectObjectEntry.getValue() + "'" );
            }
        }
        out.print( "}]" );
    }

}
