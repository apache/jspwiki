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

import com.vladsch.flexmark.html.renderer.DelegatingNodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.util.data.DataHolder;
import org.apache.wiki.WikiContext;

import java.util.Set;


/**
 * Simple {@link NodeRendererFactory} to instantiate {@link JSPWikiLinkRenderer}s.
 */
public class JSPWikiNodeRendererFactory implements DelegatingNodeRendererFactory {

    final WikiContext wikiContext;

    public JSPWikiNodeRendererFactory( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeRenderer apply( final DataHolder options ) {
        return new JSPWikiLinkRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set< Class< ? extends NodeRendererFactory > > getDelegates() {
        // return null if renderer does not delegate or delegates only to core node renderer
        return null;
    }

}
