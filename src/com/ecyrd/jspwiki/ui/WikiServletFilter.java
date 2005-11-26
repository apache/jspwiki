package com.ecyrd.jspwiki.ui;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.tags.WikiTagBase;


public class WikiServletFilter implements Filter
{
    protected static Logger log = Logger.getLogger( WikiServletFilter.class );
    
    public void init(FilterConfig arg0) throws ServletException
    {
    }

    public void destroy()
    {
        // TODO Auto-generated method stub
        
    }

    public void doFilter( ServletRequest  request,
                          ServletResponse response,
                          FilterChain     chain )
        throws ServletException, IOException
    {
        // Write the response to a dummy response because we want to 
        //   replace markers with scripts/stylesheet. 
        ServletResponseWrapper responseWrapper = new MyServletResponseWrapper( (HttpServletResponse)response );
        chain.doFilter( request, responseWrapper );

        // The response is now complete. Lets replace the markers now.
        
        // WikiContext is only available after doFilter! (That is after
        //   interpreting the jsp)

        WikiContext wikiContext = getWikiContext( request );
        String r = filter( wikiContext, responseWrapper.toString() );
        byte[] bytes = r.getBytes(); // TODO: Encoding?

        // Only now write the (real) response to the client.
        response.setContentLength( bytes.length );
        response.getOutputStream().write( bytes );
    }

    private WikiContext getWikiContext(ServletRequest  request)
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        WikiContext ctx = (WikiContext) httpRequest.getAttribute( WikiTagBase.ATTR_CONTEXT );
        
        return ctx;
    }

    /**
     * Goes through all types.
     * 
     * @param wikiContext
     * @param string
     * @return
     */
    private String filter(WikiContext wikiContext, String string )
    {
        String[] resourceTypes = TemplateManager.getResourceTypes( wikiContext );
        
        for( int i = 0; i < resourceTypes.length; i++ )
        {
            string = filter( wikiContext, string, resourceTypes[i] );
        }
        
        return string;
    }

    private String filter(WikiContext wikiContext, String string, String type )
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
    
    private class MyServletResponseWrapper
        extends HttpServletResponseWrapper
    {
        private CharArrayWriter output;
        
        public MyServletResponseWrapper( HttpServletResponse r )
        {
            super(r);
            output = new CharArrayWriter();
        }

        public PrintWriter getWriter()
        {
            return new PrintWriter(output);
        }

        public String toString()
        {
            return output.toString();
        }
    }
}
