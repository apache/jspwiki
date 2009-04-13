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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.wiki.ReferenceManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;


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
 *  From AbstractFilteredPlugin:
 *  <ul>
 *  <li><b>separator</b> - How to separate generated links; default is a wikitext line break,
 *             producing a vertical list.</li>
 *  <li><b>maxwidth</b> - maximum width, in chars, of generated links.</li>
 *  </ul>
 */
public class ReferringPagesPlugin
    extends AbstractFilteredPlugin
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
    public String execute( WikiContext context, Map<String,Object> params )
        throws PluginException
    {
        ReferenceManager refmgr = context.getEngine().getReferenceManager();
        String pageName = (String)params.get( PARAM_PAGE );
        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        
        StringBuilder result = new StringBuilder( 256 );
        
        if( pageName == null )
        {
            pageName = context.getPage().getName();
        }

        try
        {
            WikiPage page = context.getEngine().getPage( pageName );
        
            Collection<WikiPath> links = refmgr.findReferrers( page.getWikiPath() );
            String wikitext = "";

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
                // FIXME: Having to copy all of these is kinda stupid.
                Collection<String> tmpList = new ArrayList<String>();
                
                for( WikiPath wn : links ) tmpList.add( wn.toString() );
                
                tmpList= filterCollection( tmpList );
                wikitext = wikitizeCollection( tmpList, m_separator, items );

                result.append( makeHTML( context, wikitext ) );
                
                if( items < tmpList.size() && items > 0 )
                {
                    Object[] args = { "" + ( tmpList.size() - items) };
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
        catch( PageNotFoundException e )
        {} // Fine
        catch( ProviderException e )
        {
            throw new PluginException("Unable to get the latest page",e);
        }
        
        return "";
    }

}
