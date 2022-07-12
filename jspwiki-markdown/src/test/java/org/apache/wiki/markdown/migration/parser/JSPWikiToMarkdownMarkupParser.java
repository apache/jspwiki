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
package org.apache.wiki.markdown.migration.parser;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.parser.WikiDocument;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import java.io.IOException;
import java.io.Reader;
import java.util.List;


public class JSPWikiToMarkdownMarkupParser extends JSPWikiMarkupParser {

    /**
     * Creates a markup parser.
     *
     * @param context The WikiContext which controls the parsing
     * @param in      Where the data is read from.
     */
    public JSPWikiToMarkdownMarkupParser( final Context context, final Reader in ) {
        super( context, in );
    }

    /** {@inheritDoc} */
    @Override
    public WikiDocument parse() throws IOException {
        final WikiDocument doc = super.parse();
        translatePluginACLAndVariableTextLinksToMarkdown( doc.getRootElement(), 0 );
        return doc;
    }

    void translatePluginACLAndVariableTextLinksToMarkdown( final Content element, final int childNumber ) {
        if( element instanceof PluginContent ) {
            final PluginContent plugin = ( PluginContent ) element;
            final String str = plugin.getText();
            if( str.startsWith( "[{" ) && str.endsWith( "}]" ) ) {
                final Element parent = plugin.getParent();
                plugin.detach();
                if( parent != null ) {
                    parent.addContent( childNumber, new Text( str + "()" ) );
                }
            }
        } else if( element instanceof Text ) {
            final Text text = ( Text )element;
            if( text.getText().startsWith( "[{" ) && text.getText().endsWith( "}]" ) ) {
                text.append( "()" );
            }
        } else if( element instanceof Element ) {
            final Element base = ( Element )element;
            base.getContent();
            final List< Content > content = base.getContent();
            for( int i = 0; i < content.size(); i++ ) {
                final Content c = content.get( i );
                translatePluginACLAndVariableTextLinksToMarkdown( c, i );
            }
        }
    }

}
