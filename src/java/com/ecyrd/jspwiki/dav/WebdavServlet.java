/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.dav;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
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
        
        // System.out.println("METHOD="+method+"; request="+request.getPathInfo() );
        
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
