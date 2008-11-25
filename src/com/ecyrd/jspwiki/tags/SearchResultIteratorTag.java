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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Iterates through Search result results.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>max = how many search results should be shown.
 *  </UL>
 *
 *  @since 2.0
 */

// FIXME: Shares MUCH too much in common with IteratorTag.  Must refactor.
public class SearchResultIteratorTag
    extends IteratorTag
{
    private static final long serialVersionUID = 0L;
    
    private   int         m_maxItems;
    private   int         m_count = 0;
    private   int         m_start = 0;
    
    static Logger log = Logger.getLogger(SearchResultIteratorTag.class);
    
    public void release()
    {
        super.release();
        m_maxItems = m_count = 0;
    }

    public void setMaxItems( int arg )
    {
        m_maxItems = arg;
    }

    public void setStart( int arg )
    {
        m_start = arg;
    }
    
    public final int doStartTag()
    {
        //
        //  Do lazy eval if the search results have not been set.
        //
        if( m_iterator == null )
        {
            Collection searchresults = (Collection) pageContext.getAttribute( "searchresults",
                                                                              PageContext.REQUEST_SCOPE );
            setList( searchresults );
            
            int skip = 0;
            
            //  Skip the first few ones...
            m_iterator = searchresults.iterator();
            while( m_iterator.hasNext() && (skip++ < m_start) ) m_iterator.next();
        }

        m_count       = 0;
        m_wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT,
                                                                PageContext.REQUEST_SCOPE );

        return nextResult();
    }

    private int nextResult()
    {
        if( m_iterator != null && m_iterator.hasNext() && m_count++ < m_maxItems )
        {
            SearchResult r = (SearchResult) m_iterator.next();
            
            // Create a wiki context for the result
            WikiEngine engine = m_wikiContext.getEngine();
            WikiContext context = engine.getWikiContextFactory().newViewContext( null, null, r.getPage() );
            
            // Stash it in the page context
            pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                      context,
                                      PageContext.REQUEST_SCOPE );
            pageContext.setAttribute( getId(), r );

            return EVAL_BODY_BUFFERED;
        }

        return SKIP_BODY;
    }

    public int doAfterBody()
    {
        if( bodyContent != null )
        {
            try
            {
                JspWriter out = getPreviousOut();
                out.print(bodyContent.getString());
                bodyContent.clearBody();
            }
            catch( IOException e )
            {
                log.error("Unable to get inner tag text", e);
                // FIXME: throw something?
            }
        }

        return nextResult();
    }

    public int doEndTag()
    {
        m_iterator = null;

        return super.doEndTag();
    }
}
