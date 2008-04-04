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
 *    <LI>increment - The amount to increment, may be a negative value, default is 1.  Optional.
 *    <LI>showResult - Should the counter value be visible on the page, default is true.  Optional.
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

    private static final String  PARAM_NAME          = "name";
    private static final String  PARAM_INCREMENT     = "increment";
    private static final String  PARAM_SHOW_RESULT   = "showResult";
    private static final String  PARAM_START         = "start";
    private static final String  DEFAULT_NAME        = "counter";
    private static final int     DEFAULT_INCREMENT   = 1;
    private static final boolean DEFAULT_SHOW_RESULT = true;

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        //
        //  First, determine which kind of name we use to store in
        //  the WikiContext.
        //
        String  countername = (String)params.get(  PARAM_NAME);

        if( countername == null ) 
        {
            countername = DEFAULT_NAME;
        }
        else
        {
            countername = DEFAULT_NAME+"-"+countername;
        }

        //
        //  Fetch the old value
        //
        Integer val = (Integer)context.getVariable( countername );

        if( val == null )
        {
            val = new Integer( 0 );
        }
        
        //
        //  Check if we need to reset this
        //
        
        String start = (String)params.get( PARAM_START );
        
        if( start != null )
        {
            val = Integer.parseInt( start );
        }
        else
        {
            //
            //  Determine how much to increment
            //
            Object incrementObj = params.get( PARAM_INCREMENT );
        
            int increment = DEFAULT_INCREMENT;
        
            if (incrementObj != null) 
            {
                increment = (new Integer((String)incrementObj)).intValue();
            }

            val = new Integer( val.intValue() + increment );
        }
        
        context.setVariable( countername, val );

        //
        // check if we want to hide the result (just count, don't show result on the page
        //
        Object showObj = params.get(PARAM_SHOW_RESULT);
        
        boolean show = DEFAULT_SHOW_RESULT;
        
        if( showObj != null ) 
        {
            show = TextUtil.isPositive( (String) showObj );
        }
        
        if( show )
        {
            return val.toString();
        } 
       
        return "";
    }

}
