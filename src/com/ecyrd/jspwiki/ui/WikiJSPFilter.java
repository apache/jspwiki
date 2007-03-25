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

import org.apache.log4j.NDC;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
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
public class WikiJSPFilter extends WikiServletFilter
{
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
          
            ServletResponseWrapper responseWrapper = new MyServletResponseWrapper( (HttpServletResponse)response );
        
            super.doFilter( request, responseWrapper, chain );

            // The response is now complete. Lets replace the markers now.
        
            // WikiContext is only available after doFilter! (That is after
            //   interpreting the jsp)

            try
            {
                w.enterState( "Delivering response", 30 );
                WikiContext wikiContext = getWikiContext( request );
                String r = filter( wikiContext, responseWrapper.toString() );
        
                //String encoding = "UTF-8";
                //if( wikiContext != null ) encoding = wikiContext.getEngine().getContentEncoding();
        
                // Only now write the (real) response to the client.
                // response.setContentLength(r.length());
                // response.setContentType(encoding);
                response.getWriter().write(r);
            
                // Clean up the UI messages and loggers
                if ( wikiContext != null )
                {
                    wikiContext.getWikiSession().clearMessages();
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
     * Goes through all types.
     * 
     * @param wikiContext The usual processing context
     * @param string The source string
     * @return The modified string with all the insertions in place.
     */
    private String filter(WikiContext wikiContext, String string )
    {
        String[] resourceTypes = TemplateManager.getResourceTypes( wikiContext );
        
        for( int i = 0; i < resourceTypes.length; i++ )
        {
            string = insertResources( wikiContext, string, resourceTypes[i] );
        }
        
        return string;
    }

    /**
     *  Inserts whatever resources
     *  were requested by any plugins or other components for this particular
     *  type.
     *  
     *  @param wikiContext The usual processing context
     *  @param string The source string
     *  @param type Type identifier for insertion
     *  @return The filtered string.
     */
    private String insertResources(WikiContext wikiContext, String string, String type )
    {
        if( wikiContext == null )
        {
            return string;
        }

        String marker = TemplateManager.getMarker( type );
        int idx = string.indexOf( marker );
        
        if( idx == -1 )
        {
            return string;
        }
        
        log.debug("...Inserting...");
        
        String[] resources = TemplateManager.getResourceRequests( wikiContext, type );
        
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
        private int INIT_BUFFER_SIZE = 4096;
        
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
            CharArrayWriter buffer;

            public MyServletOutputStream(CharArrayWriter aCharArrayWriter)
            {
                super();
                buffer = aCharArrayWriter;
            }

            public void write(int aInt)
            {
                buffer.write( aInt );
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
}
