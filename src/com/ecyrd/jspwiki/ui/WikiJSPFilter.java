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
package com.ecyrd.jspwiki.ui;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.event.*;
import com.ecyrd.jspwiki.util.WatchDog;

/**
 * This filter goes through the generated page response prior and
 * places requested resources at the appropriate inclusion markers.
 * This is done to let dynamic content (e.g. plugins, editors) 
 * include custom resources, even after the HTML head section is
 * in fact built. This filter is typically the last filter to execute,
 * and it <em>must</em> run after servlet or JSP code that performs
 * redirections or sends error codes (such as access control methods).
 * <p>
 * Inclusion markers are placed by the IncludeResourcesTag; the
 * defult content templates (see .../templates/default/commonheader.jsp)
 * are configured to do this. As an example, a JavaScript resource marker
 * is added like this:
 * <pre>
 * &lt;wiki:IncludeResources type="script"/&gt;
 * </pre>
 * Any code that requires special resources must register a resource
 * request with the TemplateManager. For example:
 * <pre>
 * &lt;wiki:RequestResource type="script" path="scripts/custom.js" /&gt;
 * </pre>
 * or programmatically,
 * <pre>
 * TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT, "scripts/customresource.js" );
 * </pre>
 * 
 * @see TemplateManager
 * @see com.ecyrd.jspwiki.tags.RequestResourceTag
 */
public class WikiJSPFilter implements Filter
{
    protected static final Logger log = Logger.getLogger( WikiJSPFilter.class );
    protected WikiEngine m_engine = null;

    public WikiJSPFilter()
    {
        super();
    }
    
    public void init(FilterConfig config) throws ServletException
    {
        ServletContext context = config.getServletContext();
        m_engine = WikiEngine.getInstance( context, null );
    }

    public void destroy()
    {
        log.info("WikiJSPFilter destroyed; telling WikiEngine to stop..");
        m_engine.shutdown();
    }
    
    public void doFilter( ServletRequest  request,
                          ServletResponse response,
                          FilterChain     chain )
        throws ServletException, IOException
    {
        WatchDog w = m_engine.getCurrentWatchDog();
        try
        {
            NDC.push( m_engine.getApplicationName()+":"+((HttpServletRequest)request).getRequestURI() );

            w.enterState("Filtering for URL "+((HttpServletRequest)request).getRequestURI(), 90 );
          
            HttpServletResponseWrapper responseWrapper = new MyServletResponseWrapper( (HttpServletResponse)response );
        
            // fire PAGE_REQUESTED event
            WikiActionBean actionBean = getWikiActionBean( request );
            boolean isWikiContext = ( actionBean instanceof WikiContext );
            if ( isWikiContext )
            {
                String pageName = ((WikiContext)actionBean).getPage().getName();
                fireEvent( WikiPageEvent.PAGE_REQUESTED, pageName );
            }

            chain.doFilter( request, responseWrapper );

            // The response is now complete. Lets replace the markers now.
        
            try
            {
                w.enterState( "Delivering response", 30 );
                String r = filter( actionBean, responseWrapper );
        
                //String encoding = "UTF-8";
                //if( wikiContext != null ) encoding = wikiContext.getEngine().getContentEncoding();
        
                // Only now write the (real) response to the client.
                // response.setContentLength(r.length());
                // response.setContentType(encoding);
                
                response.getWriter().write(r);
            
                // Clean up the UI messages and loggers
                actionBean.getWikiSession().clearMessages();

                // fire PAGE_DELIVERED event
                if ( isWikiContext )
                {
                    String pageName = ((WikiContext)actionBean).getPage().getName();
                    fireEvent( WikiPageEvent.PAGE_DELIVERED, pageName );
                }

            }
            finally
            {
                w.exitState();
            }
        }
        finally
        {
            w.exitState();
            NDC.pop();
            NDC.remove();
        }
    }

    /**
     * Goes through all types and writes the appropriate response.
     * 
     * @param actionBean The action bean for the current context
     * @param string The source string
     * @return The modified string with all the insertions in place.
     */
    private String filter(WikiActionBean actionBean, HttpServletResponse response )
    {
        String string = response.toString();

        if( actionBean != null )
        {
            String[] resourceTypes = TemplateManager.getResourceTypes( actionBean );

            for( int i = 0; i < resourceTypes.length; i++ )
            {
                string = insertResources( actionBean, string, resourceTypes[i] );
            }
        
            //
            //  Add HTTP header Resource Requests
            //
            String[] headers = TemplateManager.getResourceRequests( actionBean,
                                                                    TemplateManager.RESOURCE_HTTPHEADER );
        
            for( int i = 0; i < headers.length; i++ )
            {
                String key = headers[i];
                String value = "";
                int split = headers[i].indexOf(':');
                if( split > 0 && split < headers[i].length()-1 )
                {
                    key = headers[i].substring( 0, split );
                    value = headers[i].substring( split+1 );
                }
            
                response.addHeader( key.trim(), value.trim() );
            }
        }

        return string;
    }

    /**
     *  Inserts whatever resources
     *  were requested by any plugins or other components for this particular
     *  type.
     *  
     *  @param actionBean The action bean for the current context
     *  @param string The source string
     *  @param type Type identifier for insertion
     *  @return The filtered string.
     */
    private String insertResources( WikiActionBean actionBean, String string, String type )
    {
        if( actionBean == null )
        {
            return string;
        }

        String marker = TemplateManager.getMarker( actionBean, type );
        int idx = string.indexOf( marker );
        
        if( idx == -1 )
        {
            return string;
        }
        
        log.debug("...Inserting...");
        
        String[] resources = TemplateManager.getResourceRequests( actionBean, type );
        
        StringBuffer concat = new StringBuffer( resources.length * 40 );
        
        for( int i = 0; i < resources.length; i++  )
        {
            log.debug("...:::"+resources[i]);
            concat.append( resources[i] );
        }

        string = TextUtil.replaceString( string, 
                                         idx, 
                                         idx+marker.length(), 
                                         concat.toString() );
        
        return string;
    }
    
    /**
     *  Simple response wrapper that just allows us to gobble through the entire
     *  response before it's output.
     *  
     *  @author jalkanen
     */
    private static class MyServletResponseWrapper
        extends HttpServletResponseWrapper
    {
        private CharArrayWriter m_output;
      
        /** 
         *  How large the initial buffer should be.  This should be tuned to achieve
         *  a balance in speed and memory consumption.
         */
        private static final int INIT_BUFFER_SIZE = 4096;
        
        public MyServletResponseWrapper( HttpServletResponse r )
        {
            super(r);
            m_output = new CharArrayWriter( INIT_BUFFER_SIZE );
        }

        /**
         *  Returns a writer for output; this wraps the internal buffer
         *  into a PrintWriter.
         */
        public PrintWriter getWriter()
        {
            return new PrintWriter( m_output );
        }

        public ServletOutputStream getOutputStream()
        {
            return new MyServletOutputStream( m_output );
        }

        class MyServletOutputStream extends ServletOutputStream
        {
            CharArrayWriter m_buffer;

            public MyServletOutputStream(CharArrayWriter aCharArrayWriter)
            {
                super();
                m_buffer = aCharArrayWriter;
            }

            public void write(int aInt)
            {
                m_buffer.write( aInt );
            }

        }
        
        /**
         *  Returns whatever was written so far into the Writer.
         */
        public String toString()
        {
            return m_output.toString();
        }
    }

    /**
     *  Looks up the WikiActionBean stored in the request.  This method does not create the
     *  action bean if it does not exist.
     *  
     *  @param request The request to examine
     *  @return A valid WikiActionBean, or <code>null</code> if one could not be located
     */
    protected WikiContext getWikiActionBean(ServletRequest  request)
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
    
        WikiContext ctx = (WikiContext) httpRequest.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        
        return ctx;
    }

    // events processing .......................................................


    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners of the current WikiEngine.
     *
     * @see com.ecyrd.jspwiki.event.WikiPageEvent
     * @param type       the event type to be fired
     * @param pagename   the wiki page name as a String
     */
    protected final void fireEvent( int type, String pagename )
    {
        if ( WikiEventManager.isListening(m_engine) )
        {
            WikiEventManager.fireEvent(m_engine,new WikiPageEvent(m_engine,type,pagename));
        }
    }

}
