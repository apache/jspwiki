/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.dav.methods.DavMethod;
import com.ecyrd.jspwiki.dav.methods.GetMethod;
import com.ecyrd.jspwiki.dav.methods.PropFindMethod;
import com.ecyrd.jspwiki.dav.methods.PropPatchMethod;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class WikiDavServlet extends WebdavServlet
{
    private WikiEngine m_engine;
    Logger log = Logger.getLogger(this.getClass().getName());

    public void init( ServletConfig config )
    throws ServletException 
    {
        super.init( config );

        m_engine         = WikiEngine.getInstance( config );
        Properties props = m_engine.getWikiProperties();
    }
    
    private String parsePage( HttpServletRequest req )
    {
        return req.getContextPath();
    }
    
    public void doPropFind( HttpServletRequest req, HttpServletResponse res )
    throws IOException,ServletException
    {
        PropFindMethod m = new PropFindMethod( m_engine );
        
        m.execute( req, res );
    }
    
    protected void doOptions( HttpServletRequest req, HttpServletResponse res )
    {
        res.setHeader( "DAV", "1" ); // We support only Class 1
        res.setHeader( "Allow", "GET, PUT, POST, OPTIONS, PROPFIND, PROPPATCH, MOVE, COPY, DELETE");
        res.setStatus( HttpServletResponse.SC_OK );
    }
    
 
    public void doMkCol( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        if( request.getContentLength() > 0 )
        {
            response.sendError( HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Message may contain no body" );
        }
        else
        {
            response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );
        }
    }
    
    
    
    public void doPropPatch( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        DavMethod dm = new PropPatchMethod( m_engine );
        
        dm.execute( request, response );
    }
    
    public void doCopy( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );
    }

    public void doMove( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );

    }

    protected void doDelete( HttpServletRequest arg0, HttpServletResponse response ) throws ServletException, IOException
    {
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );
    }
    
    protected void doPost( HttpServletRequest arg0, HttpServletResponse response ) throws ServletException, IOException
    {
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );
    }

    protected void doPut( HttpServletRequest arg0, HttpServletResponse response ) throws ServletException, IOException
    {
        response.sendError( HttpServletResponse.SC_UNAUTHORIZED, "JSPWiki is read-only." );
    }
 
    /*
     * GET /dav/raw/WikiPage.txt
     * GET /dav/html/WikiPage.html
     * GET /dav/pdf/WikiPage.pdf
     * GET /dav/raw/WikiPage/attachment1.png
     * 
     */
    protected void doGet( HttpServletRequest req, HttpServletResponse res ) 
    throws ServletException, IOException
    {
        DavMethod dm = new GetMethod( m_engine );
        
        dm.execute( req, res );
    }
}
