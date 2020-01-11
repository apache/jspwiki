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
import com.vladsch.flexmark.ext.toc.TocBlock;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.preferences.Preferences;

import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * {@link NodePostProcessorState} which further post processes plugin links.
 */
public class PluginLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private static final Logger LOG = Logger.getLogger( PluginLinkNodePostProcessorState.class );
    private final WikiContext wikiContext;
    private final boolean m_wysiwygEditorMode;

    public PluginLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null ? wysiwygVariable : false;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node) 
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        if( link.getText().toString().startsWith( "{TableOfContents" ) ) {
            handleTableOfContentsPlugin( state, link );
            return;
        }
        PluginContent pluginContent = null;
        try {
            pluginContent = PluginContent.parsePluginLine( wikiContext, link.getUrl().toString(), -1 ); // -1 == do not generate _bounds parameter
            //
            //  This might sometimes fail, especially if there is something which looks
            //  like a plugin invocation but is really not.
            //
            if( pluginContent != null ) {
                final String pluginInvocation = pluginInvocation( link.getText().toString(), pluginContent );
                final HtmlInline content = new HtmlInline( CharSubSequence.of( pluginInvocation ) );
                pluginContent.executeParse( wikiContext );
                NodePostProcessorStateCommonOperations.addContent( state, link, content );
            }
        } catch( final PluginException e ) {
            LOG.info( wikiContext.getRealPage().getWiki() + " : " + wikiContext.getRealPage().getName() + " - Failed to insert plugin: " + e.getMessage() );
            if( !m_wysiwygEditorMode ) {
                final ResourceBundle rbPlugin = Preferences.getBundle( wikiContext, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
                NodePostProcessorStateCommonOperations.makeError( state, link, MessageFormat.format( rbPlugin.getString( "plugin.error.insertionfailed" ),
                                                                                                                         wikiContext.getRealPage().getWiki(),
                                                                                                                         wikiContext.getRealPage().getName(),
                                                                                                                         e.getMessage() ) );
            }
        } finally {
            if( pluginContent != null ) {
                removeLink( state, link );
            }
        }
    }

    /**
     * Return plugin execution. As plugin execution may not fire the plugin (i.e., on WYSIWYG editors), on those cases, the plugin line is returned.
     *
     * @param pluginMarkup plugin markup line
     * @param pluginContent the plugin content.
     * @return plugin execution, or plugin markup line if it wasn't executed.
     */
    String pluginInvocation( final String pluginMarkup, final PluginContent pluginContent ) {
        final String pluginInvocation = pluginContent.invoke( wikiContext );
        if( pluginMarkup.equals( pluginInvocation + "()" ) ) { // plugin line markup == plugin execution + "()" -> hasn't been executed
            return pluginMarkup;
        } else {
            return pluginInvocation;
        }
    }

    void handleTableOfContentsPlugin(final NodeTracker state, final JSPWikiLink link) {
        if( !m_wysiwygEditorMode ) {
            final ResourceBundle rb = Preferences.getBundle( wikiContext, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
            final HtmlInline divToc = new HtmlInline( CharSubSequence.of( "<div class=\"toc\">\n" ) );
            final HtmlInline divCollapseBox = new HtmlInline( CharSubSequence.of( "<div class=\"collapsebox\">\n" ) );
            final HtmlInline divsClosing = new HtmlInline( CharSubSequence.of( "</div>\n</div>\n" ) );
            final HtmlInline h4Title = new HtmlInline( CharSubSequence.of( "<h4 id=\"section-TOC\">" + // FIXME proper plugin parameters handling
                                                                           rb.getString( "tableofcontents.title" ) +
                                                                           "</h4>\n" ) );
            final TocBlock toc = new TocBlock( CharSubSequence.of( "[TOC]" ), CharSubSequence.of( "levels=1-3" ) );

            link.insertAfter( divToc );
            divToc.insertAfter( divCollapseBox );
            divCollapseBox.insertAfter( h4Title );
            h4Title.insertAfter( toc );
            toc.insertAfter( divsClosing );

        } else {
            NodePostProcessorStateCommonOperations.inlineLinkTextOnWysiwyg( state, link, m_wysiwygEditorMode );
        }
        removeLink( state, link );
    }

    void removeLink(final NodeTracker state, final JSPWikiLink link) {
        link.unlink();
        state.nodeRemoved( link );
    }

}
