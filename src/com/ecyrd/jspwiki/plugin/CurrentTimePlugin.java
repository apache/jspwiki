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

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 *  Just displays the current date and time.
 *  The time format is exactly like in the java.text.SimpleDateFormat class.
 *
 *  @since 1.7.8
 *  @see java.text.SimpleDateFormat
 */
public class CurrentTimePlugin
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( CurrentTimePlugin.class );

    public static final String DEFAULT_FORMAT = "HH:mm:ss dd-MMM-yyyy zzzz";

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String formatString = (String)params.get("format");

        if( formatString == null )
        {
            formatString = DEFAULT_FORMAT;
        }

        log.debug("Date format string is: "+formatString);

        try
        {
            SimpleDateFormat fmt = new SimpleDateFormat( formatString );

            Date d = new Date();  // Now.

            return fmt.format( d );
        }
        catch( IllegalArgumentException e )
        {
            ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
            
            throw new PluginException( rb.getString("currenttimeplugin.badformat") + e.getMessage() );
        }
    }

}
