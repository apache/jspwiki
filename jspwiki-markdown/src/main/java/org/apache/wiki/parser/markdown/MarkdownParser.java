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
package org.apache.wiki.parser.markdown;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;

import java.io.IOException;
import java.io.Reader;


/**
 * Class handling the markdown parsing.
 */
public class MarkdownParser extends MarkupParser {

	private final Parser parser;

	public MarkdownParser( final WikiContext context, final Reader in ) {
		super( context, in );
		if( context.getEngine().getUserManager().getUserDatabase() == null || context.getEngine().getAuthorizationManager() == null ) {
            disableAccessRules();
        }
		parser = Parser.builder( MarkdownDocument.options( context ) ).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WikiDocument parse() throws IOException {
		final Node document = parser.parseReader( m_in );
		final MarkdownDocument md = new MarkdownDocument( m_context.getPage(), document );
        md.setContext( m_context );

		return md;
	}

}
