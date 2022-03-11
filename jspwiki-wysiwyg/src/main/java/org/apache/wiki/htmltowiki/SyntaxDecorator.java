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
package org.apache.wiki.htmltowiki;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;

import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;


/**
 * Decorates Xhtml elements with wiki syntax
 */
public interface SyntaxDecorator {

    /**
     * Prepares the syntax decorator.
     * 
     * @param out writer that will hold the resulting wiki markup.
     * @param liStack stack containing the amount of nested {@code li}s.
     * @param preStack stack containing the amount of nested {@code pre}s.
     * @param outTrimmer writer capable of trimming whitespaces and of checking if it's currently writing a line start.
     * @param config xhtml to wiki configuration object.
     * @param chain chain (in the chain of responsabilities pattern) that is expected to be called by the different xhtml decorations.
     */
    void init( PrintWriter out, Deque< String > liStack, Deque< String > preStack, WhitespaceTrimWriter outTrimmer, XHtmlToWikiConfig config, XHtmlElementToWikiTranslator chain );

    /**
     * Decorates an {@code a} element.
     * 
     * @param e XHTML element being translated.
     * @param ref actual link.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void a( Element e, final String ref ) throws JDOMException;

    /**
     * Decorates an {@code a} element, pointing to a footnote.
     *
     * @param text text link of the footnote.
     * @param ref link to footnote.
     */
    void aFootnote( final String text, final String ref );

    /**
     * Decorates an {@code a} element to an undefined page.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void aUndefined( Element e ) throws JDOMException;

    /**
     * Decorates a {@code br} element.
     *
     * @param base parent of the XHTML element being translated.
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void br( Element base, Element e ) throws JDOMException;

    /**
     * Decorates a {@code code} ot {@code tt} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void code( Element e ) throws JDOMException;

    /**
     * Decorates a {@code dd} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void dd( Element e ) throws JDOMException;

    /**
     * Decorates a {@code dl} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void dl( Element e ) throws JDOMException;

    /**
     * Decorates a {@code dt} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void dt( Element e ) throws JDOMException;

    /**
     * Decorates an {@code em}, {@code i} or {@code address} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void em( Element e ) throws JDOMException;

    /**
     * Decorates a {@code form} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void form( Element e ) throws JDOMException;

    /**
     * Decorates an {@code hr} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void hr( Element e ) throws JDOMException;

    /**
     * Decorates an {@code h1} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void h1( Element e ) throws JDOMException;

    /**
     * Decorates an {@code h2} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void h2( Element e ) throws JDOMException;

    /**
     * Decorates an {@code h3} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void h3( Element e ) throws JDOMException;

    /**
     * Decorates an {@code h4} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void h4( Element e ) throws JDOMException;

    /**
     * Decorates an image element. This is a terminal operation, that is, chain is not expected to be called by this method.
     *
     * @param src image source
     * @param imageAttrs image attributes
     */
    void image( String src, Map< String, Object > imageAttrs );

    /**
     * Decorates an image element. This is a terminal operation, that is, chain is not expected to be called by this method.
     *
     * @param e XHTML element being translated.
     */
    void img( Element e );

    /**
     * Decorates an {@code input} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void input( Element e ) throws JDOMException;

    /**
     * Decorates a {@code li} element.
     *
     * @param base parent of the XHTML element being translated.
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void li( Element base, Element e ) throws JDOMException;

    /**
     * Decorates an {@code ol} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void ol( Element e ) throws JDOMException;

    /**
     * Decorates an {@code option} element.
     *
     * @param base parent of the XHTML element being translated.
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void option( Element base, Element e ) throws JDOMException;

    /**
     * Decorates a {@code p} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void p( Element e ) throws JDOMException;

    /**
     * Decorates a text paragraph.
     *
     * @param dto XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void paragraph( XHtmlElementToWikiTranslator.ElementDecoratorData dto ) throws JDOMException;

    /**
     * Decorates a {@code pre} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void pre( Element e ) throws JDOMException;

    /**
     * Decorates a {@code strong} or {@code b} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void strong( Element e ) throws JDOMException;

    /**
     * Decorates a {@code table} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void table( Element e ) throws JDOMException;

    /**
     * Decorates a {@code tbody} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void tbody( Element e ) throws JDOMException;

    /**
     * Decorates an {@code td} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void td( Element e ) throws JDOMException;

    /**
     * Decorates a text element. This is a terminal operation, that is, chain is not expected to be called by this method.
     *
     * @param e XHTML element being translated.
     */
    void text( Text e );

    /**
     * Decorates a {@code textarea} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void textarea( Element e ) throws JDOMException;

    /**
     * Decorates a {@code th} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void th( Element e ) throws JDOMException;

    /**
     * Decorates a {@code thead} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void thead( Element e ) throws JDOMException;

    /**
     * Decorates a {@code tr} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void tr( Element e ) throws JDOMException;

    /**
     * Decorates a {@code select} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void select( Element e ) throws JDOMException;

    /**
     * Decorates a {@code strike} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void strike( Element e ) throws JDOMException;

    /**
     * Decorates a {@code sub} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void sub( Element e ) throws JDOMException;

    /**
     * Decorates an {@code sup} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void sup( Element e ) throws JDOMException;

    /**
     * Decorates an {@code ul} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void ul( Element e ) throws JDOMException;

    /**
     * Decorates an {@code underline} element.
     *
     * @param e XHTML element being translated.
     * @throws JDOMException if an error has ocurred parsing the xhtml chain.
     */
    void underline( Element e ) throws JDOMException;

}
