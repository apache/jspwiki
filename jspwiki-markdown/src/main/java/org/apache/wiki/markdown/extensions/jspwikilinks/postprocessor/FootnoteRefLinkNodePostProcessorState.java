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

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodePostProcessorState} which further post processes footnote reference links.
 */
public class FootnoteRefLinkNodePostProcessorState implements NodePostProcessorState< Link > {

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Link)
     */
    @Override
    public void process( NodeTracker state, Link link ) {
        final Text opBracket = new Text( CharSubSequence.of( "[" ) );
        final Text clBracket = new Text( CharSubSequence.of( "]" ) );
        link.prependChild( opBracket );
        link.appendChild( clBracket );
        state.nodeAdded( opBracket );
        state.nodeAdded( clBracket );
    }

}
