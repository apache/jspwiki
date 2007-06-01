package com.ecyrd.jspwiki.rpc.json;

import java.lang.reflect.Method;
import java.security.Permission;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.RPCManager;
import com.ecyrd.jspwiki.ui.TemplateManager;
import com.metaparadigm.jsonrpc.InvocationCallback;
import com.metaparadigm.jsonrpc.JSONRPCBridge;

/**
 *  Provides JSON management methods.
 *  
 *  @author jalkanen
 *  @since 2.5.4
 */
// FIXME: Does not do authentication/authorization properly yet
public class JSONRPCManager extends RPCManager
{
    /**
     *  Emits JavaScript to do a JSON RPC Call.  
     *  
     *  @param c
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
                bridge = (JSONRPCBridge)hs.getAttribute("JSONRPCBridge");
                
                if( bridge == null )
                {
                    bridge = new JSONRPCBridge();
                
                    hs.setAttribute("JSONRPCBridge", new JSONRPCBridge());
                }
            }
        }
        
        if( bridge == null) bridge = JSONRPCBridge.getGlobalBridge();
        bridge.setDebug(true);
        
        return bridge;
    }
    
    /**
     *  Registers a callable to JSON global bridge and requests JSON libraries to be added.
     *  @param context
     *  @param c
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
     *  @param context
     */
    public static void requestJSON( WikiContext context )
    {
        TemplateManager.addResourceRequest(context, 
                                           TemplateManager.RESOURCE_SCRIPT, 
                                           context.getURL(WikiContext.NONE,"scripts/json-rpc/jsonrpc.js"));        
        
        String jsonurl = context.getURL( WikiContext.NONE, "JSON-RPC" );
        TemplateManager.addResourceRequest(context, 
                                           TemplateManager.RESOURCE_JSFUNCTION, 
                                           "jsonrpc = new JSONRpcClient(\""+jsonurl+"\");");
        
        getBridge(context).registerCallback(new WikiJSONAccessor(), HttpServletRequest.class);
    }
    
    // FIXME: Does not work yet
    public static class WikiJSONAccessor implements InvocationCallback
    {
        private static final long serialVersionUID = 1L;
        private Permission m_permission = PagePermission.VIEW;
        
        public WikiJSONAccessor()
        {}
        
        public WikiJSONAccessor( Permission perm )
        {
            m_permission = perm;
        }
        
        public void postInvoke(Object context, Object instance, Method method, Object result) throws Exception
        {
        }

        public void preInvoke(Object context, Object instance, Method method, Object[] arguments) throws Exception
        {
            if( context instanceof HttpServletRequest )
            {
                HttpServletRequest req = (HttpServletRequest) context;
                
                WikiEngine e = WikiEngine.getInstance( req.getSession().getServletContext(), null );
                
                boolean canDo = e.getAuthorizationManager().checkPermission( WikiSession.getWikiSession(e, req), 
                                                                             m_permission );
                
                if( canDo )
                {
                    return;
                }
            }
            
            throw new WikiSecurityException("Permission to call this method not given");
        }
        
    }

    public static void registerGlobalObject(String id, RPCCallable object)
    {
        JSONRPCBridge bridge = JSONRPCBridge.getGlobalBridge();
        
        bridge.registerObject( id, object );
        bridge.registerCallback( new WikiJSONAccessor(), HttpServletRequest.class );
    }

    public static void registerGlobalObject(String id, RPCCallable object, Permission perm )
    {
        JSONRPCBridge bridge = JSONRPCBridge.getGlobalBridge();
        
        bridge.registerObject( id, object );
        bridge.registerCallback( new WikiJSONAccessor(perm), HttpServletRequest.class );
    }

}
