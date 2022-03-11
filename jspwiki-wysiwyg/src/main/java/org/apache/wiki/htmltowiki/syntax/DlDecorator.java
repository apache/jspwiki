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
 * Translates to wiki syntax from a {@code DL} element.
 */
public abstract class DlDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;

    protected DlDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        out.print( markupDlOpen() );
        chain.translate( e );

        // print a newline after the definition list. If we don't,
        // it may cause problems for the subsequent element.
        out.print( markupDlClose() );
    }

    /**
     * Opening wiki markup for a {@code DL} element.
     *
     * @return Opening wiki markup for a {@code DL} element.
     */
    protected abstract String markupDlOpen();

    /**
     * Closing wiki markup for a {@code DL} element.
     *
     * @return Closing wiki markup for a {@code DL} element.
     */
    protected abstract String markupDlClose();

}
