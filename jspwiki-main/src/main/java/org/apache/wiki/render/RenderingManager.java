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
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;

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

    /** The name of the default renderer. */
    String DEFAULT_PARSER = JSPWikiMarkupParser.class.getName();

    /** The name of the default renderer. */
    String DEFAULT_RENDERER = XHTMLRenderer.class.getName();

    /** The name of the default WYSIWYG renderer. */
    String DEFAULT_WYSIWYG_RENDERER = WysiwygEditingRenderer.class.getName();

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

}
