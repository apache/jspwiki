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

import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.*;

import java.text.MessageFormat;
import java.util.*;

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
 *  <li><b>separator</b> - How to separate generated links; default is a wikitext line break,
 *             producing a vertical list.</li>
 *  <li><b>maxwidth</b> - maximum width, in chars, of generated links.</li>
 *  </ul>
 */
public class ReferringPagesPlugin
    extends AbstractReferralPlugin
{
    private static Logger log = LoggerFactory.getLogger( ReferringPagesPlugin.class );

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
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        String pageName = (String)params.get( PARAM_PAGE );
        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        
        StringBuffer result = new StringBuffer( 256 );
        
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
            
            if( log.isDebugEnabled() )
                log.debug( "Fetching referring pages for "+page.getName()+
                           " with a max of "+items);
        
            if( links != null && links.size() > 0 )
            {
                links = filterCollection( links );
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
                    result = new StringBuffer();
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
