/*
JSPWiki - a JSP-based WikiWiki clone.

Copyright (C) 2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.QueryItem;
import com.ecyrd.jspwiki.SearchMatcher;
import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.SearchResultComparator;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;

/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @author Arent-Jan Banck for Informatica
 *  @since 2.2.21.
 */
public class BasicSearchProvider implements SearchProvider 
{
    private static final Logger log = Logger.getLogger(BasicSearchProvider.class);

    private WikiEngine m_engine;

    public void initialize(WikiEngine engine, Properties props)
            throws NoRequiredPropertyException, IOException 
    {
        m_engine = engine;
    }

    public void pageRemoved(WikiPage page) {};

    public void reindexPage(WikiPage page) {};

    public  QueryItem[] parseQuery(String query)
    {
        StringTokenizer st = new StringTokenizer( query, " \t," );

        QueryItem[] items = new QueryItem[st.countTokens()];
        int word = 0;

        log.debug("Expecting "+items.length+" items");

        //
        //  Parse incoming search string
        //

        while( st.hasMoreTokens() )
        {
            log.debug("Item "+word);
            String token = st.nextToken().toLowerCase();

            items[word] = new QueryItem();

            switch( token.charAt(0) )
            {
              case '+':
                items[word].type = QueryItem.REQUIRED;
                token = token.substring(1);
                log.debug("Required word: "+token);
                break;

              case '-':
                items[word].type = QueryItem.FORBIDDEN;
                token = token.substring(1);
                log.debug("Forbidden word: "+token);
                break;

              default:
                items[word].type = QueryItem.REQUESTED;
                log.debug("Requested word: "+token);
                break;
            }

            items[word++].word = token;
        }

        return items;
    }

    private String attachmentNames(WikiPage page, String seperator)
    {
    	if(m_engine.getAttachmentManager().hasAttachments(page))
    	{
            Collection attachments;
			try {
				attachments = m_engine.getAttachmentManager().listAttachments(page);
			} catch (ProviderException e) {
				log.error("Unable to get attachments for page", e);
				return "";
			}

            StringBuffer attachmentNames = new StringBuffer();
            for( Iterator it = attachments.iterator(); it.hasNext(); )
            {
                Attachment att = (Attachment) it.next();
                attachmentNames.append(att.getName());
                if(it.hasNext())
                    attachmentNames.append(seperator);
            }
            return attachmentNames.toString();
    	} else {
    	    return "";
    	}
    }
    private Collection findPages( QueryItem[] query )
    {
        TreeSet res = new TreeSet( new SearchResultComparator() );
        SearchMatcher matcher = new SearchMatcher( query );

        Collection allPages = null;
        try
        {
        	allPages = m_engine.getPageManager().getAllPages();
        }
        catch( ProviderException pe )
        {
            log.error( "Unable to retrieve page list", pe );
            return( null );
        }

        Iterator it = allPages.iterator();
        while( it.hasNext() )
        {
            try
            {
                WikiPage page = (WikiPage) it.next();
                if (page != null)
                {
                    String pageName = page.getName();
                    String pageContent = m_engine.getPageManager().getPageText(pageName, WikiPageProvider.LATEST_VERSION) +
                                         attachmentNames(page, " ");
                    SearchResult comparison = matcher.matchPageContent( pageName, pageContent );

                    if( comparison != null )
                    {
                        res.add( comparison );
                    }
                }
            }
            catch( ProviderException pe )
            {
                log.error( "Unable to retrieve page from cache", pe );
            }
            catch( IOException ioe )
            {
                log.error( "Failed to search page", ioe );
            }
        }

        return( res );
    }

    public Collection findPages(String query) 
    {
        return findPages(parseQuery(query));
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "BasicSearchProvider";
    }

}
