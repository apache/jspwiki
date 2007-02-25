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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.tags.WikiTagBase;

/**
 *  Provides basic filtering for JSPWiki.  This filter only makes sure
 *  JSPWiki is running, and also does a bunch of sanitychecks.
 *  
 *  @author Janne Jalkanen
 *
 */
public class WikiServletFilter implements Filter
{
    protected static final Logger log = Logger.getLogger( WikiServletFilter.class );
    protected WikiEngine m_engine = null;

    public WikiServletFilter()
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
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        //
        //  Sanity check; it might be true in some conditions, but we need to know where.
        //
        if( chain == null )
        {
            throw new ServletException("FilterChain is null, even if it should not be.  Please report this to the jspwiki development team.");
        }
        
        if( m_engine == null )
        {
            PrintWriter out = response.getWriter();
            out.print("<html><head><title>Fatal problem with JSPWiki</title></head>");
            out.print("<body>");
            out.print("<h1>JSPWiki has not been started</h1>");
            out.print("<p>JSPWiki is not running.  This is probably due to a configuration error in your jspwiki.properties file, ");
            out.print("or a problem with your servlet container.  Please double-check everything before issuing a bug report ");
            out.print("at jspwiki.org.</p>");
            out.print("<p>We apologize for the inconvenience.  No, really, we do.  We're trying to ");
            out.print("JSPWiki as easy as we can, but there is only so much we have time to test ");
            out.print("platforms.</p>");
            out.print("</body>");
            return;
        }   
        
        // Write the response to a dummy response because we want to 
        //   replace markers with scripts/stylesheet. 
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        httpRequest.setCharacterEncoding( m_engine.getContentEncoding() );

        try
        {
            NDC.push( m_engine.getApplicationName()+":"+httpRequest.getRequestURL() );
            
            chain.doFilter( request, response );
        }
        finally
        {
            NDC.pop();
            NDC.remove();
        }

    }


    /**
     *  Figures out the wiki context from the request.  This method does not create the
     *  context if it does not exist.
     *  
     *  @param request The request to examine
     *  @return A valid WikiContext value (or null, if the context could not be located).
     */
    protected WikiContext getWikiContext(ServletRequest  request)
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
    
        WikiContext ctx = (WikiContext) httpRequest.getAttribute( WikiTagBase.ATTR_CONTEXT );
        
        return ctx;
    }

}
