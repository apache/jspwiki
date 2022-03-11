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
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Translates to wiki syntax from a plain text handling bold.
 */
public abstract class PlainTextBoldDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;
    final protected PlainTextItalicDecorator ptid;

    protected PlainTextBoldDecorator( final PlainTextItalicDecorator ptid, final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
        this.ptid = ptid;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param dto XHTML element being translated.
     */
    public void decorate( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) throws JDOMException {
        if( dto.bold ) {
            out.print( markupBoldOpen() );
        }
        ptid.decorate( dto );
        if( dto.bold ) {
            out.print( markupBoldClose() );
        }
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        out.print( markupBoldOpen() );
        chain.translate( e );
        out.print( markupBoldClose() );
    }

    /**
     * Opening wiki markup for a bold element.
     *
     * @return Opening wiki markup for a bold element.
     */
    protected abstract String markupBoldOpen();

    /**
     * Closing wiki markup for a bold element.
     *
     * @return Closing wiki markup for a bold element.
     */
    protected abstract String markupBoldClose();

}
