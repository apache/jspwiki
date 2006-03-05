package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.filters.PageFilter;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 *  This class provides a facade towards the differing rendering routines.  You should
 *  use the routines in this manager instead of the ones in WikiEngine, if you don't
 *  want the different side effects to occur - such as WikiFilters.
 *  
 *  @author jalkanen
 *
 */
public class RenderingManager implements PageFilter
{
    private int m_cacheExpiryPeriod = 24*60*60; // This can be relatively long
    private static Logger log = Logger.getLogger( RenderingManager.class );

    public static final String PROP_USECACHE = "jspwiki.renderingManager.useCache";
 
    /** 
     * True if RenderingManager should cache built DOM trees. 
     * The default is true.
     * Set jspwiki.renderingManager.useCache in the properties to false
     * to prevent this.
     */
    private boolean m_useCache = true;
   
    /**
     *  Creates a new unlimited cache.  A good question is, whether this
     *  cache should be limited - at the moment it will just keep on growing,
     *  if the page is never accessed.
     */
    // FIXME: Memory leak
    private Cache m_documentCache;
    
    public void initialize( WikiEngine engine, Properties properties )
    {
        String s = properties.getProperty( PROP_USECACHE );
        s = (s == null ? "true" : s);
        Boolean b = new Boolean( s );
        m_useCache = b.booleanValue();
        if( m_useCache ) 
        {
            m_documentCache = new Cache(true,false,false); 
        }
        else
        {
            log.info( "RenderingManager caching is disabled." );
        }
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
     *  Returns a cached object, if one is found.
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

        if( m_useCache ) 
        {
            try
            {
                WikiDocument doc = (WikiDocument) m_documentCache.getFromCache( pageid, 
                                                                                m_cacheExpiryPeriod );
                
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

        MarkupParser parser = getParser( context, pagedata );
        try
        {
            WikiDocument doc = parser.parse();
            doc.setPageData( pagedata );
            if( m_useCache ) 
            {
                m_documentCache.putInCache( pageid, doc );
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
     *  from the cache - in fact, it does not cache the document at all.
     *  
     *  @param context The WikiContext to render in
     *  @param doc A proper WikiDocument
     *  @return Rendered HTML.
     *  @throws IOException If the WikiDocument is poorly formed. 
     */
    public String getHTML( WikiContext context, WikiDocument doc )
        throws IOException
    {
        WikiRenderer rend = new XHTMLRenderer( context, doc );
        
        return rend.getString();
    }

    /**
     *   Convinience method for rendering, using the default parser and renderer.  Note that
     *   you can't use this method to do any arbitrary rendering, as the pagedata MUST
     *   be the data from the that the WikiContext refers to - this method caches the HTML
     *   internally, and will return the cached version.  If the pagedata is different
     *   from what was cached, will re-render and store the pagedata into the internal cache.
     *   
     *  @param context the wiki context
     *  @param pagedata the page data
     *  @return XHTML data.
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

    public void postSave( WikiContext wikiContext, String content ) throws FilterException
    {
        String pageName = wikiContext.getPage().getName();
        if( m_useCache )
        {
            m_documentCache.flushEntry( pageName );
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
