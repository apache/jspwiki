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
 * Translates to wiki syntax from a {@code LI} element.
 */
public abstract class LiDecorator {

    final protected PrintWriter out;
    final protected Deque< String > liStack;
    final protected XHtmlElementToWikiTranslator chain;

    protected LiDecorator( final PrintWriter out, final Deque< String > liStack, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.liStack = liStack;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param base the parent of the XHTML element being translated.
     * @param e XHTML element being translated.
     */
    public void decorate( final Element base, final Element e ) throws JDOMException {
        out.print( markupLi( liStack ) );
        chain.translate( e );

        // The following line assumes that the XHTML has been "pretty-printed"
        // (newlines separate child elements from their parents).
        final boolean lastListItem = base.indexOf( e ) == ( base.getContentSize() - 2 );
        final boolean sublistItem = liStack.size() > 1;

        // only print a newline if this <li> element is not the last item within a sublist.
        if ( !sublistItem || !lastListItem ) {
            out.println();
        }
    }

    /**
     * Wiki markup for a {@code LI} element.
     *
     * @return Wiki markup for a {@code LI} element.
     */
    protected abstract String markupLi( Deque< String > liStack );

}
