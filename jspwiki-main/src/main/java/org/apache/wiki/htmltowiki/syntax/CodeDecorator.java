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
import java.util.Deque;


/**
 * Translates to wiki syntax from a {@code CODE} element.
 */
public abstract class CodeDecorator {

    final protected PrintWriter out;
    final protected Deque< String > preStack;
    final protected XHtmlElementToWikiTranslator chain;

    protected CodeDecorator( final PrintWriter out, final Deque< String > preStack, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.preStack = preStack;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        out.print( markupCodeOpen() );
        preStack.addFirst( markupCodeOpen() );
        chain.translate( e );
        preStack.removeFirst();
        out.print( markupCodeClose() );
        // NOTE: don't print a newline after the closing brackets because if the Text is inside
        // a table or list, it would break it if there was a subsequent row or list item.
    }

    /**
     * Opening wiki markup for a {@code CODE} element.
     *
     * @return Opening wiki markup for a {@code CODE} element.
     */
    protected abstract String markupCodeOpen();

    /**
     * Closing wiki markup for a {@code CODE} element.
     *
     * @return Closing wiki markup for a {@code CODE} element.
     */
    protected abstract String markupCodeClose();

}
