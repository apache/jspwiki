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

import java.io.IOException;
import java.io.StringReader;

import org.apache.wiki.WikiContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.XMLOutputter;

/**
 * Converting Html to Wiki Markup with NekoHtml for converting html to xhtml and
 * Xhtml2WikiTranslator for converting xhtml to Wiki Markup.
 *
 */
public class HtmlStringToWikiTranslator
{

    private static final String CYBERNEKO_PARSER = "org.cyberneko.html.parsers.SAXParser";

    /**
     *  Create a new translator.
     */
    public HtmlStringToWikiTranslator()
    {}

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
    public String translate( String html ) throws JDOMException, IOException
    {
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
    public String translate( String html, WikiContext wikiContext ) throws JDOMException, IOException
    {
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

    public String translate( String html, XHtmlToWikiConfig config ) throws JDOMException, IOException
    {
        Element element = htmlStringToElement( html );
        XHtmlElementToWikiTranslator xhtmlTranslator = new XHtmlElementToWikiTranslator( element, config );
        String wikiMarkup = xhtmlTranslator.getWikiString();
        return wikiMarkup;
    }

    /**
     * Use NekoHtml to parse HTML like well formed XHTML
     *
     * @param html
     * @return xhtml jdom root element (node "HTML")
     * @throws JDOMException
     * @throws IOException
     */
    private Element htmlStringToElement( String html ) throws JDOMException, IOException
    {
        SAXBuilder builder = new SAXBuilder( new XMLReaderSAX2Factory( true, CYBERNEKO_PARSER), null, null );
        Document doc = builder.build( new StringReader( html ) );
        Element element = doc.getRootElement();
        return element;
    }

    /**
     *  A static helper method to create HTML from an Element.
     *
     *  @param element The element to get HTML from.
     *  @return HTML
     */
    public static String element2String( Element element )
    {
        Document document = new Document( element );
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString( document );
    }

}
