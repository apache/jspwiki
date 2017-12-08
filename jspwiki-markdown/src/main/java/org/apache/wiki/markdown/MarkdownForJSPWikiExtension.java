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
package org.apache.wiki.markdown;

import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.extensions.jspwikilinks.attributeprovider.MarkdownForJSPWikiAttributeProvider;
import org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor.JSPWikiLinkNodePostProcessor;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;


/**
 * Flexmark entry point for JSPWiki extensions
 */
public class MarkdownForJSPWikiExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

	private final WikiContext context;

	public MarkdownForJSPWikiExtension( final WikiContext context ) {
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rendererOptions( final MutableDataHolder options ) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void parserOptions( final MutableDataHolder options ) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void extend( final HtmlRenderer.Builder rendererBuilder, final String rendererType ) {
        rendererBuilder.attributeProviderFactory( jspWikiAttributeProviderFactory( context ) );
	}

    /**
	 * {@inheritDoc}
	 */
	@Override
	public void extend( final Parser.Builder parserBuilder ) {
	    parserBuilder.postProcessorFactory( new JSPWikiNodePostProcessorFactory( context, parserBuilder ) );
	}

	AttributeProviderFactory jspWikiAttributeProviderFactory( final WikiContext wContext ) {
		return new IndependentAttributeProviderFactory() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public AttributeProvider create( final NodeRendererContext context ) {
				return new MarkdownForJSPWikiAttributeProvider( wContext );
			}
		};
	}

    static class JSPWikiNodePostProcessorFactory extends NodePostProcessorFactory {

        private final WikiContext m_context;

        public JSPWikiNodePostProcessorFactory( final WikiContext m_context, final DataHolder options ) {
            super( true );
            addNodes( Link.class ); // needs to be called before create( Document )
            this.m_context = m_context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodePostProcessor create( final Document document ) {
            return new JSPWikiLinkNodePostProcessor( m_context, document );
        }
    }

}
