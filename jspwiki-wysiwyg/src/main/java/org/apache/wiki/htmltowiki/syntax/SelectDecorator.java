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
 * Translates to wiki syntax from a {@code SELECT} element.
 */
public abstract class SelectDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;

    public SelectDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        final String name = MarkupHelper.nameSansNbf( e );
        out.print( markupSelectOpen() );
        if( name != null ) {
            out.print( markupForSelectAttributeOpen( "name" ) );
            out.print( name );
            out.print( markupForSelectAttributeClose() );
        }

        out.print( markupForSelectAttributeOpen( "value" ) );
        chain.translate( e );
        out.print( markupForSelectAttributeClose() );
        out.print( markupSelectClose() );
    }

    /**
     * Opening wiki markup for a {@code SELECT}'s attribute.
     *
     * @param attr attribute's name.
     * @return Opening wiki markup for a {@code SELECT} element.
     */
    protected abstract String markupForSelectAttributeOpen( String attr );

    /**
     * Closing wiki markup for a {@code SELECT}'s attribute.
     *
     * @return Closing wiki markup for a {@code SELECT} element.
     */
    protected abstract String markupForSelectAttributeClose();

    /**
     * Opening wiki markup for an {@code SELECT} element.
     *
     * @return Opening wiki markup for an {@code SELECT} element.
     */
    protected abstract String markupSelectOpen();

    /**
     * Closing wiki markup for an {@code SELECT} element.
     *
     * @return Closing wiki markup for an {@code SELECT} element.
     */
    protected abstract String markupSelectClose();

}
