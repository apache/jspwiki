/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.xmlrpc;

import java.io.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import org.apache.xmlrpc.XmlRpcServer;

import com.ecyrd.jspwiki.*;

/**
 *  Handles all incoming servlet requests for XML-RPC calls.
 *  
 *  @author Janne Jalkanen
 *  @since 1.6.6
 */
public class RPCServlet extends HttpServlet
{
    private WikiEngine       m_engine;
    private XmlRpcServer     m_xmlrpcServer = new XmlRpcServer();

    Category log = Category.getInstance( RPCServlet.class ); 

    public void init( ServletConfig config )
    {
        m_engine = WikiEngine.getInstance( config );

        RPCHandler rpchandler = new RPCHandler( m_engine );

        m_xmlrpcServer.addHandler( "jspwiki", rpchandler );
    }

    public void doPost( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received POST to RPCServlet");

        try
        {
            byte[] result = m_xmlrpcServer.execute( request.getInputStream() );

            response.setContentType( "text/xml" );
            response.setContentLength( result.length );
        
            OutputStream out = response.getOutputStream();
            out.write( result );
            out.flush();

            log.debug("Result = "+new String(result) );
        }
        catch( IOException e )
        {
            throw new ServletException("Failed to build RPC result", e);
        }
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException
    {
        log.debug("Received HTTP GET to RPCServlet");

        try
        {
            String msg = "We do not support HTTP GET here.  Sorry.";
            response.setContentType( "text/plain" );
            response.setContentLength( msg.length() );
        
            PrintWriter writer = new PrintWriter( new OutputStreamWriter( response.getOutputStream() ) );

            writer.println( msg );
            writer.flush();
        }
        catch( IOException e )
        {
            throw new ServletException("Failed to build RPC result", e);
        }
    }

}
