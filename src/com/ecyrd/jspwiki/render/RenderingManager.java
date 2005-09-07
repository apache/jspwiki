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
    private WikiEngine m_engine;

    private int m_cacheExpiryPeriod = 24*60*60; // This can be relatively long
    private static Logger log = Logger.getLogger( RenderingManager.class );
    
    /**
     *  Creates a new unlimited cache.  A good question is, whether this
     *  cache should be limited - at the moment it will just keep on growing,
     *  if the page is never accessed.
     */
    // FIXME: Memory leak
    private Cache m_documentCache = new Cache(true,false,false); 
    
    public void initialize( WikiEngine engine, Properties properties )
    {
        m_engine = engine;
    }
    
    public MarkupParser getParser( WikiContext context, String pagedata )
    {
        MarkupParser parser = new JSPWikiMarkupParser( context, new StringReader(pagedata) );
        
        return parser;
    }
    
    /**
     *  Returns a cached object, if one is found.
     *  
     * @param context
     * @param pagedata
     * @return
     * @throws IOException
     */
    // FIXME: The cache management policy is not very good: deleted/changed pages
    //        should be detected better.
    protected WikiDocument getRenderedDocument( WikiContext context, String pagedata )
        throws IOException
    {
        String pagename = context.getRealPage().getName();
        
        try
        {
            WikiDocument doc = (WikiDocument) m_documentCache.getFromCache( pagename, 
                                                                            m_cacheExpiryPeriod );
            
            //
            //  This check is needed in case the different filters have actually
            //  changed the page data.
            //  FIXME: Figure out a faster method
            if( !pagedata.equals(doc.getPageData()) )
                throw new NeedsRefreshException(doc);
            
            if( log.isDebugEnabled() ) log.debug("Using cached HTML for page "+pagename );
            return doc;
        }
        catch( NeedsRefreshException e )
        {
            if( log.isDebugEnabled() ) log.debug("Re-rendering and storing "+pagename );

            MarkupParser parser = getParser( context, pagedata );

            try
            {
                WikiDocument doc = parser.parse();
                doc.setPageData( pagedata );
                
                m_documentCache.putInCache( pagename, doc );
                
                return doc;
            }
            catch( IOException ex )
            {
                log.error("Unable to parse",ex);
            }
        }
        
        return null;
    }
    
    public String getHTML( WikiContext context, WikiDocument doc )
        throws IOException
    {
        WikiRenderer rend = new XHTMLRenderer( context, doc );
        
        return rend.getString();
    }

    /**
     *   Convinience method for rendering, using the default parser and renderer.
     *   
     *  @param context
     *  @param pagedata
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
        
        m_documentCache.flushEntry( pageName );
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
