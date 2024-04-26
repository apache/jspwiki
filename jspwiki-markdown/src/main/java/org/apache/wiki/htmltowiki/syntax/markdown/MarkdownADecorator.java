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
package org.apache.wiki.htmltowiki.syntax.markdown;

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.htmltowiki.syntax.ADecorator;
import org.apache.wiki.htmltowiki.syntax.MarkupHelper;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Markdown syntax implementation of {@link ADecorator}.
 */
class MarkdownADecorator extends ADecorator {

    MarkdownADecorator( final PrintWriter out, final XHtmlToWikiConfig config, final XHtmlElementToWikiTranslator chain ) {
        super( out, config, chain );
    }

    /** {@inheritDoc} */
    @Override
    protected void linkMarkup( final Element e ) throws JDOMException {
        out.print( "[" );
        chain.translate( e );
        out.print( "]()" );
    }

    /** {@inheritDoc} */
    @Override
    protected void linkMarkup( final Element e, final String ref ) throws JDOMException {
        out.print( "[" );
        chain.translate( e );
        out.print( "](" );
        MarkupHelper.printUnescaped( out, ref );
        out.print( ")" );
    }

    /** {@inheritDoc} */
    @Override
    protected void linkMarkup( final Element e, final String ref, final String additionalAttrs ) throws JDOMException {
        // If the ref has the same value as the text and also if there
        // are attributes, then just print: [ref](ref){attributes}.
        out.print( "[" );
        chain.translate( e );
        out.print( "](" );
        MarkupHelper.printUnescaped( out, ref );
        out.print( "){" );
        out.print( additionalAttrs );
        out.print( "}" );
    }

    /** {@inheritDoc} */
    @Override
    protected void linkMarkup( final String text, final String ref ) {
        if( ref.equals( text ) ) {
            out.print( "[" + text + "]()" );
        } else {
            out.print( "[" + text + "](" + ref + ")" );
        }
    }

}
