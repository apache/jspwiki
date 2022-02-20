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
 * Translates to wiki syntax from a {@code BR} element.
 */
public abstract class BrDecorator {

    final protected PrintWriter out;
    final protected Deque< String > preStack;
    final protected XHtmlElementToWikiTranslator chain;

    protected BrDecorator( final PrintWriter out, final Deque< String > preStack, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.preStack = preStack;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param base parent of the XHTML element being translated.
     * @param e element being translated.
     */
    public void decorate( final Element base, final Element e ) throws JDOMException {
        if ( !preStack.isEmpty() ) {
            out.println();
        } else {
            final String parentElementName = base.getName().toLowerCase();

            // To beautify the generated wiki markup, we print a newline character after a linebreak.
            // It's only safe to do this when the parent element is a <p> or <div>; when the parent
            // element is a table cell or list item, a newline character would break the markup.
            // We also check that this isn't being done inside a plugin body.
            if ( parentElementName.matches( "p|div" ) && !base.getText().matches( "(?s).*\\[\\{.*}].*" ) ) {
                out.print( " " + markupBr() + "\n" );
            } else {
                out.print( " " + markupBr() );
            }
        }
        chain.translate( e );
    }

    /**
     * Wiki markup for a {@code BR} element.
     *
     * @return Wiki markup for a {@code BR} element.
     */
    protected abstract String markupBr();

}
