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
package org.apache.wiki.htmltowiki.syntax;

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;
import java.util.Map;


/**
 * Translates to wiki syntax from an {@code A} element.
 */
public abstract class ADecorator {

    final protected PrintWriter out;
    final protected XHtmlToWikiConfig config;
    final protected XHtmlElementToWikiTranslator chain;

    protected ADecorator( final PrintWriter out, final XHtmlToWikiConfig config, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.config = config;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e, final String ref ) throws JDOMException {
        final Map< String, String > augmentedWikiLinkAttributes = MarkupHelper.getAugmentedWikiLinkAttributes( e );
        if( containsAdditionalLinkAttributes( augmentedWikiLinkAttributes ) ) {
            final String augmentedWikiLink = MarkupHelper.augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
            linkMarkup( e, ref, augmentedWikiLink );
        } else if( !e.getTextTrim().equalsIgnoreCase( ref ) ) {
            linkMarkup( e, ref );
        } else {
            linkMarkup( e );
        }
    }

    boolean containsAdditionalLinkAttributes( final Map< String, String > augmentedWikiLinkAttributes ) {
        return !augmentedWikiLinkAttributes.isEmpty();
    }

    /**
     * Translate a link to a footnote to wiki syntax.
     *
     * @param text link's text
     * @param href link's href
     */
    public void decorateFootnote( final String text, final String href ) {
        linkMarkup( text, href );
    }

    /**
     * Translate an undefined link to wiki syntax.
     *
     * @param e element being translated.
     */
    public void decorateUndefinedLink( final Element e ) throws JDOMException {
        linkMarkup( e );
    }

    /**
     * Provide wiki markup for the XHTML element being translated.
     *
     * @param e element being translated.
     * @throws JDOMException error parsing the element being translated.
     */
    protected abstract void linkMarkup( Element e ) throws JDOMException;

    /**
     * Provide wiki markup for the XHTML element being translated.
     *
     * @param e element being translated.
     * @param ref link's href.
     * @throws JDOMException error parsing the element being translated.
     */
    protected abstract void linkMarkup( Element e, String ref ) throws JDOMException;

    /**
     * Provide wiki markup for the XHTML element being translated.
     *
     * @param e element being translated.
     * @param ref link's href.
     * @param additionalAttrs link's additional attributes.
     * @throws JDOMException error parsing the element being translated.
     */
    protected abstract void linkMarkup( Element e, String ref, String additionalAttrs ) throws JDOMException;

    /**
     * Provide wiki markup for the XHTML element being translated.
     *
     * @param text link's text
     * @param ref link's href - might be equals to {@code text}!
     */
    protected abstract void linkMarkup( String text, String ref );

}
