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

import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import org.apache.oro.text.regex.Pattern;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.markdown.MarkdownForJSPWikiExtension;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.WikiDocument;

import java.util.Arrays;
import java.util.List;


/**
 * Simple placeholder for Markdown Nodes
 */
public class MarkdownDocument extends WikiDocument {

	private static final long serialVersionUID = 1L;

	private final Node md;

	public MarkdownDocument( final Page page, final Node md ) {
		super( page );
		this.md = md;
	}

	public Node getMarkdownNode() {
		return md;
	}

	/**
	 * configuration options for MarkdownRenderers.
	 *
	 * @param context current wikicontext
	 * @return configuration options for MarkdownRenderers.
	 */
	public static MutableDataSet options( final Context context, final boolean isImageInlining, final List< Pattern > inlineImagePatterns ) {
		final MutableDataSet options = new MutableDataSet();
		options.setFrom( ParserEmulationProfile.COMMONMARK );
		// align style of Markdown's footnotes extension with jspwiki footnotes refs
		options.set( FootnoteExtension.FOOTNOTE_LINK_REF_CLASS, JSPWikiMarkupParser.CLASS_FOOTNOTE_REF );
		options.set( Parser.EXTENSIONS, Arrays.asList( new Extension[] { new MarkdownForJSPWikiExtension( context, isImageInlining, inlineImagePatterns ),
		                                                                 FootnoteExtension.create(),
		                                                                 TocExtension.create() } ) );
		return options;
	}

}
