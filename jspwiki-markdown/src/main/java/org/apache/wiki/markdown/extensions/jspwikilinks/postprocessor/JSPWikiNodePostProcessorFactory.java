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
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;
import org.apache.wiki.WikiContext;


/**
 * Simple {@link NodePostProcessorFactory} to instantiate {@link JSPWikiLinkNodePostProcessor}s.
 */
public class JSPWikiNodePostProcessorFactory extends NodePostProcessorFactory {

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
    public NodePostProcessor apply( final Document document ) {
        return new JSPWikiLinkNodePostProcessor( m_context, document );
    }

}
