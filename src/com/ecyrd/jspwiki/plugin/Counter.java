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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import java.util.*;

/**
 *  Provides a page-specific counter.
 *  <P>Parameters
 *  <UL>
 *    <LI>name - Name of the counter.  Optional.
 *  </UL>
 *
 *  Stores a variable in the WikiContext called "counter", with the name of the
 *  optionally attached.  For example:<BR>
 *  If name is "thispage", then the variable name is called "counter-thispage".
 *
 *  @since 1.9.30
 */
public class Counter
    implements WikiPlugin
{
    // private static Logger log = Logger.getLogger( Counter.class );

    static final String VARIABLE_NAME = "counter";

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        //
        //  First, determine which kind of name we use to store in
        //  the WikiContext.
        //
        String  countername = (String)params.get( "name" );

        if( countername == null ) 
        {
            countername = VARIABLE_NAME;
        }
        else
        {
            countername = VARIABLE_NAME+"-"+countername;
        }

        //
        //  Fetch, increment, and store back.
        //
        Integer val = (Integer)context.getVariable( countername );

        if( val == null )
        {
            val = new Integer( 0 );
        }

        val = new Integer( val.intValue() + 1 );

        context.setVariable( countername, val );

        return val.toString();
    }

}
