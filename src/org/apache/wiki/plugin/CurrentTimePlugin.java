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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;

/**
 *  Just displays the current date and time.
 *  The time format is exactly like in the java.text.SimpleDateFormat class.
 *  
 *  <p>Parameters : </p>
 *  NONE
 *  @since 1.7.8
 *  @see java.text.SimpleDateFormat
 */
public class CurrentTimePlugin implements WikiPlugin
{
    // private static Logger log = Logger.getLogger( CurrentTimePlugin.class );

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String, String> params )
        throws PluginException
    {
        String formatString = params.get("format");
        
        try
        {
            SimpleDateFormat fmt;
            
            if( formatString != null )
            {
                fmt = new SimpleDateFormat( formatString );
            }
            else
            {
                fmt = Preferences.getDateFormat( context, TimeFormat.DATETIME );
            }

            Date d = new Date();  // Now.

            return fmt.format( d );
        }
        catch( IllegalArgumentException e )
        {
            ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
            
            throw new PluginException( rb.getString("currenttimeplugin.badformat") + e.getMessage() );
        }
    }

}
