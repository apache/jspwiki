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
import org.jdom2.Element;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Translates to JSPWiki syntax from an XHTML Image.
 */
class ImageDecorator {

    final PrintWriter out;
    final XHtmlToWikiConfig config;

    ImageDecorator( final PrintWriter out, final XHtmlToWikiConfig config ) {
        this.out = out;
        this.config = config;
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param src image source.
     * @param imageAttrs image attributes.
     */
    void decorate( final String src, final Map< String, Object > imageAttrs ) {
        if( imageAttrs.isEmpty() ) {
            out.print( "[" + src + "]" );
        } else {
            out.print( "[{Image src='" + src + "'" );
            for( final Map.Entry< String, Object > objectObjectEntry : imageAttrs.entrySet() ) {
                if ( !objectObjectEntry.getValue().equals( "" ) ) {
                    out.print( " " + objectObjectEntry.getKey() + "='" + objectObjectEntry.getValue() + "'" );
                }
            }
            out.print( "}]" );
        }
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param e XHTML element being translated.
     */
    void decorate( final Element e ) {
        if( config.isNotIgnorableWikiMarkupLink( e ) ) {
            out.print( "[" );
            MarkupHelper.printUnescaped( out, config.trimLink( e.getAttributeValue( "src" ) ) );
            out.print( "]" );
        }
    }

}
