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
package org.apache.wiki.ui;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.NDC;

import org.apache.wiki.WikiContext;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.url.DefaultURLConstructor;
import org.apache.wiki.util.UtilJ2eeCompat;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.util.WatchDog;

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
 * @see org.apache.wiki.tags.RequestResourceTag
 */
public class WikiJSPFilter extends WikiServletFilter
{
    private Boolean m_useOutputStream;

    /** {@inheritDoc} */
    public void init( FilterConfig config ) throws ServletException
    {
        super.init( config );
        ServletContext context = config.getServletContext();
        m_useOutputStream = UtilJ2eeCompat.useOutputStream( context.getServerInfo() );
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
            HttpServletResponseWrapper responseWrapper;
         
            if( m_useOutputStream )
            {
                log.debug( "Using ByteArrayResponseWrapper" );
                responseWrapper = new ByteArrayResponseWrapper( (HttpServletResponse)response );
            }
            else
            {
                log.debug( "Using MyServletResponseWrapper" );
                responseWrapper = new MyServletResponseWrapper( (HttpServletResponse)response );
                
            }
        
            // fire PAGE_REQUESTED event
            String pagename = DefaultURLConstructor.parsePageFromURL(
                    (HttpServletRequest)request, response.getCharacterEncoding() );
            fireEvent( WikiPageEvent.PAGE_REQUESTED, pagename );

            super.doFilter( request, responseWrapper, chain );

            // The response is now complete. Lets replace the markers now.
        
            // WikiContext is only available after doFilter! (That is after
            //   interpreting the jsp)

            try
            {
                w.enterState( "Delivering response", 30 );
                WikiContext wikiContext = getWikiContext( request );
                String r = filter( wikiContext, responseWrapper );
        
                //String encoding = "UTF-8";
                //if( wikiContext != null ) encoding = wikiContext.getEngine().getContentEncoding();
        
                // Only now write the (real) response to the client.
                // response.setContentLength(r.length());
                // response.setContentType(encoding);
                
                response.getWriter().write(r);
            
                // Clean up the UI messages and loggers
                if( wikiContext != null )
                {
                    wikiContext.getWikiSession().clearMessages();
                }

                // fire PAGE_DELIVERED event
                fireEvent( WikiPageEvent.PAGE_DELIVERED, pagename );

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
     * @param wikiContext The usual processing context
     * @param string The source string
     * @return The modified string with all the insertions in place.
     */
    private String filter(WikiContext wikiContext, HttpServletResponse response )
    {
        String string = response.toString();

        if( wikiContext != null )
        {
            String[] resourceTypes = TemplateManager.getResourceTypes( wikiContext );

            for( int i = 0; i < resourceTypes.length; i++ )
            {
                string = insertResources( wikiContext, string, resourceTypes[i] );
            }
        
            //
            //  Add HTTP header Resource Requests
            //
            String[] headers = TemplateManager.getResourceRequests( wikiContext,
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

        String marker = TemplateManager.getMarker( wikiContext, type );
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

        static class MyServletOutputStream extends ServletOutputStream
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
     *  Response wrapper for application servers which do not work with CharArrayWriter
     *  Currently only OC4J
     */
    private static class ByteArrayResponseWrapper
        extends HttpServletResponseWrapper
    {
        private ByteArrayOutputStream m_output;
        private HttpServletResponse m_response;
      
        /** 
         *  How large the initial buffer should be.  This should be tuned to achieve
         *  a balance in speed and memory consumption.
         */
        private static final int INIT_BUFFER_SIZE = 4096;
        
        public ByteArrayResponseWrapper( HttpServletResponse r )
        {
            super(r);
            m_output = new ByteArrayOutputStream( INIT_BUFFER_SIZE );
            m_response = r;
        }
        
        /**
         *  Returns a writer for output; this wraps the internal buffer
         *  into a PrintWriter.
         */
        public PrintWriter getWriter()
        {
            return new PrintWriter( getOutputStream(), true );
        }

        public ServletOutputStream getOutputStream()
        {
            return new MyServletOutputStream( m_output );
        }

        static class MyServletOutputStream extends ServletOutputStream
        {
            private DataOutputStream m_stream;

            public MyServletOutputStream( OutputStream aOutput )
            {
                super();
                m_stream = new DataOutputStream( aOutput );
            }

            public void write( int aInt ) throws IOException
            {
                m_stream.write( aInt );
            }
        }
        
        /**
         *  Returns whatever was written so far into the Writer.
         */
        public String toString()
        {
            try
            {
                return m_output.toString( m_response.getCharacterEncoding() );
            }
            catch( UnsupportedEncodingException e )
            {
                log.error( ByteArrayResponseWrapper.class + " Unsupported Encoding", e );
                return null;
            }
        }
    }

    // events processing .......................................................


    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners of the current WikiEngine.
     *
     * @see org.apache.wiki.event.WikiPageEvent
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
