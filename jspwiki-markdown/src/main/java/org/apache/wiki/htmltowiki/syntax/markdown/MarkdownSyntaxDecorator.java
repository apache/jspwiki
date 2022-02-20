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

import org.apache.wiki.htmltowiki.WhitespaceTrimWriter;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.htmltowiki.syntax.OptionDecorator;
import org.apache.wiki.htmltowiki.syntax.PlainTextDecorator;
import org.apache.wiki.htmltowiki.syntax.TableDecorator;
import org.apache.wiki.htmltowiki.syntax.TbodyDecorator;
import org.apache.wiki.htmltowiki.syntax.TextElementDecorator;
import org.apache.wiki.htmltowiki.syntax.TrDecorator;
import org.apache.wiki.htmltowiki.syntax.WikiSyntaxDecorator;

import java.io.PrintWriter;
import java.util.Deque;


/**
 * Markdown wiki syntax decorator which translates to wiki syntax. Delegates each kind of XHTML element to its specific decorator.
 */
public class MarkdownSyntaxDecorator extends WikiSyntaxDecorator {

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

        this.cssStyle = new MarkdownPlainTextCssSpecialDecorator( out, chain );
        this.pre = new MarkdownPlainTextMonospaceDecorator( cssStyle, out, preStack, chain );
        this.em = new MarkdownPlainTextItalicDecorator( pre, out, chain );
        this.strong = new MarkdownPlainTextBoldDecorator( em, out, chain );
        this.css = new MarkdownPlainTextCssDecorator( strong, out, chain );
        this.plainText = new PlainTextDecorator( css, out, chain );

        this.a = new MarkdownADecorator( out, config, chain );
        this.br = new MarkdownBrDecorator( out, preStack, chain );
        this.code = new MarkdownCodeDecorator( out, preStack, chain );
        this.dd = new MarkdownDdDecorator( out, chain );
        this.dl = new MarkdownDlDecorator( out, chain );
        this.dt = new MarkdownDtDecorator( out, chain );
        this.form = new MarkdownFormDecorator( out, chain );
        this.hr = new MarkdownHrDecorator( out, chain );
        this.h1 = new MarkdownH1Decorator( out, chain );
        this.h2 = new MarkdownH2Decorator( out, chain );
        this.h3 = new MarkdownH3Decorator( out, chain );
        this.h4 = new MarkdownH4Decorator( out, chain );
        this.img = new MarkdownImageDecorator( out, config );
        this.input = new MarkdownInputDecorator( out, chain );
        this.li = new MarkdownLiDecorator( out, liStack, chain );
        this.ol = new MarkdownOlDecorator( out, liStack, chain );
        this.option = new OptionDecorator( out, chain );
        this.p = new MarkdownPDecorator( out, chain );
        this.table = new TableDecorator( out, outTrimmer, chain );
        this.tbody = new TbodyDecorator( out, chain );
        this.td = new MarkdownTdDecorator( out, preStack, chain );
        this.th = new MarkdownThDecorator( out, preStack, chain );
        this.thead = new MarkdownTheadDecorator( out, chain );
        this.tr = new TrDecorator( out, chain );
        this.textarea = new MarkdownTextAreaDecorator( out, chain );
        this.textElement = new TextElementDecorator( out, preStack );
        this.select = new MarkdownSelectDecorator( out, chain );
        this.strike = new MarkdownStrikeDecorator( out, chain );
        this.sub = new MarkdownSubDecorator( out, chain );
        this.sup = new MarkdownSupDecorator( out, chain );
        this.ul = new MarkdownUlDecorator( out, liStack, chain );
        this.underline = new MarkdownUnderlineDecorator( out, chain );
    }

}
