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
package org.apache.wiki.markdown.migration;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.htmltowiki.HtmlStringToWikiTranslator;
import org.apache.wiki.markdown.migration.parser.JSPWikiToMarkdownMarkupParser;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import static org.apache.wiki.TestEngine.with;
import org.junit.jupiter.api.Assertions;


public class WikiSyntaxConverter {

    /**
     * unit test setup only
     * @throws Exception 
     */
    @Test
    void jspwikiToMarkdownConverter() throws Exception {
        List<String> errors = convert("../jspwiki-wikipages/en/src/main/resources", "jspwiki",
                "./target/pages-markdown", "markdown");
        Assertions.assertEquals(true, errors.isEmpty(), StringUtils.join(errors));
    }
    
    public List<String> convert(String sourceDirectory, String sourceFormat, String destinationDirectory, String destinationFormat) throws Exception {
        final Engine jspw = buildEngine( sourceFormat, sourceDirectory );
        final Engine md = buildEngine( destinationFormat, destinationDirectory );
        jspw.getManager( PluginManager.class ).enablePlugins( false );
        List<String> errors = new ArrayList<>();

        final Collection< Page > pages = jspw.getManager( PageManager.class ).getAllPages();
        for( final Page p : pages ) {
            try{
                final Context context = Wiki.context().create( jspw, p );
                context.setRequestContext( ContextEnum.PAGE_NONE.getRequestContext() );
                context.setVariable( Context.VAR_WYSIWYG_EDITOR_MODE, Boolean.TRUE );
                final String pagedata = jspw.getManager( PageManager.class ).getPureText( p.getName(), p.getVersion() );
                final String html = jspw.getManager( RenderingManager.class ).textToHTML( context, pagedata, null, null, null, false, false );
                final String syntax = new HtmlStringToWikiTranslator( md ).translate( html );
                final Context contextMD = Wiki.context().create( md, p );
                md.getManager( PageManager.class ).saveText( contextMD, clean( syntax ) );
                final List< Attachment > attachments = jspw.getManager( AttachmentManager.class ).listAttachments( p );
                for( final Attachment attachment : attachments ) {
                    final InputStream bytes = jspw.getManager( AttachmentManager.class ).getAttachmentStream( context, attachment );
                    md.getManager( AttachmentManager.class ).storeAttachment( attachment, bytes );
                }
            } catch ( Exception ex ) {
                errors.add( p.getWiki() + " " + p.getName() + " failed to convert " + ex.getMessage() );
            }
        }
        return errors;
    }

    Engine buildEngine( final String syntax, final String pageDir ) {
        return TestEngine.build( with( "jspwiki.fileSystemProvider.pageDir", pageDir ),
                                 with( RenderingManager.PROP_PARSER, JSPWikiToMarkdownMarkupParser.class.getName() ), // will be overwritten if jspwiki.syntax=markdown
                                 with( "jspwiki.test.disable-clean-props", "true" ),
                                 with( "jspwiki.workDir", "./target/workDir" + syntax ),
                                 with( "appender.rolling.fileName", "./target/wiki-" + syntax + ".log" ),
                                 with( "jspwiki.cache.enable", "false" ),
                                 with( "jspwiki.syntax", syntax ) );
    }

    String clean( final String wikiSyntax ) {
        return wikiSyntax;
    }

}
