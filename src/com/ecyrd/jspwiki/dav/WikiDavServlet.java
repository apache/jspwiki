/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.methods.DavMethod;
import com.ecyrd.jspwiki.dav.methods.GetMethod;
import com.ecyrd.jspwiki.dav.methods.PropFindMethod;
import com.ecyrd.jspwiki.dav.methods.PropPatchMethod;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class WikiDavServlet extends WebdavServlet
{
    private WikiEngine m_engine;
    Logger log = Logger.getLogger(this.getClass().getName());
    private DavProvider m_rawProvider;
    private DavProvider m_rootProvider;
    
    public void init( ServletConfig config )
    throws ServletException 
    {
        super.init( config );

        m_engine         = WikiEngine.getInstance( config );
        Properties props = m_engine.getWikiProperties();
        
        m_rawProvider    = new RawPagesDavProvider( m_engine );
        m_rootProvider   = new WikiRootProvider( m_engine );
    }
    
    private String parsePage( HttpServletRequest req )
    {
        return req.getContextPath();
    }
    
    public void doPropFind( HttpServletRequest req, HttpServletResponse res )
        throws IOException,ServletException
    {
        // Do the "sanitize url" trick
        String p = new String(req.getPathInfo().getBytes("ISO-8859-1"), "UTF-8");
        
        DavPath path = new DavPath( p );
        if( path.isRoot() )
        {
            DavMethod dm = new PropFindMethod( m_rootProvider );
            dm.execute( req, res, path );
        }
        else if( path.get(0).equals("raw") )
        {
            PropFindMethod m = new PropFindMethod( m_rawProvider );
            m.execute( req, res, path.subPath(1) );
        }        
    }
    
    protected void doOptions( HttpServletRequest req, HttpServletResponse res )
    {
        log.debug("DAV doOptions for path "+req.getPathInfo());
        
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
        //DavMethod dm = new PropPatchMethod( m_rawProvider );
        
        //dm.execute( request, response );
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
        // Do the "sanitize url" trick
        String p = new String(req.getPathInfo().getBytes("ISO-8859-1"), "UTF-8");
        
        DavPath path = new DavPath( p );
        
        if( path.isRoot() )
        {
            DavMethod dm = new GetMethod( m_rootProvider );
            dm.execute( req, res, path );
        }
        else if( path.get(0).equals("raw") )
        {
            DavMethod dm = new GetMethod( m_rawProvider );
        
            dm.execute( req, res, path.subPath(1) );
        }
    }
}
