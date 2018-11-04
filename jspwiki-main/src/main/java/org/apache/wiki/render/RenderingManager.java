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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventUtils;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.util.ClassUtil;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


/**
 *  This class provides a facade towards the differing rendering routines.  You should
 *  use the routines in this manager instead of the ones in WikiEngine, if you don't
 *  want the different side effects to occur - such as WikiFilters.
 *  <p>
 *  This class also manages a rendering cache, i.e. documents are stored between calls.
 *  You may control the cache by tweaking the ehcache.xml file.
 *  <p>
 *
 *  @since  2.4
 */
public class RenderingManager implements WikiEventListener, InternalModule
{
    private static Logger log = Logger.getLogger( RenderingManager.class );

    private int        m_cacheExpiryPeriod = 24*60*60; // This can be relatively long

    private WikiEngine m_engine;

    private boolean m_useCache = true;

    private CacheManager m_cacheManager = CacheManager.getInstance();

    /** The capacity of the caches, if you want something else, tweak ehcache.xml. */
    private static final int    DEFAULT_CACHESIZE     = 1000;
    private static final String VERSION_DELIMITER     = "::";
    private static final String PROP_PARSER           = "jspwiki.renderingManager.markupParser";
    private static final String PROP_RENDERER         = "jspwiki.renderingManager.renderer";
    private static final String PROP_WYSIWYG_RENDERER = "jspwiki.renderingManager.renderer.wysiwyg";

    /** The name of the default renderer. */
    public static final String DEFAULT_PARSER = JSPWikiMarkupParser.class.getName();

    /** The name of the default renderer. */
    public  static final String DEFAULT_RENDERER  = XHTMLRenderer.class.getName();

    /** The name of the default WYSIWYG renderer. */
    public  static final String DEFAULT_WYSIWYG_RENDERER  = WysiwygEditingRenderer.class.getName();

    /** Stores the WikiDocuments that have been cached. */
    private Cache m_documentCache;

    /** Name of the regular page cache. */
    public static final String DOCUMENTCACHE_NAME = "jspwiki.renderingCache";

    private Constructor< ? > m_rendererConstructor;
    private Constructor< ? > m_rendererWysiwygConstructor;
    private String m_markupParserClass = DEFAULT_PARSER;

    /**
     *  Name of the WikiContext variable which is set to Boolean.TRUE or Boolean.FALSE
     *  depending on whether WYSIWYG is currently in effect.
     */
    public static final String WYSIWYG_EDITOR_MODE = "WYSIWYG_EDITOR_MODE";

    /**
     *  Variable name which tells whether plugins should be executed or not.  Value can be either
     *  {@code Boolean.TRUE} or {@code Boolean.FALSE}. While not set it's value is {@code null}
     */
    public static final String VAR_EXECUTE_PLUGINS = "_PluginContent.execute";

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
    public void initialize( WikiEngine engine, Properties properties )
        throws WikiException
    {
        m_engine = engine;

        m_markupParserClass = properties.getProperty( PROP_PARSER, DEFAULT_PARSER );
        if( !ClassUtil.assignable( m_markupParserClass, MarkupParser.class.getName() ) ) {
        	log.warn( m_markupParserClass + " does not subclass " + MarkupParser.class.getName() + " reverting to default markup parser." );
        	m_markupParserClass = DEFAULT_PARSER;
        }
        log.info( "Using " + m_markupParserClass + " as markup parser." );

        m_useCache = "true".equals(properties.getProperty(PageManager.PROP_USECACHE));

        if (m_useCache) {
            String documentCacheName = engine.getApplicationName() + "." + DOCUMENTCACHE_NAME;

            if (m_cacheManager.cacheExists(documentCacheName)) {
                m_documentCache = m_cacheManager.getCache(documentCacheName);
            } else {
                log.info("cache with name " + documentCacheName + " not found in ehcache.xml, creating it with defaults.");
                m_documentCache = new Cache(documentCacheName, DEFAULT_CACHESIZE, false, false, m_cacheExpiryPeriod, m_cacheExpiryPeriod);
                m_cacheManager.addCache(m_documentCache);
            }
        }

        final String renderImplName = properties.getProperty( PROP_RENDERER, DEFAULT_RENDERER );
        final String renderWysiwygImplName = properties.getProperty( PROP_WYSIWYG_RENDERER, DEFAULT_WYSIWYG_RENDERER );

        final Class< ? >[] rendererParams = { WikiContext.class, WikiDocument.class };
        m_rendererConstructor = initRenderer( renderImplName, rendererParams );
        m_rendererWysiwygConstructor = initRenderer( renderWysiwygImplName, rendererParams );

        log.info( "Rendering content with " + renderImplName + "." );

        WikiEventUtils.addWikiEventListener(m_engine, WikiPageEvent.POST_SAVE_BEGIN, this);
    }

    private Constructor< ? > initRenderer( final String renderImplName, final Class< ? >[] rendererParams ) throws WikiException {
        Constructor< ? > c = null;
        try {
            final Class< ? > clazz = Class.forName( renderImplName );
            c = clazz.getConstructor( rendererParams );
        } catch( final ClassNotFoundException e ) {
            log.error( "Unable to find WikiRenderer implementation " + renderImplName );
        } catch( final SecurityException e ) {
            log.error( "Unable to access the WikiRenderer(WikiContext,WikiDocument) constructor for "  + renderImplName );
        } catch( final NoSuchMethodException e ) {
            log.error( "Unable to locate the WikiRenderer(WikiContext,WikiDocument) constructor for "  + renderImplName );
        }
        if( c == null ) {
            throw new WikiException( "Failed to get WikiRenderer '" + renderImplName + "'." );
        }
        return c;
    }

    /**
     *  Returns the wiki Parser
     *  @param pagedata the page data
     *  @return A MarkupParser instance.
     */
    public MarkupParser getParser( WikiContext context, String pagedata ) {
    	try {
			return ( MarkupParser )ClassUtil.getMappedObject( m_markupParserClass, context, new StringReader( pagedata ) );
		} catch( ReflectiveOperationException | IllegalArgumentException e ) {
			log.error( "unable to get an instance of " + m_markupParserClass + " (" + e.getMessage() + "), returning default markup parser.", e );
			return new JSPWikiMarkupParser( context, new StringReader( pagedata ) );
		}
    }

    /**
     *  Returns a cached document, if one is found.
     *
     * @param context the wiki context
     * @param pagedata the page data
     * @return the rendered wiki document
     * @throws IOException If rendering cannot be accomplished
     */
    // FIXME: The cache management policy is not very good: deleted/changed pages should be detected better.
    protected WikiDocument getRenderedDocument( WikiContext context, String pagedata ) throws IOException {
        String pageid = context.getRealPage().getName() + VERSION_DELIMITER +
                        context.getRealPage().getVersion() + VERSION_DELIMITER +
                        context.getVariable( RenderingManager.VAR_EXECUTE_PLUGINS );

        if( useCache( context ) ) {
            Element element = m_documentCache.get( pageid );
            if ( element != null ) {
                WikiDocument doc = (WikiDocument) element.getObjectValue();

                //
                //  This check is needed in case the different filters have actually changed the page data.
                //  FIXME: Figure out a faster method
                if( pagedata.equals( doc.getPageData() ) ) {
                    if( log.isDebugEnabled() ) {
                        log.debug( "Using cached HTML for page " + pageid );
                    }
                    return doc;
                }
            } else if( log.isDebugEnabled() ) {
                log.debug( "Re-rendering and storing " + pageid );
            }
        }

        //  Refresh the data content
        //
        try {
            MarkupParser parser = getParser( context, pagedata );
            WikiDocument doc = parser.parse();
            doc.setPageData( pagedata );
            if( useCache( context ) ) {
                m_documentCache.put( new Element( pageid, doc ) );
            }
            return doc;
        } catch( IOException ex ) {
            log.error( "Unable to parse", ex );
        }

        return null;
    }

    boolean useCache( WikiContext context ) {
        return m_useCache && WikiContext.VIEW.equals( context.getRequestContext() );
    }

    /**
     *  Simply renders a WikiDocument to a String.  This version does not get the document
     *  from the cache - in fact, it does not cache the document at all.  This is
     *  very useful, if you have something that you want to render outside the caching
     *  routines.  Because the cache is based on full pages, and the cache keys are
     *  based on names, use this routine if you're rendering anything for yourself.
     *
     *  @param context The WikiContext to render in
     *  @param doc A proper WikiDocument
     *  @return Rendered HTML.
     *  @throws IOException If the WikiDocument is poorly formed.
     */
    public String getHTML( WikiContext context, WikiDocument doc ) throws IOException
    {
        final Boolean wysiwygVariable = ( Boolean )context.getVariable( WYSIWYG_EDITOR_MODE );
        final boolean wysiwygEditorMode;
        if( wysiwygVariable != null ) {
            wysiwygEditorMode = wysiwygVariable.booleanValue();
        } else {
            wysiwygEditorMode = false;
        }
        WikiRenderer rend;
        if( wysiwygEditorMode ) {
            rend = getWysiwygRenderer( context, doc );
        } else {
            rend = getRenderer( context, doc );
        }

        return rend.getString();
    }

    /**
     * Returns a WikiRenderer instance, initialized with the given
     * context and doc. The object is an XHTMLRenderer, unless overridden
     * in jspwiki.properties with PROP_RENDERER.
     *
     * @param context The WikiContext
     * @param doc The document to render
     * @return A WikiRenderer for this document, or null, if no such renderer could be instantiated.
     */
    public WikiRenderer getRenderer( WikiContext context, WikiDocument doc ) {
        final Object[] params = { context, doc };
        return getRenderer( params, m_rendererConstructor );
    }

    /**
     * Returns a WikiRenderer instance meant for WYSIWYG editing, initialized with the given
     * context and doc. The object is an WysiwygEditingRenderer, unless overridden
     * in jspwiki.properties with PROP_WYSIWYG_RENDERER.
     *
     * @param context The WikiContext
     * @param doc The document to render
     * @return A WikiRenderer instance meant for WYSIWYG editing, for this document, or null, if
     *         no such renderer could be instantiated.
     */
    public WikiRenderer getWysiwygRenderer( WikiContext context, WikiDocument doc ) {
        final Object[] params = { context, doc };
        return getRenderer( params, m_rendererWysiwygConstructor );
    }

    @SuppressWarnings("unchecked")
    private < T extends WikiRenderer > T getRenderer( Object[] params, Constructor<?> rendererConstructor ) {
        T rval = null;

        try {
            rval = (T)rendererConstructor.newInstance( params );
        } catch( final Exception e ) {
            log.error( "Unable to create WikiRenderer", e );
        }
        return rval;
    }

    /**
     *   Convenience method for rendering, using the default parser and renderer.  Note that
     *   you can't use this method to do any arbitrary rendering, as the pagedata MUST
     *   be the data from the that the WikiContext refers to - this method caches the HTML
     *   internally, and will return the cached version.  If the pagedata is different
     *   from what was cached, will re-render and store the pagedata into the internal cache.
     *
     *   @param context the wiki context
     *   @param pagedata the page data
     *   @return XHTML data.
     */
    public String getHTML( WikiContext context, String pagedata )
    {
        try
        {
            WikiDocument doc = getRenderedDocument( context, pagedata );

            return getHTML( context, doc );
        }
        catch( IOException e )
        {
            log.error("Unable to parse",e);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Flushes the document cache in response to a POST_SAVE_BEGIN event.
     *
     * @see org.apache.wiki.event.WikiEventListener#actionPerformed(org.apache.wiki.event.WikiEvent)
     */
    @Override
    public void actionPerformed( WikiEvent event ) {
        log.debug( "event received: " + event.toString() );
        if( m_useCache ) {
            if( ( event instanceof WikiPageEvent ) && ( event.getType() == WikiPageEvent.POST_SAVE_BEGIN ) ) {
                if( m_documentCache != null ) {
                    String pageName = ( ( WikiPageEvent ) event ).getPageName();
                    m_documentCache.remove( pageName );
                    Collection< String > referringPages = m_engine.getReferenceManager().findReferrers( pageName );

                    //
                    //  Flush also those pages that refer to this page (if an nonexistent page
                    //  appears, we need to flush the HTML that refers to the now-existent page)
                    //
                    if( referringPages != null ) {
                        for( String page : referringPages ) {
                            if( log.isDebugEnabled() ) {
                                log.debug( "Flushing latest version of " + page );
                            }
                            // as there is a new version of the page expire both plugin and pluginless versions of the old page
                            m_documentCache.remove( page + VERSION_DELIMITER + WikiPageProvider.LATEST_VERSION  + VERSION_DELIMITER + Boolean.FALSE );
                            m_documentCache.remove( page + VERSION_DELIMITER + WikiPageProvider.LATEST_VERSION  + VERSION_DELIMITER + Boolean.TRUE );
                            m_documentCache.remove( page + VERSION_DELIMITER + WikiPageProvider.LATEST_VERSION  + VERSION_DELIMITER + null );
                        }
                    }
                }
            }
        }
    }

}
