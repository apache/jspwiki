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
     *  @param c An RPCCallable
     *  @return An unique id for the callable.
     */
    public static String getId( RPCCallable c )
    {
        return "RPC"+c.hashCode();
    }
    

}
