package com.ecyrd.jspwiki.plugin;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.json.JSONRPCManager;

/**
 *  Simple plugin which shows how to add JSON calls to your plugin.
 * 
 *  @author Janne Jalkanen
 *  @since  2.5.4
 */
public class RPCSamplePlugin implements WikiPlugin, RPCCallable
{  
    /**
     *  This method is called when the Javascript is encountered by
     *  the browser.
     *  @param echo
     *  @return the string <code>JSON says:</code>, plus the value 
     *  supplied by the <code>echo</code> parameter
     */
    public String myFunction(String echo)
    {
        return "JSON says: "+echo;
    }
    

    
    public String execute(WikiContext context, Map params) throws PluginException
    {
        JSONRPCManager.registerJSONObject( context, this );
        
        String s = JSONRPCManager.emitJSONCall( context, this, "myFunction", "'foo'" );
        
        return s;
    }

}
