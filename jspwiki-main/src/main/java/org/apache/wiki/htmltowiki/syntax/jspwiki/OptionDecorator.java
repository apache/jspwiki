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
package org.apache.wiki.htmltowiki.syntax.jspwiki;

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Translates to JSPWiki syntax from a OPTION element.
 */
class OptionDecorator {

    final PrintWriter out;
    final XHtmlElementToWikiTranslator chain;

    OptionDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param base the parent of the XHTML element being translated.
     * @param e XHTML element being translated.
     */
    void decorate( final Element base, final Element e ) throws JDOMException {
        // If this <option> element isn't the second child element within the parent <select>
        // element, then we need to print a semicolon as a separator. (The first child element
        // is expected to be a newline character which is at index of 0).
        if( base.indexOf( e ) != 1 ) {
            out.print( ";" );
        }

        final Attribute selected = e.getAttribute( "selected" );
        if( selected != null ) {
            out.print( "*" );
        }

        final String value = e.getAttributeValue( "value" );
        if( value != null ) {
            out.print( value );
        } else {
            chain.translate( e );
        }
    }

}
