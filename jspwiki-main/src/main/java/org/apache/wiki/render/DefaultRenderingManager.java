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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.StringTransmutator;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.cache.CachingManager;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.variables.VariableManager;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Properties;


/**
 *  This class provides a facade towards the differing rendering routines.  You should use the routines in this manager
 *  instead of the ones in Engine, if you don't want the different side effects to occur - such as WikiFilters.
 *  <p>
 *  This class also delegates to a rendering cache, i.e. documents are stored between calls. You may control the cache by
 *  tweaking the ehcache configuration file.
 *  <p>
 *
 *  @since  2.4
 */
public class DefaultRenderingManager implements RenderingManager {

    private static final Logger LOG = LogManager.getLogger( DefaultRenderingManager.class );
    private static final String VERSION_DELIMITER = "::";
    /** The name of the default renderer. */
    private static final String DEFAULT_PARSER = JSPWikiMarkupParser.class.getName();
    /** The name of the default renderer. */
    private static final String DEFAULT_RENDERER = XHTMLRenderer.class.getName();
    /** The name of the default WYSIWYG renderer. */
    private static final String DEFAULT_WYSIWYG_RENDERER = WysiwygEditingRenderer.class.getName();

    private Engine m_engine;
    private CachingManager cachingManager;

    /** If true, all titles will be cleaned. */
    private boolean m_beautifyTitle;

    private Constructor< ? > m_rendererConstructor;
    private Constructor< ? > m_rendererWysiwygConstructor;
    private String m_markupParserClass = DEFAULT_PARSER;

    /**
     *  {@inheritDoc}
     *
     *  Checks for cache size settings, initializes the document cache. Looks for alternative WikiRenderers, initializes one, or the
     *  default XHTMLRenderer, for use.
     */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws WikiException {
        m_engine = engine;
        cachingManager = m_engine.getManager( CachingManager.class );
        m_markupParserClass = properties.getProperty( PROP_PARSER, DEFAULT_PARSER );
        if( !ClassUtil.assignable( m_markupParserClass, MarkupParser.class.getName() ) ) {
        	LOG.warn( "{} does not subclass {} reverting to default markup parser.", m_markupParserClass, MarkupParser.class.getName() );
        	m_markupParserClass = DEFAULT_PARSER;
        }
        LOG.info( "Using {} as markup parser.", m_markupParserClass );

        m_beautifyTitle  = TextUtil.getBooleanProperty( properties, PROP_BEAUTIFYTITLE, m_beautifyTitle );
        final String renderImplName = properties.getProperty( PROP_RENDERER, DEFAULT_RENDERER );
        final String renderWysiwygImplName = properties.getProperty( PROP_WYSIWYG_RENDERER, DEFAULT_WYSIWYG_RENDERER );

        final Class< ? >[] rendererParams = { Context.class, WikiDocument.class };
        m_rendererConstructor = initRenderer( renderImplName, rendererParams );
        m_rendererWysiwygConstructor = initRenderer( renderWysiwygImplName, rendererParams );

        LOG.info( "Rendering content with {}.", renderImplName );

        WikiEventManager.addWikiEventListener( m_engine.getManager( FilterManager.class ),this );
    }

    private Constructor< ? > initRenderer( final String renderImplName, final Class< ? >[] rendererParams ) throws WikiException {
        Constructor< ? > c = null;
        try {
            final Class< ? > clazz = Class.forName( renderImplName );
            c = clazz.getConstructor( rendererParams );
        } catch( final ClassNotFoundException e ) {
            LOG.error( "Unable to find WikiRenderer implementation {}", renderImplName );
        } catch( final SecurityException e ) {
            LOG.error( "Unable to access the WikiRenderer(WikiContext,WikiDocument) constructor for {}", renderImplName );
        } catch( final NoSuchMethodException e ) {
            LOG.error( "Unable to locate the WikiRenderer(WikiContext,WikiDocument) constructor for {}", renderImplName );
        }
        if( c == null ) {
            throw new WikiException( "Failed to get WikiRenderer '" + renderImplName + "'." );
        }
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String beautifyTitle( final String title ) {
        if( m_beautifyTitle ) {
            try {
                final Attachment att = m_engine.getManager( AttachmentManager.class ).getAttachmentInfo( title );
                if( att == null ) {
                    return TextUtil.beautifyString( title );
                }

                final String parent = TextUtil.beautifyString( att.getParentName() );
                return parent + "/" + att.getFileName();
            } catch( final ProviderException e ) {
                return title;
            }
        }

        return title;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String beautifyTitleNoBreak( final String title ) {
        if( m_beautifyTitle ) {
            return TextUtil.beautifyString( title, "&nbsp;" );
        }

        return title;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public MarkupParser getParser( final Context context, final String pagedata ) {
    	try {
			return ClassUtil.getMappedObject( m_markupParserClass, context, new StringReader( pagedata ) );
		} catch( final ReflectiveOperationException | IllegalArgumentException e ) {
			LOG.error( "unable to get an instance of {} ({}), returning default markup parser.", m_markupParserClass, e.getMessage(), e );
			return new JSPWikiMarkupParser( context, new StringReader( pagedata ) );
		}
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    // FIXME: The cache management policy is not very good: deleted/changed pages should be detected better.
    public WikiDocument getRenderedDocument( final Context context, final String pagedata ) {
        final String pageid = context.getRealPage().getName() + VERSION_DELIMITER +
                              context.getRealPage().getVersion() + VERSION_DELIMITER +
                              context.getVariable( Context.VAR_EXECUTE_PLUGINS );

        if( useCache( context ) ) {
            final WikiDocument doc = cachingManager.get( CachingManager.CACHE_DOCUMENTS, pageid, () -> null );
            if ( doc != null ) {
                //  This check is needed in case the different filters have actually changed the page data.
                //  FIXME: Figure out a faster method
                if( pagedata.equals( doc.getPageData() ) ) {
                    LOG.debug( "Using cached HTML for page {}", pageid );
                    return doc;
                }
            } else {
                LOG.debug( "Re-rendering and storing {}", pageid );
            }
        }

        // Refresh the data content
        try {
            final MarkupParser parser = getParser( context, pagedata );
            final WikiDocument doc = parser.parse();
            doc.setPageData( pagedata );
            if( useCache( context ) ) {
                cachingManager.put( CachingManager.CACHE_DOCUMENTS, pageid, doc );
            }
            return doc;
        } catch( final IOException ex ) {
            LOG.error( "Unable to parse", ex );
        }

        return null;
    }

    boolean useCache( final Context context ) {
        return cachingManager.enabled( CachingManager.CACHE_DOCUMENTS )
               && ContextEnum.PAGE_VIEW.getRequestContext().equals( context.getRequestContext() );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getHTML( final Context context, final WikiDocument doc ) throws IOException {
        final Boolean wysiwygVariable = context.getVariable( Context.VAR_WYSIWYG_EDITOR_MODE );
        final boolean wysiwygEditorMode;
        if( wysiwygVariable != null ) {
            wysiwygEditorMode = wysiwygVariable;
        } else {
            wysiwygEditorMode = false;
        }
        final WikiRenderer rend;
        if( wysiwygEditorMode ) {
            rend = getWysiwygRenderer( context, doc );
        } else {
            rend = getRenderer( context, doc );
        }

        return rend.getString();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getHTML( final Context context, final Page page ) {
        final String pagedata = m_engine.getManager( PageManager.class ).getPureText( page.getName(), page.getVersion() );
        return textToHTML( context, pagedata );
    }

    /**
     *  Returns the converted HTML of the page's specific version. The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param pagename WikiName of the page to convert.
     *  @param version Version number to fetch
     *  @return HTML-rendered page text.
     */
    @Override
    public String getHTML( final String pagename, final int version ) {
        final Page page = m_engine.getManager( PageManager.class ).getPage( pagename, version );
        final Context context = Wiki.context().create( m_engine, page );
        context.setRequestContext( ContextEnum.PAGE_NONE.getRequestContext() );
        return getHTML( context, page );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String textToHTML( final Context context, String pagedata ) {
        String result = "";

        final boolean runFilters = "true".equals( m_engine.getManager( VariableManager.class ).getValue( context,VariableManager.VAR_RUNFILTERS,"true" ) );

        final StopWatch sw = new StopWatch();
        sw.start();
        try {
            if( runFilters ) {
                pagedata = m_engine.getManager( FilterManager.class ).doPreTranslateFiltering( context, pagedata );
            }

            result = getHTML( context, pagedata );

            if( runFilters ) {
                result = m_engine.getManager( FilterManager.class ).doPostTranslateFiltering( context, result );
            }
        } catch( final FilterException e ) {
            LOG.error( "page filter threw exception: ", e );
            // FIXME: Don't yet know what to do
        }
        sw.stop();
        LOG.debug( "Page {} rendered, took {}", context.getRealPage().getName(), sw );

        return result;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String textToHTML( final Context context,
                              String pagedata,
                              final StringTransmutator localLinkHook,
                              final StringTransmutator extLinkHook,
                              final StringTransmutator attLinkHook,
                              final boolean parseAccessRules,
                              final boolean justParse ) {
        String result = "";

        if( pagedata == null ) {
            LOG.error( "NULL pagedata to textToHTML()" );
            return null;
        }

        final boolean runFilters = "true".equals( m_engine.getManager( VariableManager.class ).getValue( context, VariableManager.VAR_RUNFILTERS,"true" ) );

        try {
            final StopWatch sw = new StopWatch();
            sw.start();

            if( runFilters && m_engine.getManager( FilterManager.class ) != null ) {
                pagedata = m_engine.getManager( FilterManager.class ).doPreTranslateFiltering( context, pagedata );
            }

            final MarkupParser mp = getParser( context, pagedata );
            mp.addLocalLinkHook( localLinkHook );
            mp.addExternalLinkHook( extLinkHook );
            mp.addAttachmentLinkHook( attLinkHook );

            if( !parseAccessRules ) {
                mp.disableAccessRules();
            }

            final WikiDocument doc = mp.parse();
            //  In some cases it's better just to parse, not to render
            if( !justParse ) {
                result = getHTML( context, doc );
                if( runFilters && m_engine.getManager( FilterManager.class ) != null ) {
                    result = m_engine.getManager( FilterManager.class ).doPostTranslateFiltering( context, result );
                }
            }

            sw.stop();

            LOG.debug( "Page {} rendered, took {}", context.getRealPage().getName(), sw );
        } catch( final IOException e ) {
            LOG.error( "Failed to scan page data: ", e );
        } catch( final FilterException e ) {
            LOG.error( "page filter threw exception: ", e );
            // FIXME: Don't yet know what to do
        }

        return result;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public WikiRenderer getRenderer( final Context context, final WikiDocument doc ) {
        final Object[] params = { context, doc };
        return getRenderer( params, m_rendererConstructor );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public WikiRenderer getWysiwygRenderer( final Context context, final WikiDocument doc ) {
        final Object[] params = { context, doc };
        return getRenderer( params, m_rendererWysiwygConstructor );
    }

    @SuppressWarnings("unchecked")
    private < T extends WikiRenderer > T getRenderer( final Object[] params, final Constructor<?> rendererConstructor ) {
        try {
            return ( T )rendererConstructor.newInstance( params );
        } catch( final Exception e ) {
            LOG.error( "Unable to create WikiRenderer", e );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Flushes the document cache in response to a POST_SAVE_BEGIN event.
     *
     * @see WikiEventListener#actionPerformed(WikiEvent)
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        LOG.debug( "event received: {}", event.toString() );
        if( isBeginningAWikiPagePostSaveEventAndDocumentCacheIsEnabled( event ) ) {
            final String pageName = ( ( WikiPageEvent ) event ).getPageName();
            cachingManager.remove( CachingManager.CACHE_DOCUMENTS, pageName );
            final Collection< String > referringPages = m_engine.getManager( ReferenceManager.class ).findReferrers( pageName );

            // Flush also those pages that refer to this page (if a nonexistent page
            // appears, we need to flush the HTML that refers to the now-existent page)
            if( referringPages != null ) {
                for( final String page : referringPages ) {
                    LOG.debug( "Flushing latest version of {}", page );
                    // as there is a new version of the page expire both plugin and pluginless versions of the old page
                    cachingManager.remove( CachingManager.CACHE_DOCUMENTS, page + VERSION_DELIMITER + PageProvider.LATEST_VERSION  + VERSION_DELIMITER + Boolean.FALSE );
                    cachingManager.remove( CachingManager.CACHE_DOCUMENTS, page + VERSION_DELIMITER + PageProvider.LATEST_VERSION  + VERSION_DELIMITER + Boolean.TRUE );
                    cachingManager.remove( CachingManager.CACHE_DOCUMENTS, page + VERSION_DELIMITER + PageProvider.LATEST_VERSION  + VERSION_DELIMITER + null );
                }
            }
        }
    }

    boolean isBeginningAWikiPagePostSaveEventAndDocumentCacheIsEnabled( final WikiEvent event ) {
        return event instanceof WikiPageEvent
               && event.getType() == WikiPageEvent.POST_SAVE_BEGIN
               && cachingManager.enabled( CachingManager.CACHE_DOCUMENTS );
    }

}
