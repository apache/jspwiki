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
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Translates to wiki syntax from a plain text handling css classes.
 */
public abstract class PlainTextCssDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;
    final protected PlainTextBoldDecorator ptbd;

    protected PlainTextCssDecorator( final PlainTextBoldDecorator ptbd, final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
        this.ptbd = ptbd;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param dto XHTML element being translated.
     */
    public void decorate( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) throws JDOMException {
        if( dto.cssClass != null && !dto.ignoredCssClass ) {
            if( MarkupHelper.isHtmlBaseDiv( dto ) ) {
                out.print( markupCssDivOpen( dto.cssClass ) );
            } else if( MarkupHelper.isHtmlBaseSpan( dto ) ) {
                out.print( markupCssSpanOpen( dto.cssClass ) );
            }
        }
        ptbd.decorate( dto );
        if( dto.cssClass != null && !dto.ignoredCssClass ) {
            if( MarkupHelper.isHtmlBaseDiv( dto ) ) {
                out.print( markupCssDivClose( dto.cssClass ) );
            } else if( MarkupHelper.isHtmlBaseSpan( dto ) ) {
                out.print( markupCssSpanClose( dto.cssClass ) );
            }
        }
    }

    /**
     * Opening wiki markup for a css element.
     *
     * @return Opening wiki markup for a css element.
     */
    protected abstract String markupCssDivOpen( String cssClass );

    /**
     * Closing wiki markup for a css element.
     *
     * @return Closing wiki markup for a css element.
     */
    protected abstract String markupCssDivClose( String cssClass );

    /**
     * Opening wiki markup for a css element.
     *
     * @return Opening wiki markup for a css element.
     */
    protected abstract String markupCssSpanOpen( String cssClass );

    /**
     * Closing wiki markup for a css element.
     *
     * @return Closing wiki markup for a css element.
     */
    protected abstract String markupCssSpanClose( String cssClass );

}
