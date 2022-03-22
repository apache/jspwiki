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
package org.apache.wiki.markdown;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.engine.EngineLifecycleExtension;

import java.util.Properties;


/**
 * {@link EngineLifecycleExtension} that sets up all the relevant properties to enable markdown syntax if the
 * {@code jspwiki.syntax} property has been given, with the {@code markdown} value.
 */
public class MarkdownSetupEngineLifecycleExtension implements EngineLifecycleExtension {

    private static final Logger LOG = LogManager.getLogger( MarkdownSetupEngineLifecycleExtension.class );

    /** {@inheritDoc} */
    @Override
    public void onInit( final Properties properties ) {
        if( "markdown".equalsIgnoreCase( properties.getProperty( "jspwiki.syntax" ) ) ) {
            setWikiProperty( properties, "jspwiki.renderingManager.markupParser", "org.apache.wiki.parser.markdown.MarkdownParser" );
            setWikiProperty( properties, "jspwiki.renderingManager.renderer", "org.apache.wiki.render.markdown.MarkdownRenderer" );
            setWikiProperty( properties, "jspwiki.renderingManager.renderer.wysiwyg", "org.apache.wiki.render.markdown.MarkdownRenderer" );
            setWikiProperty( properties, "jspwiki.syntax.decorator", "org.apache.wiki.htmltowiki.syntax.markdown.MarkdownSyntaxDecorator" );
            setWikiProperty( properties, "jspwiki.syntax.plain", "plain/wiki-snips-markdown.js" );
        }
    }

    void setWikiProperty( final Properties properties, final String key, final String value ) {
        properties.setProperty( key, value );
        LOG.info( "{} set to {}", key, value );
    }

}
