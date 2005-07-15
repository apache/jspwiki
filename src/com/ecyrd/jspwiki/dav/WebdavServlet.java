/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class WebdavServlet extends HttpServlet
{
    private static final long          serialVersionUID = 1L;

    private static final String METHOD_PROPPATCH = "PROPPATCH";
    private static final String METHOD_PROPFIND  = "PROPFIND";
    private static final String METHOD_MKCOL     = "MKCOL";
    private static final String METHOD_COPY      = "COPY";
    private static final String METHOD_MOVE      = "MOVE";
    private static final String METHOD_LOCK      = "LOCK";
    private static final String METHOD_UNLOCK    = "UNLOCK";
    
    public static final int SC_PROCESSING        = 102;
    public static final int SC_MULTISTATUS       = 207;
    public static final int SC_UNPROCESSABLE     = 422;
    public static final int SC_LOCKED            = 423;
    public static final int SC_FAILED_DEPENDENCY = 424;
    public static final int SC_INSUFFICIENT_STORAGE = 507;
    
    /**
     * 
     */
    public WebdavServlet()
    {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void doPropFind( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        
    }
    
    public void doPropPatch( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        
    }
    
    public void doMkCol( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        
    }
    
    public void doCopy( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        
    }
    
    public void doMove( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        
    }

    /**
     *  The default implementation of this class just returns an error code.
     * 
     * @param request
     * @param response
     */
    public void doLock( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        try
        {
            response.sendError( HttpServletResponse.SC_NOT_IMPLEMENTED, "Sorry" );
        }
        catch( IOException e ) {}
    }
    
    /**
     *  The default implementation of this class just returns an error code.
     * 
     * @param request
     * @param response
     */
    
    public void doUnlock( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        try
        {
            response.sendError( HttpServletResponse.SC_NOT_IMPLEMENTED, "Sorry" );
        }
        catch( IOException e ) {}        
    }
    
    protected void service( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        String method = request.getMethod();
        
        System.out.println("METHOD="+method+"; request="+request.getPathInfo() );
        
        try
        {
            if( METHOD_PROPPATCH.equals(method) )
            {
                doPropPatch( request, response );
            }
            else if( METHOD_PROPFIND.equals(method) )
            {
                doPropFind( request, response );
            }
            else if( METHOD_MKCOL.equals(method) )
            {
                doMkCol( request, response );
            }
            else if( METHOD_COPY.equals(method) )
            {
                doCopy( request, response );
            }
            else if( METHOD_MOVE.equals(method) )
            {
                doMove( request, response );
            }
            else if( METHOD_LOCK.equals(method) )
            {
                doLock( request, response );
            }
            else if( METHOD_UNLOCK.equals(method) )
            {
                doUnlock( request, response );
            }
            else if( "OPTIONS".equals(method) )
            {
                doOptions( request, response );
            }
            else
            {
                super.service( request, response );
            }
        }
        catch( Throwable t )
        {
            t.printStackTrace( System.out );

            throw new ServletException( t );
        }
    }
}
