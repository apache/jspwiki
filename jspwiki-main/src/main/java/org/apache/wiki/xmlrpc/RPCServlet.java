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
package org.apache.wiki.xmlrpc;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.xmlrpc.ContextXmlRpcHandler;
import org.apache.xmlrpc.Invoker;
import org.apache.xmlrpc.XmlRpcContext;
import org.apache.xmlrpc.XmlRpcHandlerMapping;
import org.apache.xmlrpc.XmlRpcServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;

/**
 *  Handles all incoming servlet requests for XML-RPC calls.
 *  <P>
 *  Uses two initialization parameters:
 *  <UL>
 *  <LI><B>handler</B> : the class which is used to handle the RPC calls.
 *  <LI><B>prefix</B> : The command prefix for that particular handler.
 *  </UL>
 *
 *  @since 1.6.6
 */
public class RPCServlet extends HttpServlet
{
    private static final long serialVersionUID = 3976735878410416180L;

    /** This is what is appended to each command, if the handler has
        not been specified.  */
    // FIXME: Should this be $default?
    public static final String XMLRPC_PREFIX = "wiki";

    private WikiEngine       m_engine;
    private XmlRpcServer     m_xmlrpcServer = new XmlRpcServer();

    private static final Logger log = Logger.getLogger( RPCServlet.class );

    public void initHandler( final String prefix, final String handlerName ) throws ClassNotFoundException {
        /*
        Class handlerClass = Class.forName( handlerName );
        WikiRPCHandler rpchandler = (WikiRPCHandler) handlerClass.newInstance();
        rpchandler.initialize( m_engine );
        m_xmlrpcServer.addHandler( prefix, rpchandler );
        */
        final Class< ? > handlerClass = Class.forName( handlerName );
        m_xmlrpcServer.addHandler( prefix, new LocalHandler(handlerClass) );
    }

    /**
     *  Initializes the servlet.
     */
    public void init( final ServletConfig config ) throws ServletException {
        m_engine = WikiEngine.getInstance( config );

        String handlerName = config.getInitParameter( "handler" );
        String prefix      = config.getInitParameter( "prefix" );

        if( handlerName == null ) {
            handlerName = "org.apache.wiki.xmlrpc.RPCHandler";
        }
        if( prefix == null ) {
            prefix = XMLRPC_PREFIX;
        }

        try {
            initHandler( prefix, handlerName );

            //
            // FIXME: The metaweblog API should be possible to turn off.
            //
            initHandler( "metaWeblog", "org.apache.wiki.xmlrpc.MetaWeblogHandler" );
        } catch( final Exception e ) {
            log.fatal("Unable to start RPC interface: ", e);
            throw new ServletException( "No RPC interface", e );
        }
    }

    /**
     *  Handle HTTP POST.  This is an XML-RPC call, and we'll just forward the query to an XmlRpcServer.
     */
    public void doPost( final HttpServletRequest request, final HttpServletResponse response ) throws ServletException {
        log.debug("Received POST to RPCServlet");

        try {
            final WikiContext ctx = new WikiContext( m_engine, request, WikiContext.NONE );
            final XmlRpcContext xmlrpcContext = new WikiXmlRpcContext( m_xmlrpcServer.getHandlerMapping(), ctx );
            final byte[] result = m_xmlrpcServer.execute( request.getInputStream(), xmlrpcContext );

            //
            //  I think it's safe to write the output as UTF-8: The XML-RPC standard never creates other than USASCII
            //  (which is UTF-8 compatible), and our special UTF-8 hack just creates UTF-8.  So in all cases our butt
            //  should be covered.
            //
            response.setContentType( "text/xml; charset=utf-8" );
            response.setContentLength( result.length );

            final OutputStream out = response.getOutputStream();
            out.write( result );
            out.flush();

            // log.debug("Result = "+new String(result) );
        } catch( final IOException e ) {
            throw new ServletException("Failed to build RPC result", e);
        }
    }

    /**
     *  Handles HTTP GET.  However, we do not respond to GET requests, other than to show an explanatory text.
     */
    public void doGet( final HttpServletRequest request, final HttpServletResponse response ) throws ServletException {
        log.debug("Received HTTP GET to RPCServlet");

        try {
            final String msg = "We do not support HTTP GET here.  Sorry.";
            response.setContentType( "text/plain" );
            response.setContentLength( msg.length() );

            final PrintWriter writer = new PrintWriter( new OutputStreamWriter( response.getOutputStream() ) );

            writer.println( msg );
            writer.flush();
        } catch( final IOException e ) {
            throw new ServletException("Failed to build RPC result", e);
        }
    }

    private static class LocalHandler implements ContextXmlRpcHandler {
        private Class< ? > m_clazz;

        public LocalHandler( final Class< ? > clazz )
        {
            m_clazz = clazz;
        }

        public Object execute( final String method, final Vector params, final XmlRpcContext context ) throws Exception {
            final WikiRPCHandler rpchandler = (WikiRPCHandler) m_clazz.newInstance();
            rpchandler.initialize( ((WikiXmlRpcContext)context).getWikiContext() );

            final Invoker invoker = new Invoker( rpchandler );
            return invoker.execute( method, params );
        }
    }

    private static class WikiXmlRpcContext implements XmlRpcContext {

        private XmlRpcHandlerMapping m_mapping;
        private WikiContext m_context;

        public WikiXmlRpcContext( final XmlRpcHandlerMapping map, final WikiContext ctx ) {
            m_mapping = map;
            m_context = ctx;
        }

        public XmlRpcHandlerMapping getHandlerMapping()
        {
            return m_mapping;
        }

        public String getPassword() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getUserName() {
            // TODO Auto-generated method stub
            return null;
        }

        public WikiContext getWikiContext()
        {
            return m_context;
        }
    }
}
