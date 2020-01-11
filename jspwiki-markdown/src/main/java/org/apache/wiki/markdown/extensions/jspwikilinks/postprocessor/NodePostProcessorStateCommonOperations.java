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
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.MarkupParser;


/**
 * Internal class with common post-processor operations.
 */
class NodePostProcessorStateCommonOperations {

    static void addContent( final NodeTracker state, final Node node, final Node content ) {
        final Node previous = node.getPrevious() != null ? node.getPrevious() : node.getNext();
        if( previous != null ) {
            previous.insertAfter( content );
            node.unlink();
            state.nodeRemoved( node );
            content.takeChildren( node );
            state.nodeAddedWithChildren( content );
        } else {
            node.getParent().appendChild( content );
        }
    }

    static void addOutlinkImage( final NodeTracker state, final Node node, final WikiContext wikiContext, final boolean useOutlinkImage ) {
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        final boolean wysiwygEditorMode = wysiwygVariable != null && wysiwygVariable;

        if( useOutlinkImage && !wysiwygEditorMode ) {
            final String m_outlinkImageURL = wikiContext.getURL( WikiContext.NONE, MarkupParser.OUTLINK_IMAGE );
            final HtmlInline img = new HtmlInline( CharSubSequence.of( "<img class=\""+ MarkupParser.OUTLINK + "\" " +
                                                                              "alt=\"\" src=\""+ m_outlinkImageURL + "\" />" ) );
            node.insertAfter( img );
            state.nodeAdded( img );
        }
    }

    static String inlineLinkTextOnWysiwyg( final NodeTracker state, final JSPWikiLink link, final boolean wysiwygEditorMode ) {
        final String line = link.getUrl().toString();
        if( wysiwygEditorMode ) {
            final HtmlInline content = new HtmlInline( CharSubSequence.of( "[" + line + "]()" ) );
            addContent( state, link, content );
        }
        return line;
    }

    static void makeError( final NodeTracker state, final Node node, final String errMsg ) {
        final HtmlInline error = new HtmlInline( CharSubSequence.of( "<span class=\"error\">" +
                                                                     errMsg +
                                                                     "</span>" ) );
        addContent( state, node, error );
    }

}
