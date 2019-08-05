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

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.extensions.jspwikilinks.attributeprovider.JSPWikiLinkAttributeProviderFactory;
import org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor.JSPWikiNodePostProcessorFactory;
import org.apache.wiki.markdown.renderer.JSPWikiNodeRendererFactory;


/**
 * Flexmark entry point to bootstrap JSPWiki extensions.
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
	    rendererBuilder.nodeRendererFactory( new JSPWikiNodeRendererFactory( context ) );
        rendererBuilder.attributeProviderFactory( new JSPWikiLinkAttributeProviderFactory( context ) );
	}

    /**
	 * {@inheritDoc}
	 */
	@Override
	public void extend( final Parser.Builder parserBuilder ) {
	    parserBuilder.postProcessorFactory( new JSPWikiNodePostProcessorFactory( context, parserBuilder ) );
	}

}
