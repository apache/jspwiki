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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.pages.PageLock;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.preferences.Preferences;

/**
 *  This is a plugin for the administrator: It allows him to see in a single
 *  glance who is editing what.
 *
 *  <p>Parameters : </p>
 *   NONE
 *  @since 2.0.22.
 */
public class ListLocksPlugin
    implements WikiPlugin
{
    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String, String> params )
        throws PluginException
    {
    	StringBuilder result = new StringBuilder();

        PageManager mgr = context.getEngine().getPageManager();
        List<PageLock> locks = mgr.getActiveLocks();
        ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );

        result.append("<table class=\"wikitable\">\n");
        result.append("<tr>\n");
        result.append( "<th>" + rb.getString( "plugin.listlocks.page" ) + "</th><th>" + rb.getString( "plugin.listlocks.locked.by" )
                       + "</th><th>" + rb.getString( "plugin.listlocks.acquired" ) + "</th><th>"
                       + rb.getString( "plugin.listlocks.expires" ) + "</th>\n" );
        result.append("</tr>");

        if( locks.size() == 0 )
        {
            result.append( "<tr><td colspan=\"4\" class=\"odd\">" + rb.getString( "plugin.listlocks.no.locks.exist" )
                           + "</td></tr>\n" );
        }
        else
        {
            int rowNum = 1;
            for( Iterator<PageLock> i = locks.iterator(); i.hasNext(); )
            {
                PageLock lock = i.next();

                result.append( rowNum % 2 != 0 ? "<tr class=\"odd\">" : "<tr>" );
                result.append( "<td>" + lock.getPage() + "</td>" );
                result.append( "<td>" + lock.getLocker() + "</td>" );
                result.append( "<td>" + Preferences.renderDate( context, lock.getAcquisitionTime(), Preferences.TimeFormat.DATETIME ) + "</td>" );
                result.append( "<td>" + Preferences.renderDate( context, lock.getExpiryTime(), Preferences.TimeFormat.DATETIME ) + "</td>" );
                result.append( "</tr>\n" );
                rowNum++;
            }
        }

        result.append("</table>");

        return result.toString();
    }

}
