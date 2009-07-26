/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
import java.util.List;
import java.util.Properties;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventUtils;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.providers.CachingProvider;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;

/**
 *  This class provides a facade towards the differing rendering routines.  You should
 *  use the routines in this manager instead of the ones in WikiEngine, if you don't
 *  want the different side effects to occur - such as WikiFilters.
 *  <p>
 *  This class also manages a rendering cache, i.e. documents are stored between calls.
 *  You may control the size of the cache by using the "jspwiki.renderingManager.cacheSize"
 *  parameter in jspwiki.properties.  The property value is the number of items that
 *  are stored in the cache.  By default, the value of this parameter is taken from
 *  the "jspwiki.cachingProvider.cacheSize" parameter (i.e. the rendering cache is
 *  the same size as the page cache), but you may control them separately.
 *  <p>
 *  You can turn caching completely off by stating a cacheSize of zero.
 *  <p>
 *  Since 3.0, the underlying cache implementation is based on EhCache, with the cache
 *  name set to {@link RenderingManager#CACHE_NAME} (Currently set to {@value RenderingManager#CACHE_NAME}.)
 *  If a cache by that name exists already (e.g. configured in ehcache.xml), then that
 *  cache is used.  If the cache does not exist, then we'll just simply use the settings
 *  from jspwiki.properties.
 *  
 *  @since  2.4
 */
public class RenderingManager implements WikiEventListener, InternalModule
{
    private static Logger log = LoggerFactory.getLogger( RenderingManager.class );

    private              int    m_cacheExpiryPeriod = 24*60*60; // This can be relatively long

    private          WikiEngine m_engine;

    /**
     *  Parameter value for setting the cache size.
     */
    public  static final String PROP_CACHESIZE    = "jspwiki.renderingManager.capacity";
    private static final int    DEFAULT_CACHESIZE = 1000;
    private static final String VERSION_DELIMITER = "::";
    private static final String PROP_RENDERER     = "jspwiki.renderingManager.renderer";
    
    /** The name of the default renderer. */
    public  static final String DEFAULT_RENDERER  = XHTMLRenderer.class.getName();

    /** Name of the EhCache cache. */
    public  static final String CACHE_NAME        = "jspwiki.renderingCache";
    
    /**
     *  Create a private caching manager.
     */
    private CacheManager m_cacheManager = CacheManager.getInstance();
    /**
     *  Stores the WikiDocuments that have been cached.
     */
    private              Cache  m_documentCache;

    /**
     *
     */
    private         Constructor<?> m_rendererConstructor;

    /**
     *  Name of the WikiContext variable which is set to Boolean.TRUE or Boolean.FALSE
     *  depending on whether WYSIWYG is currently in effect.
     */
    public static final String WYSIWYG_EDITOR_MODE = "WYSIWYG_EDITOR_MODE";

    /**
     *  Variable name which tells whether plugins should be executed or not.  Value
     *  can be either Boolean.TRUE or Boolean.FALSE.
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
        int cacheSize = TextUtil.getIntegerProperty( properties, PROP_CACHESIZE, DEFAULT_CACHESIZE );

        //
        //  First check, if there's already a cache configured in ehcache.xml
        //  If not, create our own.
        //
        Cache c = m_cacheManager.getCache( CACHE_NAME );
        
        if( c == null && cacheSize > 0 )
        {
            c = new Cache(CACHE_NAME,cacheSize,false,false,m_cacheExpiryPeriod,m_cacheExpiryPeriod);
            m_cacheManager.addCache( c );
        }
        else
        {
            log.info( "RenderingManager caching is disabled." );
        }
        
        m_documentCache = m_cacheManager.getCache( CACHE_NAME );

        String renderImplName = properties.getProperty( PROP_RENDERER );
        if( renderImplName == null )
        {
            renderImplName = DEFAULT_RENDERER;
        }
        Class<?>[] rendererParams = { WikiContext.class, WikiDocument.class };
        try
        {
            Class<?> cc = Class.forName( renderImplName );
            m_rendererConstructor = cc.getConstructor( rendererParams );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "Unable to find WikiRenderer implementation " + renderImplName );
        }
        catch( SecurityException e )
        {
            log.error( "Unable to access the WikiRenderer(WikiContext,WikiDocument) constructor for "  + renderImplName );
        }
        catch( NoSuchMethodException e )
        {
            log.error( "Unable to locate the WikiRenderer(WikiContext,WikiDocument) constructor for "  + renderImplName );
        }
        if( m_rendererConstructor == null )
        {
            throw new WikiException( "Failed to get WikiRenderer '" + renderImplName + "'." );
        }
        log.info( "Rendering content with " + renderImplName + "." );

        WikiEventUtils.addWikiEventListener(m_engine, WikiPageEvent.POST_SAVE_BEGIN, this);
    }

    /**
     *  Returns the default Parser for this context.
     *
     *  @param context the wiki context
     *  @param pagedata the page data
     *  @return A MarkupParser instance.
     */
    public MarkupParser getParser( WikiContext context, String pagedata )
    {
        MarkupParser parser = new JSPWikiMarkupParser( context, new StringReader(pagedata) );

        return parser;
    }

    /**
     *  Returns a cached document, if one is found.
     *
     * @param context the wiki context
     * @param pagedata the page data
     * @return the rendered wiki document
     * @throws IOException If rendering cannot be accomplished
     */
    // FIXME: The cache management policy is not very good: deleted/changed pages
    //        should be detected better.
    protected WikiDocument getRenderedDocument( WikiContext context, String pagedata )
        throws IOException
    {
        String pageid = context.getRealPage().getName()+VERSION_DELIMITER+context.getRealPage().getVersion();

        if( m_documentCache != null )
        {
            Element e = m_documentCache.get( pageid );
            
            if( e != null )
            {
                WikiDocument doc = (WikiDocument) e.getObjectValue();

                //
                //  This check is needed in case the different filters have actually
                //  changed the page data.
                //  FIXME: Figure out a faster method
                
                if( pagedata.equals(doc.getPageData()) )
                {
                    if( log.isDebugEnabled() ) log.debug("Using cached HTML for page "+pageid );
                    return doc;
                }
            }
            else
            {
                if( log.isDebugEnabled() ) log.debug("Re-rendering and storing "+pageid );
            }
        }

        //
        //  Refresh the data content
        //
        try
        {
            MarkupParser parser = getParser( context, pagedata );
            WikiDocument doc = parser.parse();
            doc.setPageData( pagedata );
            if( m_documentCache != null )
            {
                m_documentCache.put( new Element( pageid, doc ) );
            }
            return doc;
        }
        catch( IOException ex )
        {
            log.error("Unable to parse",ex);
        }

        return null;
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
    public String getHTML( WikiContext context, WikiDocument doc )
        throws IOException
    {
        WikiRenderer rend = getRenderer( context, doc );

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
    public WikiRenderer getRenderer( WikiContext context, WikiDocument doc )
    {
        Object[] params = { context, doc };
        WikiRenderer rval = null;

        try
        {
            rval = (WikiRenderer)m_rendererConstructor.newInstance( params );
        }
        catch( Exception e )
        {
            log.error( "Unable to create WikiRenderer", e );
        }
        return rval;
    }

    /**
     *   Convinience method for rendering, using the default parser and renderer.  Note that
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

    private void flushCache( String key )
    {
        // We use Strings always
        List<String> keys = (List<String>)m_documentCache.getKeys();
        
        for( String k : keys )
        {
            if( k.startsWith( key ) )
                m_documentCache.remove( k );
        }
        
    }
    
    /**
     * Flushes the document cache in response to a POST_SAVE_BEGIN event.
     *
     * @see org.apache.wiki.event.WikiEventListener#actionPerformed(org.apache.wiki.event.WikiEvent)
     * @param event {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    public void actionPerformed(WikiEvent event)
    {
        if( (event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.POST_SAVE_BEGIN) )
        {
            if( m_documentCache != null )
            {
                String pageName = ((WikiPageEvent) event).getPageName();

                flushCache( pageName );

                try
                {
                    Collection<WikiPath> referringPages = m_engine.getReferenceManager().getReferredBy( WikiPath.valueOf(pageName) );

                    //
                    //  Flush also those pages that refer to this page (if an nonexistant page
                    //  appears; we need to flush the HTML that refers to the now-existant page
                    //
                    if( referringPages != null )
                    {
                        for ( WikiPath path : referringPages )
                        {
                            if( log.isDebugEnabled() ) log.debug( "Flushing " + path );
                            flushCache( path.toString() );
                        }
                    }
                }
                catch( ProviderException e )
                {
                    // Just skip this
                    // FIXME: Is this the right way to work?
                }
            }
        }
    }

}
