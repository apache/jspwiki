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
 * Translates to wiki syntax from a plain text handling special css.
 */
public abstract class PlainTextCssSpecialDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;

    protected PlainTextCssSpecialDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param dto XHTML element being translated.
     */
    public void decorate( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) throws JDOMException {
        if( dto.cssSpecial != null ) {
            if( MarkupHelper.isHtmlBaseDiv( dto ) ) {
                out.print( markupCssSpecialDivOpen( dto.cssSpecial ) );
            } else {
                out.print( markupCssSpecialOpen( dto.cssSpecial ) );
            }
        }
        chain.translateChildren( dto.base );
        if( dto.cssSpecial != null ) {
            if( MarkupHelper.isHtmlBaseDiv( dto ) ) {
                out.print( markupCssSpecialDivClose( dto.cssSpecial ) );
            } else {
                out.print( markupCssSpecialClose( dto.cssSpecial ) );
            }
        }
    }

    /**
     * Opening wiki markup for a css style element.
     *
     * @param cssStyle css styles to apply
     * @return Opening wiki markup for a css style element.
     */
    protected abstract String markupCssSpecialDivOpen( String cssStyle );

    /**
     * Closing wiki markup for a css style element.
     *
     * @param cssStyle css styles to apply
     * @return Closing wiki markup for a css style element.
     */
    protected abstract String markupCssSpecialDivClose( String cssStyle );

    /**
     * Opening wiki markup for a css style element.
     *
     * @param cssStyle css styles to apply
     * @return Opening wiki markup for a css style element.
     */
    protected abstract String markupCssSpecialOpen( String cssStyle );

    /**
     * Closing wiki markup for a css style element.
     *
     * @param cssStyle css styles to apply
     * @return Closing wiki markup for a css style element.
     */
    protected abstract String markupCssSpecialClose( String cssStyle );

}
