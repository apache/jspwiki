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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.util.TextUtil;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *  Displays the pages referring to the current page.
 *
 *  Parameters:
 *  <ul>
 *  <li><b>max</b> - How many items to show.</li>
 *  <li><b>extras</b> - How to announce extras.</li>
 *  <li><b>page</b> - Which page to get the table of contents from.</li>
 *  </ul>
 *
 *  From AbstractReferralPlugin:
 *  <ul>
 *  <li><b>separator</b> - How to separate generated links; default is a wikitext line break, producing a vertical list.</li>
 *  <li><b>maxwidth</b> - maximum width, in chars, of generated links.</li>
 *  </ul>
 */
public class ReferringPagesPlugin
    extends AbstractReferralPlugin
{
    private static Logger log = Logger.getLogger( ReferringPagesPlugin.class );

    /** Parameter name for setting the maximum items to show.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_MAX      = "max";

    /** Parameter name for setting the text to show when the maximum items is overruled.
     *  Value is <tt>{@value}</tt>.
     */
    public static final String PARAM_EXTRAS   = "extras";

    /**
     *  Parameter name for choosing the page.  Value is <tt>{@value}</tt>.
     */
    public static final String PARAM_PAGE     = "page";

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String, String> params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        String pageName = params.get( PARAM_PAGE );
        ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );

        StringBuilder result = new StringBuilder( 256 );

        if( pageName == null )
        {
            pageName = context.getPage().getName();
        }

        WikiPage page = context.getEngine().getPageManager().getPage( pageName );

        if( page != null )
        {
            Collection< String > links  = refmgr.findReferrers( page.getName() );
            String wikitext = "";

            super.initialize( context, params );

            int items = TextUtil.parseIntParameter( params.get( PARAM_MAX ), ALL_ITEMS );

            String extras = TextUtil.replaceEntities( params.get( PARAM_EXTRAS ) );
            if( extras == null )
            {
                extras = rb.getString("referringpagesplugin.more");
            }

            if( log.isDebugEnabled() ) {
                log.debug( "Fetching referring pages for " + page.getName() + " with a max of "+items);
            }

            if( links != null && links.size() > 0 )
            {
                links = filterAndSortCollection( links );
                wikitext = wikitizeCollection( links, m_separator, items );

                result.append( makeHTML( context, wikitext ) );

                if( items < links.size() && items > 0 )
                {
                    Object[] args = { "" + ( links.size() - items) };
                    extras = MessageFormat.format(extras, args);

                    result.append( "<br />" );
                    result.append( "<a class='morelink' href='"+context.getURL( WikiContext.INFO, page.getName() )+"' ");
                    result.append( ">"+extras+"</a><br />");
                }
            }

            //
            // If nothing was left after filtering or during search
            //
            if (links == null || links.size() == 0)
            {
                wikitext = rb.getString("referringpagesplugin.nobody");

                result.append( makeHTML( context, wikitext ) );
            }
            else
            {
                if( m_show.equals( PARAM_SHOW_VALUE_COUNT ) )
                {
                    result = new StringBuilder();
                    result.append( links.size() );
                    if( m_lastModified )
                    {
                        result.append( " (" + m_dateFormat.format( m_dateLastModified ) + ")" );
                    }
                }
            }

            return result.toString();
        }

        return "";
    }

}
