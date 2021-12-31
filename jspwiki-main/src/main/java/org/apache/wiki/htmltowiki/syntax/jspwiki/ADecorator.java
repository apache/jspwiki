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

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.htmltowiki.syntax.MarkupHelper;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;
import java.util.Map;


/**
 * Translates to JSPWiki syntax from an A element.
 */
class ADecorator {

    final PrintWriter out;
    final XHtmlToWikiConfig config;
    final XHtmlElementToWikiTranslator chain;

    ADecorator( final PrintWriter out, final XHtmlToWikiConfig config, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.config = config;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param e XHTML element being translated.
     */
    void decorate( final Element e, final String ref ) throws JDOMException {
        out.print( "[" );
        chain.translate( e );

        // May end up with duplicated text/ref ([ref|ref] or [ref|ref|attributes]), but that's ok, as it's valid and required code gets simpler
        out.print( "|" );
        MarkupHelper.printUnescaped( out, ref );

        final Map< String, String > augmentedWikiLinkAttributes = MarkupHelper.getAugmentedWikiLinkAttributes( e );
        if( containsAdditionalLinkAttributes( augmentedWikiLinkAttributes ) ) {
            out.print( "|" );
            final String augmentedWikiLink = MarkupHelper.augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
            out.print( augmentedWikiLink );
        }
        out.print( "]" );
    }

    boolean containsAdditionalLinkAttributes( final Map< String, String > augmentedWikiLinkAttributes ) {
        return !augmentedWikiLinkAttributes.isEmpty();
    }

    void decorateFootnote( final String text, final String href ) {
        out.print( "[" + text + "|" + href + "]" );
    }

    void decorateUndefinedLink( final Element e ) throws JDOMException {
        out.print( "[" );
        chain.translate( e );
        out.print( "]" );
    }

}
