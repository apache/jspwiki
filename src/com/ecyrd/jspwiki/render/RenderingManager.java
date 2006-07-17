/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.filters.FilterManager;
import com.ecyrd.jspwiki.filters.PageFilter;
import com.ecyrd.jspwiki.modules.InternalModule;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.providers.CachingProvider;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

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
 *   
 *  @author jalkanen
 *  @since  2.4
 */
public class RenderingManager implements PageFilter, InternalModule
{
    private static Logger log = Logger.getLogger( RenderingManager.class );

    private              int    m_cacheExpiryPeriod = 24*60*60; // This can be relatively long

    public  static final String PROP_CACHESIZE    = "jspwiki.renderingManager.capacity";    
    private static final int    DEFAULT_CACHESIZE = 1000;
    private static final String OSCACHE_ALGORITHM = "com.opensymphony.oscache.base.algorithm.LRUCache";
    private static final String PROP_RENDERER     = "jspwiki.renderingManager.renderer";
    public  static final String DEFAULT_RENDERER  = XHTMLRenderer.class.getName();

    /**
     *  Stores the WikiDocuments that have been cached.
     */
    private              Cache  m_documentCache;

    /**
     * 
     */
    private         Constructor m_rendererConstructor;
    
    /**
     *  Initializes the RenderingManager.
     *  Checks for cache size settings, initializes the document cache.
     *  Looks for alternative WikiRenderers, initializes one, or the default
     *  XHTMLRenderer, for use.
     *  
     *  @param engine A WikiEngine instance.
     *  @param properties A list of properties to get parameters from.
     */
    public void initialize( WikiEngine engine, Properties properties )
    throws WikiException
    {
        int cacheSize = TextUtil.getIntegerProperty( properties, PROP_CACHESIZE, -1 );
            
        if( cacheSize == -1 )
        {
            cacheSize = TextUtil.getIntegerProperty( properties, 
                                                     CachingProvider.PROP_CACHECAPACITY, 
                                                     DEFAULT_CACHESIZE );
        }
        
        if( cacheSize > 0 )
        {
            m_documentCache = new Cache(true,false,false,false,
                                        OSCACHE_ALGORITHM,
                                        cacheSize);
        }
        else
        {
            log.info( "RenderingManager caching is disabled." );
        }

        String renderImplName = properties.getProperty( PROP_RENDERER );
        if( renderImplName == null ) {
            renderImplName = DEFAULT_RENDERER;
        }
        Class[] rendererParams = { WikiContext.class, WikiDocument.class };
        try 
        {
            Class c = Class.forName( renderImplName );
            m_rendererConstructor = c.getConstructor( rendererParams );
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
        
        engine.getFilterManager().addPageFilter( this, FilterManager.SYSTEM_FILTER_PRIORITY );
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
     * @throws IOException
     */
    // FIXME: The cache management policy is not very good: deleted/changed pages
    //        should be detected better.
    protected WikiDocument getRenderedDocument( WikiContext context, String pagedata )
        throws IOException
    {
        String pageid = context.getRealPage().getName()+"::"+context.getRealPage().getVersion();

        boolean wasUpdated = false;
        
        if( m_documentCache != null ) 
        {
            try
            {
                WikiDocument doc = (WikiDocument) m_documentCache.getFromCache( pageid, 
                                                                                m_cacheExpiryPeriod );

                wasUpdated = true;
                
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
            catch( NeedsRefreshException e )
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
                m_documentCache.putInCache( pageid, doc );
                wasUpdated = true;
            }
            return doc;
        }
        catch( IOException ex )
        {
            log.error("Unable to parse",ex);
        }
        finally
        {
            if( m_documentCache != null && !wasUpdated ) m_documentCache.cancelUpdate( pageid );
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
     */
    public WikiRenderer getRenderer( WikiContext context, WikiDocument doc ) 
    {
        Object[] params = { context, doc };
        WikiRenderer rval = null;
        try {
            rval = (WikiRenderer)m_rendererConstructor.newInstance( params );
        } catch( Exception e ) {
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

    //
    //  The following methods are for the PageFilter interface
    //
    public void initialize( Properties properties ) throws FilterException
    {
    }

    /**
     *  Flushes the cache objects that refer to this page.
     */
    public void postSave( WikiContext wikiContext, String content ) throws FilterException
    {
        String pageName = wikiContext.getPage().getName();
        if( m_documentCache != null )
        {
            m_documentCache.flushPattern( pageName );
            Set referringPages = wikiContext.getEngine().getReferenceManager().findReferredBy( pageName );
            
            //
            //  Flush also those pages that refer to this page (if an nonexistant page
            //  appears; we need to flush the HTML that refers to the now-existant page
            //
            if( referringPages != null )
            {
                Iterator i = referringPages.iterator();
                while (i.hasNext())
                {
                    String page = (String) i.next();
                    log.debug( "Flusing " + page );
                    m_documentCache.flushPattern( page );
                }
            }
        }
    }

    public String postTranslate( WikiContext wikiContext, String htmlContent ) throws FilterException
    {
        return htmlContent;
    }

    public String preSave( WikiContext wikiContext, String content ) throws FilterException
    {
        return content;
    }

    public String preTranslate( WikiContext wikiContext, String content ) throws FilterException
    {
        return content;
    }
}
