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
package org.apache.wiki.markdown.renderer;

import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import org.apache.wiki.markdown.nodes.JSPWikiLink;

import java.util.HashSet;
import java.util.Set;


/**
 * Flexmark {@link NodeRenderer} for {@link JSPWikiLink}s.
 */
public class JSPWikiLinkRenderer implements NodeRenderer {

    /**
     * {@inheritDoc}
     *
     * @see com.vladsch.flexmark.html.renderer.NodeRenderer#getNodeRenderingHandlers()
     */
    @Override
    public Set< NodeRenderingHandler< ? > > getNodeRenderingHandlers() {
        final HashSet< NodeRenderingHandler< ? > > set = new HashSet<>();
        set.add( new NodeRenderingHandler<>( JSPWikiLink.class, new CustomNodeRenderer< JSPWikiLink >() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void render( final JSPWikiLink node, final NodeRendererContext context, final HtmlWriter html ) {
                if( context.isDoNotRenderLinks() ) {
                    context.renderChildren( node );
                } else {
                    // standard Link Rendering
                    final ResolvedLink resolvedLink = context.resolveLink( LinkType.LINK, node.getUrl().unescape(), null );

                    html.attr( "href", resolvedLink.getUrl() );
                    if( node.getTitle().isNotNull() ) {
                        html.attr( "title", node.getTitle().unescape() );
                    }
                    html.srcPos( node.getChars() ).withAttr( resolvedLink ).tag( "a" );
                    context.renderChildren( node );
                    html.tag( "/a" );
                }
            }
        } ) );
        return set;
    }

}
