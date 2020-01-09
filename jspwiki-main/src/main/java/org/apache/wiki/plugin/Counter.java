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
package org.apache.wiki.plugin;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.util.TextUtil;

import java.util.Map;

/**
 *  Provides a page-specific counter, it is reset every time a page is rendered, so it is not usable as a hitcounter.
 *  <br>Stores a variable in the WikiContext called "counter", with the name of the optionally specified variable "name".  
 *  <br>For example:  If name is "thispage", then the variable name is called "counter-thispage".
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>name</b> - Name of the counter.  Optional.</li>
 *  <li><b>increment</b> - The amount to increment, may be a negative value, default is 1.  Optional.</li>
 *  <li><b>showResult</b> - Should the counter value be visible on the page, default is true.  Optional.</li>
 *  </ul>
 *
 *  @since 1.9.30
 */
public class Counter implements WikiPlugin {
    // private static Logger log = Logger.getLogger( Counter.class );

    /** Parameter name for setting the name.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_NAME          = "name";
    /** Parameter name for setting the increment.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_INCREMENT     = "increment";
    /** Parameter name for setting the showResult.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_SHOW_RESULT   = "showResult";
    /** Parameter name for setting the start.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_START         = "start";
    public static final String  DEFAULT_NAME        = "counter";
    private static final int     DEFAULT_INCREMENT   = 1;
    private static final boolean DEFAULT_SHOW_RESULT = true;

    /**
     *  {@inheritDoc}
     */
    public String execute( final WikiContext context, final Map< String, String > params ) throws PluginException {
        //  First, determine which kind of name we use to store in the WikiContext.
        String  countername = params.get(  PARAM_NAME);

        if( countername == null ) {
            countername = DEFAULT_NAME;
        } else {
            countername = DEFAULT_NAME+"-"+countername;
        }

        //  Fetch the old value
        Integer val = (Integer)context.getVariable( countername );
        if( val == null ) {
            val = 0;
        }
        
        //  Check if we need to reset this
        final String start = params.get( PARAM_START );
        if( start != null ) {
            val = Integer.parseInt( start );
        } else {
            //  Determine how much to increment
            final String incrementObj = params.get( PARAM_INCREMENT );
            int increment = DEFAULT_INCREMENT;
            if( incrementObj != null ) {
                increment = Integer.parseInt( incrementObj );
            }

            val = val + increment;
        }
        
        context.setVariable( countername, val );

        // check if we want to hide the result (just count, don't show result on the page
        final String showObj = params.get(PARAM_SHOW_RESULT);
        boolean show = DEFAULT_SHOW_RESULT;
        if( showObj != null ) {
            show = TextUtil.isPositive( showObj );
        }
        
        if( show ) {
            return val.toString();
        } 
       
        return "";
    }

}
