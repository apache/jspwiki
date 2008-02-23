/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.rpc.json;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.action.NoneActionBean;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.RPCManager;
import com.ecyrd.jspwiki.ui.TemplateManager;
import com.metaparadigm.jsonrpc.InvocationCallback;
import com.metaparadigm.jsonrpc.JSONRPCBridge;

/**
 *  Provides an easy-to-use interface for different modules to AJAX-enable
 *  themselves.  This class is a static class, so it cannot be instantiated,
 *  but it easily available from anywhere (including JSP pages).
 *  <p>
 *  Any object which wants to expose its methods through JSON calls, needs
 *  to implement the RPCCallable interface.  JSONRPCManager will expose
 *  <i>all</i> methods, so be careful which you want to expose.
 *  <p>
 *  Due to some limitations of the JSON-RPC library, we do not use the
 *  Global bridge object. 
 *  @see com.ecyrd.jspwiki.rpc.RPCCallable
 *  @author Janne Jalkanen
 *  @since 2.5.4
 */
// FIXME: Must be mootool-ified.
public final class JSONRPCManager extends RPCManager
{
    private static final String JSONRPCBRIDGE = "JSONRPCBridge";
    private static HashMap c_globalObjects = new HashMap();
    
    /** Prevent instantiation */
    private JSONRPCManager()
    {
        super();
    }
    
    /**
     *  Emits JavaScript to do a JSON RPC Call.  You would use this method e.g.
     *  in your plugin generation code to embed an AJAX call to your object.
     *  
     *  @param context The Wiki Context
     *  @param c An RPCCallable object
     *  @param function Name of the method to call
     *  @param params Parameters to pass to the method
     *  @return generated JavasSript code snippet that calls the method
     */
    public static String emitJSONCall( WikiContext context, RPCCallable c, String function, String params )
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<script>");
        sb.append("var result = jsonrpc."+getId(c)+"."+function+"("+params+");\r\n");
        sb.append("document.write(result);\r\n");
        sb.append("</script>");
        
        return sb.toString();        
    }
    
    /**
     *  Finds this user's personal RPC Bridge.  If it does not exist, will
     *  create one and put it in the context.  If there is no HTTP Request included,
     *  returns the global bridge.
     *  
     *  @param context WikiContext to find the bridge in
     *  @return A JSON RPC Bridge
     */
    // FIXME: Is returning the global bridge a potential security threat?
    private static JSONRPCBridge getBridge( WikiContext context )
    {
        JSONRPCBridge bridge = null;
        HttpServletRequest req = context.getHttpRequest();
        
        if( req != null )
        {
            HttpSession hs = req.getSession();
            
            if( hs != null )
            {
                bridge = (JSONRPCBridge)hs.getAttribute(JSONRPCBRIDGE);
                
                if( bridge == null )
                {
                    bridge = new JSONRPCBridge();
                
                    hs.setAttribute(JSONRPCBRIDGE, bridge);
                }
            }
        }
        
        if( bridge == null) bridge = JSONRPCBridge.getGlobalBridge();
        bridge.setDebug(false);
        
        return bridge;
    }
    
    /**
     *  Registers a callable to JSON global bridge and requests JSON libraries to be added
     *  to the page.  
     *  
     *  @param context The WikiContext.
     *  @param c The RPCCallable to register
     *  @return the ID of the registered callable object
     */
    public static String registerJSONObject( WikiContext context, RPCCallable c )
    {
        String id = getId(c);
        getBridge(context).registerObject( id, c );

        requestJSON( context );
        return id;
    }
    
    /**
     *  Requests the JSON Javascript and object to be generated in the HTML.
     *  @param context The WikiContext.
     */
    public static void requestJSON( WikiContext context )
    {
        TemplateManager.addResourceRequest(context, 
                                           TemplateManager.RESOURCE_SCRIPT, 
                                           context.getContext().getURL(NoneActionBean.class,"scripts/json-rpc/jsonrpc.js"));        
        
        String jsonurl = context.getContext().getURL( NoneActionBean.class, "JSON-RPC" );
        TemplateManager.addResourceRequest(context, 
                                           TemplateManager.RESOURCE_JSFUNCTION, 
                                           "jsonrpc = new JSONRpcClient(\""+jsonurl+"\");");
        
        getBridge(context).registerCallback(new WikiJSONAccessor(), HttpServletRequest.class);
    }
    
    /**
     *  Provides access control to the JSON calls.  Rather private.
     *  Unfortunately we have to check the permission every single time, because
     *  the user can log in and we would need to reset the permissions at that time.
     *  Note that this is an obvious optimization piece if this becomes
     *  a bottleneck.
     *  
     *  @author Janne Jalkanen
     */
    static class WikiJSONAccessor implements InvocationCallback
    {
        private static final long serialVersionUID = 1L;
        private static final Logger log = Logger.getLogger( WikiJSONAccessor.class );
        
        /**
         *  Create an accessor.
         */
        public WikiJSONAccessor()
        {}
        
        /**
         *  Does not do anything.
         * 
         *  {@inheritDoc}
         */
        public void postInvoke(Object context, Object instance, Method method, Object result) throws Exception
        {
        }

        /**
         *  Checks access against the permission given.
         *  
         *  {@inheritDoc}
         */
        public void preInvoke(Object context, Object instance, Method method, Object[] arguments) throws Exception
        {
            if( context instanceof HttpServletRequest )
            {
                boolean canDo = false;
                HttpServletRequest req = (HttpServletRequest) context;
                
                WikiEngine e = WikiEngine.getInstance( req.getSession().getServletContext(), null );
               
                for( Iterator i = c_globalObjects.values().iterator(); i.hasNext(); )
                {
                    CallbackContainer cc = (CallbackContainer) i.next();
                    
                    if( cc.m_object == instance )
                    {
                        canDo = e.getAuthorizationManager().checkPermission( WikiSession.getWikiSession(e, req), 
                                                                             cc.m_permission );

                        break;
                    }
                }
                                    
                if( canDo )
                {
                    return;
                }
            }

            log.debug("Failed JSON permission check: "+instance);
            throw new WikiSecurityException("No permission to access this AJAX method!");
        }
        
    }

    /**
     *  Registers a global object (i.e. something which can be called by any
     *  JSP page).  Typical examples is e.g. "search".  By default, the RPCCallable
     *  shall need a "view" permission to access.
     *  
     *  @param id     The name under which this shall be registered (e.g. "search")
     *  @param object The RPCCallable which shall be associated to this id.
     */
    public static void registerGlobalObject(String id, RPCCallable object)
    {
        registerGlobalObject(id, object, PagePermission.VIEW);    
    }
    
    /**
     *  Registers a global object (i.e. something which can be called by any
     *  JSP page) with a specific permission.  
     *  
     *  @param id     The name under which this shall be registered (e.g. "search")
     *  @param object The RPCCallable which shall be associated to this id.
     *  @param perm   The permission which is required to access this object.
     */
    public static void registerGlobalObject(String id, RPCCallable object, Permission perm )
    {
        CallbackContainer cc = new CallbackContainer();
        cc.m_permission = perm;
        cc.m_id = id;
        cc.m_object = object;
        
        c_globalObjects.put( id, cc );
    }

    /**
     *  Is called whenever a session is created.  This method creates a new JSONRPCBridge
     *  and adds it to the user session.  This is done because the global JSONRPCBridge
     *  InvocationCallbacks are not called; only session locals.  This may be a bug
     *  in JSON-RPC, or it may be a design feature...
     *  <p>
     *  The JSONRPCBridge object will go away once the session expires.
     *  
     *  @param session The HttpSession which was created.
     */
    public static void sessionCreated( HttpSession session )
    {
        JSONRPCBridge bridge = (JSONRPCBridge)session.getAttribute(JSONRPCBRIDGE);
        
        if( bridge == null )
        {
            bridge = new JSONRPCBridge();
        
            session.setAttribute( JSONRPCBRIDGE, bridge );
        }

        WikiJSONAccessor acc = new WikiJSONAccessor();
        
        bridge.registerCallback( acc, HttpServletRequest.class );
        
        for( Iterator i = c_globalObjects.values().iterator(); i.hasNext(); )
        {
            CallbackContainer cc = (CallbackContainer) i.next();
       
            bridge.registerObject( cc.m_id, cc.m_object );
        }

    }
     
    /**
     *  Just stores the registered global method.
     *  
     *  @author Janne Jalkanen
     *
     */
    private static class CallbackContainer
    {
        String m_id;
        RPCCallable m_object;
        Permission m_permission;
    }
}
