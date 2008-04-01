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

import java.text.MessageFormat;
import java.util.*;

/**
 *  Displays the pages referring to the current page.
 *
 *  Parameters: <BR>
 *  max: How many items to show.<BR>
 *  extras: How to announce extras.<BR>
 *  From AbstractReferralPlugin:<BR>
 *  separator: How to separate generated links; default is a wikitext line break,
 *             producing a vertical list.<BR>
 *  maxwidth: maximum width, in chars, of generated links.
 *
 */
public class ReferringPagesPlugin
    extends AbstractReferralPlugin
{
    private static Logger log = Logger.getLogger( ReferringPagesPlugin.class );

    public static final String PARAM_MAX      = "max";
    public static final String PARAM_EXTRAS   = "extras";
    public static final String PARAM_PAGE     = "page";
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        String pageName = (String)params.get( PARAM_PAGE );
        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        
        if( pageName == null )
        {
            pageName = context.getPage().getName();
        }

        WikiPage page = context.getEngine().getPage( pageName );
        
        if( page != null )
        {
            Collection   links  = refmgr.findReferrers( page.getName() );
            String       wikitext = "";

            super.initialize( context, params );

            int items = TextUtil.parseIntParameter( (String)params.get( PARAM_MAX ), ALL_ITEMS );
            String extras = (String)params.get( PARAM_EXTRAS );
            if( extras == null )
            {
                extras = rb.getString("referringpagesplugin.more");
            }
            
            log.debug( "Fetching referring pages for "+page.getName()+
                       " with a max of "+items);
        
            if( links != null && links.size() > 0 )
            {
                links = filterCollection( links );
                wikitext = wikitizeCollection( links, m_separator, items );

                if( items < links.size() && items > 0 )
                {
                    Object[] args = { "" + ( links.size() - items),
                                      context.getURL( WikiContext.INFO, page.getName() ) };
                    extras = MessageFormat.format(extras, args); 
                    wikitext += extras;
                }
            }

            //
            //  If nothing was left after filtering or during search
            //
            if( links == null || links.size() == 0 )
            {
                wikitext = rb.getString("referringpagesplugin.nobody");
            }

            return makeHTML( context, wikitext );
        }

        return "";
    }

}
