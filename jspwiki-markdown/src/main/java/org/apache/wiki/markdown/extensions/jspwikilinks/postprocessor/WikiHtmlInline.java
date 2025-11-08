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
package org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor;

import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;


/**
 * <p>Regular {@link HtmlInline} get escaped depending on the value of the {@code MarkupParser.PROP_ALLOWHTML} property.</p>
 * <p>However, wikilink post processors inject additional HtmlInline that must not be escaped. Subclassing {@link HtmlInline}
 * allows us to register a custom {@code NodeRenderingHandler} at {@code JSPWikiLinkRenderer} to bypass this limitation.</p>
 */
public class WikiHtmlInline extends HtmlInline {

    private WikiHtmlInline( final BasedSequence chars ) {
        super( chars );
    }

    public static WikiHtmlInline of( final String str ) {
        return new WikiHtmlInline( BasedSequence.of( str ) );
    }

    public static WikiHtmlInline of( final String str, final Context context ) {
        final boolean allowHtml = context.getBooleanWikiProperty( MarkupParser.PROP_ALLOWHTML, false );
        if( allowHtml ) {
            return WikiHtmlInline.of( str );
        } else {
            return new WikiHtmlInline( BasedSequence.of( TextUtil.escapeHTMLEntities( str ) ) );
        }
    }

}
