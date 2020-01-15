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
package org.apache.wiki.render;

import org.apache.log4j.Logger;
import org.apache.wiki.StringTransmutator;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.providers.WikiPageProvider;

import java.io.IOException;
import java.util.Properties;


/**
 *  This class provides a facade towards the differing rendering routines.  You should use the routines in this manager
 *  instead of the ones in WikiEngine, if you don't want the different side effects to occur - such as WikiFilters.
 *  <p>
 *  This class also manages a rendering cache, i.e. documents are stored between calls. You may control the cache by
 *  tweaking the ehcache.xml file.
 *  <p>
 *
 *  @since  2.4
 */
public interface RenderingManager extends WikiEventListener, InternalModule {

    /** markup parser property. */
    String PROP_PARSER = "jspwiki.renderingManager.markupParser";

    /** default renderer property. */
    String PROP_RENDERER = "jspwiki.renderingManager.renderer";

    /** default wysiwyg renderer property. */
    String PROP_WYSIWYG_RENDERER = "jspwiki.renderingManager.renderer.wysiwyg";

    String PROP_BEAUTIFYTITLE = "jspwiki.breakTitleWithSpaces";

    /** Name of the regular page cache. */
    String DOCUMENTCACHE_NAME = "jspwiki.renderingCache";

    /**
     *  Initializes the RenderingManager.
     *  Checks for cache size settings, initializes the document cache.
     *  Looks for alternative WikiRenderers, initializes one, or the default
     *  XHTMLRenderer, for use.
     *
     *  @param engine A WikiEngine instance.
     *  @param properties A list of properties to get parameters from.
     *  @throws WikiException If the manager could not be initialized.
     */
    void initialize( WikiEngine engine, Properties properties ) throws WikiException;

    /**
     *  Beautifies the title of the page by appending spaces in suitable places, if the user has so decreed in the properties when
     *  constructing this WikiEngine.  However, attachment names are only beautified by the name.
     *
     *  @param title The title to beautify
     *  @return A beautified title (or, if beautification is off, returns the title without modification)
     *  @since 1.7.11, moved to PageManager on 2.11.0
     */
    String beautifyTitle( String title );

    /**
     *  Beautifies the title of the page by appending non-breaking spaces in suitable places.  This is really suitable only for HTML output,
     *  as it uses the &amp;nbsp; -character.
     *
     *  @param title The title to beautify
     *  @return A beautified title.
     *  @since 2.1.127
     */
    String beautifyTitleNoBreak( String title );

    /**
     *  Returns the wiki Parser
     *  @param pagedata the page data
     *  @return A MarkupParser instance.
     */
    MarkupParser getParser( WikiContext context, String pagedata );

    /**
     *  Returns a cached document, if one is found.
     *
     * @param context the wiki context
     * @param pagedata the page data
     * @return the rendered wiki document
     */
    WikiDocument getRenderedDocument( WikiContext context, String pagedata );

    /**
     * Returns a WikiRenderer instance, initialized with the given context and doc. The object is an XHTMLRenderer,
     * unless overridden in jspwiki.properties with PROP_RENDERER.
     *
     * @param context The WikiContext
     * @param doc The document to render
     * @return A WikiRenderer for this document, or null, if no such renderer could be instantiated.
     */
    WikiRenderer getRenderer( WikiContext context, WikiDocument doc );

    /**
     * Returns a WikiRenderer instance meant for WYSIWYG editing, initialized with the given
     * context and doc. The object is an WysiwygEditingRenderer, unless overridden
     * in jspwiki.properties with PROP_WYSIWYG_RENDERER.
     *
     * @param context The WikiContext
     * @param doc The document to render
     * @return A WikiRenderer instance meant for WYSIWYG editing, for this document, or null, if no such renderer could be instantiated.
     */
    WikiRenderer getWysiwygRenderer( WikiContext context, WikiDocument doc );

    /**
     *  Simply renders a WikiDocument to a String.  This version does not get the document from the cache - in fact, it does
     *  not cache the document at all.  This is very useful, if you have something that you want to render outside the caching
     *  routines.  Because the cache is based on full pages, and the cache keys are based on names, use this routine if you're
     *  rendering anything for yourself.
     *
     *  @param context The WikiContext to render in
     *  @param doc A proper WikiDocument
     *  @return Rendered HTML.
     *  @throws IOException If the WikiDocument is poorly formed.
     */
    String getHTML( WikiContext context, WikiDocument doc ) throws IOException;

    /**
     *  Returns the converted HTML of the page using a different context than the default context.
     *
     *  @param  context A WikiContext in which you wish to render this page in.
     *  @param  page WikiPage reference.
     *  @return HTML-rendered version of the page.
     */
    String getHTML( WikiContext context, WikiPage page );

    /**
     *  Returns the converted HTML of the page's specific version. The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param pagename WikiName of the page to convert.
     *  @param version Version number to fetch
     *  @return HTML-rendered page text.
     */
    String getHTML( String pagename, int version );

    /**
     *   Convenience method for rendering, using the default parser and renderer.  Note that you can't use this method
     *   to do any arbitrary rendering, as the pagedata MUST be the data from the that the WikiContext refers to - this
     *   method caches the HTML internally, and will return the cached version.  If the pagedata is different from what
     *   was cached, will re-render and store the pagedata into the internal cache.
     *
     *   @param context the wiki context
     *   @param pagedata the page data
     *   @return XHTML data.
     */
    default String getHTML( final WikiContext context, final String pagedata ) {
        try {
            final WikiDocument doc = getRenderedDocument( context, pagedata );
            return getHTML( context, doc );
        } catch( final IOException e ) {
            Logger.getLogger( RenderingManager.class ).error("Unable to parse", e );
        }

        return null;
    }

    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     *  @return HTML-rendered version of the page.
     */
    default String getHTML( final String page ) {
        return getHTML( page, WikiPageProvider.LATEST_VERSION );
    }

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     *  @param context  The WikiContext in which the page is to be rendered
     *  @return Rendered page text
     */
    String textToHTML( WikiContext context, String pagedata );

    /**
     *  Helper method for doing the HTML translation.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *  @param parseAccessRules Parse the access rules if we encounter them
     *  @param justParse Just parses the pagedata, does not actually render.  In this case, this methods an empty string.
     *  @return HTML-rendered page text.
     */
    String textToHTML( WikiContext context,
                       String pagedata,
                       StringTransmutator localLinkHook,
                       StringTransmutator extLinkHook,
                       StringTransmutator attLinkHook,
                       boolean parseAccessRules,
                       boolean justParse );

    /**
     *  Just convert WikiText to HTML.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *
     *  @return HTML-rendered page text.
     */
    default String textToHTML( final WikiContext context,
                               final String pagedata,
                               final StringTransmutator localLinkHook,
                               final StringTransmutator extLinkHook ) {
        return textToHTML( context, pagedata, localLinkHook, extLinkHook, null, true, false );
    }

    /**
     *  Just convert WikiText to HTML.
     *
     *  @param context The WikiContext in which to do the conversion
     *  @param pagedata The data to render
     *  @param localLinkHook Is called whenever a wiki link is found
     *  @param extLinkHook   Is called whenever an external link is found
     *  @param attLinkHook   Is called whenever an attachment link is found
     *  @return HTML-rendered page text.
     */
    default String textToHTML( final WikiContext context,
                               final String pagedata,
                               final StringTransmutator localLinkHook,
                               final StringTransmutator extLinkHook,
                               final StringTransmutator attLinkHook ) {
        return textToHTML( context, pagedata, localLinkHook, extLinkHook, attLinkHook, true, false );
    }

}
