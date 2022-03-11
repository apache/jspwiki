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

import org.apache.wiki.htmltowiki.WhitespaceTrimWriter;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.htmltowiki.syntax.OptionDecorator;
import org.apache.wiki.htmltowiki.syntax.PlainTextDecorator;
import org.apache.wiki.htmltowiki.syntax.TableDecorator;
import org.apache.wiki.htmltowiki.syntax.TbodyDecorator;
import org.apache.wiki.htmltowiki.syntax.TextElementDecorator;
import org.apache.wiki.htmltowiki.syntax.TheadDecorator;
import org.apache.wiki.htmltowiki.syntax.TrDecorator;
import org.apache.wiki.htmltowiki.syntax.WikiSyntaxDecorator;

import java.io.PrintWriter;
import java.util.Deque;


/**
 * JSPWiki wiki syntax decorator which translates to wiki syntax. Delegates each kind of XHTML element to its specific decorator.
 */
public class JSPWikiSyntaxDecorator extends WikiSyntaxDecorator {

    /** {@inheritDoc} */
    @Override
    public void init( final PrintWriter out,
                      final Deque< String > liStack,
                      final Deque< String > preStack,
                      final WhitespaceTrimWriter outTrimmer,
                      final XHtmlToWikiConfig config,
                      final XHtmlElementToWikiTranslator chain ) {
        this.config = config;
        this.outTrimmer = outTrimmer;
        this.chain = chain;

        this.cssStyle = new JSPWikiPlainTextCssSpecialDecorator( out, chain );
        this.pre = new JSPWikiPlainTextMonospaceDecorator( cssStyle, out, preStack, chain );
        this.em = new JSPWikiPlainTextItalicDecorator( pre, out, chain );
        this.strong = new JSPWikiPlainTextBoldDecorator( em, out, chain );
        this.css = new JSPWikiPlainTextCssDecorator( strong, out, chain );
        this.plainText = new PlainTextDecorator( css, out, chain );

        this.a = new JSPWikiADecorator( out, config, chain );
        this.br = new JSPWikiBrDecorator( out, preStack, chain );
        this.code = new JSPWikiCodeDecorator( out, preStack, chain );
        this.dd = new JSPWikiDdDecorator( out, chain );
        this.dl = new JSPWikiDlDecorator( out, chain );
        this.dt = new JSPWikiDtDecorator( out, chain );
        this.form = new JSPWikiFormDecorator( out, chain );
        this.hr = new JSPWikiHrDecorator( out, chain );
        this.h1 = new JSPWikiH1Decorator( out, chain );
        this.h2 = new JSPWikiH2Decorator( out, chain );
        this.h3 = new JSPWikiH3Decorator( out, chain );
        this.h4 = new JSPWikiH4Decorator( out, chain );
        this.img = new JSPWikiImageDecorator( out, config );
        this.input = new JSPWikiInputDecorator( out, chain );
        this.li = new JSPWikiLiDecorator( out, liStack, chain );
        this.ol = new JSPWikiOlDecorator( out, liStack, chain );
        this.option = new OptionDecorator( out, chain );
        this.p = new JSPWikiPDecorator( out, chain );
        this.table = new TableDecorator( out, outTrimmer, chain );
        this.tbody = new TbodyDecorator( out, chain );
        this.td = new JSPWikiTdDecorator( out, preStack, chain );
        this.th = new JSPWikiThDecorator( out, preStack, chain );
        this.thead = new TheadDecorator( out, chain );
        this.tr = new TrDecorator( out, chain );
        this.textarea = new JSPWikiTextAreaDecorator( out, chain );
        this.textElement = new TextElementDecorator( out, preStack );
        this.select = new JSPWikiSelectDecorator( out, chain );
        this.strike = new JSPWikiStrikeDecorator( out, chain );
        this.sub = new JSPWikiSubDecorator( out, chain );
        this.sup = new JSPWikiSupDecorator( out, chain );
        this.ul = new JSPWikiUlDecorator( out, liStack, chain );
        this.underline = new JSPWikiUnderlineDecorator( out, chain );
    }

}
