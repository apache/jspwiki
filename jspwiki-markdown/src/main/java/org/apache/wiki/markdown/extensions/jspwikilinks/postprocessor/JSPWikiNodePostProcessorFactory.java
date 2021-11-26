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
import org.apache.oro.text.regex.Pattern;
import org.apache.wiki.api.core.Context;

import java.util.List;


/**
 * Simple {@link NodePostProcessorFactory} to instantiate {@link JSPWikiLinkNodePostProcessor}s.
 */
public class JSPWikiNodePostProcessorFactory extends NodePostProcessorFactory {

    private final Context m_context;
    private final boolean isImageInlining;
    private final List< Pattern > inlineImagePatterns;

    public JSPWikiNodePostProcessorFactory( final Context m_context,
                                            final DataHolder options,
                                            final boolean isImageInlining,
                                            final List< Pattern > inlineImagePatterns ) {
        super( true );
        addNodes( Link.class ); // needs to be called before create( Document )
        this.m_context = m_context;
        this.isImageInlining = isImageInlining;
        this.inlineImagePatterns = inlineImagePatterns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodePostProcessor apply( final Document document ) {
        return new JSPWikiLinkNodePostProcessor( m_context, document, isImageInlining, inlineImagePatterns );
    }

}
