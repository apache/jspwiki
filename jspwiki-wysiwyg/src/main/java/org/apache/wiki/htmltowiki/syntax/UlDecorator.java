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
 * Translates to wiki syntax from a {@code UL} element.
 */
public abstract class UlDecorator {

    final protected PrintWriter out;
    final protected Deque< String > liStack;
    final protected XHtmlElementToWikiTranslator chain;

    protected UlDecorator( final PrintWriter out, final Deque< String > liStack, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.liStack = liStack;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        out.println();
        liStack.addFirst( markupUl() );
        chain.translate( e );
        liStack.removeFirst();
    }

    /**
     * Wiki markup for a {@code UL} element.
     *
     * @return Wiki markup for a {@code UL} element.
     */
    protected abstract String markupUl();

}
