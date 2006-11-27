package com.ecyrd.jspwiki.rpc.json;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;
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
     *  Emits Javascript to do a JSON RPC Call.  
     *  
     *  @param c
     *  @param function Name of the method to call
     *  @param params Parameters to pass to the method
     *  @return
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
     *  Registers a callable to JSON global bridge and requests JSON libraries to be added.
     *  @param context
     *  @param c
     *  @return
     */
    public static String registerJSONObject( WikiContext context, RPCCallable c )
    {
        String id = getId(c);
        JSONRPCBridge.getGlobalBridge().registerObject( id, c );

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
        
        JSONRPCBridge.getGlobalBridge().registerCallback(new WikiJSONAccessor(), HttpServletRequest.class);
    }
    
    // FIXME: Does not work yet
    public static class WikiJSONAccessor implements InvocationCallback
    {

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
                                                                             PagePermission.VIEW );
                
                if( canDo )
                {
                    return;
                }
            }
            
            throw new WikiSecurityException("Permission to call this method not given");
        }
        
    }
}
