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
package org.apache.wiki.render.markdown;

import java.io.IOException;

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.parser.markdown.MarkdownDocument;
import org.apache.wiki.render.WikiRenderer;

import com.vladsch.flexmark.html.HtmlRenderer;


/**
 * Class handling the markdown rendering.
 */
public class MarkdownRenderer extends WikiRenderer {

	private final HtmlRenderer renderer;

	public MarkdownRenderer( final WikiContext context, final WikiDocument doc ) {
		super( context, doc );
		renderer = HtmlRenderer.builder( MarkdownDocument.options( context ) ).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getString() throws IOException {
		m_document.setContext( m_context );
		if( m_document instanceof MarkdownDocument ) {
			return renderer.render( ( ( MarkdownDocument )m_document ).getMarkdownNode() );
		} else {
			throw new IOException( "MarkdownRenderer requires to be used with MarkdownParser" );
		}
	}

}
