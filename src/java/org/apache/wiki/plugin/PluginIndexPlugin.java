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
package org.apache.wiki.plugin;

import java.util.Collection;
import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.ModuleData;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.plugin.PluginManager.WikiPluginInfo;


/**
 * <p>
 * Displays which plugins are available in this wiki. Also shows all metadata
 * available for each plugin in an HTML table
 * <p>
 * Parameters :
 * </p>
 * <ul>
 * <li><b>details</b> - value can be true or false, default is false, in which
 * case only the name of the plugin is listed. If true, then all available meta
 * data of the plugin is shown</li>
 * </ul>
 * 
 * @since 3.0
 * @author Harry Metske
 */
@ModuleData( author = "Harry Metske", minVersion = "3.0", maxVersion = "1000000", minAPIVersion = "2.8" )
public class PluginIndexPlugin extends AbstractFilteredPlugin implements WikiPlugin
{
    /** Parameter name for the details parameter. Value is <tt>{@value}</tt>. */
    public static final String PARAM_DETAILS = "details";

    private static final String BLANK = "&nbsp;";

    /**
     * {@inheritDoc}
     */
    public String execute( WikiContext context, Map params ) throws PluginException
    {
        super.initialize( context, params );

        String details = (String) params.get( PARAM_DETAILS );

        String summaryHeader = "\n||name";
        String detailHeader = "\n||Name||Class Name||alias's||author||minVersion||maxVersion||adminBean Class";

        Collection<WikiPluginInfo> plugins = context.getEngine().getPluginManager().modules();

        StringBuilder wikitext = new StringBuilder();

        if( "true".equals( details ) )
        {
            wikitext.append( detailHeader );
            for( WikiPluginInfo pluginInfo : plugins )
            {
                String name = pluginInfo.getName();
                String clazz = pluginInfo.getClassName();
                String[] aliass = pluginInfo.getAliases();
                String author = pluginInfo.getAuthor();
                if( author == null )
                    author = BLANK;
                String minVersion = pluginInfo.getMinVersion();
                if( minVersion == null )
                    minVersion = BLANK;
                String maxVersion = pluginInfo.getMaxVersion();
                if( maxVersion == null )
                    maxVersion = BLANK;
                String adminBeanClazz = pluginInfo.getAdminBeanClass();
                if( adminBeanClazz == null )
                    adminBeanClazz = BLANK;
                StringBuilder aliassString = new StringBuilder( BLANK );

                if( aliass != null )
                {
                    for( int i = 0; i < aliass.length; i++ )
                    {
                        aliassString.append( " " + aliass[i] );
                    }
                }

                wikitext.append( "\n|" + name + "|" + clazz + "|" + aliassString + "|" + author + "|" + minVersion + "|"
                                 + maxVersion + "|" + adminBeanClazz );
            }
        }
        else
        {
            wikitext.append( summaryHeader );
            for( WikiPluginInfo pluginInfo : plugins )
            {
                String name = pluginInfo.getName();
                wikitext.append( "\n|" + name );
            }
        }
        return makeHTML( context, wikitext.toString() );
    }
}
