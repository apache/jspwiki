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
package org.apache.wiki.htmltowiki;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.StringReader;

/**
 * Converting Html to Wiki Markup with NekoHtml for converting html to xhtml and
 * Xhtml2WikiTranslator for converting xhtml to Wiki Markup.
 *
 */
public class HtmlStringToWikiTranslator {

    private static final String CYBERNEKO_PARSER = "org.cyberneko.html.parsers.SAXParser";
    private final Engine e;

    /**
     *  Create a new translator.
     */
    public HtmlStringToWikiTranslator( final Engine e ) {
        this.e = e;
    }

    /**
     *  Translates text from HTML into WikiMarkup without a WikiContext (meaning
     *  some things perhaps cannot be translated).  Uses the default configuration.
     *
     *  @param html HTML text to translate
     *  @return WikiMarkup
     *
     *  @throws JDOMException If parsing fails
     *  @throws IOException For other kinds of errors.
     */
    public String translate( final String html ) throws JDOMException, IOException, ReflectiveOperationException {
        return translate( html, new XHtmlToWikiConfig() );
    }

    /**
     *  Translates text from HTML into WikiMarkup with a WikiContext.  The translation
     *  accuracy is better.  Uses the default configuration.
     *
     *  @param html HTML text to translate
     *  @param wikiContext The WikiContext to use.
     *  @return WikiMarkup
     *
     *  @throws JDOMException If parsing fails
     *  @throws IOException For other kinds of errors.
     */
    public String translate( final String html, final Context wikiContext ) throws JDOMException, IOException, ReflectiveOperationException {
        return translate( html, new XHtmlToWikiConfig( wikiContext ) );
    }

    /**
     *  Translates text from HTML into WikiMarkup using a specified configuration.
     *
     *  @param html HTML text to translate
     *  @param config The configuration to use.
     *  @return WikiMarkup
     *
     *  @throws JDOMException If parsing fails
     *  @throws IOException For other kinds of errors.
     */
    public String translate( final String html, final XHtmlToWikiConfig config ) throws JDOMException, IOException, ReflectiveOperationException {
        final Element element = htmlStringToElement( html );
        final XHtmlElementToWikiTranslator xhtmlTranslator = new XHtmlElementToWikiTranslator( e, element, config );
        return xhtmlTranslator.getWikiString();
    }

    /**
     * Use NekoHtml to parse HTML like well-formed XHTML
     *
     * @param html HTML to parse.
     * @return xhtml jdom root element (node "HTML")
     * @throws JDOMException when errors occur in parsing
     * @throws IOException when an I/O error prevents a document from being fully parsed
     */
    private Element htmlStringToElement( final String html ) throws JDOMException, IOException {
        final SAXBuilder builder = new SAXBuilder( new XMLReaderSAX2Factory( true, CYBERNEKO_PARSER ), null, null );
        //builder.setProperty( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
        //builder.setProperty( XMLConstants.ACCESS_EXTERNAL_SCHEMA, "" );
        final Document doc = builder.build( new StringReader( html ) );
        return doc.getRootElement();
    }

    /**
     *  A static helper method to create HTML from an Element.
     *
     *  @param element The element to get HTML from.
     *  @return HTML
     */
    public static String element2String( final Element element ) {
        final Document document = new Document( element );
        final XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString( document );
    }

}
