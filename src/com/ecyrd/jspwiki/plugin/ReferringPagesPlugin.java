/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
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
 *  @author Janne Jalkanen
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
        
        if( pageName == null )
        {
            pageName = context.getPage().getName();
        }

        WikiPage page = context.getEngine().getPage( pageName );
        
        if( page != null )
        {
            Collection   links  = refmgr.findReferrers( page.getName() );
            String       wikitext;

            super.initialize( context, params );

            int items = TextUtil.parseIntParameter( (String)params.get( PARAM_MAX ), ALL_ITEMS );
            String extras = (String)params.get( PARAM_EXTRAS );
            if( extras == null )
            {
                extras = "...and %d more\\\\";
            }
            
            log.debug( "Fetching referring pages for "+page.getName()+
                       " with a max of "+items);
        
            if( links != null && links.size() > 0 )
            {
                links = filterCollection( links );
                wikitext = wikitizeCollection( links, m_separator, items );

                if( items < links.size() && items > 0 )
                {
                    extras = TextUtil.replaceString( extras, "%d", 
                                                     ""+(links.size()-items) );
                    wikitext += extras;
                }
            }
            else
            {
                wikitext = "...nobody";
            }

            return makeHTML( context, wikitext );
        }

        return "";
    }

}
