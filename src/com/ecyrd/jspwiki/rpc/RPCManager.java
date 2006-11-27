package com.ecyrd.jspwiki.rpc;

/**
 *  A base class for managing RPC calls.
 *  
 *  @author jalkanen
 *  @since 2.5.4
 */
public class RPCManager
{
    /**
     *  Gets an unique RPC ID for a callable object.  This is required because a plugin
     *  does not know how many times it is already been invoked.
     *  <p>
     *  The id returned contains only upper and lower ASCII characters and digits, and
     *  it always starts with an ASCII character.  Therefore the id is suitable as a
     *  programming language construct directly (e.g. object name).
     *  
     *  @param c
     *  @return An unique id for the callable.
     */
    public static String getId( RPCCallable c )
    {
        return "RPC"+c.hashCode();
    }
    

}
